package com.stocksense.order;

import com.stocksense.common.exception.ConflictException;
import com.stocksense.common.exception.ResourceNotFoundException;
import com.stocksense.operation.StockOperationService;
import com.stocksense.operation.dto.CreateReceiptRequest;
import com.stocksense.order.dto.CreateReceiptOrderRequest;
import com.stocksense.order.dto.ReceiptOrderLineDto;
import com.stocksense.order.dto.ReceiptOrderResponse;
import com.stocksense.order.dto.UpdateOrderStatusRequest;
import com.stocksense.product.Product;
import com.stocksense.product.ProductRepository;
import com.stocksense.warehouse.Location;
import com.stocksense.warehouse.LocationRepository;
import com.stocksense.warehouse.LocationService;
import com.stocksense.warehouse.dto.LocationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReceiptOrderService {

    private final ReceiptOrderRepository receiptOrderRepository;
    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;
    private final LocationService locationService;
    private final StockOperationService stockOperationService;

    public ReceiptOrderService(ReceiptOrderRepository receiptOrderRepository,
                               ProductRepository productRepository,
                               LocationRepository locationRepository,
                               LocationService locationService,
                               StockOperationService stockOperationService) {
        this.receiptOrderRepository = receiptOrderRepository;
        this.productRepository = productRepository;
        this.locationRepository = locationRepository;
        this.locationService = locationService;
        this.stockOperationService = stockOperationService;
    }

    @Transactional
    public ReceiptOrderResponse createReceiptOrder(CreateReceiptOrderRequest request) {
        String reference = request.getReference().trim();
        if (receiptOrderRepository.existsByReferenceIgnoreCase(reference)) {
            throw new ConflictException("Receipt order with reference '" + reference + "' already exists");
        }

        Location destinationLocation = null;
        if (request.getDestinationLocationId() != null) {
            destinationLocation = locationRepository.findById(request.getDestinationLocationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Destination location not found with id: " + request.getDestinationLocationId()));
        } else {
            destinationLocation = locationService.getOrCreateDefaultLocation();
        }

        ReceiptOrder order = new ReceiptOrder(
                reference,
                request.getSupplierName(),
                request.getSupplierContact(),
                destinationLocation,
                OrderStatus.DRAFT,
                request.getNotes()
        );

        for (CreateReceiptOrderRequest.CreateLineRequest lineReq : request.getLines()) {
            Product product = productRepository.findById(lineReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + lineReq.getProductId()));
            BigDecimal receivedQty = lineReq.getReceivedQuantity() != null ? lineReq.getReceivedQuantity() : BigDecimal.ZERO;
            ReceiptOrderLine line = new ReceiptOrderLine(order, product, lineReq.getExpectedQuantity(), receivedQty);
            order.addLine(line);
        }

        ReceiptOrder saved = receiptOrderRepository.save(order);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ReceiptOrderResponse getReceiptOrderById(Long id) {
        ReceiptOrder order = findOrThrow(id);
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<ReceiptOrderResponse> getAllReceiptOrders(OrderStatus status, Pageable pageable) {
        Page<ReceiptOrder> page = (status != null)
                ? receiptOrderRepository.findByStatus(status, pageable)
                : receiptOrderRepository.findAll(pageable);
        return page.map(this::toResponse);
    }

    @Transactional
    public ReceiptOrderResponse updateStatus(Long id, UpdateOrderStatusRequest request) {
        ReceiptOrder order = findOrThrow(id);
        OrderStatus current = order.getStatus();
        OrderStatus target = request.getStatus();

        if (current == OrderStatus.DONE) {
            throw new ConflictException("Receipt order '" + order.getReference() + "' is already completed (DONE) and cannot be modified");
        }
        if (current == OrderStatus.CANCELED) {
            throw new ConflictException("Receipt order '" + order.getReference() + "' is canceled and cannot be modified");
        }
        if (current == target) {
            return toResponse(order);
        }

        if (target == OrderStatus.CANCELED) {
            order.setStatus(OrderStatus.CANCELED);
            return toResponse(receiptOrderRepository.save(order));
        }

        if (target == OrderStatus.DONE) {
            // Process inventory receipt atomically for all lines
            for (ReceiptOrderLine line : order.getLines()) {
                BigDecimal qty = (line.getReceivedQuantity() != null && line.getReceivedQuantity().compareTo(BigDecimal.ZERO) > 0)
                        ? line.getReceivedQuantity()
                        : line.getExpectedQuantity();

                CreateReceiptRequest receiptRequest = new CreateReceiptRequest();
                receiptRequest.setProductId(line.getProduct().getId());
                receiptRequest.setQuantity(qty);
                receiptRequest.setReference(order.getReference());
                receiptRequest.setNotes("Receipt Order " + order.getReference() + " from " + order.getSupplierName());
                if (order.getDestinationLocation() != null) {
                    receiptRequest.setLocationId(order.getDestinationLocation().getId());
                }

                stockOperationService.createReceipt(receiptRequest);
            }
            order.setStatus(OrderStatus.DONE);
            order.setCompletedAt(LocalDateTime.now());
            return toResponse(receiptOrderRepository.save(order));
        }

        // Transition between DRAFT, WAITING, READY
        order.setStatus(target);
        return toResponse(receiptOrderRepository.save(order));
    }

    @Transactional
    public ReceiptOrderResponse updateLineReceivedQuantity(Long orderId, Long lineId, BigDecimal receivedQuantity) {
        ReceiptOrder order = findOrThrow(orderId);
        if (order.getStatus() == OrderStatus.DONE || order.getStatus() == OrderStatus.CANCELED) {
            throw new ConflictException("Cannot edit lines on a completed or canceled receipt order");
        }

        ReceiptOrderLine line = order.getLines().stream()
                .filter(l -> l.getId().equals(lineId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Receipt line not found with id: " + lineId));

        if (receivedQuantity == null || receivedQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Received quantity must not be negative");
        }

        line.setReceivedQuantity(receivedQuantity);
        return toResponse(receiptOrderRepository.save(order));
    }

    private ReceiptOrder findOrThrow(Long id) {
        return receiptOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt order not found with id: " + id));
    }

    private ReceiptOrderResponse toResponse(ReceiptOrder order) {
        LocationResponse locResp = null;
        if (order.getDestinationLocation() != null) {
            locResp = LocationResponse.fromEntity(order.getDestinationLocation());
        }

        List<ReceiptOrderLineDto> lineDtos = order.getLines().stream()
                .map(l -> new ReceiptOrderLineDto(
                        l.getId(),
                        l.getProduct().getId(),
                        l.getProduct().getSku(),
                        l.getProduct().getName(),
                        l.getExpectedQuantity(),
                        l.getReceivedQuantity()
                ))
                .collect(Collectors.toList());

        return new ReceiptOrderResponse(
                order.getId(),
                order.getReference(),
                order.getSupplierName(),
                order.getSupplierContact(),
                locResp,
                order.getStatus(),
                order.getNotes(),
                order.getCompletedAt(),
                lineDtos,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
