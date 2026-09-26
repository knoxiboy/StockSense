package com.stocksense.transfer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocksense.inventory.StockBalanceRepository;
import com.stocksense.operation.StockOperationService;
import com.stocksense.operation.dto.CreateReceiptRequest;
import com.stocksense.product.Product;
import com.stocksense.product.ProductRepository;
import com.stocksense.transfer.dto.CreateTransferRequest;
import com.stocksense.warehouse.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InternalTransferIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockBalanceRepository stockBalanceRepository;

    @Autowired
    private LocationStockBalanceRepository locationStockBalanceRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private StockOperationService stockOperationService;

    private Product product;
    private Location sourceLoc;
    private Location destLoc;

    @BeforeEach
    void setUp() {
        Warehouse wh = warehouseRepository.findByCode("WH-TR-TEST").orElseGet(() ->
                warehouseRepository.save(new Warehouse("Transfer WH", "WH-TR-TEST", "Transfer Zone")));

        sourceLoc = locationRepository.findByCode("LOC-SRC-01").orElseGet(() ->
                locationRepository.save(new Location("Source Shelf", "LOC-SRC-01", wh, LocationType.INTERNAL)));

        destLoc = locationRepository.findByCode("LOC-DST-01").orElseGet(() ->
                locationRepository.save(new Location("Dest Shelf", "LOC-DST-01", wh, LocationType.INTERNAL)));

        String uniqueSku = "TR-PROD-" + System.nanoTime();
        Product p = new Product("Transfer Item", uniqueSku, "Hardware", "pcs", BigDecimal.valueOf(5), null, BigDecimal.TEN);
        product = productRepository.save(p);
        stockBalanceRepository.save(new com.stocksense.inventory.StockBalance(product, BigDecimal.ZERO));

        // Initialize source stock to 50 pcs
        CreateReceiptRequest receipt = new CreateReceiptRequest(product.getId(), BigDecimal.valueOf(50), "INIT-SRC-" + System.nanoTime(), "Init source stock");
        receipt.setLocationId(sourceLoc.getId());
        stockOperationService.createReceipt(receipt);
    }

    @Test
    void testSuccessfulTransfer_DecreasesSource_IncreasesDest_PreservesTotal() throws Exception {
        BigDecimal totalBefore = stockBalanceRepository.findByProductId(product.getId()).orElseThrow().getQuantity();

        String ref = "TR-TEST-" + System.nanoTime();
        CreateTransferRequest request = new CreateTransferRequest(
                ref,
                product.getId(),
                sourceLoc.getId(),
                destLoc.getId(),
                BigDecimal.valueOf(20),
                "Moving inventory to front shelf"
        );

        String response = mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reference").value(ref))
                .andExpect(jsonPath("$.status").value("READY"))
                .andReturn().getResponse().getContentAsString();

        Long transferId = objectMapper.readTree(response).get("id").asLong();

        // Execute transfer
        mockMvc.perform(post("/api/transfers/" + transferId + "/execute"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));

        // Verify total stock is completely unchanged (Invariance Law)
        BigDecimal totalAfter = stockBalanceRepository.findByProductId(product.getId()).orElseThrow().getQuantity();
        assertThat(totalAfter).isEqualByComparingTo(totalBefore);

        // Verify source stock decreased by 20
        LocationStockBalance srcBal = locationStockBalanceRepository.findByProductIdAndLocationId(product.getId(), sourceLoc.getId()).orElseThrow();
        assertThat(srcBal.getQuantity()).isEqualByComparingTo(BigDecimal.valueOf(30));

        // Verify destination stock increased by 20
        LocationStockBalance dstBal = locationStockBalanceRepository.findByProductIdAndLocationId(product.getId(), destLoc.getId()).orElseThrow();
        assertThat(dstBal.getQuantity()).isEqualByComparingTo(BigDecimal.valueOf(20));

        // Prevent duplicate execution
        mockMvc.perform(post("/api/transfers/" + transferId + "/execute"))
                .andExpect(status().isConflict());
    }

    @Test
    void testInsufficientStockTransfer_Rejected() throws Exception {
        CreateTransferRequest request = new CreateTransferRequest(
                "TR-FAIL-001",
                product.getId(),
                sourceLoc.getId(),
                destLoc.getId(),
                BigDecimal.valueOf(9999), // Exceeds available 50
                "Too much quantity"
        );

        String response = mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long transferId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(post("/api/transfers/" + transferId + "/execute"))
                .andExpect(status().isConflict());
    }

    @Test
    void testSameSourceAndDestination_Rejected() throws Exception {
        CreateTransferRequest request = new CreateTransferRequest(
                "TR-SAME-001",
                product.getId(),
                sourceLoc.getId(),
                sourceLoc.getId(), // Same
                BigDecimal.valueOf(10),
                "Invalid same location"
        );

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
