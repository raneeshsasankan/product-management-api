package com.nzuretech.product_management.service;

import com.nzuretech.product_management.dto.ProductRequest;
import com.nzuretech.product_management.dto.ProductResponse;
import com.nzuretech.product_management.entity.Product;
import com.nzuretech.product_management.exception.DuplicateProductException;
import com.nzuretech.product_management.exception.ProductNotFoundException;
import com.nzuretech.product_management.repository.ProductRepository;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
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

        if (productRepository.existsByNormalizedName(request.getName())) {
            throw new DuplicateProductException(
                    "Product already exists with name: " + request.getName());
        }

        Product product = new Product();
        mapRequestToEntity(request, product);

        try {
            Product savedProduct = productRepository.saveAndFlush(product);
            return mapEntityToResponse(savedProduct);

        } catch (DataIntegrityViolationException ex) {

            if (isDuplicateProductNameViolation(ex)) {
                throw new DuplicateProductException(
                        "Product already exists with name: " + request.getName());
            }

            throw ex;
        }
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

        if (!product.getName().trim().equalsIgnoreCase(request.getName().trim())
                && productRepository.existsByNormalizedName(request.getName())) {

            throw new DuplicateProductException(
                    "Product already exists with name: " + request.getName());
        }

        mapRequestToEntity(request, product);

        try {
            productRepository.saveAndFlush(product);
            return mapEntityToResponse(product);

        } catch (DataIntegrityViolationException ex) {

            if (isDuplicateProductNameViolation(ex)) {
                throw new DuplicateProductException(
                        "Product already exists with name: " + request.getName());
            }

            throw ex;
        }
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

    /**
     * Checks whether a database integrity error was caused by
     * the product-name unique index.
     *
     * @param exception the database integrity exception
     * @return true if the product-name unique index was violated
     */
    private boolean isDuplicateProductNameViolation(
            DataIntegrityViolationException exception) {

        Throwable cause = exception;

        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintException) {
                return "ux_products_normalized_name"
                        .equalsIgnoreCase(constraintException.getConstraintName());
            }

            cause = cause.getCause();
        }

        return false;
    }

}
