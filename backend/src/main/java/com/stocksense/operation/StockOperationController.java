package com.stocksense.operation;

import com.stocksense.operation.dto.CreateAdjustmentRequest;
import com.stocksense.operation.dto.CreateDeliveryRequest;
import com.stocksense.operation.dto.CreateReceiptRequest;
import com.stocksense.operation.dto.StockOperationResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/operations")
public class StockOperationController {

    private final StockOperationService stockOperationService;

    public StockOperationController(StockOperationService stockOperationService) {
        this.stockOperationService = stockOperationService;
    }

    @PostMapping("/receipts")
    public ResponseEntity<StockOperationResponse> createReceipt(
            @Valid @RequestBody CreateReceiptRequest request) {
        StockOperationResponse response = stockOperationService.createReceipt(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/deliveries")
    public ResponseEntity<StockOperationResponse> createDelivery(
            @Valid @RequestBody CreateDeliveryRequest request) {
        StockOperationResponse response = stockOperationService.createDelivery(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/adjustments")
    public ResponseEntity<StockOperationResponse> createAdjustment(
            @Valid @RequestBody CreateAdjustmentRequest request) {
        StockOperationResponse response = stockOperationService.createAdjustment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StockOperationResponse> getOperationById(@PathVariable Long id) {
        StockOperationResponse response = stockOperationService.getOperationById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<StockOperationResponse>> getOperations(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) OperationType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<StockOperationResponse> operations = stockOperationService.getOperations(productId, type, page, size);
        return ResponseEntity.ok(operations);
    }
}
