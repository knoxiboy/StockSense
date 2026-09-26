package com.stocksense.operation;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class StockOperationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockBalanceRepository stockBalanceRepository;

    @Autowired
    private StockOperationRepository stockOperationRepository;

    @Autowired
    private com.stocksense.ledger.StockLedgerRepository stockLedgerRepository;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        stockLedgerRepository.deleteAll();
        stockOperationRepository.deleteAll();
        stockBalanceRepository.deleteAll();
        productRepository.deleteAll();

        // Setup base test product
        testProduct = new Product(
                "Steel Flange 4-inch",
                "FLG-4IN-01",
                "Piping",
                "pcs",
                new BigDecimal("5.0000"),
                "Forged carbon steel flange",
                new BigDecimal("42.00")
        );
        testProduct = productRepository.save(testProduct);
        stockBalanceRepository.save(new StockBalance(testProduct, BigDecimal.ZERO));
    }

    // 1 & 2. Receipt increases stock by exact quantity and creates StockOperation with RECEIPT and positive quantityChange
    @Test
    void testReceipt_IncreasesStockAndCreatesReceiptRecord() throws Exception {
        CreateReceiptRequest request = new CreateReceiptRequest(
                testProduct.getId(), new BigDecimal("50.0000"), "PO-1001", "Received from Supplier");

        MvcResult result = mockMvc.perform(post("/api/operations/receipts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.operationType").value("RECEIPT"))
                .andExpect(jsonPath("$.quantity").value(50.0))
                .andExpect(jsonPath("$.quantityChange").value(50.0))
                .andExpect(jsonPath("$.resultingQuantity").value(50.0))
                .andExpect(jsonPath("$.reference").value("PO-1001"))
                .andReturn();

        StockOperationResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), StockOperationResponse.class);

        // Verify balance in database
        StockBalance balance = stockBalanceRepository.findByProductId(testProduct.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("50.0000").compareTo(balance.getQuantity()));

        // Verify operation saved in repository
        StockOperation op = stockOperationRepository.findById(response.getId()).orElseThrow();
        assertEquals(OperationType.RECEIPT, op.getOperationType());
        assertEquals(0, new BigDecimal("50.0000").compareTo(op.getQuantityChange()));
    }

    // 3 & 4. Delivery decreases stock by exact quantity and creates StockOperation with DELIVERY and negative quantityChange
    @Test
    void testDelivery_DecreasesStockAndCreatesDeliveryRecord() throws Exception {
        // Pre-fill stock with 50 units
        StockBalance balance = stockBalanceRepository.findByProductId(testProduct.getId()).orElseThrow();
        balance.setQuantity(new BigDecimal("50.0000"));
        stockBalanceRepository.save(balance);

        CreateDeliveryRequest request = new CreateDeliveryRequest(
                testProduct.getId(), new BigDecimal("12.0000"), "DO-2001", "Outbound to Client");

        MvcResult result = mockMvc.perform(post("/api/operations/deliveries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.operationType").value("DELIVERY"))
                .andExpect(jsonPath("$.quantity").value(12.0))
                .andExpect(jsonPath("$.quantityChange").value(-12.0))
                .andExpect(jsonPath("$.resultingQuantity").value(38.0))
                .andReturn();

        StockOperationResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), StockOperationResponse.class);

        // Verify balance is exactly 38.0000
        StockBalance updatedBalance = stockBalanceRepository.findByProductId(testProduct.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("38.0000").compareTo(updatedBalance.getQuantity()));

        // Verify operation record has negative quantityChange
        StockOperation op = stockOperationRepository.findById(response.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("-12.0000").compareTo(op.getQuantityChange()));
    }

    // 5. Delivery exceeding available stock returns 409 and leaves balance unchanged
    @Test
    void testDelivery_ExceedingAvailableStock_Returns409AndLeavesBalanceUnchanged() throws Exception {
        // Stock balance is currently 0.0000
        CreateDeliveryRequest request = new CreateDeliveryRequest(
                testProduct.getId(), new BigDecimal("10.0000"), "DO-EXCEED", null);

        mockMvc.perform(post("/api/operations/deliveries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Insufficient stock")));

        // Verify stock is still 0
        StockBalance balance = stockBalanceRepository.findByProductId(testProduct.getId()).orElseThrow();
        assertEquals(0, BigDecimal.ZERO.compareTo(balance.getQuantity()));

        // Verify no operation record was written
        assertEquals(0, stockOperationRepository.count());
    }

    // 6 & 7. Adjustment sets exact counted quantity and calculates difference (counted minus previous)
    @Test
    void testAdjustment_SetsExactQuantityAndCalculatesDifference() throws Exception {
        // Current stock: 20
        StockBalance balance = stockBalanceRepository.findByProductId(testProduct.getId()).orElseThrow();
        balance.setQuantity(new BigDecimal("20.0000"));
        stockBalanceRepository.save(balance);

        // Physical count reveals 35 units (difference: +15)
        CreateAdjustmentRequest request = new CreateAdjustmentRequest(
                testProduct.getId(), new BigDecimal("35.0000"), "COUNT-01", "Physical count adjustment");

        mockMvc.perform(post("/api/operations/adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.operationType").value("ADJUSTMENT"))
                .andExpect(jsonPath("$.quantity").value(35.0))
                .andExpect(jsonPath("$.quantityChange").value(15.0))
                .andExpect(jsonPath("$.resultingQuantity").value(35.0));

        StockBalance updatedBalance = stockBalanceRepository.findByProductId(testProduct.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("35.0000").compareTo(updatedBalance.getQuantity()));
    }

    // 8. Adjustment to zero works
    @Test
    void testAdjustment_ToZeroWorks() throws Exception {
        // Current stock: 18
        StockBalance balance = stockBalanceRepository.findByProductId(testProduct.getId()).orElseThrow();
        balance.setQuantity(new BigDecimal("18.0000"));
        stockBalanceRepository.save(balance);

        CreateAdjustmentRequest request = new CreateAdjustmentRequest(
                testProduct.getId(), BigDecimal.ZERO, "COUNT-ZERO", "Scrapped items");

        mockMvc.perform(post("/api/operations/adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantity").value(0))
                .andExpect(jsonPath("$.quantityChange").value(-18.0))
                .andExpect(jsonPath("$.resultingQuantity").value(0));

        StockBalance updatedBalance = stockBalanceRepository.findByProductId(testProduct.getId()).orElseThrow();
        assertEquals(0, BigDecimal.ZERO.compareTo(updatedBalance.getQuantity()));
    }

    // 9. Zero/negative receipt quantity is rejected
    @Test
    void testReceipt_ZeroOrNegativeQuantityRejected() throws Exception {
        CreateReceiptRequest zeroReq = new CreateReceiptRequest(
                testProduct.getId(), BigDecimal.ZERO, null, null);

        mockMvc.perform(post("/api/operations/receipts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zeroReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.quantity").exists());

        CreateReceiptRequest negReq = new CreateReceiptRequest(
                testProduct.getId(), new BigDecimal("-5.00"), null, null);

        mockMvc.perform(post("/api/operations/receipts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(negReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.quantity").exists());
    }

    // 10. Zero/negative delivery quantity is rejected
    @Test
    void testDelivery_ZeroOrNegativeQuantityRejected() throws Exception {
        CreateDeliveryRequest zeroReq = new CreateDeliveryRequest(
                testProduct.getId(), BigDecimal.ZERO, null, null);

        mockMvc.perform(post("/api/operations/deliveries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zeroReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.quantity").exists());

        CreateDeliveryRequest negReq = new CreateDeliveryRequest(
                testProduct.getId(), new BigDecimal("-1.00"), null, null);

        mockMvc.perform(post("/api/operations/deliveries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(negReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.quantity").exists());
    }

    // 11. Negative adjustment quantity is rejected
    @Test
    void testAdjustment_NegativeQuantityRejected() throws Exception {
        CreateAdjustmentRequest negReq = new CreateAdjustmentRequest(
                testProduct.getId(), new BigDecimal("-0.01"), null, null);

        mockMvc.perform(post("/api/operations/adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(negReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.countedQuantity").exists());
    }

    // 12. Missing product returns 404
    @Test
    void testOperations_MissingProductReturns404() throws Exception {
        CreateReceiptRequest req = new CreateReceiptRequest(99999L, BigDecimal.TEN, null, null);

        mockMvc.perform(post("/api/operations/receipts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // 13. Failed operations do not persist an operation record or partially update stock
    @Test
    void testFailedOperation_RollsBackCleanly() throws Exception {
        // Stock starts at 5.0
        StockBalance balance = stockBalanceRepository.findByProductId(testProduct.getId()).orElseThrow();
        balance.setQuantity(new BigDecimal("5.0000"));
        stockBalanceRepository.save(balance);

        CreateDeliveryRequest failingReq = new CreateDeliveryRequest(
                testProduct.getId(), new BigDecimal("100.0000"), null, null);

        mockMvc.perform(post("/api/operations/deliveries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(failingReq)))
                .andExpect(status().isConflict());

        // Stock remains untouched
        StockBalance checkBalance = stockBalanceRepository.findByProductId(testProduct.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("5.0000").compareTo(checkBalance.getQuantity()));

        // No operation was written
        assertEquals(0, stockOperationRepository.count());
    }

    // 14. GET list filters by product and operation type
    @Test
    void testGetOperations_FiltersByProductAndType() throws Exception {
        Product secondProduct = new Product(
                "Secondary Item", "SEC-01", "Piping", "pcs", BigDecimal.ZERO, null, null);
        secondProduct = productRepository.save(secondProduct);
        stockBalanceRepository.save(new StockBalance(secondProduct, BigDecimal.ZERO));

        // Create receipt on testProduct
        mockMvc.perform(post("/api/operations/receipts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateReceiptRequest(testProduct.getId(), BigDecimal.TEN, "REC-1", null))))
                .andExpect(status().isCreated());

        // Create delivery on testProduct
        mockMvc.perform(post("/api/operations/deliveries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateDeliveryRequest(testProduct.getId(), BigDecimal.valueOf(2), "DEL-1", null))))
                .andExpect(status().isCreated());

        // Create receipt on secondProduct
        mockMvc.perform(post("/api/operations/receipts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateReceiptRequest(secondProduct.getId(), BigDecimal.valueOf(15), "REC-2", null))))
                .andExpect(status().isCreated());

        // Total operations = 3
        mockMvc.perform(get("/api/operations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        // Filter by productId = testProduct.getId() -> 2 items
        mockMvc.perform(get("/api/operations?productId=" + testProduct.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // Filter by operationType = DELIVERY -> 1 item
        mockMvc.perform(get("/api/operations?type=DELIVERY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].reference").value("DEL-1"));

        // Filter by both productId and type
        mockMvc.perform(get("/api/operations?productId=" + secondProduct.getId() + "&type=RECEIPT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].reference").value("REC-2"));
    }

    // 15. GET by missing ID returns 404
    @Test
    void testGetOperationById_MissingReturns404() throws Exception {
        mockMvc.perform(get("/api/operations/88888"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // 16. A product with an operation history cannot be deleted, even when its balance is zero
    @Test
    void testProductWithOperationHistory_CannotBeDeletedEvenWhenBalanceIsZero() throws Exception {
        // 1. Add 10 units via receipt
        mockMvc.perform(post("/api/operations/receipts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateReceiptRequest(testProduct.getId(), BigDecimal.TEN, null, null))))
                .andExpect(status().isCreated());

        // 2. Adjust back down to 0 units
        mockMvc.perform(post("/api/operations/adjustments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateAdjustmentRequest(testProduct.getId(), BigDecimal.ZERO, null, null))))
                .andExpect(status().isCreated());

        // Current balance is 0
        StockBalance balance = stockBalanceRepository.findByProductId(testProduct.getId()).orElseThrow();
        assertEquals(0, BigDecimal.ZERO.compareTo(balance.getQuantity()));

        // 3. Attempt to delete product
        mockMvc.perform(delete("/api/products/" + testProduct.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("recorded stock movement transactions")));

        // Verify product still exists in catalog
        assertTrue(productRepository.existsById(testProduct.getId()));
    }
}
