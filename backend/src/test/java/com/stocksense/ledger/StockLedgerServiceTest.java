package com.stocksense.ledger;

import com.stocksense.common.exception.ResourceNotFoundException;
import com.stocksense.ledger.dto.StockLedgerEntryResponse;
import com.stocksense.operation.OperationType;
import com.stocksense.operation.StockOperation;
import com.stocksense.product.Product;
import com.stocksense.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockLedgerServiceTest {

    @Mock
    private StockLedgerRepository stockLedgerRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private StockLedgerService stockLedgerService;

    private Product sampleProduct;
    private StockOperation sampleOperation;
    private StockLedgerEntry sampleEntry;

    @BeforeEach
    void setUp() {
        sampleProduct = new Product(
                "Steel Sheet 2mm",
                "SHT-02MM",
                "Raw Materials",
                "pcs",
                new BigDecimal("10.0000"),
                null,
                new BigDecimal("50.00")
        );
        sampleProduct.setId(1L);

        sampleOperation = new StockOperation(
                OperationType.RECEIPT,
                sampleProduct,
                new BigDecimal("20.0000"),
                new BigDecimal("20.0000"),
                "PO-100",
                "Receipt note"
        );
        sampleOperation.setId(10L);

        sampleEntry = new StockLedgerEntry(
                sampleOperation,
                sampleProduct,
                OperationType.RECEIPT,
                new BigDecimal("20.0000"),
                BigDecimal.ZERO,
                new BigDecimal("20.0000")
        );
        sampleEntry.setId(100L);
        sampleEntry.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void testGetLedgerEntryById_Success() {
        when(stockLedgerRepository.findWithDetailsById(100L)).thenReturn(Optional.of(sampleEntry));

        StockLedgerEntryResponse response = stockLedgerService.getLedgerEntryById(100L);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(10L, response.getOperationId());
        assertEquals(1L, response.getProductId());
        assertEquals("Steel Sheet 2mm", response.getProductName());
        assertEquals("SHT-02MM", response.getSku());
        assertEquals(OperationType.RECEIPT, response.getOperationType());
        assertEquals(new BigDecimal("20.0000"), response.getQuantityChange());
        assertEquals(BigDecimal.ZERO, response.getPreviousQuantity());
        assertEquals(new BigDecimal("20.0000"), response.getResultingQuantity());
        assertEquals("PO-100", response.getReference());
        assertEquals("Receipt note", response.getNotes());
    }

    @Test
    void testGetLedgerEntryById_NotFoundThrows() {
        when(stockLedgerRepository.findWithDetailsById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> stockLedgerService.getLedgerEntryById(999L));
    }

    @Test
    void testGetLedgerEntryById_NullIdThrowsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> stockLedgerService.getLedgerEntryById(null));
    }

    @Test
    void testGetLedgerEntries_InvalidPageThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                stockLedgerService.getLedgerEntriesPaged(null, null, null, null, -1, 20));
    }

    @Test
    void testGetLedgerEntries_InvalidSizeThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                stockLedgerService.getLedgerEntriesPaged(null, null, null, null, 0, 0));
        assertThrows(IllegalArgumentException.class, () ->
                stockLedgerService.getLedgerEntriesPaged(null, null, null, null, 0, 101));
    }

    @Test
    void testGetLedgerEntries_FromAfterToThrows() {
        LocalDate from = LocalDate.of(2026, 9, 27);
        LocalDate to = LocalDate.of(2026, 9, 26);

        assertThrows(IllegalArgumentException.class, () ->
                stockLedgerService.getLedgerEntriesPaged(null, null, from, to, 0, 20));
    }

    @Test
    void testGetLedgerEntries_NonExistentProductThrows() {
        when(productRepository.existsById(999L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () ->
                stockLedgerService.getLedgerEntriesPaged(999L, null, null, null, 0, 20));
    }

    @Test
    void testGetLedgerEntries_Success() {
        Page<StockLedgerEntry> pageResult = new PageImpl<>(List.of(sampleEntry));
        when(stockLedgerRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(pageResult);

        Page<StockLedgerEntryResponse> result = stockLedgerService.getLedgerEntriesPaged(
                null, OperationType.RECEIPT, null, null, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("SHT-02MM", result.getContent().get(0).getSku());
    }
}
