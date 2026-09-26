package com.stocksense.inventory;

import com.stocksense.product.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockBalanceRepository extends JpaRepository<StockBalance, Long> {

    Optional<StockBalance> findByProduct(Product product);

    Optional<StockBalance> findByProductId(Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT sb FROM StockBalance sb WHERE sb.product.id = :productId")
    Optional<StockBalance> findWithLockByProductId(@Param("productId") Long productId);

    void deleteByProductId(Long productId);

    boolean existsByProductId(Long productId);
}
