package com.nzuretech.product_management.controller;

import com.nzuretech.product_management.dto.ProductRequest;
import com.nzuretech.product_management.dto.ProductResponse;
import com.nzuretech.product_management.service.ProductService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for managing product resources.
 *
 * <p>Exposes endpoints for product CRUD operations,
 * name searches, and category filtering.</p>
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    /**
     * Constructs the controller with its service dependency.
     *
     * @param productService the product business service
     */
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * Creates a new product.
     *
     * @param request the validated product creation request
     * @return the created product with HTTP 201 status
     */
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductRequest request) {

        ProductResponse response = productService.createProduct(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves all products.
     *
     * @return all products with HTTP 200 status
     */
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    /**
     * Retrieves a product by its ID.
     *
     * @param id the product ID
     * @return the matching product with HTTP 200 status
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(
            @PathVariable Long id) {

        return ResponseEntity.ok(productService.getProductById(id));
    }

    /**
     * Updates an existing product.
     *
     * @param id the product ID
     * @param request the validated update request
     * @return the updated product with HTTP 200 status
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {

        return ResponseEntity.ok(
                productService.updateProduct(id, request));
    }

    /**
     * Deletes a product.
     *
     * @param id the product ID
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long id) {

        productService.deleteProduct(id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Searches products by name.
     *
     * @param name the search keyword
     * @return matching products with HTTP 200 status
     */
    @GetMapping("/search")
    public ResponseEntity<List<ProductResponse>> searchProducts(
            @RequestParam(name = "name") String name) {

        return ResponseEntity.ok(productService.searchProducts(name));
    }

    /**
     * Retrieves products by category.
     *
     * @param category the category name
     * @return matching products with HTTP 200 status
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<ProductResponse>> getProductsByCategory(
            @PathVariable String category) {

        return ResponseEntity.ok(
                productService.getProductsByCategory(category));
    }
}
