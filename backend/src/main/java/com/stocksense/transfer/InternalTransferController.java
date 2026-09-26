package com.stocksense.transfer;

import com.stocksense.transfer.dto.CreateTransferRequest;
import com.stocksense.transfer.dto.TransferResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transfers")
public class InternalTransferController {

    private final InternalTransferService internalTransferService;

    public InternalTransferController(InternalTransferService internalTransferService) {
        this.internalTransferService = internalTransferService;
    }

    @GetMapping
    public ResponseEntity<List<TransferResponse>> getAllTransfers(
            @RequestParam(required = false) TransferStatus status,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long locationId) {
        return ResponseEntity.ok(internalTransferService.getAllTransfers(status, productId, locationId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransferResponse> getTransferById(@PathVariable Long id) {
        return ResponseEntity.ok(internalTransferService.getTransferById(id));
    }

    @PostMapping
    public ResponseEntity<TransferResponse> createTransfer(@Valid @RequestBody CreateTransferRequest request) {
        TransferResponse response = internalTransferService.createTransfer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<TransferResponse> executeTransfer(@PathVariable Long id) {
        return ResponseEntity.ok(internalTransferService.executeTransfer(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<TransferResponse> cancelTransfer(@PathVariable Long id) {
        return ResponseEntity.ok(internalTransferService.cancelTransfer(id));
    }
}
