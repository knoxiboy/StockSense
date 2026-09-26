package com.stocksense.order;

import com.stocksense.order.dto.CreateDeliveryOrderRequest;
import com.stocksense.order.dto.DeliveryOrderResponse;
import com.stocksense.order.dto.UpdateOrderStatusRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/delivery-orders")
public class DeliveryOrderController {

    private final DeliveryOrderService deliveryOrderService;

    public DeliveryOrderController(DeliveryOrderService deliveryOrderService) {
        this.deliveryOrderService = deliveryOrderService;
    }

    @PostMapping
    public ResponseEntity<DeliveryOrderResponse> createDeliveryOrder(@Valid @RequestBody CreateDeliveryOrderRequest request) {
        DeliveryOrderResponse response = deliveryOrderService.createDeliveryOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeliveryOrderResponse> getDeliveryOrder(@PathVariable Long id) {
        return ResponseEntity.ok(deliveryOrderService.getDeliveryOrderById(id));
    }

    @GetMapping
    public ResponseEntity<List<DeliveryOrderResponse>> getAllDeliveryOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<DeliveryOrderResponse> pageResult = deliveryOrderService.getAllDeliveryOrders(status, pageable);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", String.valueOf(pageResult.getTotalElements()));
        headers.add("X-Total-Pages", String.valueOf(pageResult.getTotalPages()));
        headers.add("X-Current-Page", String.valueOf(pageResult.getNumber()));
        headers.add("X-Page-Size", String.valueOf(pageResult.getSize()));

        return ResponseEntity.ok().headers(headers).body(pageResult.getContent());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<DeliveryOrderResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(deliveryOrderService.updateStatus(id, request));
    }

    @PutMapping("/{id}/lines/{lineId}")
    public ResponseEntity<DeliveryOrderResponse> updateLineProgress(
            @PathVariable Long id,
            @PathVariable Long lineId,
            @RequestBody Map<String, Object> body) {
        Boolean picked = body.containsKey("picked") ? (Boolean) body.get("picked") : null;
        Boolean packed = body.containsKey("packed") ? (Boolean) body.get("packed") : null;
        BigDecimal deliveredQuantity = null;
        if (body.containsKey("deliveredQuantity") && body.get("deliveredQuantity") != null) {
            deliveredQuantity = new BigDecimal(body.get("deliveredQuantity").toString());
        }
        return ResponseEntity.ok(deliveryOrderService.updateLineProgress(id, lineId, picked, packed, deliveredQuantity));
    }
}
