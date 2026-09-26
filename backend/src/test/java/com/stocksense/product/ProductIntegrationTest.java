package com.stocksense.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocksense.inventory.StockBalance;
import com.stocksense.inventory.StockBalanceRepository;
import com.stocksense.product.dto.CreateProductRequest;
import com.stocksense.product.dto.ProductResponse;
import com.stocksense.product.dto.UpdateProductRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProductIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockBalanceRepository stockBalanceRepository;

    @BeforeEach
    void setUp() {
        stockBalanceRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void testCreateProduct_AndConfirmBalanceIsZero() throws Exception {
        CreateProductRequest req = new CreateProductRequest(
                "Industrial Gearbox",
                "GBX-001",
                "Machinery",
                "units",
                new BigDecimal("5.0000"),
                "Heavy duty planetary gearbox",
                new BigDecimal("1200.00")
        );

        MvcResult result = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.sku").value("GBX-001"))
                .andReturn();

        ProductResponse created = objectMapper.readValue(
                result.getResponse().getContentAsString(), ProductResponse.class);

        // Verify balance endpoint returns 0
        mockMvc.perform(get("/api/products/" + created.getId() + "/stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(created.getId()))
                .andExpect(jsonPath("$.sku").value("GBX-001"))
                .andExpect(jsonPath("$.quantity").value(0));

        // Verify in database repository directly
        StockBalance balance = stockBalanceRepository.findByProductId(created.getId()).orElse(null);
        assertNotNull(balance);
        assertEquals(0, BigDecimal.ZERO.compareTo(balance.getQuantity()));
    }

    @Test
    void testDuplicateSku_IsRejectedWith409() throws Exception {
        CreateProductRequest req1 = new CreateProductRequest(
                "Ball Bearing",
                "BRG-01",
                "Components",
                "pcs",
                BigDecimal.TEN,
                null,
                BigDecimal.ONE
        );

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        CreateProductRequest req2 = new CreateProductRequest(
                "Duplicate Bearing",
                "BRG-01",
                "Components",
                "pcs",
                BigDecimal.TEN,
                null,
                BigDecimal.ONE
        );

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Product already exists with SKU: 'BRG-01'"));
    }

    @Test
    void testMissingProduct_Returns404() throws Exception {
        mockMvc.perform(get("/api/products/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        mockMvc.perform(get("/api/products/99999/stock"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void testInvalidOrNegativeReorderLevel_IsRejected() throws Exception {
        CreateProductRequest negativeReq = new CreateProductRequest(
                "Negative Reorder Item",
                "NEG-01",
                "General",
                "units",
                new BigDecimal("-1.00"),
                null,
                null
        );

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(negativeReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.reorderLevel").exists());
    }

    @Test
    void testSearchByNameOrSku_AndFilterByCategory() throws Exception {
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateProductRequest(
                        "Copper Cable 5m", "CAB-COP-05", "Electrical", "meters", BigDecimal.ZERO, null, null))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateProductRequest(
                        "Steel Wire 10m", "WIR-STL-10", "Hardware", "meters", BigDecimal.ZERO, null, null))))
                .andExpect(status().isCreated());

        // Search by name substring "Cable"
        mockMvc.perform(get("/api/products?search=Cable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].sku").value("CAB-COP-05"));

        // Search by SKU substring "STL"
        mockMvc.perform(get("/api/products?search=STL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Steel Wire 10m"));

        // Filter by category "Electrical"
        mockMvc.perform(get("/api/products?category=Electrical"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].category").value("Electrical"));

        // Filter by category "Plumbing" (no items)
        mockMvc.perform(get("/api/products?category=Plumbing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void testProductUpdate_DoesNotAlterStock() throws Exception {
        CreateProductRequest req = new CreateProductRequest(
                "Original Product", "UPD-01", "Tools", "units", BigDecimal.TEN, null, null);

        MvcResult result = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        ProductResponse created = objectMapper.readValue(
                result.getResponse().getContentAsString(), ProductResponse.class);

        // Manually adjust stock balance to simulate inventory movement
        StockBalance balance = stockBalanceRepository.findByProductId(created.getId()).orElseThrow();
        balance.setQuantity(new BigDecimal("75.5000"));
        stockBalanceRepository.save(balance);

        // Now perform a Product update
        UpdateProductRequest updateReq = new UpdateProductRequest(
                "Renamed Product", "UPD-01-V2", "Tools", "units", new BigDecimal("25.0000"), "Updated notes", BigDecimal.valueOf(50));

        mockMvc.perform(put("/api/products/" + created.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed Product"))
                .andExpect(jsonPath("$.sku").value("UPD-01-V2"))
                .andExpect(jsonPath("$.reorderLevel").value(25.0));

        // Verify stock balance is completely unchanged
        mockMvc.perform(get("/api/products/" + created.getId() + "/stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(75.5));
    }

    @Test
    void testDeleteProduct_ConflictWhenStockPresent_SuccessWhenZero() throws Exception {
        CreateProductRequest req = new CreateProductRequest(
                "Deletion Test Item", "DEL-01", "Testing", "pcs", BigDecimal.ZERO, null, null);

        MvcResult result = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        ProductResponse created = objectMapper.readValue(
                result.getResponse().getContentAsString(), ProductResponse.class);

        // Case 1: When stock is > 0, deletion must return 409 Conflict
        StockBalance balance = stockBalanceRepository.findByProductId(created.getId()).orElseThrow();
        balance.setQuantity(new BigDecimal("10.0000"));
        stockBalanceRepository.save(balance);

        mockMvc.perform(delete("/api/products/" + created.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        // Case 2: When stock is 0, deletion succeeds with 204 No Content
        balance.setQuantity(BigDecimal.ZERO);
        stockBalanceRepository.save(balance);

        mockMvc.perform(delete("/api/products/" + created.getId()))
                .andExpect(status().isNoContent());

        // Verify product and balance are deleted
        assertFalse(productRepository.existsById(created.getId()));
        assertFalse(stockBalanceRepository.existsByProductId(created.getId()));
    }
}
