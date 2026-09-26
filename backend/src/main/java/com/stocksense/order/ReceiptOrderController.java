package com.stocksense.order;

import com.stocksense.order.dto.CreateReceiptOrderRequest;
import com.stocksense.order.dto.ReceiptOrderResponse;
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
@RequestMapping("/api/receipt-orders")
public class ReceiptOrderController {

    private final ReceiptOrderService receiptOrderService;

    public ReceiptOrderController(ReceiptOrderService receiptOrderService) {
        this.receiptOrderService = receiptOrderService;
    }

    @PostMapping
    public ResponseEntity<ReceiptOrderResponse> createReceiptOrder(@Valid @RequestBody CreateReceiptOrderRequest request) {
        ReceiptOrderResponse response = receiptOrderService.createReceiptOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReceiptOrderResponse> getReceiptOrder(@PathVariable Long id) {
        return ResponseEntity.ok(receiptOrderService.getReceiptOrderById(id));
    }

    @GetMapping
    public ResponseEntity<List<ReceiptOrderResponse>> getAllReceiptOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ReceiptOrderResponse> pageResult = receiptOrderService.getAllReceiptOrders(status, pageable);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", String.valueOf(pageResult.getTotalElements()));
        headers.add("X-Total-Pages", String.valueOf(pageResult.getTotalPages()));
        headers.add("X-Current-Page", String.valueOf(pageResult.getNumber()));
        headers.add("X-Page-Size", String.valueOf(pageResult.getSize()));

        return ResponseEntity.ok().headers(headers).body(pageResult.getContent());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ReceiptOrderResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(receiptOrderService.updateStatus(id, request));
    }

    @PutMapping("/{id}/lines/{lineId}")
    public ResponseEntity<ReceiptOrderResponse> updateLineReceivedQuantity(
            @PathVariable Long id,
            @PathVariable Long lineId,
            @RequestBody Map<String, BigDecimal> body) {
        BigDecimal receivedQuantity = body.get("receivedQuantity");
        return ResponseEntity.ok(receiptOrderService.updateLineReceivedQuantity(id, lineId, receivedQuantity));
    }
}
