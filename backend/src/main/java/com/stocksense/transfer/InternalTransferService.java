package com.stocksense.transfer;

import com.stocksense.common.exception.ConflictException;
import com.stocksense.common.exception.DuplicateResourceException;
import com.stocksense.common.exception.ResourceNotFoundException;
import com.stocksense.ledger.StockLedgerEntry;
import com.stocksense.ledger.StockLedgerRepository;
import com.stocksense.operation.OperationType;
import com.stocksense.operation.StockOperation;
import com.stocksense.operation.StockOperationRepository;
import com.stocksense.product.Product;
import com.stocksense.product.ProductRepository;
import com.stocksense.transfer.dto.CreateTransferRequest;
import com.stocksense.transfer.dto.TransferResponse;
import com.stocksense.warehouse.Location;
import com.stocksense.warehouse.LocationRepository;
import com.stocksense.warehouse.LocationStockBalance;
import com.stocksense.warehouse.LocationStockBalanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InternalTransferService {

    private final InternalTransferRepository internalTransferRepository;
    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;
    private final LocationStockBalanceRepository locationStockBalanceRepository;
    private final StockOperationRepository stockOperationRepository;
    private final StockLedgerRepository stockLedgerRepository;

    public InternalTransferService(InternalTransferRepository internalTransferRepository,
                                   ProductRepository productRepository,
                                   LocationRepository locationRepository,
                                   LocationStockBalanceRepository locationStockBalanceRepository,
                                   StockOperationRepository stockOperationRepository,
                                   StockLedgerRepository stockLedgerRepository) {
        this.internalTransferRepository = internalTransferRepository;
        this.productRepository = productRepository;
        this.locationRepository = locationRepository;
        this.locationStockBalanceRepository = locationStockBalanceRepository;
        this.stockOperationRepository = stockOperationRepository;
        this.stockLedgerRepository = stockLedgerRepository;
    }

    @Transactional(readOnly = true)
    public List<TransferResponse> getAllTransfers(TransferStatus status, Long productId, Long locationId) {
        return internalTransferRepository.searchTransfers(status, productId, locationId).stream()
                .map(TransferResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TransferResponse getTransferById(Long id) {
        InternalTransfer transfer = internalTransferRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("InternalTransfer", "id", id));
        return TransferResponse.fromEntity(transfer);
    }

    @Transactional
    public TransferResponse createTransfer(CreateTransferRequest request) {
        if (request.getSourceLocationId().equals(request.getDestinationLocationId())) {
            throw new IllegalArgumentException("Source location and destination location must differ");
        }

        BigDecimal quantity = request.getQuantity();
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer quantity must be strictly greater than zero");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

        Location sourceLocation = locationRepository.findById(request.getSourceLocationId())
                .orElseThrow(() -> new ResourceNotFoundException("Location", "id (source)", request.getSourceLocationId()));

        Location destinationLocation = locationRepository.findById(request.getDestinationLocationId())
                .orElseThrow(() -> new ResourceNotFoundException("Location", "id (destination)", request.getDestinationLocationId()));

        String reference = request.getReference();
        if (reference == null || reference.trim().isEmpty()) {
            reference = "TR-" + (System.currentTimeMillis() % 1000000);
        } else {
            reference = reference.trim();
            if (internalTransferRepository.existsByReference(reference)) {
                throw new DuplicateResourceException("InternalTransfer", "reference", reference);
            }
        }

        TransferStatus initialStatus = request.getStatus() != null ? request.getStatus() : TransferStatus.READY;

        InternalTransfer transfer = new InternalTransfer(
                reference,
                product,
                sourceLocation,
                destinationLocation,
                quantity,
                initialStatus,
                request.getNotes()
        );
        InternalTransfer saved = internalTransferRepository.save(transfer);

        if (request.isAutoExecute() || initialStatus == TransferStatus.DONE) {
            saved.setStatus(TransferStatus.READY); // reset so executeTransfer can transition to DONE
            return executeTransfer(saved.getId());
        }

        return TransferResponse.fromEntity(saved);
    }

    @Transactional
    public TransferResponse executeTransfer(Long transferId) {
        InternalTransfer transfer = internalTransferRepository.findWithDetailsById(transferId)
                .orElseThrow(() -> new ResourceNotFoundException("InternalTransfer", "id", transferId));

        if (transfer.getStatus() == TransferStatus.DONE) {
            throw new ConflictException(String.format("Transfer '%s' is already completed.", transfer.getReference()));
        }

        if (transfer.getStatus() == TransferStatus.CANCELED) {
            throw new ConflictException(String.format("Cannot execute transfer '%s' because it is canceled.", transfer.getReference()));
        }

        Product product = transfer.getProduct();
        Location sourceLoc = transfer.getSourceLocation();
        Location destLoc = transfer.getDestinationLocation();
        BigDecimal transferQty = transfer.getQuantity();

        // 1. Lock and fetch source location stock balance
        LocationStockBalance sourceBalance = locationStockBalanceRepository
                .findWithLockByProductIdAndLocationId(product.getId(), sourceLoc.getId())
                .orElseGet(() -> {
                    LocationStockBalance newBal = new LocationStockBalance(product, sourceLoc, BigDecimal.ZERO);
                    return locationStockBalanceRepository.save(newBal);
                });

        BigDecimal prevSourceQty = sourceBalance.getQuantity();
        if (prevSourceQty.compareTo(transferQty) < 0) {
            throw new ConflictException(String.format(
                    "Insufficient stock at source location '%s' (%s) for product '%s'. Requested: %s %s, Available: %s %s.",
                    sourceLoc.getName(), sourceLoc.getCode(), product.getName(),
                    transferQty, product.getUnit(), prevSourceQty, product.getUnit()
            ));
        }

        // 2. Lock and fetch destination location stock balance
        LocationStockBalance destBalance = locationStockBalanceRepository
                .findWithLockByProductIdAndLocationId(product.getId(), destLoc.getId())
                .orElseGet(() -> {
                    LocationStockBalance newBal = new LocationStockBalance(product, destLoc, BigDecimal.ZERO);
                    return locationStockBalanceRepository.save(newBal);
                });

        BigDecimal prevDestQty = destBalance.getQuantity();

        // 3. Update stock balances atomically
        BigDecimal newSourceQty = prevSourceQty.subtract(transferQty);
        BigDecimal newDestQty = prevDestQty.add(transferQty);

        sourceBalance.setQuantity(newSourceQty);
        destBalance.setQuantity(newDestQty);
        locationStockBalanceRepository.save(sourceBalance);
        locationStockBalanceRepository.save(destBalance);

        // 4. Record StockOperations & Ledger Entries: Outflow from Source and Inflow into Destination
        StockOperation sourceOp = new StockOperation();
        sourceOp.setOperationType(OperationType.TRANSFER);
        sourceOp.setProduct(product);
        sourceOp.setLocation(sourceLoc);
        sourceOp.setSourceLocation(sourceLoc);
        sourceOp.setDestinationLocation(destLoc);
        sourceOp.setQuantity(transferQty);
        sourceOp.setQuantityChange(transferQty.negate());
        sourceOp.setReference(transfer.getReference());
        sourceOp.setNotes(transfer.getNotes());
        StockOperation savedSourceOp = stockOperationRepository.save(sourceOp);

        StockLedgerEntry sourceLedger = new StockLedgerEntry();
        sourceLedger.setOperation(savedSourceOp);
        sourceLedger.setProduct(product);
        sourceLedger.setOperationType(OperationType.TRANSFER);
        sourceLedger.setLocation(sourceLoc);
        sourceLedger.setWarehouse(sourceLoc.getWarehouse());
        sourceLedger.setSourceLocation(sourceLoc);
        sourceLedger.setDestinationLocation(destLoc);
        sourceLedger.setPreviousQuantity(prevSourceQty);
        sourceLedger.setQuantityChange(transferQty.negate());
        sourceLedger.setResultingQuantity(newSourceQty);
        stockLedgerRepository.save(sourceLedger);

        StockOperation destOp = new StockOperation();
        destOp.setOperationType(OperationType.TRANSFER);
        destOp.setProduct(product);
        destOp.setLocation(destLoc);
        destOp.setSourceLocation(sourceLoc);
        destOp.setDestinationLocation(destLoc);
        destOp.setQuantity(transferQty);
        destOp.setQuantityChange(transferQty);
        destOp.setReference(transfer.getReference());
        destOp.setNotes(transfer.getNotes());
        StockOperation savedDestOp = stockOperationRepository.save(destOp);

        StockLedgerEntry destLedger = new StockLedgerEntry();
        destLedger.setOperation(savedDestOp);
        destLedger.setProduct(product);
        destLedger.setOperationType(OperationType.TRANSFER);
        destLedger.setLocation(destLoc);
        destLedger.setWarehouse(destLoc.getWarehouse());
        destLedger.setSourceLocation(sourceLoc);
        destLedger.setDestinationLocation(destLoc);
        destLedger.setPreviousQuantity(prevDestQty);
        destLedger.setQuantityChange(transferQty);
        destLedger.setResultingQuantity(newDestQty);
        stockLedgerRepository.save(destLedger);

        // 6. Complete transfer
        LocalDateTime now = LocalDateTime.now();
        transfer.setStatus(TransferStatus.DONE);
        transfer.setCompletedAt(now);
        InternalTransfer updated = internalTransferRepository.save(transfer);

        return TransferResponse.fromEntity(updated);
    }

    @Transactional
    public TransferResponse cancelTransfer(Long transferId) {
        InternalTransfer transfer = internalTransferRepository.findWithDetailsById(transferId)
                .orElseThrow(() -> new ResourceNotFoundException("InternalTransfer", "id", transferId));

        if (transfer.getStatus() == TransferStatus.DONE) {
            throw new ConflictException(String.format("Cannot cancel transfer '%s' because it is already completed.", transfer.getReference()));
        }

        transfer.setStatus(TransferStatus.CANCELED);
        InternalTransfer updated = internalTransferRepository.save(transfer);
        return TransferResponse.fromEntity(updated);
    }
}
