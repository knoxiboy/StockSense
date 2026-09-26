package com.stocksense.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocksense.inventory.StockBalanceRepository;
import com.stocksense.order.dto.CreateDeliveryOrderRequest;
import com.stocksense.order.dto.CreateReceiptOrderRequest;
import com.stocksense.order.dto.UpdateOrderStatusRequest;
import com.stocksense.product.Product;
import com.stocksense.product.ProductRepository;
import com.stocksense.warehouse.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockBalanceRepository stockBalanceRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private LocationRepository locationRepository;

    private Product product;
    private Location location;

    @BeforeEach
    void setUp() {
        Warehouse wh = warehouseRepository.findByCode("WH-ORD-TEST").orElseGet(() ->
                warehouseRepository.save(new Warehouse("Order WH", "WH-ORD-TEST", "Order Zone")));

        location = locationRepository.findByCode("LOC-ORD-01").orElseGet(() ->
                locationRepository.save(new Location("Order Shelf", "LOC-ORD-01", wh, LocationType.INTERNAL)));

        product = productRepository.findBySku("ORD-PROD-01").orElseGet(() -> {
            Product p = new Product("Workflow Product", "ORD-PROD-01", "Tools", "units", BigDecimal.valueOf(10), null, BigDecimal.valueOf(25));
            Product saved = productRepository.save(p);
            stockBalanceRepository.save(new com.stocksense.inventory.StockBalance(saved, BigDecimal.ZERO));
            return saved;
        });
    }

    @Test
    void testReceiptOrderWorkflow_LifecycleAndInventoryApplication() throws Exception {
        BigDecimal initialStock = stockBalanceRepository.findByProductId(product.getId()).orElseThrow().getQuantity();

        CreateReceiptOrderRequest request = new CreateReceiptOrderRequest();
        request.setReference("REC-WF-001");
        request.setSupplierName("Apex Industrial Supply");
        request.setDestinationLocationId(location.getId());
        request.setLines(List.of(
                new CreateReceiptOrderRequest.CreateLineRequest(product.getId(), BigDecimal.valueOf(100), BigDecimal.valueOf(100))
        ));

        // Create as DRAFT
        String res = mockMvc.perform(post("/api/receipt-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();

        Long orderId = objectMapper.readTree(res).get("id").asLong();

        // Stock MUST NOT change in DRAFT
        BigDecimal stockAfterDraft = stockBalanceRepository.findByProductId(product.getId()).orElseThrow().getQuantity();
        assertThat(stockAfterDraft).isEqualByComparingTo(initialStock);

        // Transition to WAITING
        mockMvc.perform(put("/api/receipt-orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderStatusRequest(OrderStatus.WAITING))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING"));

        // Transition to READY
        mockMvc.perform(put("/api/receipt-orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderStatusRequest(OrderStatus.READY))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"));

        // Still no stock change
        BigDecimal stockAfterReady = stockBalanceRepository.findByProductId(product.getId()).orElseThrow().getQuantity();
        assertThat(stockAfterReady).isEqualByComparingTo(initialStock);

        // Complete receipt (DONE)
        mockMvc.perform(put("/api/receipt-orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderStatusRequest(OrderStatus.DONE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));

        // Now stock MUST have increased by 100
        BigDecimal stockAfterDone = stockBalanceRepository.findByProductId(product.getId()).orElseThrow().getQuantity();
        assertThat(stockAfterDone).isEqualByComparingTo(initialStock.add(BigDecimal.valueOf(100)));

        // Duplicate completion must be prevented
        mockMvc.perform(put("/api/receipt-orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderStatusRequest(OrderStatus.DONE))))
                .andExpect(status().isConflict());
    }

    @Test
    void testDeliveryOrderWorkflow_InsufficientStock_Rejected() throws Exception {
        CreateDeliveryOrderRequest request = new CreateDeliveryOrderRequest();
        request.setReference("DEL-WF-EXCEED");
        request.setCustomerName("Acme Global");
        request.setSourceLocationId(location.getId());
        request.setLines(List.of(
                new CreateDeliveryOrderRequest.CreateLineRequest(product.getId(), BigDecimal.valueOf(999999), BigDecimal.valueOf(999999))
        ));

        String res = mockMvc.perform(post("/api/delivery-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long orderId = objectMapper.readTree(res).get("id").asLong();

        // Attempting to complete delivery exceeding stock must fail
        mockMvc.perform(put("/api/delivery-orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderStatusRequest(OrderStatus.DONE))))
                .andExpect(status().isConflict());
    }
}
