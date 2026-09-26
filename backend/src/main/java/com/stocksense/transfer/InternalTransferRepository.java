package com.stocksense.transfer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InternalTransferRepository extends JpaRepository<InternalTransfer, Long> {

    Optional<InternalTransfer> findByReference(String reference);

    boolean existsByReference(String reference);

    @Query("SELECT t FROM InternalTransfer t " +
            "JOIN FETCH t.product " +
            "JOIN FETCH t.sourceLocation sl JOIN FETCH sl.warehouse " +
            "JOIN FETCH t.destinationLocation dl JOIN FETCH dl.warehouse " +
            "WHERE (:status IS NULL OR t.status = :status) " +
            "AND (:productId IS NULL OR t.product.id = :productId) " +
            "AND (:locationId IS NULL OR t.sourceLocation.id = :locationId OR t.destinationLocation.id = :locationId) " +
            "ORDER BY t.createdAt DESC")
    List<InternalTransfer> searchTransfers(
            @Param("status") TransferStatus status,
            @Param("productId") Long productId,
            @Param("locationId") Long locationId
    );

    @Query("SELECT t FROM InternalTransfer t " +
            "JOIN FETCH t.product " +
            "JOIN FETCH t.sourceLocation sl JOIN FETCH sl.warehouse " +
            "JOIN FETCH t.destinationLocation dl JOIN FETCH dl.warehouse " +
            "WHERE t.id = :id")
    Optional<InternalTransfer> findWithDetailsById(@Param("id") Long id);

    long countByStatus(TransferStatus status);

    long countByStatusIn(java.util.Collection<TransferStatus> statuses);
}
