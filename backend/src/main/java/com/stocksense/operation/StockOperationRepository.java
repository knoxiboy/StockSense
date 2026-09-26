package com.stocksense.operation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockOperationRepository extends JpaRepository<StockOperation, Long> {

    boolean existsByProductId(Long productId);

    @Query("SELECT so FROM StockOperation so JOIN FETCH so.product p WHERE " +
           "(:productId IS NULL OR p.id = :productId) AND " +
           "(:operationType IS NULL OR so.operationType = :operationType) " +
           "ORDER BY so.createdAt DESC, so.id DESC")
    Page<StockOperation> findFiltered(
            @Param("productId") Long productId,
            @Param("operationType") OperationType operationType,
            Pageable pageable
    );

    @Query("SELECT so FROM StockOperation so JOIN FETCH so.product p WHERE " +
           "(:productId IS NULL OR p.id = :productId) AND " +
           "(:operationType IS NULL OR so.operationType = :operationType) " +
           "ORDER BY so.createdAt DESC, so.id DESC")
    List<StockOperation> findFilteredList(
            @Param("productId") Long productId,
            @Param("operationType") OperationType operationType
    );
}
