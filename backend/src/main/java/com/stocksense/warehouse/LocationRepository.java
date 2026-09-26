package com.stocksense.warehouse;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

    Optional<Location> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    List<Location> findByWarehouseId(Long warehouseId);

    List<Location> findByWarehouseIdAndActiveTrue(Long warehouseId);

    List<Location> findByActiveTrue();

    @Query("SELECT l FROM Location l JOIN FETCH l.warehouse WHERE l.id = :id")
    Optional<Location> findWithWarehouseById(@Param("id") Long id);

    @Query("SELECT l FROM Location l JOIN FETCH l.warehouse ORDER BY l.warehouse.name ASC, l.name ASC")
    List<Location> findAllWithWarehouse();
}
