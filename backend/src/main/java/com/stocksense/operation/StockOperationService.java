package com.stocksense.operation;

import com.stocksense.common.exception.ConflictException;
import com.stocksense.common.exception.ResourceNotFoundException;
import com.stocksense.inventory.StockBalance;
import com.stocksense.inventory.StockBalanceRepository;
import com.stocksense.ledger.StockLedgerEntry;
import com.stocksense.ledger.StockLedgerRepository;
import com.stocksense.operation.dto.CreateAdjustmentRequest;
import com.stocksense.operation.dto.CreateDeliveryRequest;
import com.stocksense.operation.dto.CreateReceiptRequest;
import com.stocksense.operation.dto.StockOperationResponse;
import com.stocksense.product.Product;
import com.stocksense.product.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StockOperationService {

    private final ProductRepository productRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final StockOperationRepository stockOperationRepository;
    private final StockLedgerRepository stockLedgerRepository;

    public StockOperationService(ProductRepository productRepository,
                                 StockBalanceRepository stockBalanceRepository,
                                 StockOperationRepository stockOperationRepository,
                                 StockLedgerRepository stockLedgerRepository) {
        this.productRepository = productRepository;
        this.stockBalanceRepository = stockBalanceRepository;
        this.stockOperationRepository = stockOperationRepository;
        this.stockLedgerRepository = stockLedgerRepository;
    }

    @Transactional
    public StockOperationResponse createReceipt(CreateReceiptRequest request) {
        Product product = findProductOrThrow(request.getProductId());

        BigDecimal quantity = request.getQuantity();
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Receipt quantity must be strictly greater than zero");
        }

        // Acquire pessimistic write lock within transaction
        StockBalance balance = getOrCreateLockedBalance(product);

        BigDecimal previousQuantity = balance.getQuantity();
        BigDecimal quantityChange = quantity;
        BigDecimal newBalance = previousQuantity.add(quantity);
        balance.setQuantity(newBalance);
        stockBalanceRepository.save(balance);

        StockOperation operation = new StockOperation(
                OperationType.RECEIPT,
                product,
                quantity,
                quantityChange,
                request.getReference(),
                request.getNotes()
        );
        StockOperation savedOperation = stockOperationRepository.save(operation);

        // Atomic creation of immutable ledger entry in the same transaction
        StockLedgerEntry ledgerEntry = new StockLedgerEntry(
                savedOperation,
                product,
                OperationType.RECEIPT,
                quantityChange,
                previousQuantity,
                newBalance
        );
        stockLedgerRepository.save(ledgerEntry);

        return StockOperationResponse.fromEntity(savedOperation, newBalance);
    }

    @Transactional
    public StockOperationResponse createDelivery(CreateDeliveryRequest request) {
        Product product = findProductOrThrow(request.getProductId());

        BigDecimal quantity = request.getQuantity();
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Delivery quantity must be strictly greater than zero");
        }

        // Acquire pessimistic write lock before evaluating stock availability
        StockBalance balance = getOrCreateLockedBalance(product);
        BigDecimal previousQuantity = balance.getQuantity();

        if (previousQuantity.compareTo(quantity) < 0) {
            throw new ConflictException(String.format(
                    "Insufficient stock for product '%s' (SKU: %s). Requested: %s %s, Available: %s %s.",
                    product.getName(), product.getSku(), quantity, product.getUnit(),
                    previousQuantity, product.getUnit()
            ));
        }

        BigDecimal quantityChange = quantity.negate();
        BigDecimal newBalance = previousQuantity.subtract(quantity);
        balance.setQuantity(newBalance);
        stockBalanceRepository.save(balance);

        StockOperation operation = new StockOperation(
                OperationType.DELIVERY,
                product,
                quantity,
                quantityChange,
                request.getReference(),
                request.getNotes()
        );
        StockOperation savedOperation = stockOperationRepository.save(operation);

        // Atomic creation of immutable ledger entry in the same transaction
        StockLedgerEntry ledgerEntry = new StockLedgerEntry(
                savedOperation,
                product,
                OperationType.DELIVERY,
                quantityChange,
                previousQuantity,
                newBalance
        );
        stockLedgerRepository.save(ledgerEntry);

        return StockOperationResponse.fromEntity(savedOperation, newBalance);
    }

    @Transactional
    public StockOperationResponse createAdjustment(CreateAdjustmentRequest request) {
        Product product = findProductOrThrow(request.getProductId());

        BigDecimal countedQuantity = request.getCountedQuantity();
        if (countedQuantity == null || countedQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Counted quantity cannot be negative");
        }

        // Acquire pessimistic write lock within transaction
        StockBalance balance = getOrCreateLockedBalance(product);

        BigDecimal previousQuantity = balance.getQuantity();
        BigDecimal quantityChange = countedQuantity.subtract(previousQuantity);
        balance.setQuantity(countedQuantity);
        stockBalanceRepository.save(balance);

        StockOperation operation = new StockOperation(
                OperationType.ADJUSTMENT,
                product,
                countedQuantity,
                quantityChange,
                request.getReference(),
                request.getNotes()
        );
        StockOperation savedOperation = stockOperationRepository.save(operation);

        // Atomic creation of immutable ledger entry in the same transaction
        StockLedgerEntry ledgerEntry = new StockLedgerEntry(
                savedOperation,
                product,
                OperationType.ADJUSTMENT,
                quantityChange,
                previousQuantity,
                countedQuantity
        );
        stockLedgerRepository.save(ledgerEntry);

        return StockOperationResponse.fromEntity(savedOperation, countedQuantity);
    }

    @Transactional(readOnly = true)
    public StockOperationResponse getOperationById(Long id) {
        StockOperation op = stockOperationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StockOperation", "id", id));

        BigDecimal currentBalance = stockBalanceRepository.findByProductId(op.getProduct().getId())
                .map(StockBalance::getQuantity)
                .orElse(BigDecimal.ZERO);

        return StockOperationResponse.fromEntity(op, currentBalance);
    }

    @Transactional(readOnly = true)
    public List<StockOperationResponse> getOperations(Long productId, OperationType operationType, int page, int size) {
        if (productId != null && !productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", "id", productId);
        }

        int validatedPage = Math.max(page, 0);
        int validatedSize = (size <= 0 || size > 100) ? 20 : size;
        Pageable pageable = PageRequest.of(validatedPage, validatedSize);

        Page<StockOperation> pageResult = stockOperationRepository.findFiltered(productId, operationType, pageable);

        // Pre-fetch balances to avoid N+1 queries
        List<Long> productIds = pageResult.getContent().stream()
                .map(op -> op.getProduct().getId())
                .distinct()
                .toList();

        Map<Long, BigDecimal> balanceMap = stockBalanceRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(
                        b -> b.getProduct().getId(),
                        StockBalance::getQuantity,
                        (existing, replacement) -> existing
                ));

        return pageResult.getContent().stream()
                .map(op -> {
                    BigDecimal bal = balanceMap.getOrDefault(op.getProduct().getId(), BigDecimal.ZERO);
                    return StockOperationResponse.fromEntity(op, bal);
                })
                .collect(Collectors.toList());
    }

    private Product findProductOrThrow(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID cannot be null");
        }
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
    }

    private StockBalance getOrCreateLockedBalance(Product product) {
        return stockBalanceRepository.findWithLockByProductId(product.getId())
                .orElseGet(() -> {
                    StockBalance newBalance = new StockBalance(product, BigDecimal.ZERO);
                    return stockBalanceRepository.save(newBalance);
                });
    }
}
