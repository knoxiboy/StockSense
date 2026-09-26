package com.stocksense.operation;

import com.stocksense.common.exception.ConflictException;
import com.stocksense.common.exception.ResourceNotFoundException;
import com.stocksense.inventory.StockBalance;
import com.stocksense.inventory.StockBalanceRepository;
import com.stocksense.operation.dto.CreateAdjustmentRequest;
import com.stocksense.operation.dto.CreateDeliveryRequest;
import com.stocksense.operation.dto.CreateReceiptRequest;
import com.stocksense.operation.dto.StockOperationResponse;
import com.stocksense.product.Product;
import com.stocksense.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockOperationServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockBalanceRepository stockBalanceRepository;

    @Mock
    private StockOperationRepository stockOperationRepository;

    @Mock
    private com.stocksense.ledger.StockLedgerRepository stockLedgerRepository;

    @InjectMocks
    private StockOperationService stockOperationService;

    private Product sampleProduct;
    private StockBalance sampleBalance;

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

        sampleBalance = new StockBalance(sampleProduct, new BigDecimal("20.0000"));
        sampleBalance.setId(10L);
    }

    @Test
    void testCreateReceipt_IncreasesStockAndSavesOperation() {
        CreateReceiptRequest request = new CreateReceiptRequest(
                1L, new BigDecimal("30.0000"), "PO-1001", "Received from mill");

        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(stockBalanceRepository.findWithLockByProductId(1L)).thenReturn(Optional.of(sampleBalance));
        when(stockOperationRepository.save(any(StockOperation.class))).thenAnswer(i -> {
            StockOperation op = i.getArgument(0);
            op.setId(100L);
            return op;
        });

        StockOperationResponse response = stockOperationService.createReceipt(request);

        assertNotNull(response);
        assertEquals(OperationType.RECEIPT, response.getOperationType());
        assertEquals(new BigDecimal("30.0000"), response.getQuantity());
        assertEquals(new BigDecimal("30.0000"), response.getQuantityChange());
        assertEquals(new BigDecimal("50.0000"), response.getResultingQuantity());
        assertEquals(new BigDecimal("50.0000"), sampleBalance.getQuantity());

        verify(stockBalanceRepository).save(sampleBalance);
        verify(stockOperationRepository).save(any(StockOperation.class));
        verify(stockLedgerRepository).save(any(com.stocksense.ledger.StockLedgerEntry.class));
    }

    @Test
    void testCreateDelivery_DecreasesStockAndSavesOperation() {
        CreateDeliveryRequest request = new CreateDeliveryRequest(
                1L, new BigDecimal("8.0000"), "DO-2001", "Order #442");

        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(stockBalanceRepository.findWithLockByProductId(1L)).thenReturn(Optional.of(sampleBalance));
        when(stockOperationRepository.save(any(StockOperation.class))).thenAnswer(i -> {
            StockOperation op = i.getArgument(0);
            op.setId(101L);
            return op;
        });

        StockOperationResponse response = stockOperationService.createDelivery(request);

        assertNotNull(response);
        assertEquals(OperationType.DELIVERY, response.getOperationType());
        assertEquals(new BigDecimal("8.0000"), response.getQuantity());
        assertEquals(new BigDecimal("-8.0000"), response.getQuantityChange());
        assertEquals(new BigDecimal("12.0000"), response.getResultingQuantity());
        assertEquals(new BigDecimal("12.0000"), sampleBalance.getQuantity());

        verify(stockBalanceRepository).save(sampleBalance);
        verify(stockOperationRepository).save(any(StockOperation.class));
        verify(stockLedgerRepository).save(any(com.stocksense.ledger.StockLedgerEntry.class));
    }

    @Test
    void testCreateDelivery_InsufficientStockThrowsConflict() {
        CreateDeliveryRequest request = new CreateDeliveryRequest(
                1L, new BigDecimal("25.0000"), "DO-EXCEED", "Too much stock requested");

        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(stockBalanceRepository.findWithLockByProductId(1L)).thenReturn(Optional.of(sampleBalance));

        assertThrows(ConflictException.class, () -> stockOperationService.createDelivery(request));

        // Ensure stock balance was not modified and operation was not saved
        assertEquals(new BigDecimal("20.0000"), sampleBalance.getQuantity());
        verify(stockBalanceRepository, never()).save(any(StockBalance.class));
        verify(stockOperationRepository, never()).save(any(StockOperation.class));
        verify(stockLedgerRepository, never()).save(any(com.stocksense.ledger.StockLedgerEntry.class));
    }

    @Test
    void testCreateAdjustment_SetsExactQuantityAndCalculatesDifference() {
        CreateAdjustmentRequest request = new CreateAdjustmentRequest(
                1L, new BigDecimal("15.0000"), "ADJ-01", "Cycle count mismatch");

        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(stockBalanceRepository.findWithLockByProductId(1L)).thenReturn(Optional.of(sampleBalance));
        when(stockOperationRepository.save(any(StockOperation.class))).thenAnswer(i -> {
            StockOperation op = i.getArgument(0);
            op.setId(102L);
            return op;
        });

        StockOperationResponse response = stockOperationService.createAdjustment(request);

        assertNotNull(response);
        assertEquals(OperationType.ADJUSTMENT, response.getOperationType());
        assertEquals(new BigDecimal("15.0000"), response.getQuantity());
        // 15 - 20 = -5
        assertEquals(new BigDecimal("-5.0000"), response.getQuantityChange());
        assertEquals(new BigDecimal("15.0000"), response.getResultingQuantity());
        assertEquals(new BigDecimal("15.0000"), sampleBalance.getQuantity());

        verify(stockBalanceRepository).save(sampleBalance);
        verify(stockOperationRepository).save(any(StockOperation.class));
        verify(stockLedgerRepository).save(any(com.stocksense.ledger.StockLedgerEntry.class));
    }

    @Test
    void testMissingProduct_ThrowsResourceNotFoundException() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                stockOperationService.createReceipt(new CreateReceiptRequest(999L, BigDecimal.TEN, null, null)));
    }
}
