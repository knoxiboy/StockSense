package com.stocksense.warehouse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocksense.warehouse.dto.CreateLocationRequest;
import com.stocksense.warehouse.dto.CreateWarehouseRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class WarehouseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private com.stocksense.auth.TokenService tokenService;

    @Autowired
    private org.springframework.web.context.WebApplicationContext webApplicationContext;

    @BeforeEach
    void setUp() {
        String token = tokenService.generateToken(1L, "manager@stocksense.io", "MANAGER");
        mockMvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .defaultRequest(get("/").header("Authorization", "Bearer " + token))
                .build();
    }

    @Test
    void testCreateAndListWarehouseAndLocations() throws Exception {
        CreateWarehouseRequest whReq = new CreateWarehouseRequest("North Logistics Center", "WH-NORTH", "100 Highway Rd");

        String whResponse = mockMvc.perform(post("/api/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(whReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.code").value("WH-NORTH"))
                .andExpect(jsonPath("$.name").value("North Logistics Center"))
                .andReturn().getResponse().getContentAsString();

        Long whId = objectMapper.readTree(whResponse).get("id").asLong();

        // Create Location in North Warehouse
        CreateLocationRequest locReq = new CreateLocationRequest("Bin A-01", "LOC-NORTH-A01", whId, LocationType.INTERNAL);

        mockMvc.perform(post("/api/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(locReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("LOC-NORTH-A01"))
                .andExpect(jsonPath("$.warehouseId").value(whId));

        // Get Locations for Warehouse
        mockMvc.perform(get("/api/locations?warehouseId=" + whId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[?(@.code == 'LOC-NORTH-A01')]").exists());
    }

    @Test
    void testDuplicateWarehouseCodeRejected() throws Exception {
        CreateWarehouseRequest wh1 = new CreateWarehouseRequest("Hub One", "WH-DUP", "Street 1");
        mockMvc.perform(post("/api/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wh1)))
                .andExpect(status().isCreated());

        CreateWarehouseRequest wh2 = new CreateWarehouseRequest("Hub Two", "WH-DUP", "Street 2");
        mockMvc.perform(post("/api/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wh2)))
                .andExpect(status().isConflict());
    }
}
