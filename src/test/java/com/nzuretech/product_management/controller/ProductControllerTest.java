package com.nzuretech.product_management.controller;

import com.nzuretech.product_management.dto.ProductRequest;
import com.nzuretech.product_management.dto.ProductResponse;
import com.nzuretech.product_management.exception.DuplicateProductException;
import com.nzuretech.product_management.exception.ProductNotFoundException;
import com.nzuretech.product_management.service.ProductService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests the HTTP endpoints exposed by ProductController.
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */
@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Verifies that creating a valid product returns HTTP 201
     * and the expected product details.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void createProduct_ShouldReturnCreatedProduct() throws Exception {

        ProductResponse response = createTestProductResponse(
                1L,
                "Samsung Galaxy S26",
                "Samsung smartphone",
                new BigDecimal("999.99"),
                25,
                "Electronics"
        );

        when(productService.createProduct(any(ProductRequest.class)))
                .thenReturn(response);

        ProductRequest request = createTestProductRequest("Samsung Galaxy S26",
                "Samsung smartphone", new BigDecimal("999.99"),
                25, "Electronics");

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Samsung Galaxy S26"))
                .andExpect(jsonPath("$.description").value("Samsung smartphone"))
                .andExpect(jsonPath("$.price").value(999.99))
                .andExpect(jsonPath("$.quantity").value(25))
                .andExpect(jsonPath("$.category").value("Electronics"));

        verify(productService).createProduct(any(ProductRequest.class));
    }

    /**
     * Verifies that creating a product with a blank name
     * returns HTTP 400 and a field validation error.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void createProduct_ShouldReturnBadRequest_WhenNameIsBlank()
            throws Exception {

        ProductRequest request = createTestProductRequest("",
                "Samsung smartphone", new BigDecimal("999.99"),
                25, "Electronics");

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.validationErrors.name").value("Product name is required"))
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(productService);
    }

    /**
     * Verifies that creating a product with a negative price
     * returns HTTP 400 and a price validation error.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void createProduct_ShouldReturnBadRequest_WhenPriceIsNegative()
            throws Exception {

        ProductRequest request = createTestProductRequest("Samsung Galaxy S26",
                "Samsung smartphone", new BigDecimal("-100.99"),
                25, "Electronics");

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.validationErrors.price").
                        value("Price must be greater than zero"))
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(productService);
    }

    /**
     * Verifies that creating a product with a negative quantity
     * returns HTTP 400 and a quantity validation error.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void createProduct_ShouldReturnBadRequest_WhenQuantityIsNegative()
            throws Exception {

        ProductRequest request = createTestProductRequest("Samsung Galaxy S26",
                "Samsung smartphone", new BigDecimal("999.99"),
                -5, "Electronics");

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.validationErrors.quantity").
                        value("Quantity cannot be negative"))
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(productService);
    }

    /**
     * Verifies that creating a product with an existing name
     * returns HTTP 409 and the expected conflict error response.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void createProduct_ShouldReturnConflict_WhenNameAlreadyExists()
            throws Exception {

        String productName = "Samsung Galaxy S26";
        String errorMessage = "Product already exists with name: " + productName;

        ProductRequest request = createTestProductRequest("Samsung Galaxy S26",
                "Samsung smartphone", new BigDecimal("999.99"),
                25, "Electronics");
        when(productService.createProduct(any(ProductRequest.class)))
                .thenThrow(new DuplicateProductException(errorMessage));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(errorMessage))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService).createProduct(any(ProductRequest.class));
    }

    /**
     * Verifies that retrieving an existing product by ID
     * returns HTTP 200 and the expected product details.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void getProductById_ShouldReturnProduct() throws Exception {

        ProductResponse response = createTestProductResponse(
                1L,
                "Samsung Galaxy S26",
                "Samsung smartphone",
                new BigDecimal("999.99"),
                25,
                "Electronics"
        );

        when(productService.getProductById(1L))
                .thenReturn(response);

        mockMvc.perform(get("/api/products/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Samsung Galaxy S26"))
                .andExpect(jsonPath("$.price").value(999.99))
                .andExpect(jsonPath("$.quantity").value(25))
                .andExpect(jsonPath("$.category").value("Electronics"));

        verify(productService).getProductById(1L);
    }


    /**
     * Verifies that retrieving a nonexistent product returns
     * HTTP 404 and the expected error response.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void getProductById_ShouldReturnNotFound_WhenProductDoesNotExist()
            throws Exception {

        Long productId = 100L;
        String errorMessage = "Product not found with ID: " + productId;

        when(productService.getProductById(productId))
                .thenThrow(new ProductNotFoundException(errorMessage));

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(errorMessage))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService).getProductById(productId);
    }

    /**
     * Verifies that unexpected service exceptions result in
     * HTTP 500 without exposing internal error details.
     */
    @Test
    void getProductById_whenUnexpectedExceptionOccurs_returnsInternalServerError()
            throws Exception {

        Long productId = 1L;

        when(productService.getProductById(productId))
                .thenThrow(new RuntimeException(
                        "Sensitive internal database error"
                ));

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error")
                        .value("Internal Server Error"))
                .andExpect(jsonPath("$.message")
                        .value("An unexpected error occurred."))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService).getProductById(productId);
    }


    /**
     * Verifies that retrieving products with pagination and sorting
     * returns HTTP 200 and passes the correct Pageable to the service.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void getAllProducts_ShouldReturnPagedProducts() throws Exception {

        ProductResponse response = createTestProductResponse(
                1L,
                "Samsung Galaxy S26",
                "Samsung smartphone",
                new BigDecimal("999.99"),
                25,
                "Electronics"
        );

        Pageable pageable = PageRequest.of(
                1,
                2,
                Sort.by(Sort.Direction.DESC, "price")
        );

        Page<ProductResponse> productPage = new PageImpl<>(
                List.of(response),
                pageable,
                3
        );

        when(productService.getAllProducts(pageable))
                .thenReturn(productPage);

        mockMvc.perform(get("/api/products")
                        .param("page", "1")
                        .param("size", "2")
                        .param("sortBy", "price")
                        .param("direction", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name")
                        .value("Samsung Galaxy S26"))
                .andExpect(jsonPath("$.content[0].price").value(999.99));

        verify(productService).getAllProducts(pageable);
    }

    /**
     * Verifies that requesting a negative page number returns
     * HTTP 400 and the appropriate validation error message.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void getAllProducts_ShouldReturnBadRequest_WhenPageIsNegative()
            throws Exception {

        mockMvc.perform(get("/api/products")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message")
                        .value("Page number cannot be negative."))
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(productService);
    }

    /**
     * Verifies that requesting a page size greater than 100
     * returns HTTP 400 and the appropriate validation error.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void getAllProducts_ShouldReturnBadRequest_WhenPageSizeExceedsMaximum()
            throws Exception {

        mockMvc.perform(get("/api/products")
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message")
                        .value("Page size must be between 1 and 100."))
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(productService);
    }

    /**
     * Verifies that an unsupported sorting field returns
     * HTTP 400 and the appropriate validation error.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void getAllProducts_ShouldReturnBadRequest_WhenSortFieldIsInvalid()
            throws Exception {

        mockMvc.perform(get("/api/products")
                        .param("sortBy", "invalidField"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message")
                        .value("Invalid sort field: invalidField"))
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(productService);
    }

    /**
     * Verifies that an unsupported sorting direction returns
     * HTTP 400 and the appropriate validation error.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void getAllProducts_ShouldReturnBadRequest_WhenSortDirectionIsInvalid()
            throws Exception {

        mockMvc.perform(get("/api/products")
                        .param("direction", "random"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message")
                        .value("Invalid sort direction: random"))
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(productService);
    }

    /**
     * Verifies that updating an existing product returns
     * HTTP 200 and the updated product details.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void updateProduct_ShouldReturnUpdatedProduct() throws Exception {

        Long productId = 1L;

        ProductResponse response = createTestProductResponse(
                productId,
                "Samsung Galaxy S26 Ultra",
                "Updated Samsung smartphone",
                new BigDecimal("1299.99"),
                15,
                "Electronics"
        );

        when(productService.updateProduct(
                eq(productId), any(ProductRequest.class)))
                .thenReturn(response);

        ProductRequest request = createTestProductRequest("Samsung Galaxy S26 Ultra",
                "Updated Samsung smartphone", new BigDecimal("1299.99"),
                15, "Electronics");

        mockMvc.perform(put("/api/products/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Samsung Galaxy S26 Ultra"))
                .andExpect(jsonPath("$.description").value("Updated Samsung smartphone"))
                .andExpect(jsonPath("$.price").value(1299.99))
                .andExpect(jsonPath("$.quantity").value(15))
                .andExpect(jsonPath("$.category").value("Electronics"));

        verify(productService).updateProduct(
                eq(productId), any(ProductRequest.class));
    }

    /**
     * Verifies that updating a nonexistent product returns
     * HTTP 404 and the expected error response.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void updateProduct_ShouldReturnNotFound_WhenProductDoesNotExist()
            throws Exception {

        Long productId = 100L;
        String errorMessage = "Product not found with ID: " + productId;

        when(productService.updateProduct(
                eq(productId), any(ProductRequest.class)))
                .thenThrow(new ProductNotFoundException(errorMessage));

        ProductRequest request = createTestProductRequest("Samsung Galaxy S26 Ultra",
                "Updated Samsung smartphone", new BigDecimal("1299.99"),
                15, "Electronics");

        mockMvc.perform(put("/api/products/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(errorMessage))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService).updateProduct(
                eq(productId), any(ProductRequest.class));
    }


    /**
     * Verifies that updating a product with a name already used
     * by another product returns HTTP 409 Conflict.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void updateProduct_ShouldReturnConflict_WhenNameAlreadyExists()
            throws Exception {

        Long productId = 2L;
        String productName = "Samsung Galaxy S26";
        String errorMessage = "Product already exists with name: " + productName;

        ProductRequest request = createTestProductRequest(productName, "Samsung smartphone",
                new BigDecimal("999.99"), 25, "Electronics");

        when(productService.updateProduct(
                eq(productId), any(ProductRequest.class)))
                .thenThrow(new DuplicateProductException(errorMessage));

        mockMvc.perform(put("/api/products/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(errorMessage))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService).updateProduct(
                eq(productId), any(ProductRequest.class));
    }


    /**
     * Verifies that deleting an existing product returns
     * HTTP 204 with an empty response body.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void deleteProduct_ShouldReturnNoContent() throws Exception {

        Long productId = 1L;

        doNothing().when(productService).deleteProduct(productId);

        mockMvc.perform(delete("/api/products/{id}", productId))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(productService).deleteProduct(productId);
    }

    /**
     * Verifies that deleting a nonexistent product returns
     * HTTP 404 and the expected error response.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void deleteProduct_ShouldReturnNotFound_WhenProductDoesNotExist()
            throws Exception {

        Long productId = 100L;
        String errorMessage = "Product not found with ID: " + productId;

        doThrow(new ProductNotFoundException(errorMessage))
                .when(productService)
                .deleteProduct(productId);

        mockMvc.perform(delete("/api/products/{id}", productId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(errorMessage))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService).deleteProduct(productId);
    }

    /**
     * Verifies that searching products by name returns
     * HTTP 200 and the expected matching products.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void searchProducts_ShouldReturnMatchingProducts() throws Exception {

        String searchKeyword = "Samsung";

        ProductResponse response = createTestProductResponse(
                1L,
                "Samsung Galaxy S26",
                "Samsung smartphone",
                new BigDecimal("999.99"),
                25,
                "Electronics"
        );

        Pageable pageable = PageRequest.of(
                0,
                10,
                Sort.by(Sort.Direction.ASC, "name")
        );

        Page<ProductResponse> productPage = new PageImpl<>(
                List.of(response),
                pageable,
                1
        );

        when(productService.searchProducts(searchKeyword, pageable))
                .thenReturn(productPage);

        mockMvc.perform(get("/api/products/search")
                        .param("name", searchKeyword)
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "name")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name")
                        .value("Samsung Galaxy S26"))
                .andExpect(jsonPath("$.content[0].category")
                        .value("Electronics"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(productService).searchProducts(searchKeyword, pageable);
    }

    /**
     * Verifies that filtering products by category returns
     * HTTP 200 and the expected products.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void getProductsByCategory_ShouldReturnMatchingProducts()
            throws Exception {

        String category = "Electronics";

        ProductResponse response = createTestProductResponse(
                1L,
                "Samsung Galaxy S26",
                "Samsung smartphone",
                new BigDecimal("999.99"),
                25,
                category
        );

        Pageable pageable = PageRequest.of(
                0,
                10,
                Sort.by(Sort.Direction.ASC, "name")
        );

        Page<ProductResponse> productPage = new PageImpl<>(
                List.of(response),
                pageable,
                1
        );

        when(productService.getProductsByCategory(category, pageable))
                .thenReturn(productPage);

        mockMvc.perform(get("/api/products/category/{category}", category)
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "name")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name")
                        .value("Samsung Galaxy S26"))
                .andExpect(jsonPath("$.content[0].category")
                        .value("Electronics"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(productService).getProductsByCategory(category, pageable);
    }


    /**
     * Creates a test product response with the specified field values.
     *
     * @param id          the product ID
     * @param name        the product name
     * @param description the product description
     * @param price       the unit price of the product
     * @param quantity    the available inventory quantity
     * @param category    the product category
     * @return a product response populated with the specified test values
     */
    private static ProductResponse createTestProductResponse(
            Long id,
            String name,
            String description,
            BigDecimal price,
            int quantity,
            String category) {

        ProductResponse response = new ProductResponse();

        ReflectionTestUtils.setField(response, "id", id);

        response.setName(name);
        response.setDescription(description);
        response.setPrice(price);
        response.setQuantity(quantity);
        response.setCategory(category);

        return response;
    }

    /**
     * Creates a test product request with the specified field values.
     *
     * @param name        the product name
     * @param description the product description
     * @param price       the unit price of the product
     * @param quantity    the available inventory quantity
     * @param category    the product category
     * @return a product request populated with the specified test values
     */
    private static ProductRequest createTestProductRequest(
            String name,
            String description,
            BigDecimal price,
            int quantity,
            String category) {
        ProductRequest request = new ProductRequest();
        request.setName(name);
        request.setDescription(description);
        request.setPrice(price);
        request.setQuantity(quantity);
        request.setCategory(category);
        return request;
    }

}
