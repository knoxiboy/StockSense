package com.stocksense.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReceiptOrderRepository extends JpaRepository<ReceiptOrder, Long> {
    Optional<ReceiptOrder> findByReferenceIgnoreCase(String reference);
    boolean existsByReferenceIgnoreCase(String reference);
    Page<ReceiptOrder> findByStatus(OrderStatus status, Pageable pageable);
    long countByStatusIn(java.util.Collection<OrderStatus> statuses);
}
