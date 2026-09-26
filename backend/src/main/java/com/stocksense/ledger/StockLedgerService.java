package com.stocksense.ledger;

import com.stocksense.common.exception.ResourceNotFoundException;
import com.stocksense.ledger.dto.StockLedgerEntryResponse;
import com.stocksense.operation.OperationType;
import com.stocksense.product.ProductRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class StockLedgerService {

    private final StockLedgerRepository stockLedgerRepository;
    private final ProductRepository productRepository;

    public StockLedgerService(StockLedgerRepository stockLedgerRepository,
                              ProductRepository productRepository) {
        this.stockLedgerRepository = stockLedgerRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public StockLedgerEntryResponse getLedgerEntryById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Ledger entry ID cannot be null");
        }
        StockLedgerEntry entry = stockLedgerRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StockLedgerEntry", "id", id));
        return StockLedgerEntryResponse.fromEntity(entry);
    }

    @Transactional(readOnly = true)
    public Page<StockLedgerEntryResponse> getLedgerEntriesPaged(
            Long productId,
            OperationType type,
            LocalDate from,
            LocalDate to,
            Long locationId,
            Long warehouseId,
            int page,
            int size) {

        if (page < 0) {
            throw new IllegalArgumentException("Page index cannot be negative");
        }
        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("'from' date cannot be after 'to' date");
        }
        if (productId != null) {
            if (productId <= 0) {
                throw new IllegalArgumentException("Product ID must be greater than zero");
            }
            if (!productRepository.existsById(productId)) {
                throw new ResourceNotFoundException("Product", "id", productId);
            }
        }

        LocalDateTime fromDate = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDate = to != null ? to.atTime(LocalTime.MAX) : null;

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));

        Specification<StockLedgerEntry> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (productId != null) {
                predicates.add(cb.equal(root.get("product").get("id"), productId));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("operationType"), type));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate));
            }
            if (locationId != null) {
                predicates.add(cb.or(
                        cb.equal(root.get("location").get("id"), locationId),
                        cb.equal(root.get("sourceLocation").get("id"), locationId),
                        cb.equal(root.get("destinationLocation").get("id"), locationId)
                ));
            }
            if (warehouseId != null) {
                predicates.add(cb.equal(root.get("warehouse").get("id"), warehouseId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<StockLedgerEntry> entries = stockLedgerRepository.findAll(spec, pageable);
        return entries.map(StockLedgerEntryResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<StockLedgerEntryResponse> getLedgerEntriesPaged(
            Long productId,
            OperationType type,
            LocalDate from,
            LocalDate to,
            int page,
            int size) {
        return getLedgerEntriesPaged(productId, type, from, to, null, null, page, size);
    }

    @Transactional(readOnly = true)
    public List<StockLedgerEntryResponse> getLedgerEntries(
            Long productId,
            OperationType type,
            LocalDate from,
            LocalDate to,
            int page,
            int size) {
        return getLedgerEntriesPaged(productId, type, from, to, page, size).getContent();
    }
}
