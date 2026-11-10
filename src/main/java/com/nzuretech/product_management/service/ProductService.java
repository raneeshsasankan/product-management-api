package com.nzuretech.product_management.service;

import com.nzuretech.product_management.dto.ProductRequest;
import com.nzuretech.product_management.dto.ProductResponse;
import com.nzuretech.product_management.exception.ProductNotFoundException;
import com.nzuretech.product_management.exception.DuplicateProductException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Defines business operations for managing products.
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */
public interface ProductService {

    /**
     * Creates and persists a new product.
     *
     * @param request the product details to create
     * @return the created product
     * @throws DuplicateProductException if the product name already exists
     */
    ProductResponse createProduct(ProductRequest request);

    /**
     * Retrieves products using pagination and sorting.
     *
     * @param pageable the pagination and sorting information
     * @return a page containing products and pagination metadata
     */
    Page<ProductResponse> getAllProducts(Pageable pageable);

    /**
     * Retrieves a product by its unique identifier.
     *
     * @param id the product ID
     * @return the matching product
     * @throws ProductNotFoundException if the product does not exist
     */
    ProductResponse getProductById(Long id);

    /**
     * Updates an existing product.
     *
     * @param id the product ID
     * @param request the updated product details
     * @return the updated product
     * @throws ProductNotFoundException if the product does not exist
     * @throws DuplicateProductException if another product already uses
     *         the requested name
     */
    ProductResponse updateProduct(Long id, ProductRequest request);

    /**
     * Deletes a product by its unique identifier.
     *
     * @param id the product ID
     * @throws ProductNotFoundException if the product does not exist
     */
    void deleteProduct(Long id);

    /**
     * Searches for products by name using pagination and sorting.
     *
     * @param name the search text
     * @param pageable the pagination and sorting information
     * @return a page containing matching products
     */
    Page<ProductResponse> searchProducts(String name, Pageable pageable);

    /**
     * Retrieves products belonging to the specified category
     * using pagination and sorting.
     *
     * @param category the category name
     * @param pageable the pagination and sorting information
     * @return a page containing matching products
     */
    Page<ProductResponse> getProductsByCategory(String category, Pageable pageable);
}
