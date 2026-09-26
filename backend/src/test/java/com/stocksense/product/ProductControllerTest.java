package com.stocksense.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocksense.common.exception.ConflictException;
import com.stocksense.common.exception.DuplicateResourceException;
import com.stocksense.common.exception.ResourceNotFoundException;
import com.stocksense.inventory.dto.StockBalanceResponse;
import com.stocksense.product.dto.CreateProductRequest;
import com.stocksense.product.dto.ProductResponse;
import com.stocksense.product.dto.UpdateProductRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @Test
    void testGetAllProducts() throws Exception {
        ProductResponse p = new ProductResponse();
        p.setId(1L);
        p.setName("Box of Screws");
        p.setSku("SCRW-01");
        p.setCategory("Hardware");
        p.setUnit("boxes");
        p.setReorderLevel(BigDecimal.valueOf(10));

        when(productService.getAllProducts(null, null)).thenReturn(List.of(p));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Box of Screws"))
                .andExpect(jsonPath("$[0].sku").value("SCRW-01"))
                .andExpect(jsonPath("$[0].unit").value("boxes"));
    }

    @Test
    void testCreateProduct_Success() throws Exception {
        CreateProductRequest req = new CreateProductRequest(
                "Box of Screws",
                "SCRW-01",
                "Hardware",
                "boxes",
                BigDecimal.valueOf(10),
                "Fasteners",
                BigDecimal.valueOf(5.99)
        );

        ProductResponse res = new ProductResponse();
        res.setId(1L);
        res.setName("Box of Screws");
        res.setSku("SCRW-01");
        res.setCategory("Hardware");
        res.setUnit("boxes");
        res.setReorderLevel(BigDecimal.valueOf(10));
        res.setPrice(BigDecimal.valueOf(5.99));

        when(productService.createProduct(any(CreateProductRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.sku").value("SCRW-01"));
    }

    @Test
    void testCreateProduct_ValidationFailsOnEmptyFields() throws Exception {
        CreateProductRequest req = new CreateProductRequest();

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.sku").exists())
                .andExpect(jsonPath("$.validationErrors.category").exists())
                .andExpect(jsonPath("$.validationErrors.unit").exists());
    }

    @Test
    void testCreateProduct_ValidationFailsOnNegativeReorderLevel() throws Exception {
        CreateProductRequest req = new CreateProductRequest(
                "Screws",
                "SCRW-02",
                "Hardware",
                "boxes",
                new BigDecimal("-5.00"),
                null,
                BigDecimal.ONE
        );

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.reorderLevel").exists());
    }

    @Test
    void testCreateProduct_DuplicateSkuReturns409() throws Exception {
        CreateProductRequest req = new CreateProductRequest(
                "Screws",
                "SCRW-DUP",
                "Hardware",
                "boxes",
                BigDecimal.TEN,
                null,
                null
        );

        when(productService.createProduct(any(CreateProductRequest.class)))
                .thenThrow(new DuplicateResourceException("Product", "SKU", "SCRW-DUP"));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void testGetProduct_NotFoundReturns404() throws Exception {
        when(productService.getProductById(999L)).thenThrow(new ResourceNotFoundException("Product", "id", 999L));

        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void testGetProductStockBalance_Success() throws Exception {
        StockBalanceResponse balance = new StockBalanceResponse();
        balance.setId(1L);
        balance.setProductId(1L);
        balance.setProductName("Box of Screws");
        balance.setSku("SCRW-01");
        balance.setQuantity(BigDecimal.ZERO);
        balance.setUnit("boxes");

        when(productService.getStockBalance(1L)).thenReturn(balance);

        mockMvc.perform(get("/api/products/1/stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.quantity").value(0))
                .andExpect(jsonPath("$.unit").value("boxes"));
    }

    @Test
    void testDeleteProduct_ConflictReturns409() throws Exception {
        doThrow(new ConflictException("Cannot delete product with existing stock"))
                .when(productService).deleteProduct(1L);

        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}
