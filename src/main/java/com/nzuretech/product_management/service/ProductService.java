package com.nzuretech.product_management.service;

import com.nzuretech.product_management.dto.ProductRequest;
import com.nzuretech.product_management.dto.ProductResponse;
import com.nzuretech.product_management.exception.ProductNotFoundException;

import java.util.List;

/**
 * Defines business operations for managing products.
 */
public interface ProductService {

    /**
     * Creates and persists a new product.
     *
     * @param request the product details to create
     * @return the created product
     */
    ProductResponse createProduct(ProductRequest request);

    /**
     * Retrieves all products.
     *
     * @return a list of products
     */
    List<ProductResponse> getAllProducts();

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
     * Searches products by name using case-insensitive substring matching.
     *
     * @param name the search keyword
     * @return a list of matching products
     */
    List<ProductResponse> searchProducts(String name);

    /**
     * Retrieves products belonging to a specified category.
     *
     * @param category the category name
     * @return a list of matching products
     */
    List<ProductResponse> getProductsByCategory(String category);
}
