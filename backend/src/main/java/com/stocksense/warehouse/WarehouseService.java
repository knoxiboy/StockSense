package com.stocksense.warehouse;

import com.stocksense.common.exception.ConflictException;
import com.stocksense.common.exception.DuplicateResourceException;
import com.stocksense.common.exception.ResourceNotFoundException;
import com.stocksense.warehouse.dto.CreateWarehouseRequest;
import com.stocksense.warehouse.dto.UpdateWarehouseRequest;
import com.stocksense.warehouse.dto.WarehouseResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WarehouseService {

    public static final String DEFAULT_WAREHOUSE_CODE = "WH-MAIN";

    private final WarehouseRepository warehouseRepository;
    private final LocationRepository locationRepository;

    public WarehouseService(WarehouseRepository warehouseRepository,
                            LocationRepository locationRepository) {
        this.warehouseRepository = warehouseRepository;
        this.locationRepository = locationRepository;
    }

    @Transactional(readOnly = true)
    public List<WarehouseResponse> getAllWarehouses() {
        return warehouseRepository.findAll().stream()
                .map(WarehouseResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WarehouseResponse getWarehouseById(Long id) {
        Warehouse warehouse = findEntityById(id);
        return WarehouseResponse.fromEntity(warehouse);
    }

    @Transactional
    public WarehouseResponse createWarehouse(CreateWarehouseRequest request) {
        String code = request.getCode().trim().toUpperCase();
        if (warehouseRepository.existsByCode(code)) {
            throw new DuplicateResourceException("Warehouse", "code", code);
        }

        Warehouse warehouse = new Warehouse(
                request.getName(),
                code,
                request.getAddress()
        );
        Warehouse saved = warehouseRepository.save(warehouse);
        return WarehouseResponse.fromEntity(saved);
    }

    @Transactional
    public WarehouseResponse updateWarehouse(Long id, UpdateWarehouseRequest request) {
        Warehouse warehouse = findEntityById(id);
        String code = request.getCode().trim().toUpperCase();

        if (warehouseRepository.existsByCodeAndIdNot(code, id)) {
            throw new DuplicateResourceException("Warehouse", "code", code);
        }

        warehouse.setName(request.getName().trim());
        warehouse.setCode(code);
        warehouse.setAddress(request.getAddress() != null ? request.getAddress().trim() : null);
        if (request.getActive() != null) {
            warehouse.setActive(request.getActive());
        }

        Warehouse updated = warehouseRepository.save(warehouse);
        return WarehouseResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteWarehouse(Long id) {
        Warehouse warehouse = findEntityById(id);

        List<Location> locations = locationRepository.findByWarehouseId(id);
        if (!locations.isEmpty()) {
            throw new ConflictException(String.format(
                    "Cannot delete warehouse '%s' (Code: %s) because it has %d associated location(s). Please reassign or delete locations first.",
                    warehouse.getName(), warehouse.getCode(), locations.size()
            ));
        }

        warehouseRepository.delete(warehouse);
    }

    @Transactional(readOnly = true)
    public Warehouse findEntityById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Warehouse ID cannot be null");
        }
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", id));
    }

    @Transactional
    public Warehouse getOrCreateDefaultWarehouse() {
        return warehouseRepository.findByCode(DEFAULT_WAREHOUSE_CODE)
                .orElseGet(() -> {
                    Warehouse wh = new Warehouse("Main Central Warehouse", DEFAULT_WAREHOUSE_CODE, "Primary Logistics & Storage Hub");
                    return warehouseRepository.save(wh);
                });
    }
}
