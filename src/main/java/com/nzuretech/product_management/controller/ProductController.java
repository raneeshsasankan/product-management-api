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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import java.util.Set;

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
     * Defines the product fields allowed for sorting.
     */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id",
            "name",
            "description",
            "price",
            "quantity",
            "category",
            "createdAt",
            "updatedAt"
    );

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
            @ApiResponse(responseCode = "409",
                    description = "A product with the specified name already exists"),
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
     * Retrieves products using pagination and sorting.
     *
     * @param page the zero-based page number
     * @param size the number of products per page
     * @param sortBy the product field used for sorting
     * @param direction the sorting direction (asc or desc)
     * @return a page containing products and pagination metadata
     * @throws IllegalArgumentException if pagination or sorting parameters are invalid
     */
    @Operation(
            summary = "Get all products with pagination and sorting",
            description = "Retrieves all products with support for pagination and sorting."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Products retrieved successfully"),
            @ApiResponse(responseCode = "400",
                    description = "Invalid pagination or sorting parameters"),
            @ApiResponse(responseCode = "500",
                    description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getAllProducts(

            @Parameter(description = "Zero-based page number",
                    example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Number of products per page (1-100)",
                    example = "10")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Product field used for sorting",
                    example = "name")
            @RequestParam(defaultValue = "name") String sortBy,

            @Parameter(description = "Sorting direction: asc or desc",
                    example = "asc")
            @RequestParam(defaultValue = "asc") String direction) {

        Pageable pageable = createPageable(page, size, sortBy, direction);

        return ResponseEntity.ok(productService.getAllProducts(pageable));
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
            @ApiResponse(responseCode = "409",
                    description = "Another product already uses the specified name"),
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
     * Searches for products by name using pagination and sorting.
     *
     * @param name the search keyword
     * @param page the zero-based page number
     * @param size the number of products per page
     * @param sortBy the product field used for sorting
     * @param direction the sorting direction (asc or desc)
     * @return a page containing matching products with HTTP 200 status
     */
    @Operation(
            summary = "Search products by name with pagination and sorting",
            description = "Finds products whose names contain the supplied search keyword. "
                    + "Supports pagination and sorting."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Search completed successfully"),
            @ApiResponse(responseCode = "400",
                    description = "Invalid pagination or sorting parameters"),
            @ApiResponse(responseCode = "500",
                    description = "Unexpected server error")
    })
    @GetMapping("/search")
    public ResponseEntity<Page<ProductResponse>> searchProducts(
            @Parameter(description = "Product name or partial name to search for",
                    example = "Samsung")
            @RequestParam(name = "name") String name,

            @Parameter(description = "Zero-based page number",
                    example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Number of products per page (1-100)",
                    example = "10")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Product field used for sorting",
                    example = "name")
            @RequestParam(defaultValue = "name") String sortBy,

            @Parameter(description = "Sorting direction: asc or desc",
                    example = "asc")
            @RequestParam(defaultValue = "asc") String direction) {

        Pageable pageable = createPageable(page, size, sortBy, direction);

        return ResponseEntity.ok(productService.searchProducts(name, pageable));
    }

    /**
     * Retrieves products belonging to a category using pagination and sorting.
     *
     * @param category the category name
     * @param page the zero-based page number
     * @param size the number of products per page
     * @param sortBy the product field used for sorting
     * @param direction the sorting direction (asc or desc)
     * @return a page containing matching products with HTTP 200 status
     */
    @Operation(
            summary = "Get products by category with pagination and sorting",
            description = "Retrieves products belonging to the specified category. "
                    + "Supports pagination and sorting."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Products retrieved successfully"),
            @ApiResponse(responseCode = "400",
                    description = "Invalid pagination or sorting parameters"),
            @ApiResponse(responseCode = "500",
                    description = "Unexpected server error")
    })
    @GetMapping("/category/{category}")
    public ResponseEntity<Page<ProductResponse>> getProductsByCategory(
            @Parameter(description = "Product category to filter by",
                    example = "Electronics")
            @PathVariable("category") String category,

            @Parameter(description = "Zero-based page number",
                    example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Number of products per page (1-100)",
                    example = "10")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Product field used for sorting",
                    example = "name")
            @RequestParam(defaultValue = "name") String sortBy,

            @Parameter(description = "Sorting direction: asc or desc",
                    example = "asc")
            @RequestParam(defaultValue = "asc") String direction) {

        Pageable pageable = createPageable(page, size, sortBy, direction);

        return ResponseEntity.ok(
                productService.getProductsByCategory(category, pageable));
    }

    /**
     * Creates pagination and sorting information after validating the parameters.
     *
     * @param page the zero-based page number
     * @param size the number of products per page
     * @param sortBy the product field used for sorting
     * @param direction the sorting direction (asc or desc)
     * @return the validated pagination and sorting information
     * @throws IllegalArgumentException if any pagination or sorting parameter is invalid
     */
    private Pageable createPageable(int page, int size, String sortBy, String direction) {

        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be negative.");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100.");
        }

        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException("Invalid sort field: " + sortBy);
        }

        if (!"asc".equalsIgnoreCase(direction) && !"desc".equalsIgnoreCase(direction)) {
            throw new IllegalArgumentException("Invalid sort direction: " + direction);
        }

        Sort.Direction sortDirection = Sort.Direction.fromString(direction);

        return PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
    }
}
