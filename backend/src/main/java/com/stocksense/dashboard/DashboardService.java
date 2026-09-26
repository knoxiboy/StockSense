package com.stocksense.dashboard;

import com.stocksense.inventory.StockBalance;
import com.stocksense.inventory.StockBalanceRepository;
import com.stocksense.order.OrderStatus;
import com.stocksense.order.DeliveryOrderRepository;
import com.stocksense.order.ReceiptOrderRepository;
import com.stocksense.product.Product;
import com.stocksense.product.ProductRepository;
import com.stocksense.transfer.InternalTransferRepository;
import com.stocksense.transfer.TransferStatus;
import com.stocksense.warehouse.LocationStockBalance;
import com.stocksense.warehouse.LocationStockBalanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final ProductRepository productRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final LocationStockBalanceRepository locationStockBalanceRepository;
    private final ReceiptOrderRepository receiptOrderRepository;
    private final DeliveryOrderRepository deliveryOrderRepository;
    private final InternalTransferRepository internalTransferRepository;

    public DashboardService(ProductRepository productRepository,
                            StockBalanceRepository stockBalanceRepository,
                            @Autowired(required = false) LocationStockBalanceRepository locationStockBalanceRepository,
                            @Autowired(required = false) ReceiptOrderRepository receiptOrderRepository,
                            @Autowired(required = false) DeliveryOrderRepository deliveryOrderRepository,
                            @Autowired(required = false) InternalTransferRepository internalTransferRepository) {
        this.productRepository = productRepository;
        this.stockBalanceRepository = stockBalanceRepository;
        this.locationStockBalanceRepository = locationStockBalanceRepository;
        this.receiptOrderRepository = receiptOrderRepository;
        this.deliveryOrderRepository = deliveryOrderRepository;
        this.internalTransferRepository = internalTransferRepository;
    }

    @Transactional(readOnly = true)
    public DashboardKpiResponse getKpis(Long warehouseId, Long locationId, String category) {
        List<Product> products = productRepository.searchProducts(null, category);
        long totalProducts = products.size();

        long inStockCount = 0;
        long lowStockCount = 0;
        long outOfStockCount = 0;
        BigDecimal totalUnits = BigDecimal.ZERO;

        Map<Long, BigDecimal> stockByProduct;

        if (locationId != null && locationStockBalanceRepository != null) {
            List<LocationStockBalance> balances = locationStockBalanceRepository.findAll();
            stockByProduct = balances.stream()
                    .filter(b -> b.getLocation().getId().equals(locationId))
                    .collect(Collectors.toMap(b -> b.getProduct().getId(), LocationStockBalance::getQuantity, BigDecimal::add));
        } else if (warehouseId != null && locationStockBalanceRepository != null) {
            List<LocationStockBalance> balances = locationStockBalanceRepository.findAll();
            stockByProduct = balances.stream()
                    .filter(b -> b.getLocation().getWarehouse().getId().equals(warehouseId))
                    .collect(Collectors.toMap(b -> b.getProduct().getId(), LocationStockBalance::getQuantity, BigDecimal::add));
        } else {
            List<StockBalance> balances = stockBalanceRepository.findAll();
            stockByProduct = balances.stream()
                    .collect(Collectors.toMap(b -> b.getProduct().getId(), StockBalance::getQuantity, (a, b) -> a));
        }

        for (Product product : products) {
            BigDecimal qty = stockByProduct.getOrDefault(product.getId(), BigDecimal.ZERO);
            totalUnits = totalUnits.add(qty);

            if (qty.compareTo(BigDecimal.ZERO) > 0) {
                inStockCount++;
            } else {
                outOfStockCount++;
            }

            BigDecimal reorder = product.getReorderLevel() != null ? product.getReorderLevel() : BigDecimal.ZERO;
            if (qty.compareTo(reorder) <= 0) {
                lowStockCount++;
            }
        }

        long pendingReceipts = 0;
        if (receiptOrderRepository != null) {
            pendingReceipts = receiptOrderRepository.countByStatusIn(List.of(OrderStatus.WAITING, OrderStatus.READY));
        }

        long pendingDeliveries = 0;
        if (deliveryOrderRepository != null) {
            pendingDeliveries = deliveryOrderRepository.countByStatusIn(List.of(OrderStatus.WAITING, OrderStatus.READY));
        }

        long scheduledTransfers = 0;
        if (internalTransferRepository != null) {
            scheduledTransfers = internalTransferRepository.countByStatusIn(List.of(TransferStatus.READY, TransferStatus.DRAFT));
        }

        return new DashboardKpiResponse(
                totalProducts,
                inStockCount,
                totalUnits,
                lowStockCount,
                outOfStockCount,
                pendingReceipts,
                pendingDeliveries,
                scheduledTransfers
        );
    }
}
