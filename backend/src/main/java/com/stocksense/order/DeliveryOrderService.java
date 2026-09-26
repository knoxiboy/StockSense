package com.stocksense.order;

import com.stocksense.common.exception.ConflictException;
import com.stocksense.common.exception.ResourceNotFoundException;
import com.stocksense.operation.StockOperationService;
import com.stocksense.operation.dto.CreateDeliveryRequest;
import com.stocksense.order.dto.CreateDeliveryOrderRequest;
import com.stocksense.order.dto.DeliveryOrderLineDto;
import com.stocksense.order.dto.DeliveryOrderResponse;
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
public class DeliveryOrderService {

    private final DeliveryOrderRepository deliveryOrderRepository;
    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;
    private final LocationService locationService;
    private final StockOperationService stockOperationService;

    public DeliveryOrderService(DeliveryOrderRepository deliveryOrderRepository,
                                ProductRepository productRepository,
                                LocationRepository locationRepository,
                                LocationService locationService,
                                StockOperationService stockOperationService) {
        this.deliveryOrderRepository = deliveryOrderRepository;
        this.productRepository = productRepository;
        this.locationRepository = locationRepository;
        this.locationService = locationService;
        this.stockOperationService = stockOperationService;
    }

    @Transactional
    public DeliveryOrderResponse createDeliveryOrder(CreateDeliveryOrderRequest request) {
        String reference = request.getReference().trim();
        if (deliveryOrderRepository.existsByReferenceIgnoreCase(reference)) {
            throw new ConflictException("Delivery order with reference '" + reference + "' already exists");
        }

        Location sourceLocation = null;
        if (request.getSourceLocationId() != null) {
            sourceLocation = locationRepository.findById(request.getSourceLocationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Source location not found with id: " + request.getSourceLocationId()));
        } else {
            sourceLocation = locationService.getOrCreateDefaultLocation();
        }

        DeliveryOrder order = new DeliveryOrder(
                reference,
                request.getCustomerName(),
                request.getCustomerContact(),
                request.getDeliveryAddress(),
                sourceLocation,
                OrderStatus.DRAFT,
                request.getNotes()
        );

        for (CreateDeliveryOrderRequest.CreateLineRequest lineReq : request.getLines()) {
            Product product = productRepository.findById(lineReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + lineReq.getProductId()));
            BigDecimal deliveredQty = lineReq.getDeliveredQuantity() != null ? lineReq.getDeliveredQuantity() : BigDecimal.ZERO;
            DeliveryOrderLine line = new DeliveryOrderLine(order, product, lineReq.getOrderedQuantity(), deliveredQty);
            if (lineReq.getPicked() != null) line.setPicked(lineReq.getPicked());
            if (lineReq.getPacked() != null) line.setPacked(lineReq.getPacked());
            order.addLine(line);
        }

        DeliveryOrder saved = deliveryOrderRepository.save(order);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public DeliveryOrderResponse getDeliveryOrderById(Long id) {
        DeliveryOrder order = findOrThrow(id);
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<DeliveryOrderResponse> getAllDeliveryOrders(OrderStatus status, Pageable pageable) {
        Page<DeliveryOrder> page = (status != null)
                ? deliveryOrderRepository.findByStatus(status, pageable)
                : deliveryOrderRepository.findAll(pageable);
        return page.map(this::toResponse);
    }

    @Transactional
    public DeliveryOrderResponse updateStatus(Long id, UpdateOrderStatusRequest request) {
        DeliveryOrder order = findOrThrow(id);
        OrderStatus current = order.getStatus();
        OrderStatus target = request.getStatus();

        if (current == OrderStatus.DONE) {
            throw new ConflictException("Delivery order '" + order.getReference() + "' is already completed (DONE) and cannot be modified");
        }
        if (current == OrderStatus.CANCELED) {
            throw new ConflictException("Delivery order '" + order.getReference() + "' is canceled and cannot be modified");
        }
        if (current == target) {
            return toResponse(order);
        }

        if (target == OrderStatus.CANCELED) {
            order.setStatus(OrderStatus.CANCELED);
            return toResponse(deliveryOrderRepository.save(order));
        }

        if (target == OrderStatus.DONE) {
            // Deduct inventory atomically for all lines with insufficient stock protection
            for (DeliveryOrderLine line : order.getLines()) {
                BigDecimal qty = (line.getDeliveredQuantity() != null && line.getDeliveredQuantity().compareTo(BigDecimal.ZERO) > 0)
                        ? line.getDeliveredQuantity()
                        : line.getOrderedQuantity();

                CreateDeliveryRequest deliveryRequest = new CreateDeliveryRequest();
                deliveryRequest.setProductId(line.getProduct().getId());
                deliveryRequest.setQuantity(qty);
                deliveryRequest.setReference(order.getReference());
                deliveryRequest.setNotes("Delivery Order " + order.getReference() + " to " + order.getCustomerName());
                if (order.getSourceLocation() != null) {
                    deliveryRequest.setLocationId(order.getSourceLocation().getId());
                }

                // If insufficient stock, this throws ConflictException and rolls back the entire transaction!
                stockOperationService.createDelivery(deliveryRequest);
                line.setDeliveredQuantity(qty);
                line.setPicked(true);
                line.setPacked(true);
            }
            order.setStatus(OrderStatus.DONE);
            order.setCompletedAt(LocalDateTime.now());
            return toResponse(deliveryOrderRepository.save(order));
        }

        // Transition between DRAFT, WAITING, READY
        order.setStatus(target);
        return toResponse(deliveryOrderRepository.save(order));
    }

    @Transactional
    public DeliveryOrderResponse updateLineProgress(Long orderId, Long lineId, Boolean picked, Boolean packed, BigDecimal deliveredQuantity) {
        DeliveryOrder order = findOrThrow(orderId);
        if (order.getStatus() == OrderStatus.DONE || order.getStatus() == OrderStatus.CANCELED) {
            throw new ConflictException("Cannot edit lines on a completed or canceled delivery order");
        }

        DeliveryOrderLine line = order.getLines().stream()
                .filter(l -> l.getId().equals(lineId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Delivery line not found with id: " + lineId));

        if (picked != null) line.setPicked(picked);
        if (packed != null) line.setPacked(packed);
        if (deliveredQuantity != null) {
            if (deliveredQuantity.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Delivered quantity cannot be negative");
            }
            line.setDeliveredQuantity(deliveredQuantity);
        }

        return toResponse(deliveryOrderRepository.save(order));
    }

    private DeliveryOrder findOrThrow(Long id) {
        return deliveryOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery order not found with id: " + id));
    }

    private DeliveryOrderResponse toResponse(DeliveryOrder order) {
        LocationResponse locResp = null;
        if (order.getSourceLocation() != null) {
            locResp = LocationResponse.fromEntity(order.getSourceLocation());
        }

        List<DeliveryOrderLineDto> lineDtos = order.getLines().stream()
                .map(l -> new DeliveryOrderLineDto(
                        l.getId(),
                        l.getProduct().getId(),
                        l.getProduct().getSku(),
                        l.getProduct().getName(),
                        l.getOrderedQuantity(),
                        l.getDeliveredQuantity(),
                        l.getPicked(),
                        l.getPacked()
                ))
                .collect(Collectors.toList());

        return new DeliveryOrderResponse(
                order.getId(),
                order.getReference(),
                order.getCustomerName(),
                order.getCustomerContact(),
                order.getDeliveryAddress(),
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
