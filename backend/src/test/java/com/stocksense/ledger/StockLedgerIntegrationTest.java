package com.stocksense.ledger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocksense.inventory.StockBalance;
import com.stocksense.inventory.StockBalanceRepository;
import com.stocksense.ledger.dto.StockLedgerEntryResponse;
import com.stocksense.operation.OperationType;
import com.stocksense.operation.StockOperation;
import com.stocksense.operation.StockOperationRepository;
import com.stocksense.operation.dto.CreateAdjustmentRequest;
import com.stocksense.operation.dto.CreateDeliveryRequest;
import com.stocksense.operation.dto.CreateReceiptRequest;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class StockLedgerIntegrationTest {

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
    private StockLedgerRepository stockLedgerRepository;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        stockLedgerRepository.deleteAll();
        stockOperationRepository.deleteAll();
        stockBalanceRepository.deleteAll();
        productRepository.deleteAll();

        testProduct = new Product(
                "Industrial Valve 2-inch",
                "VLV-2IN-01",
                "Valves",
                "pcs",
                new BigDecimal("5.0000"),
                "High pressure ball valve",
                new BigDecimal("85.00")
        );
        testProduct = productRepository.save(testProduct);
        stockBalanceRepository.save(new StockBalance(testProduct, BigDecimal.ZERO));
    }

    // 1 & 2. A receipt creates exactly one ledger entry and receipt ledger change is positive
    @Test
    void testReceipt_CreatesExactlyOnePositiveLedgerEntry() throws Exception {
        CreateReceiptRequest request = new CreateReceiptRequest(
                testProduct.getId(), new BigDecimal("25.0000"), "PO-REC-1", "First shipment");

        mockMvc.perform(post("/api/operations/receipts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        List<StockLedgerEntry> entries = stockLedgerRepository.findAll();
        assertEquals(1, entries.size(), "Receipt must create exactly one ledger entry");

        StockLedgerEntry entry = entries.get(0);
        assertEquals(OperationType.RECEIPT, entry.getOperationType());
        assertTrue(entry.getQuantityChange().compareTo(BigDecimal.ZERO) > 0, "Receipt change must be positive");
        assertEquals(0, new BigDecimal("25.0000").compareTo(entry.getQuantityChange()));
        assertEquals(0, BigDecimal.ZERO.compareTo(entry.getPreviousQuantity()));
        assertEquals(0, new BigDecimal("25.0000").compareTo(entry.getResultingQuantity()));

        // Invariant check: previous + change = result
        assertEquals(0, entry.getPreviousQuantity().add(entry.getQuantityChange()).compareTo(entry.getResultingQuantity()));
    }

    // 3 & 4. A delivery creates exactly one ledger entry and delivery ledger change is negative
    @Test
    void testDelivery_CreatesExactlyOneNegativeLedgerEntry() throws Exception {
        // Pre-fill stock with 30 units
        StockBalance balance = stockBalanceRepository.findByProductId(testProduct.getId()).orElseThrow();
        balance.setQuantity(new BigDecimal("30.0000"));
        stockBalanceRepository.save(balance);

        CreateDeliveryRequest request = new CreateDeliveryRequest(
                testProduct.getId(), new BigDecimal("10.0000"), "DO-DEL-1", "Outbound client delivery");

        mockMvc.perform(post("/api/operations/deliveries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        List<StockLedgerEntry> entries = stockLedgerRepository.findAll();
        assertEquals(1, entries.size(), "Delivery must create exactly one ledger entry");

        StockLedgerEntry entry = entries.get(0);
        assertEquals(OperationType.DELIVERY, entry.getOperationType());
        assertTrue(entry.getQuantityChange().compareTo(BigDecimal.ZERO) < 0, "Delivery change must be negative");
        assertEquals(0, new BigDecimal("-10.0000").compareTo(entry.getQuantityChange()));
        assertEquals(0, new BigDecimal("30.0000").compareTo(entry.getPreviousQuantity()));
        assertEquals(0, new BigDecimal("20.0000").compareTo(entry.getResultingQuantity()));

        // Invariant check: previous + change = result
        assertEquals(0, entry.getPreviousQuantity().add(entry.getQuantityChange()).compareTo(entry.getResultingQuantity()));
    }

    // 5. An adjustment records countedQuantity minus previousQuantity
    @Test
    void testAdjustment_RecordsCountedMinusPreviousQuantity() throws Exception {
        StockBalance balance = stockBalanceRepository.findByProductId(testProduct.getId()).orElseThrow();
        balance.setQuantity(new BigDecimal("20.0000"));
        stockBalanceRepository.save(balance);

        CreateAdjustmentRequest request = new CreateAdjustmentRequest(
                testProduct.getId(), new BigDecimal("17.0000"), "ADJ-001", "Physical count adjustment");

        mockMvc.perform(post("/api/operations/adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        List<StockLedgerEntry> entries = stockLedgerRepository.findAll();
        assertEquals(1, entries.size());

        StockLedgerEntry entry = entries.get(0);
        assertEquals(OperationType.ADJUSTMENT, entry.getOperationType());
        // change = 17 - 20 = -3
        assertEquals(0, new BigDecimal("-3.0000").compareTo(entry.getQuantityChange()));
        assertEquals(0, new BigDecimal("20.0000").compareTo(entry.getPreviousQuantity()));
        assertEquals(0, new BigDecimal("17.0000").compareTo(entry.getResultingQuantity()));

        // Invariant check
        assertEquals(0, entry.getPreviousQuantity().add(entry.getQuantityChange()).compareTo(entry.getResultingQuantity()));
    }

    // 6. An adjustment to zero is recorded correctly
    @Test
    void testAdjustment_ToZeroRecordedCorrectly() throws Exception {
        StockBalance balance = stockBalanceRepository.findByProductId(testProduct.getId()).orElseThrow();
        balance.setQuantity(new BigDecimal("15.0000"));
        stockBalanceRepository.save(balance);

        CreateAdjustmentRequest request = new CreateAdjustmentRequest(
                testProduct.getId(), BigDecimal.ZERO, "ADJ-ZERO", "Inventory cleared");

        mockMvc.perform(post("/api/operations/adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        List<StockLedgerEntry> entries = stockLedgerRepository.findAll();
        assertEquals(1, entries.size());

        StockLedgerEntry entry = entries.get(0);
        assertEquals(0, new BigDecimal("-15.0000").compareTo(entry.getQuantityChange()));
        assertEquals(0, new BigDecimal("15.0000").compareTo(entry.getPreviousQuantity()));
        assertEquals(0, BigDecimal.ZERO.compareTo(entry.getResultingQuantity()));

        // Invariant check
        assertEquals(0, entry.getPreviousQuantity().add(entry.getQuantityChange()).compareTo(entry.getResultingQuantity()));
    }

    // 7. An adjustment with no change records zero change and correct balances
    @Test
    void testAdjustment_WithNoChangeRecordsZeroChangeAndCorrectBalances() throws Exception {
        StockBalance balance = stockBalanceRepository.findByProductId(testProduct.getId()).orElseThrow();
        balance.setQuantity(new BigDecimal("20.0000"));
        stockBalanceRepository.save(balance);

        CreateAdjustmentRequest request = new CreateAdjustmentRequest(
                testProduct.getId(), new BigDecimal("20.0000"), "ADJ-SAME", "Count verified, matches exactly");

        mockMvc.perform(post("/api/operations/adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        List<StockLedgerEntry> entries = stockLedgerRepository.findAll();
        assertEquals(1, entries.size());

        StockLedgerEntry entry = entries.get(0);
        assertEquals(0, BigDecimal.ZERO.compareTo(entry.getQuantityChange()));
        assertEquals(0, new BigDecimal("20.0000").compareTo(entry.getPreviousQuantity()));
        assertEquals(0, new BigDecimal("20.0000").compareTo(entry.getResultingQuantity()));

        // Invariant check
        assertEquals(0, entry.getPreviousQuantity().add(entry.getQuantityChange()).compareTo(entry.getResultingQuantity()));
    }

    // 8 & 9. Invariants: previousQuantity + quantityChange equals resultingQuantity, resultingQuantity equals balance after operation
    @Test
    void testLedgerInvariants_AcrossMultipleOperations() throws Exception {
        // Step 1: Receipt 50 (0 -> 50)
        mockMvc.perform(post("/api/operations/receipts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateReceiptRequest(testProduct.getId(), new BigDecimal("50.0000"), "R1", null))))
                .andExpect(status().isCreated());

        // Step 2: Delivery 15 (50 -> 35)
        mockMvc.perform(post("/api/operations/deliveries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateDeliveryRequest(testProduct.getId(), new BigDecimal("15.0000"), "D1", null))))
                .andExpect(status().isCreated());

        // Step 3: Adjustment to 40 (35 -> 40, change +5)
        mockMvc.perform(post("/api/operations/adjustments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateAdjustmentRequest(testProduct.getId(), new BigDecimal("40.0000"), "A1", null))))
                .andExpect(status().isCreated());

        List<StockLedgerEntry> allEntries = stockLedgerRepository.findAll();
        assertEquals(3, allEntries.size());

        StockBalance currentBalance = stockBalanceRepository.findByProductId(testProduct.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("40.0000").compareTo(currentBalance.getQuantity()));

        for (StockLedgerEntry e : allEntries) {
            // Invariant 1: previousQuantity + quantityChange = resultingQuantity
            assertEquals(0, e.getPreviousQuantity().add(e.getQuantityChange()).compareTo(e.getResultingQuantity()),
                    "Entry " + e.getId() + " violated arithmetic invariant: " + e.getPreviousQuantity() + " + " + e.getQuantityChange() + " != " + e.getResultingQuantity());
        }

        // The latest entry resultingQuantity must equal current balance
        StockLedgerEntry latestEntry = allEntries.stream()
                .filter(e -> e.getOperationType() == OperationType.ADJUSTMENT)
                .findFirst().orElseThrow();
        assertEquals(0, currentBalance.getQuantity().compareTo(latestEntry.getResultingQuantity()));
    }

    // 10 & 11. A failed excessive delivery creates no ledger entry, does not update balance or operation history
    @Test
    void testFailedExcessiveDelivery_CreatesNoLedgerEntryAndRollsBackCleanly() throws Exception {
        StockBalance balance = stockBalanceRepository.findByProductId(testProduct.getId()).orElseThrow();
        balance.setQuantity(new BigDecimal("10.0000"));
        stockBalanceRepository.save(balance);

        CreateDeliveryRequest failingReq = new CreateDeliveryRequest(
                testProduct.getId(), new BigDecimal("50.0000"), "FAIL-DO", null);

        mockMvc.perform(post("/api/operations/deliveries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(failingReq)))
                .andExpect(status().isConflict());

        // Check ledger is empty
        assertEquals(0, stockLedgerRepository.count());
        // Check operations is empty
        assertEquals(0, stockOperationRepository.count());
        // Check balance is untouched
        StockBalance currentBalance = stockBalanceRepository.findByProductId(testProduct.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("10.0000").compareTo(currentBalance.getQuantity()));
    }

    // 12. A ledger persistence failure rolls back the stock and operation changes (unique operation constraint)
    @Test
    void testLedgerDuplicateOperationConstraint_RollsBackTransaction() {
        StockOperation dummyOp = new StockOperation(
                OperationType.RECEIPT, testProduct, new BigDecimal("10.0000"), new BigDecimal("10.0000"), "REF-1", "Notes");
        dummyOp = stockOperationRepository.save(dummyOp);

        StockLedgerEntry entry1 = new StockLedgerEntry(
                dummyOp, testProduct, OperationType.RECEIPT, new BigDecimal("10.0000"), BigDecimal.ZERO, new BigDecimal("10.0000"));
        stockLedgerRepository.saveAndFlush(entry1);

        // Attempting to save another ledger entry with the SAME operation must fail due to unique constraint
        StockLedgerEntry entry2 = new StockLedgerEntry(
                dummyOp, testProduct, OperationType.RECEIPT, new BigDecimal("10.0000"), BigDecimal.ZERO, new BigDecimal("10.0000"));

        assertThrows(Exception.class, () -> {
            stockLedgerRepository.saveAndFlush(entry2);
        }, "Unique constraint on operation_id must reject duplicate ledger entries for the same operation");
    }

    // 13. Filters work for product and operation type
    @Test
    void testGetLedger_FiltersByProductAndType() throws Exception {
        Product secondProduct = new Product(
                "Secondary Gasket", "GSK-02", "Valves", "pcs", BigDecimal.ZERO, null, null);
        secondProduct = productRepository.save(secondProduct);
        stockBalanceRepository.save(new StockBalance(secondProduct, BigDecimal.ZERO));

        // Operation 1: Receipt on testProduct
        mockMvc.perform(post("/api/operations/receipts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateReceiptRequest(testProduct.getId(), BigDecimal.TEN, "REC-1", null))))
                .andExpect(status().isCreated());

        // Operation 2: Delivery on testProduct
        mockMvc.perform(post("/api/operations/deliveries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateDeliveryRequest(testProduct.getId(), BigDecimal.valueOf(3), "DEL-1", null))))
                .andExpect(status().isCreated());

        // Operation 3: Receipt on secondProduct
        mockMvc.perform(post("/api/operations/receipts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateReceiptRequest(secondProduct.getId(), BigDecimal.valueOf(12), "REC-2", null))))
                .andExpect(status().isCreated());

        // Total ledger entries = 3
        mockMvc.perform(get("/api/ledger"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(header().string("X-Total-Count", "3"));

        // Filter by productId = testProduct.getId() -> 2 items
        mockMvc.perform(get("/api/ledger?productId=" + testProduct.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(header().string("X-Total-Count", "2"));

        // Filter by operationType = DELIVERY -> 1 item
        mockMvc.perform(get("/api/ledger?type=DELIVERY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].operationType").value("DELIVERY"))
                .andExpect(jsonPath("$[0].reference").value("DEL-1"));

        // Filter by both productId and type
        mockMvc.perform(get("/api/ledger?productId=" + secondProduct.getId() + "&type=RECEIPT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].productId").value(secondProduct.getId()))
                .andExpect(jsonPath("$[0].reference").value("REC-2"));
    }

    // 14. Date filters work, including boundaries
    @Test
    void testGetLedger_DateFiltersWithBoundaries() throws Exception {
        // Create an operation today
        mockMvc.perform(post("/api/operations/receipts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateReceiptRequest(testProduct.getId(), BigDecimal.TEN, "REC-TODAY", null))))
                .andExpect(status().isCreated());

        LocalDate today = LocalDate.now();
        String todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String yesterdayStr = today.minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        String tomorrowStr = today.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);

        // Exact match on today boundary [today, today]
        mockMvc.perform(get("/api/ledger?from=" + todayStr + "&to=" + todayStr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].reference").value("REC-TODAY"));

        // Match with wider boundary [yesterday, tomorrow]
        mockMvc.perform(get("/api/ledger?from=" + yesterdayStr + "&to=" + tomorrowStr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // Date in the future [tomorrow, tomorrow + 2] -> 0 results
        String futureStr = today.plusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE);
        mockMvc.perform(get("/api/ledger?from=" + tomorrowStr + "&to=" + futureStr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // Date in the past [yesterday - 2, yesterday] -> 0 results
        String pastStr = today.minusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE);
        mockMvc.perform(get("/api/ledger?from=" + pastStr + "&to=" + yesterdayStr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // 15. Pagination and newest-first sorting work
    @Test
    void testGetLedger_PaginationAndNewestFirstSorting() throws Exception {
        // Create 3 operations sequentially
        mockMvc.perform(post("/api/operations/receipts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateReceiptRequest(testProduct.getId(), BigDecimal.valueOf(10), "ORDER-1", null))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/operations/receipts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateReceiptRequest(testProduct.getId(), BigDecimal.valueOf(20), "ORDER-2", null))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/operations/receipts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateReceiptRequest(testProduct.getId(), BigDecimal.valueOf(30), "ORDER-3", null))))
                .andExpect(status().isCreated());

        // Page 0, size 2 -> should return newest two: ORDER-3 then ORDER-2
        mockMvc.perform(get("/api/ledger?page=0&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].reference").value("ORDER-3"))
                .andExpect(jsonPath("$[1].reference").value("ORDER-2"))
                .andExpect(header().string("X-Total-Count", "3"))
                .andExpect(header().string("X-Total-Pages", "2"))
                .andExpect(header().string("X-Current-Page", "0"));

        // Page 1, size 2 -> should return remaining: ORDER-1
        mockMvc.perform(get("/api/ledger?page=1&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].reference").value("ORDER-1"))
                .andExpect(header().string("X-Current-Page", "1"));
    }

    // 16. Missing ledger ID returns 404
    @Test
    void testGetLedgerEntryById_MissingReturns404() throws Exception {
        mockMvc.perform(get("/api/ledger/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not found")));
    }

    // 17. Invalid date ranges and pagination return 400
    @Test
    void testGetLedger_InvalidDateRangesAndPaginationReturn400() throws Exception {
        LocalDate today = LocalDate.now();
        String todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String yesterdayStr = today.minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);

        // from > to (today > yesterday)
        mockMvc.perform(get("/api/ledger?from=" + todayStr + "&to=" + yesterdayStr))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("'from' date cannot be after 'to' date")));

        // Negative page
        mockMvc.perform(get("/api/ledger?page=-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Page index cannot be negative")));

        // Size <= 0
        mockMvc.perform(get("/api/ledger?size=0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Page size must be between 1 and 100")));

        // Size > 100
        mockMvc.perform(get("/api/ledger?size=101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Page size must be between 1 and 100")));
    }

    // Single item retrieval returns complete DTO structure
    @Test
    void testGetLedgerEntryById_ReturnsCompleteDto() throws Exception {
        CreateReceiptRequest request = new CreateReceiptRequest(
                testProduct.getId(), new BigDecimal("15.5000"), "REF-FULL", "Detailed receipt notes");

        mockMvc.perform(post("/api/operations/receipts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        StockLedgerEntry entry = stockLedgerRepository.findAll().get(0);

        mockMvc.perform(get("/api/ledger/" + entry.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(entry.getId()))
                .andExpect(jsonPath("$.operationId").value(entry.getOperation().getId()))
                .andExpect(jsonPath("$.productId").value(testProduct.getId()))
                .andExpect(jsonPath("$.productName").value("Industrial Valve 2-inch"))
                .andExpect(jsonPath("$.sku").value("VLV-2IN-01"))
                .andExpect(jsonPath("$.operationType").value("RECEIPT"))
                .andExpect(jsonPath("$.quantityChange").value(15.5))
                .andExpect(jsonPath("$.previousQuantity").value(0.0))
                .andExpect(jsonPath("$.resultingQuantity").value(15.5))
                .andExpect(jsonPath("$.reference").value("REF-FULL"))
                .andExpect(jsonPath("$.notes").value("Detailed receipt notes"))
                .andExpect(jsonPath("$.createdAt").exists());
    }
}
