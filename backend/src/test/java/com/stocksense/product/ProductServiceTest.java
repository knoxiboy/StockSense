package com.stocksense.product;

import com.stocksense.common.exception.ConflictException;
import com.stocksense.common.exception.DuplicateResourceException;
import com.stocksense.common.exception.ResourceNotFoundException;
import com.stocksense.inventory.StockBalance;
import com.stocksense.inventory.StockBalanceRepository;
import com.stocksense.inventory.dto.StockBalanceResponse;
import com.stocksense.product.dto.CreateProductRequest;
import com.stocksense.product.dto.ProductResponse;
import com.stocksense.product.dto.UpdateProductRequest;
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
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockBalanceRepository stockBalanceRepository;

    @Mock
    private com.stocksense.operation.StockOperationRepository stockOperationRepository;

    @InjectMocks
    private ProductService productService;

    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleProduct = new Product(
                "Steel Rod",
                "STEEL-001",
                "Raw Materials",
                "kg",
                new BigDecimal("20.0000"),
                "High tensile steel rod",
                new BigDecimal("45.50")
        );
        sampleProduct.setId(1L);
    }

    @Test
    void testCreateProduct_SuccessWithZeroStockBalance() {
        CreateProductRequest request = new CreateProductRequest(
                "Steel Rod",
                "STEEL-001",
                "Raw Materials",
                "kg",
                new BigDecimal("20.0000"),
                "High tensile steel rod",
                new BigDecimal("45.50")
        );

        when(productRepository.existsBySku("STEEL-001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        ProductResponse response = productService.createProduct(request);

        assertNotNull(response);
        assertEquals("STEEL-001", response.getSku());
        assertEquals("Steel Rod", response.getName());

        // Verify that a zero-valued StockBalance was saved in the same transaction
        ArgumentCaptor<StockBalance> balanceCaptor = ArgumentCaptor.forClass(StockBalance.class);
        verify(stockBalanceRepository).save(balanceCaptor.capture());
        StockBalance savedBalance = balanceCaptor.getValue();
        assertEquals(BigDecimal.ZERO, savedBalance.getQuantity());
        assertEquals(sampleProduct, savedBalance.getProduct());
    }

    @Test
    void testCreateProduct_DuplicateSkuThrowsConflict() {
        CreateProductRequest request = new CreateProductRequest(
                "Steel Rod",
                "STEEL-001",
                "Raw Materials",
                "kg",
                BigDecimal.ZERO,
                "Desc",
                BigDecimal.TEN
        );

        when(productRepository.existsBySku("STEEL-001")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> productService.createProduct(request));
        verify(productRepository, never()).save(any(Product.class));
        verify(stockBalanceRepository, never()).save(any(StockBalance.class));
    }

    @Test
    void testGetProductById_NotFoundThrows() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.getProductById(99L));
    }

    @Test
    void testUpdateProduct_DoesNotAlterStock() {
        UpdateProductRequest updateReq = new UpdateProductRequest(
                "Updated Steel Rod",
                "STEEL-001",
                "Raw Materials",
                "kg",
                new BigDecimal("30.0000"),
                "Updated description",
                new BigDecimal("50.00")
        );

        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.existsBySkuAndIdNot("STEEL-001", 1L)).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        ProductResponse response = productService.updateProduct(1L, updateReq);

        assertNotNull(response);
        verify(productRepository).save(sampleProduct);
        // Verify stock balance repository was never modified
        verify(stockBalanceRepository, never()).save(any(StockBalance.class));
        verify(stockBalanceRepository, never()).delete(any(StockBalance.class));
    }

    @Test
    void testDeleteProduct_ConflictWhenPositiveStockPresent() {
        StockBalance positiveBalance = new StockBalance(sampleProduct, new BigDecimal("15.5000"));
        positiveBalance.setId(10L);

        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(stockBalanceRepository.findByProductId(1L)).thenReturn(Optional.of(positiveBalance));

        assertThrows(ConflictException.class, () -> productService.deleteProduct(1L));
        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    void testDeleteProduct_SuccessWhenZeroStock() {
        StockBalance zeroBalance = new StockBalance(sampleProduct, BigDecimal.ZERO);
        zeroBalance.setId(10L);

        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(stockBalanceRepository.findByProductId(1L)).thenReturn(Optional.of(zeroBalance));

        productService.deleteProduct(1L);

        verify(stockBalanceRepository).delete(zeroBalance);
        verify(productRepository).delete(sampleProduct);
    }

    @Test
    void testDeleteProduct_ConflictWhenStockMovementsExist() {
        StockBalance zeroBalance = new StockBalance(sampleProduct, BigDecimal.ZERO);
        zeroBalance.setId(10L);

        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(stockBalanceRepository.findByProductId(1L)).thenReturn(Optional.of(zeroBalance));
        when(stockOperationRepository.existsByProductId(1L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> productService.deleteProduct(1L));
        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    void testGetStockBalance_ReturnsBalance() {
        StockBalance balance = new StockBalance(sampleProduct, new BigDecimal("42.0000"));
        balance.setId(5L);

        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(stockBalanceRepository.findByProductId(1L)).thenReturn(Optional.of(balance));

        StockBalanceResponse res = productService.getStockBalance(1L);

        assertNotNull(res);
        assertEquals(new BigDecimal("42.0000"), res.getQuantity());
        assertEquals("STEEL-001", res.getSku());
    }
}
