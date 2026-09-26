package com.stocksense.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocksense.common.exception.DuplicateResourceException;
import com.stocksense.common.exception.ResourceNotFoundException;
import com.stocksense.product.dto.CreateProductRequest;
import com.stocksense.product.dto.ProductResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
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
        p.setUnitOfMeasure("boxes");
        p.setReorderLevel(BigDecimal.valueOf(10));

        when(productService.getAllProducts(null, null)).thenReturn(List.of(p));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Box of Screws"))
                .andExpect(jsonPath("$[0].sku").value("SCRW-01"));
    }

    @Test
    void testCreateProduct_Success() throws Exception {
        CreateProductRequest req = new CreateProductRequest(
                "Box of Screws",
                "SCRW-01",
                "Hardware",
                "Fasteners",
                "boxes",
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(5.99)
        );

        ProductResponse res = new ProductResponse();
        res.setId(1L);
        res.setName("Box of Screws");
        res.setSku("SCRW-01");
        res.setUnitOfMeasure("boxes");
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
    void testCreateProduct_ValidationFails() throws Exception {
        CreateProductRequest req = new CreateProductRequest();
        // Empty fields should fail validation

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.sku").exists());
    }

    @Test
    void testGetProduct_NotFound() throws Exception {
        when(productService.getProductById(999L)).thenThrow(new ResourceNotFoundException("Product", "id", 999L));

        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
