package com.stocksense.config;

import com.stocksense.inventory.StockBalance;
import com.stocksense.inventory.StockBalanceRepository;
import com.stocksense.warehouse.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final WarehouseRepository warehouseRepository;
    private final LocationRepository locationRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final LocationStockBalanceRepository locationStockBalanceRepository;
    private final com.stocksense.auth.UserRepository userRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public DatabaseInitializer(WarehouseRepository warehouseRepository,
                               LocationRepository locationRepository,
                               StockBalanceRepository stockBalanceRepository,
                               LocationStockBalanceRepository locationStockBalanceRepository,
                               com.stocksense.auth.UserRepository userRepository,
                               JdbcTemplate jdbcTemplate) {
        this.warehouseRepository = warehouseRepository;
        this.locationRepository = locationRepository;
        this.stockBalanceRepository = stockBalanceRepository;
        this.locationStockBalanceRepository = locationStockBalanceRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("StockSense DatabaseInitializer: Initializing default warehouses and locations...");

        // 1. Ensure Default Warehouse
        Warehouse mainWh = warehouseRepository.findByCode(WarehouseService.DEFAULT_WAREHOUSE_CODE)
                .orElseGet(() -> {
                    Warehouse wh = new Warehouse(
                            "Main Central Warehouse",
                            WarehouseService.DEFAULT_WAREHOUSE_CODE,
                            "100 Logistics Blvd, Industrial Zone"
                    );
                    return warehouseRepository.save(wh);
                });

        // 2. Ensure Default and Secondary Locations
        Location generalLoc = locationRepository.findByCode(LocationService.DEFAULT_LOCATION_CODE)
                .orElseGet(() -> {
                    Location loc = new Location(
                            "General Stock",
                            LocationService.DEFAULT_LOCATION_CODE,
                            mainWh,
                            LocationType.INTERNAL
                    );
                    return locationRepository.save(loc);
                });

        locationRepository.findByCode("LOC-MAIN-PROD")
                .orElseGet(() -> {
                    Location loc = new Location(
                            "Production Floor",
                            "LOC-MAIN-PROD",
                            mainWh,
                            LocationType.INTERNAL
                    );
                    return locationRepository.save(loc);
                });

        locationRepository.findByCode("LOC-MAIN-DISP")
                .orElseGet(() -> {
                    Location loc = new Location(
                            "Dispatch Dock",
                            "LOC-MAIN-DISP",
                            mainWh,
                            LocationType.INTERNAL
                    );
                    return locationRepository.save(loc);
                });

        // 3. Ensure location stock balances exist for all products in the default location
        List<StockBalance> balances = stockBalanceRepository.findAll();
        int seededCount = 0;
        for (StockBalance sb : balances) {
            if (locationStockBalanceRepository.findByProductIdAndLocationId(sb.getProduct().getId(), generalLoc.getId()).isEmpty()) {
                LocationStockBalance locBal = new LocationStockBalance(sb.getProduct(), generalLoc, sb.getQuantity());
                locationStockBalanceRepository.save(locBal);
                seededCount++;
            }
        }
        if (seededCount > 0) {
            log.info("StockSense DatabaseInitializer: Successfully initialized {} location stock balance(s) in default location.", seededCount);
        }

        // 4. Ensure Default Manager and Worker accounts
        if (userRepository.findByEmailIgnoreCase("manager@stocksense.io").isEmpty()) {
            com.stocksense.auth.User manager = new com.stocksense.auth.User(
                    "manager@stocksense.io",
                    passwordEncoder.encode("ManagerPassword123!"),
                    "StockSense Manager",
                    com.stocksense.auth.User.ROLE_MANAGER
            );
            userRepository.save(manager);
            log.info("StockSense DatabaseInitializer: Seeded default MANAGER user 'manager@stocksense.io'.");
        }

        if (userRepository.findByEmailIgnoreCase("worker@stocksense.io").isEmpty()) {
            com.stocksense.auth.User worker = new com.stocksense.auth.User(
                    "worker@stocksense.io",
                    passwordEncoder.encode("WorkerPassword123!"),
                    "Warehouse Worker",
                    com.stocksense.auth.User.ROLE_WORKER
            );
            userRepository.save(worker);
            log.info("StockSense DatabaseInitializer: Seeded default WORKER user 'worker@stocksense.io'.");
        }

        log.info("StockSense DatabaseInitializer: Initialization complete.");
    }
}
