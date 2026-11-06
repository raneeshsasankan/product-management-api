package com.nzuretech.product_management.service;

import com.nzuretech.product_management.dto.ProductRequest;
import com.nzuretech.product_management.dto.ProductResponse;
import com.nzuretech.product_management.entity.Product;
import com.nzuretech.product_management.exception.ProductNotFoundException;
import com.nzuretech.product_management.repository.ProductRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Production implementation of the ProductService interface.
 *
 * <p>Handles business logic, entity-to-DTO mapping, and transaction
 * boundaries for product catalog operations.</p>
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */
@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    /**
     * Constructs the service with its required repository dependency.
     *
     * @param productRepository the product data access repository
     */
    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public ProductResponse createProduct(ProductRequest request) {
        Product product = new Product();
        mapRequestToEntity(request, product);

        Product savedProduct = productRepository.save(product);

        return mapEntityToResponse(savedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(this::mapEntityToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ProductNotFoundException(
                                "Product not found with ID: " + id));

        return mapEntityToResponse(product);
    }

    @Override
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ProductNotFoundException(
                                "Product not found with ID: " + id));

        mapRequestToEntity(request, product);

        // Flush changes to trigger the @PreUpdate lifecycle callback.
        productRepository.saveAndFlush(product);

        return mapEntityToResponse(product);
    }

    @Override
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ProductNotFoundException(
                                "Product not found with ID: " + id));

        productRepository.delete(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(String name, Pageable pageable) {
        return productRepository.findByNameContainingIgnoreCase(name, pageable)
                .map(this::mapEntityToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsByCategory(String category, Pageable pageable) {
        return productRepository.findByCategoryIgnoreCase(category, pageable)
                .map(this::mapEntityToResponse);
    }

    /**
     * Maps request DTO fields to a product entity.
     *
     * @param request the source request DTO
     * @param product the target product entity
     */
    private void mapRequestToEntity(ProductRequest request, Product product) {
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setQuantity(request.getQuantity());
        product.setCategory(request.getCategory());
    }

    /**
     * Converts a product entity into a response DTO.
     *
     * @param product the source product entity
     * @return the product response DTO
     */
    private ProductResponse mapEntityToResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getQuantity(),
                product.getCategory(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
