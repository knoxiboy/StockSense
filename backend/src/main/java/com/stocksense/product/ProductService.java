package com.stocksense.product;

import com.stocksense.common.exception.ConflictException;
import com.stocksense.common.exception.DuplicateResourceException;
import com.stocksense.common.exception.ResourceNotFoundException;
import com.stocksense.inventory.StockBalance;
import com.stocksense.inventory.StockBalanceRepository;
import com.stocksense.inventory.dto.StockBalanceResponse;
import com.stocksense.product.dto.CreateProductRequest;
import com.stocksense.product.dto.ProductResponse;
import com.stocksense.product.dto.UpdateProductRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final com.stocksense.operation.StockOperationRepository stockOperationRepository;

    public ProductService(ProductRepository productRepository,
                          StockBalanceRepository stockBalanceRepository,
                          com.stocksense.operation.StockOperationRepository stockOperationRepository) {
        this.productRepository = productRepository;
        this.stockBalanceRepository = stockBalanceRepository;
        this.stockOperationRepository = stockOperationRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts(String search, String category) {
        List<Product> products = productRepository.searchProducts(
                search != null ? search.trim() : null,
                category != null ? category.trim() : null
        );
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

        // Atomic creation of zero-valued StockBalance in the same transaction
        StockBalance balance = new StockBalance(savedProduct, BigDecimal.ZERO);
        stockBalanceRepository.save(balance);

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
        productRepository.delete(product);
    }

    @Transactional(readOnly = true)
    public StockBalanceResponse getStockBalance(Long productId) {
        Product product = findEntityById(productId);
        StockBalance balance = stockBalanceRepository.findByProductId(productId)
                .orElseGet(() -> new StockBalance(product, BigDecimal.ZERO));
        return StockBalanceResponse.fromEntity(balance);
    }

    @Transactional(readOnly = true)
    public List<String> getCategories() {
        return productRepository.findDistinctCategories();
    }

    @Transactional(readOnly = true)
    public Product findEntityById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    }

    /**
     * Extensibility hook for validating stock movement history before deletion.
     */
    protected boolean hasStockMovements(Product product) {
        return product != null && stockOperationRepository.existsByProductId(product.getId());
    }
}
