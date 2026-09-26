package com.stocksense.ledger;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockLedgerRepository extends JpaRepository<StockLedgerEntry, Long>, JpaSpecificationExecutor<StockLedgerEntry> {

    boolean existsByProductId(Long productId);

    Optional<StockLedgerEntry> findByOperationId(Long operationId);

    @EntityGraph(attributePaths = {"product", "operation"})
    @Query("SELECT le FROM StockLedgerEntry le WHERE le.id = :id")
    Optional<StockLedgerEntry> findWithDetailsById(@Param("id") Long id);

    @Override
    @EntityGraph(attributePaths = {"product", "operation"})
    Page<StockLedgerEntry> findAll(Specification<StockLedgerEntry> spec, Pageable pageable);
}
