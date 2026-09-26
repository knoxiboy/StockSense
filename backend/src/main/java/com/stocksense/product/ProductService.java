package com.stocksense.product;

import com.stocksense.common.exception.ConflictException;
import com.stocksense.common.exception.DuplicateResourceException;
import com.stocksense.common.exception.ResourceNotFoundException;
import com.stocksense.inventory.StockBalance;
import com.stocksense.inventory.StockBalanceRepository;
import com.stocksense.inventory.dto.LocationBalanceItem;
import com.stocksense.inventory.dto.StockBalanceResponse;
import com.stocksense.operation.StockOperationRepository;
import com.stocksense.product.dto.CreateProductRequest;
import com.stocksense.product.dto.ProductResponse;
import com.stocksense.product.dto.UpdateProductRequest;
import com.stocksense.warehouse.Location;
import com.stocksense.warehouse.LocationRepository;
import com.stocksense.warehouse.LocationService;
import com.stocksense.warehouse.LocationStockBalance;
import com.stocksense.warehouse.LocationStockBalanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final StockOperationRepository stockOperationRepository;
    private final LocationRepository locationRepository;
    private final LocationStockBalanceRepository locationStockBalanceRepository;

    public ProductService(ProductRepository productRepository,
                          StockBalanceRepository stockBalanceRepository,
                          StockOperationRepository stockOperationRepository) {
        this(productRepository, stockBalanceRepository, stockOperationRepository, null, null);
    }

    @Autowired
    public ProductService(ProductRepository productRepository,
                          StockBalanceRepository stockBalanceRepository,
                          StockOperationRepository stockOperationRepository,
                          @Autowired(required = false) LocationRepository locationRepository,
                          @Autowired(required = false) LocationStockBalanceRepository locationStockBalanceRepository) {
        this.productRepository = productRepository;
        this.stockBalanceRepository = stockBalanceRepository;
        this.stockOperationRepository = stockOperationRepository;
        this.locationRepository = locationRepository;
        this.locationStockBalanceRepository = locationStockBalanceRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts(String search, String category) {
        return getAllProducts(search, category, null);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts(String search, String category, Long locationId) {
        List<Product> products = productRepository.searchProducts(
                search != null ? search.trim() : null,
                category != null ? category.trim() : null
        );
        if (locationId != null && locationStockBalanceRepository != null) {
            List<LocationStockBalance> balances = locationStockBalanceRepository.findAll();
            java.util.Set<Long> productIdsAtLoc = balances.stream()
                    .filter(b -> b.getLocation().getId().equals(locationId))
                    .map(b -> b.getProduct().getId())
                    .collect(Collectors.toSet());
            products = products.stream()
                    .filter(p -> productIdsAtLoc.contains(p.getId()))
                    .collect(Collectors.toList());
        }
        return products.stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = findEntityById(id);
        return ProductResponse.fromEntity(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductBySku(String sku) {
        Product product = productRepository.findBySku(sku != null ? sku.trim() : "")
                .orElseThrow(() -> new ResourceNotFoundException("Product", "SKU", sku));
        return ProductResponse.fromEntity(product);
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        String trimmedSku = request.getSku().trim();
        if (productRepository.existsBySku(trimmedSku)) {
            throw new DuplicateResourceException("Product", "SKU", trimmedSku);
        }

        Product product = new Product(
                request.getName().trim(),
                trimmedSku,
                request.getCategory().trim(),
                request.getUnit().trim(),
                request.getReorderLevel(),
                request.getDescription() != null ? request.getDescription().trim() : null,
                request.getPrice()
        );

        Product savedProduct = productRepository.save(product);

        // Atomic creation of StockBalance in the same transaction
        BigDecimal initialQty = request.getInitialStock() != null && request.getInitialStock().compareTo(BigDecimal.ZERO) > 0
                ? request.getInitialStock()
                : BigDecimal.ZERO;

        StockBalance balance = new StockBalance(savedProduct, initialQty);
        stockBalanceRepository.save(balance);

        // Also track location balance if warehouse module is active
        if (locationRepository != null && locationStockBalanceRepository != null) {
            Location location = null;
            if (request.getLocationId() != null) {
                location = locationRepository.findById(request.getLocationId()).orElse(null);
            }
            if (location == null) {
                location = locationRepository.findByCode(LocationService.DEFAULT_LOCATION_CODE).orElse(null);
            }
            if (location != null) {
                LocationStockBalance locBal = new LocationStockBalance(savedProduct, location, initialQty);
                locationStockBalanceRepository.save(locBal);
            }
        }

        return ProductResponse.fromEntity(savedProduct);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        Product product = findEntityById(id);

        String trimmedSku = request.getSku().trim();
        if (productRepository.existsBySkuAndIdNot(trimmedSku, id)) {
            throw new DuplicateResourceException("Product", "SKU", trimmedSku);
        }

        // Product updates must not directly change stock
        product.setName(request.getName().trim());
        product.setSku(trimmedSku);
        product.setCategory(request.getCategory().trim());
        product.setUnit(request.getUnit().trim());
        product.setReorderLevel(request.getReorderLevel());
        product.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        product.setPrice(request.getPrice());

        Product updatedProduct = productRepository.save(product);
        return ProductResponse.fromEntity(updatedProduct);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = findEntityById(id);

        // Safe deletion rule: check if product has existing stock or movements
        StockBalance balance = stockBalanceRepository.findByProductId(id)
                .orElse(null);

        if (balance != null && balance.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
            throw new ConflictException(String.format(
                    "Cannot delete product '%s' (SKU: %s) because it has an active stock balance of %s %s.",
                    product.getName(), product.getSku(), balance.getQuantity(), product.getUnit()
            ));
        }

        if (hasStockMovements(product)) {
            throw new ConflictException(String.format(
                    "Cannot delete product '%s' (SKU: %s) because it has recorded stock movement transactions.",
                    product.getName(), product.getSku()
            ));
        }

        // Delete associated zero balance then product
        if (balance != null) {
            stockBalanceRepository.delete(balance);
        }
        if (locationStockBalanceRepository != null) {
            locationStockBalanceRepository.deleteByProductId(id);
        }
        productRepository.delete(product);
    }

    @Transactional(readOnly = true)
    public StockBalanceResponse getStockBalance(Long productId) {
        Product product = findEntityById(productId);
        StockBalance balance = stockBalanceRepository.findByProductId(productId)
                .orElseGet(() -> new StockBalance(product, BigDecimal.ZERO));

        StockBalanceResponse response = StockBalanceResponse.fromEntity(balance);
        response.setTotalQuantity(balance.getQuantity());

        if (locationStockBalanceRepository != null) {
            List<LocationStockBalance> locBalances = locationStockBalanceRepository.findAllByProductIdWithDetails(productId);
            List<LocationBalanceItem> items = locBalances.stream().map(lb -> new LocationBalanceItem(
                    lb.getLocation().getId(),
                    lb.getLocation().getName(),
                    lb.getLocation().getCode(),
                    lb.getLocation().getWarehouse().getId(),
                    lb.getLocation().getWarehouse().getName(),
                    lb.getLocation().getWarehouse().getCode(),
                    lb.getQuantity()
            )).collect(Collectors.toList());
            response.setLocationBalances(items);
        }

        return response;
    }

    @Transactional(readOnly = true)
    public List<String> getCategories() {
        return productRepository.findDistinctCategories();
    }

    private Product findEntityById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Product ID cannot be null");
        }
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    }

    private boolean hasStockMovements(Product product) {
        return stockOperationRepository.existsByProductId(product.getId());
    }
}
