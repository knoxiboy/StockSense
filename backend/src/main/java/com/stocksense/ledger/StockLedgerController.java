package com.stocksense.ledger;

import com.stocksense.ledger.dto.StockLedgerEntryResponse;
import com.stocksense.operation.OperationType;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/ledger")
public class StockLedgerController {

    private final StockLedgerService stockLedgerService;

    public StockLedgerController(StockLedgerService stockLedgerService) {
        this.stockLedgerService = stockLedgerService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<StockLedgerEntryResponse> getLedgerEntryById(@PathVariable Long id) {
        StockLedgerEntryResponse response = stockLedgerService.getLedgerEntryById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<StockLedgerEntryResponse>> getLedgerEntries(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) OperationType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<StockLedgerEntryResponse> pagedResult = stockLedgerService.getLedgerEntriesPaged(
                productId, type, from, to, locationId, warehouseId, page, size);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", String.valueOf(pagedResult.getTotalElements()));
        headers.add("X-Total-Pages", String.valueOf(pagedResult.getTotalPages()));
        headers.add("X-Current-Page", String.valueOf(pagedResult.getNumber()));
        headers.add("X-Page-Size", String.valueOf(pagedResult.getSize()));

        return ResponseEntity.ok()
                .headers(headers)
                .body(pagedResult.getContent());
    }
}
