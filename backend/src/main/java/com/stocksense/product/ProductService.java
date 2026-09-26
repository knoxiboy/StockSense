package com.stocksense.product;

import com.stocksense.common.exception.DuplicateResourceException;
import com.stocksense.common.exception.ResourceNotFoundException;
import com.stocksense.product.dto.CreateProductRequest;
import com.stocksense.product.dto.ProductResponse;
import com.stocksense.product.dto.UpdateProductRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
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
        Product product = productRepository.findBySku(sku.trim())
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
                request.getCategory() != null ? request.getCategory().trim() : null,
                request.getDescription() != null ? request.getDescription().trim() : null,
                request.getUnitOfMeasure().trim(),
                request.getReorderLevel(),
                request.getPrice()
        );

        Product saved = productRepository.save(product);
        return ProductResponse.fromEntity(saved);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        Product product = findEntityById(id);

        String trimmedSku = request.getSku().trim();
        if (productRepository.existsBySkuAndIdNot(trimmedSku, id)) {
            throw new DuplicateResourceException("Product", "SKU", trimmedSku);
        }

        product.setName(request.getName().trim());
        product.setSku(trimmedSku);
        product.setCategory(request.getCategory() != null ? request.getCategory().trim() : null);
        product.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        product.setUnitOfMeasure(request.getUnitOfMeasure().trim());
        product.setReorderLevel(request.getReorderLevel());
        product.setPrice(request.getPrice());
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }

        Product updated = productRepository.save(product);
        return ProductResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product", "id", id);
        }
        productRepository.deleteById(id);
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
}
