package com.stocksense.product;

import com.stocksense.common.exception.DuplicateResourceException;
import com.stocksense.common.exception.ResourceNotFoundException;
import com.stocksense.product.dto.CreateProductRequest;
import com.stocksense.product.dto.ProductResponse;
import com.stocksense.product.dto.UpdateProductRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleProduct = new Product(
                "Steel Rod",
                "STEEL-001",
                "Raw Materials",
                "High tensile steel rod",
                "kg",
                new BigDecimal("20.0000"),
                new BigDecimal("45.50")
        );
        sampleProduct.setId(1L);
    }

    @Test
    void testCreateProduct_Success() {
        CreateProductRequest request = new CreateProductRequest(
                "Steel Rod",
                "STEEL-001",
                "Raw Materials",
                "High tensile steel rod",
                "kg",
                new BigDecimal("20.0000"),
                new BigDecimal("45.50")
        );

        when(productRepository.existsBySku("STEEL-001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        ProductResponse response = productService.createProduct(request);

        assertNotNull(response);
        assertEquals("STEEL-001", response.getSku());
        assertEquals("Steel Rod", response.getName());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void testCreateProduct_DuplicateSkuThrows() {
        CreateProductRequest request = new CreateProductRequest(
                "Steel Rod",
                "STEEL-001",
                "Raw Materials",
                "Desc",
                "kg",
                BigDecimal.ZERO,
                BigDecimal.TEN
        );

        when(productRepository.existsBySku("STEEL-001")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> productService.createProduct(request));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void testGetProductById_NotFoundThrows() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.getProductById(99L));
    }

    @Test
    void testUpdateProduct_Success() {
        UpdateProductRequest updateReq = new UpdateProductRequest(
                "Updated Steel Rod",
                "STEEL-001",
                "Raw Materials",
                "Updated description",
                "kg",
                new BigDecimal("30.0000"),
                new BigDecimal("50.00"),
                true
        );

        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.existsBySkuAndIdNot("STEEL-001", 1L)).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        ProductResponse response = productService.updateProduct(1L, updateReq);

        assertNotNull(response);
        verify(productRepository).save(sampleProduct);
    }

    @Test
    void testDeleteProduct_Success() {
        when(productRepository.existsById(1L)).thenReturn(true);
        doNothing().when(productRepository).deleteById(1L);

        productService.deleteProduct(1L);

        verify(productRepository).deleteById(1L);
    }
}
