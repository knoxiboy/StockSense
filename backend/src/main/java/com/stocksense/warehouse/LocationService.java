package com.stocksense.warehouse;

import com.stocksense.common.exception.ConflictException;
import com.stocksense.common.exception.DuplicateResourceException;
import com.stocksense.common.exception.ResourceNotFoundException;
import com.stocksense.warehouse.dto.CreateLocationRequest;
import com.stocksense.warehouse.dto.LocationResponse;
import com.stocksense.warehouse.dto.UpdateLocationRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LocationService {

    public static final String DEFAULT_LOCATION_CODE = "LOC-MAIN-GEN";

    private final LocationRepository locationRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseService warehouseService;

    public LocationService(LocationRepository locationRepository,
                           WarehouseRepository warehouseRepository,
                           WarehouseService warehouseService) {
        this.locationRepository = locationRepository;
        this.warehouseRepository = warehouseRepository;
        this.warehouseService = warehouseService;
    }

    @Transactional(readOnly = true)
    public List<LocationResponse> getAllLocations(Long warehouseId) {
        List<Location> locations;
        if (warehouseId != null) {
            locations = locationRepository.findByWarehouseId(warehouseId);
        } else {
            locations = locationRepository.findAllWithWarehouse();
        }
        return locations.stream()
                .map(LocationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LocationResponse getLocationById(Long id) {
        Location location = findEntityById(id);
        return LocationResponse.fromEntity(location);
    }

    @Transactional
    public LocationResponse createLocation(CreateLocationRequest request) {
        String code = request.getCode().trim().toUpperCase();
        if (locationRepository.existsByCode(code)) {
            throw new DuplicateResourceException("Location", "code", code);
        }

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", request.getWarehouseId()));

        Location location = new Location(
                request.getName(),
                code,
                warehouse,
                request.getLocationType()
        );
        Location saved = locationRepository.save(location);
        return LocationResponse.fromEntity(saved);
    }

    @Transactional
    public LocationResponse updateLocation(Long id, UpdateLocationRequest request) {
        Location location = findEntityById(id);
        String code = request.getCode().trim().toUpperCase();

        if (locationRepository.existsByCodeAndIdNot(code, id)) {
            throw new DuplicateResourceException("Location", "code", code);
        }

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", request.getWarehouseId()));

        location.setName(request.getName().trim());
        location.setCode(code);
        location.setWarehouse(warehouse);
        if (request.getLocationType() != null) {
            location.setLocationType(request.getLocationType());
        }
        if (request.getActive() != null) {
            location.setActive(request.getActive());
        }

        Location updated = locationRepository.save(location);
        return LocationResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteLocation(Long id) {
        Location location = findEntityById(id);
        if (DEFAULT_LOCATION_CODE.equals(location.getCode())) {
            throw new ConflictException("Cannot delete default system stock location: " + DEFAULT_LOCATION_CODE);
        }
        locationRepository.delete(location);
    }

    @Transactional(readOnly = true)
    public Location findEntityById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Location ID cannot be null");
        }
        return locationRepository.findWithWarehouseById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location", "id", id));
    }

    @Transactional
    public Location getOrCreateDefaultLocation() {
        return locationRepository.findByCode(DEFAULT_LOCATION_CODE)
                .orElseGet(() -> {
                    Warehouse defaultWh = warehouseService.getOrCreateDefaultWarehouse();
                    Location loc = new Location("General Stock", DEFAULT_LOCATION_CODE, defaultWh, LocationType.INTERNAL);
                    return locationRepository.save(loc);
                });
    }
}
