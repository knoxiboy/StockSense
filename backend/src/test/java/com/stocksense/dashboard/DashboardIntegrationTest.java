package com.stocksense.dashboard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.stocksense.auth.TokenService tokenService;

    @Autowired
    private org.springframework.web.context.WebApplicationContext webApplicationContext;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        String token = tokenService.generateToken(1L, "manager@stocksense.io", "MANAGER");
        mockMvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .defaultRequest(get("/").header("Authorization", "Bearer " + token))
                .build();
    }

    @Test
    void testGetDashboardStats() throws Exception {
        mockMvc.perform(get("/api/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProducts", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.totalProductsInStock", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.lowStockProducts", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.outOfStockProducts", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.pendingReceipts", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.pendingDeliveries", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.scheduledTransfers", greaterThanOrEqualTo(0)));
    }
}
