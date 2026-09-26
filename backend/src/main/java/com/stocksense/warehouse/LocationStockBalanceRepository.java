package com.stocksense.warehouse;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationStockBalanceRepository extends JpaRepository<LocationStockBalance, Long> {

    Optional<LocationStockBalance> findByProductIdAndLocationId(Long productId, Long locationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT lsb FROM LocationStockBalance lsb WHERE lsb.product.id = :productId AND lsb.location.id = :locationId")
    Optional<LocationStockBalance> findWithLockByProductIdAndLocationId(
            @Param("productId") Long productId,
            @Param("locationId") Long locationId
    );

    @Query("SELECT lsb FROM LocationStockBalance lsb " +
            "JOIN FETCH lsb.location l " +
            "JOIN FETCH l.warehouse " +
            "WHERE lsb.product.id = :productId")
    List<LocationStockBalance> findAllByProductIdWithDetails(@Param("productId") Long productId);

    List<LocationStockBalance> findByProductId(Long productId);

    List<LocationStockBalance> findByLocationId(Long locationId);

    void deleteByProductId(Long productId);
}
