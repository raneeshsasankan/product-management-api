package com.nzuretech.product_management.controller;

import com.nzuretech.product_management.dto.ProductRequest;
import com.nzuretech.product_management.dto.ProductResponse;
import com.nzuretech.product_management.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

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
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */
@RestController
@RequestMapping("/api/products")
@Tag(name = "Product Management",
        description = "APIs for creating, retrieving, updating, searching, and deleting products")
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
    @Operation(
            summary = "Create a product",
            description = "Creates a new product using the supplied product details."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201",
                    description = "Product created successfully"),
            @ApiResponse(responseCode = "400",
                    description = "Invalid product details or validation failure"),
            @ApiResponse(responseCode = "500",
                    description = "Unexpected server error")
    })
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
    @Operation(
            summary = "Get all products",
            description = "Retrieves a list of all products."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Products retrieved successfully"),
            @ApiResponse(responseCode = "500",
                    description = "Unexpected server error")
    })
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
    @Operation(
            summary = "Get product by ID",
            description = "Retrieves the details of a specific product using its unique ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Product retrieved successfully"),
            @ApiResponse(responseCode = "404",
                    description = "Product not found"),
            @ApiResponse(responseCode = "500",
                    description = "Unexpected server error")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(
            @Parameter(description = "Unique ID of the product", example = "1")
            @PathVariable("id") Long id) {

        return ResponseEntity.ok(productService.getProductById(id));
    }

    /**
     * Updates an existing product.
     *
     * @param id the product ID
     * @param request the validated update request
     * @return the updated product with HTTP 200 status
     */
    @Operation(
            summary = "Update a product",
            description = "Updates the details of an existing product using its ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Product updated successfully"),
            @ApiResponse(responseCode = "400",
                    description = "Invalid product details or validation failure"),
            @ApiResponse(responseCode = "404",
                    description = "Product not found"),
            @ApiResponse(responseCode = "500",
                    description = "Unexpected server error")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @Parameter(description = "Unique ID of the product to update", example = "1")
            @PathVariable("id") Long id,
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
    @Operation(
            summary = "Delete a product",
            description = "Permanently deletes a product using its unique ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204",
                    description = "Product deleted successfully"),
            @ApiResponse(responseCode = "404",
                    description = "Product not found"),
            @ApiResponse(responseCode = "500",
                    description = "Unexpected server error")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "Unique ID of the product to delete", example = "1")
            @PathVariable("id") Long id) {

        productService.deleteProduct(id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Searches products by name.
     *
     * @param name the search keyword
     * @return matching products with HTTP 200 status
     */
    @Operation(
            summary = "Search products by name",
            description = "Finds products whose names contain the supplied search keyword."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Search completed successfully"),
            @ApiResponse(responseCode = "500",
                    description = "Unexpected server error")
    })
    @GetMapping("/search")
    public ResponseEntity<List<ProductResponse>> searchProducts(
            @Parameter(description = "Product name or partial name to search for",
                    example = "Samsung")
            @RequestParam(name = "name") String name) {

        return ResponseEntity.ok(productService.searchProducts(name));
    }

    /**
     * Retrieves products by category.
     *
     * @param category the category name
     * @return matching products with HTTP 200 status
     */
    @Operation(
            summary = "Get products by category",
            description = "Retrieves all products belonging to the specified category."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Products retrieved successfully"),
            @ApiResponse(responseCode = "500",
                    description = "Unexpected server error")
    })
    @GetMapping("/category/{category}")
    public ResponseEntity<List<ProductResponse>> getProductsByCategory(
            @Parameter(description = "Product category to filter by",
                    example = "Electronics")
            @PathVariable("category") String category) {

        return ResponseEntity.ok(
                productService.getProductsByCategory(category));
    }
}
