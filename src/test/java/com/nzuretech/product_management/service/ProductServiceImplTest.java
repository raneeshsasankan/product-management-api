package com.nzuretech.product_management.service;

import com.nzuretech.product_management.dto.ProductRequest;
import com.nzuretech.product_management.entity.Product;
import com.nzuretech.product_management.exception.DuplicateProductException;
import com.nzuretech.product_management.exception.ProductNotFoundException;
import com.nzuretech.product_management.repository.ProductRepository;
import com.nzuretech.product_management.dto.ProductResponse;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ProductServiceImpl}.
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    /**
     * Verifies that an existing product is returned successfully.
     */
    @Test
    void getProductById_whenProductExists_returnsProductResponse() {

        Product product = createTestProduct(
                1L,
                "Samsung Galaxy S26 Ultra",
                "Samsung flagship smartphone",
                new BigDecimal("1299.99"),
                15,
                "Electronics"
        );

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        ProductResponse response = productService.getProductById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Samsung Galaxy S26 Ultra", response.getName());
        assertEquals("Samsung flagship smartphone", response.getDescription());
        assertEquals(new BigDecimal("1299.99"), response.getPrice());
        assertEquals(15, response.getQuantity());
        assertEquals("Electronics", response.getCategory());

        verify(productRepository).findById(1L);
    }

    /**
     * Verifies that requesting a non-existing product throws
     * ProductNotFoundException.
     */
    @Test
    void getProductById_whenProductDoesNotExist_throwsProductNotFoundException() {

        when(productRepository.findById(100L))
                .thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> productService.getProductById(100L)
        );

        verify(productRepository).findById(100L);
    }

    /**
     * Verifies that all products are retrieved with pagination
     * and correctly converted into ProductResponse objects.
     */
    @Test
    void getAllProducts_whenProductsExist_returnsPagedProductResponses() {

        Pageable pageable = PageRequest.of(0, 10);

        Product product1 = createTestProduct(
                1L,
                "Samsung Galaxy S26 Ultra",
                "Samsung flagship smartphone",
                new BigDecimal("1299.99"),
                15,
                "Electronics"
        );

        Product product2 = createTestProduct(
                2L,
                "Apple iPhone 17",
                "Apple flagship smartphone",
                new BigDecimal("1099.99"),
                20,
                "Electronics"
        );

        Page<Product> productPage = new PageImpl<>(
                List.of(product1, product2),
                pageable,
                2
        );

        when(productRepository.findAll(pageable))
                .thenReturn(productPage);

        Page<ProductResponse> response =
                productService.getAllProducts(pageable);

        assertNotNull(response);
        assertEquals(2, response.getContent().size());

        assertEquals(1L, response.getContent().get(0).getId());
        assertEquals("Samsung Galaxy S26 Ultra",
                response.getContent().get(0).getName());

        assertEquals(2L, response.getContent().get(1).getId());
        assertEquals("Apple iPhone 17",
                response.getContent().get(1).getName());

        assertEquals(2, response.getTotalElements());
        assertEquals(1, response.getTotalPages());
        assertEquals(0, response.getNumber());
        assertEquals(10, response.getSize());

        verify(productRepository).findAll(pageable);
    }

    /**
     * Verifies that an empty page is returned when no products exist.
     */
    @Test
    void getAllProducts_whenNoProductsExist_returnsEmptyPage() {

        Pageable pageable = PageRequest.of(0, 10);

        Page<Product> emptyPage = Page.empty(pageable);

        when(productRepository.findAll(pageable))
                .thenReturn(emptyPage);

        Page<ProductResponse> response =
                productService.getAllProducts(pageable);

        assertNotNull(response);
        assertTrue(response.isEmpty());
        assertEquals(0, response.getTotalElements());
        assertEquals(0, response.getTotalPages());
        assertEquals(0, response.getNumber());
        assertEquals(10, response.getSize());

        verify(productRepository).findAll(pageable);
    }

    /**
     * Verifies that a product is created successfully and the saved
     * product details are returned.
     */
    @Test
    void createProduct_whenValidRequest_returnsCreatedProductResponse() {

        ProductRequest request = createTestProductRequest("Apple MacBook Pro",
                "Apple laptop", new BigDecimal("2499.99"), 10,
                "Electronics");

        Product savedProduct = createTestProduct(
                2L,
                "Apple MacBook Pro",
                "Apple laptop",
                new BigDecimal("2499.99"),
                10,
                "Electronics"
        );

        when(productRepository.saveAndFlush(any(Product.class)))
                .thenReturn(savedProduct);

        ProductResponse response = productService.createProduct(request);

        assertNotNull(response);
        assertEquals(2L, response.getId());
        assertEquals("Apple MacBook Pro", response.getName());
        assertEquals("Apple laptop", response.getDescription());
        assertEquals(new BigDecimal("2499.99"), response.getPrice());
        assertEquals(10, response.getQuantity());
        assertEquals("Electronics", response.getCategory());

        ArgumentCaptor<Product> productCaptor =
                ArgumentCaptor.forClass(Product.class);

        verify(productRepository).saveAndFlush(productCaptor.capture());

        Product capturedProduct = productCaptor.getValue();

        assertEquals("Apple MacBook Pro", capturedProduct.getName());
        assertEquals("Apple laptop", capturedProduct.getDescription());
        assertEquals(new BigDecimal("2499.99"), capturedProduct.getPrice());
        assertEquals(10, capturedProduct.getQuantity());
        assertEquals("Electronics", capturedProduct.getCategory());
    }

    /**
     * Verifies that creating a product with an existing name
     * throws DuplicateProductException and does not save the product.
     */
    @Test
    void createProduct_ShouldThrowException_WhenNameAlreadyExists() {

        ProductRequest request = createTestProductRequest("Samsung Galaxy S26",
                "Samsung smartphone", new BigDecimal("999.99"), 25,
                "Electronics");

        when(productRepository.existsByNormalizedName(request.getName()))
                .thenReturn(true);

        DuplicateProductException exception = assertThrows(
                DuplicateProductException.class,
                () -> productService.createProduct(request)
        );

        assertEquals(
                "Product already exists with name: Samsung Galaxy S26",
                exception.getMessage()
        );

        verify(productRepository)
                .existsByNormalizedName("Samsung Galaxy S26");

        verify(productRepository, never()).saveAndFlush(any(Product.class));
    }

    /**
     * Verifies that a database violation of the product-name
     * unique index is translated into DuplicateProductException.
     */
    @Test
    void createProduct_whenUniqueIndexIsViolated_throwsDuplicateProductException() {

        ProductRequest request = createTestProductRequest("Samsung Galaxy S26",
                "Samsung smartphone", new BigDecimal("999.99"), 25,
                "Electronics");

        SQLException sqlException = new SQLException(
                "Duplicate product name",
                "23505"
        );

        ConstraintViolationException constraintException =
                new ConstraintViolationException(
                        "Unique index violation",
                        sqlException,
                        "ux_products_normalized_name"
                );

        DataIntegrityViolationException dataException =
                new DataIntegrityViolationException(
                        "Database integrity violation",
                        constraintException
                );

        when(productRepository.saveAndFlush(any(Product.class)))
                .thenThrow(dataException);

        DuplicateProductException exception = assertThrows(
                DuplicateProductException.class,
                () -> productService.createProduct(request)
        );

        assertEquals(
                "Product already exists with name: Samsung Galaxy S26",
                exception.getMessage()
        );

        verify(productRepository).existsByNormalizedName(request.getName());
        verify(productRepository).saveAndFlush(any(Product.class));
    }

    /**
     * Verifies that a database integrity violation unrelated to
     * the product-name unique index is propagated unchanged.
     */
    @Test
    void createProduct_whenUnrelatedConstraintIsViolated_propagatesException() {

        ProductRequest request = createTestProductRequest("Samsung Galaxy S26",
                "Samsung smartphone", new BigDecimal("999.99"), 25,
                "Electronics");

        SQLException sqlException = new SQLException(
                "Other database constraint violation",
                "23502"
        );

        ConstraintViolationException constraintException =
                new ConstraintViolationException(
                        "Not-null constraint violation",
                        sqlException,
                        "products_category_not_null"
                );

        DataIntegrityViolationException dataException =
                new DataIntegrityViolationException(
                        "Database integrity violation",
                        constraintException
                );

        when(productRepository.saveAndFlush(any(Product.class)))
                .thenThrow(dataException);

        DataIntegrityViolationException exception = assertThrows(
                DataIntegrityViolationException.class,
                () -> productService.createProduct(request)
        );

        assertSame(dataException, exception);

        verify(productRepository)
                .existsByNormalizedName(request.getName());

        verify(productRepository)
                .saveAndFlush(any(Product.class));
    }

    /**
     * Verifies that an existing product is updated successfully
     * and the updated product details are returned.
     */
    @Test
    void updateProduct_whenProductExists_returnsUpdatedProductResponse() {

        Long productId = 2L;

        Product product = createTestProduct(
                productId,
                "Samsung Galaxy S25",
                "Samsung smartphone",
                new BigDecimal("999.99"),
                10,
                "Electronics"
        );

        ProductRequest request = createTestProductRequest("Samsung Galaxy S26 Ultra",
                "Updated Samsung flagship smartphone", new BigDecimal("1299.99"),
                15, "Electronics");

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));
        when(productRepository.existsByNormalizedName(request.getName()))
                .thenReturn(false);
        when(productRepository.saveAndFlush(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.updateProduct(productId, request);

        assertNotNull(response);
        assertEquals(productId, response.getId());
        assertEquals("Samsung Galaxy S26 Ultra", response.getName());
        assertEquals("Updated Samsung flagship smartphone", response.getDescription());
        assertEquals(new BigDecimal("1299.99"), response.getPrice());
        assertEquals(15, response.getQuantity());
        assertEquals("Electronics", response.getCategory());

        verify(productRepository).findById(productId);
        verify(productRepository)
                .existsByNormalizedName("Samsung Galaxy S26 Ultra");
        verify(productRepository).saveAndFlush(any(Product.class));
    }

    /**
     * Verifies that updating a product with a name already used
     * by another product throws DuplicateProductException
     * and does not save the changes.
     */
    @Test
    void updateProduct_whenNameAlreadyExists_throwsDuplicateProductException() {

        Long productId = 2L;

        Product existingProduct = createTestProduct(
                productId,
                "Apple iPhone 17",
                "Apple smartphone",
                new BigDecimal("1099.99"),
                20,
                "Electronics"
        );

        ProductRequest request = createTestProductRequest("Samsung Galaxy S26",
                "Samsung smartphone", new BigDecimal("999.99"), 25,
                "Electronics");

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(existingProduct));

        when(productRepository.existsByNormalizedName(request.getName()))
                .thenReturn(true);

        DuplicateProductException exception = assertThrows(
                DuplicateProductException.class,
                () -> productService.updateProduct(productId, request)
        );

        assertEquals(
                "Product already exists with name: Samsung Galaxy S26",
                exception.getMessage()
        );

        verify(productRepository).findById(productId);

        verify(productRepository)
                .existsByNormalizedName("Samsung Galaxy S26");

        verify(productRepository, never())
                .saveAndFlush(any(Product.class));

        assertEquals("Apple iPhone 17", existingProduct.getName());
    }

    /**
     * Verifies that updating a product without changing its name
     * succeeds without performing a duplicate-name lookup.
     */
    @Test
    void updateProduct_whenNameIsUnchanged_returnsUpdatedProductResponse() {

        Long productId = 2L;

        Product product = createTestProduct(
                productId,
                "Samsung Galaxy S26",
                "Samsung smartphone",
                new BigDecimal("999.99"),
                10,
                "Electronics"
        );

        ProductRequest request = createTestProductRequest("Samsung Galaxy S26",
                "Updated Samsung smartphone", new BigDecimal("1199.99"),
                20, "Electronics");

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

        when(productRepository.saveAndFlush(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response =
                productService.updateProduct(productId, request);

        assertNotNull(response);
        assertEquals(productId, response.getId());
        assertEquals("Samsung Galaxy S26", response.getName());
        assertEquals("Updated Samsung smartphone", response.getDescription());
        assertEquals(new BigDecimal("1199.99"), response.getPrice());
        assertEquals(20, response.getQuantity());
        assertEquals("Electronics", response.getCategory());

        verify(productRepository).findById(productId);

        verify(productRepository, never())
                .existsByNormalizedName(anyString());

        verify(productRepository).saveAndFlush(product);
    }

    /**
     * Verifies that updating a non-existing product throws
     * ProductNotFoundException.
     */
    @Test
    void updateProduct_whenProductDoesNotExist_throwsProductNotFoundException() {

        Long productId = 100L;

        ProductRequest request = createTestProductRequest("Samsung Galaxy S26 Ultra",
                "Updated Samsung flagship smartphone", new BigDecimal("1299.99"),
                15, "Electronics");

        when(productRepository.findById(productId))
                .thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> productService.updateProduct(productId, request)
        );

        verify(productRepository).findById(productId);
        verify(productRepository, never()).saveAndFlush(any(Product.class));
    }

    /**
     * Verifies that a product-name unique index violation during
     * an update is translated into DuplicateProductException.
     */
    @Test
    void updateProduct_whenUniqueIndexIsViolated_throwsDuplicateProductException() {

        Long productId = 2L;

        Product product = createTestProduct(
                productId,
                "Apple iPhone 17",
                "Apple smartphone",
                new BigDecimal("1099.99"),
                20,
                "Electronics"
        );

        ProductRequest request = createTestProductRequest("Samsung Galaxy S26",
                "Samsung smartphone", new BigDecimal("999.99"), 25,
                "Electronics");

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

        when(productRepository.existsByNormalizedName(request.getName()))
                .thenReturn(false);

        SQLException sqlException = new SQLException(
                "Duplicate product name",
                "23505"
        );

        ConstraintViolationException constraintException =
                new ConstraintViolationException(
                        "Unique index violation",
                        sqlException,
                        "ux_products_normalized_name"
                );

        DataIntegrityViolationException dataException =
                new DataIntegrityViolationException(
                        "Database integrity violation",
                        constraintException
                );

        when(productRepository.saveAndFlush(any(Product.class)))
                .thenThrow(dataException);

        DuplicateProductException exception = assertThrows(
                DuplicateProductException.class,
                () -> productService.updateProduct(productId, request)
        );

        assertEquals(
                "Product already exists with name: Samsung Galaxy S26",
                exception.getMessage()
        );

        verify(productRepository).findById(productId);
        verify(productRepository)
                .existsByNormalizedName(request.getName());
        verify(productRepository).saveAndFlush(any(Product.class));
    }

    /**
     * Verifies that a database integrity violation unrelated to
     * the product-name unique index is propagated unchanged
     * during a product update.
     */
    @Test
    void updateProduct_whenUnrelatedConstraintIsViolated_propagatesException() {

        Long productId = 2L;

        Product product = createTestProduct(
                productId,
                "Apple iPhone 17",
                "Apple smartphone",
                new BigDecimal("1099.99"),
                20,
                "Electronics"
        );

        ProductRequest request = createTestProductRequest("Samsung Galaxy S26",
                "Samsung smartphone", new BigDecimal("999.99"), 25,
                "Electronics");

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

        when(productRepository.existsByNormalizedName(request.getName()))
                .thenReturn(false);

        SQLException sqlException = new SQLException(
                "Other database constraint violation",
                "23502"
        );

        ConstraintViolationException constraintException =
                new ConstraintViolationException(
                        "Not-null constraint violation",
                        sqlException,
                        "products_category_not_null"
                );

        DataIntegrityViolationException dataException =
                new DataIntegrityViolationException(
                        "Database integrity violation",
                        constraintException
                );

        when(productRepository.saveAndFlush(any(Product.class)))
                .thenThrow(dataException);

        DataIntegrityViolationException exception = assertThrows(
                DataIntegrityViolationException.class,
                () -> productService.updateProduct(productId, request)
        );

        assertSame(dataException, exception);

        verify(productRepository).findById(productId);

        verify(productRepository)
                .existsByNormalizedName(request.getName());

        verify(productRepository)
                .saveAndFlush(any(Product.class));
    }

    /**
     * Verifies that an existing product is deleted successfully.
     */
    @Test
    void deleteProduct_whenProductExists_deletesProduct() {

        Long productId = 3L;

        Product product = createTestProduct(
                productId,
                "Samsung Galaxy S26 Ultra",
                "Samsung flagship smartphone",
                new BigDecimal("1299.99"),
                15,
                "Electronics"
        );

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

        productService.deleteProduct(productId);

        verify(productRepository).findById(productId);
        verify(productRepository).delete(product);
    }

    /**
     * Verifies that deleting a non-existing product throws
     * ProductNotFoundException.
     */
    @Test
    void deleteProduct_whenProductDoesNotExist_throwsProductNotFoundException() {

        Long productId = 100L;

        when(productRepository.findById(productId))
                .thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> productService.deleteProduct(productId)
        );

        verify(productRepository).findById(productId);
        verify(productRepository, never()).delete(any(Product.class));
    }

    /**
     * Verifies that searching products by name returns matching
     * products with the correct pagination information.
     */
    @Test
    void searchProducts_whenMatchingProductsExist_returnsPagedProductResponses() {

        String searchName = "Samsung";
        Pageable pageable = PageRequest.of(0, 10);
        Product product = createTestProduct(
                1L,
                "Samsung Galaxy S26 Ultra",
                "Samsung flagship smartphone",
                new BigDecimal("1299.99"),
                15,
                "Electronics"
        );

        Page<Product> productPage = new PageImpl<>(
                List.of(product),
                pageable,
                1
        );

        when(productRepository.findByNameContainingIgnoreCase(
                searchName, pageable))
                .thenReturn(productPage);

        Page<ProductResponse> response =
                productService.searchProducts(searchName, pageable);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());

        ProductResponse productResponse = response.getContent().get(0);

        assertEquals(1L, productResponse.getId());
        assertEquals("Samsung Galaxy S26 Ultra", productResponse.getName());
        assertEquals(new BigDecimal("1299.99"), productResponse.getPrice());

        assertEquals(1, response.getTotalElements());
        assertEquals(1, response.getTotalPages());
        assertEquals(0, response.getNumber());
        assertEquals(10, response.getSize());

        verify(productRepository)
                .findByNameContainingIgnoreCase(searchName, pageable);
    }

    /**
     * Verifies that searching for a product name with no matches
     * returns an empty page.
     */
    @Test
    void searchProducts_whenNoMatchingProductsExist_returnsEmptyPage() {

        String searchName = "Nokia";
        Pageable pageable = PageRequest.of(0, 10);

        when(productRepository.findByNameContainingIgnoreCase(
                searchName, pageable))
                .thenReturn(Page.empty(pageable));

        Page<ProductResponse> response =
                productService.searchProducts(searchName, pageable);

        assertNotNull(response);
        assertTrue(response.isEmpty());
        assertEquals(0, response.getTotalElements());
        assertEquals(0, response.getTotalPages());
        assertEquals(0, response.getNumber());
        assertEquals(10, response.getSize());

        verify(productRepository)
                .findByNameContainingIgnoreCase(searchName, pageable);
    }

    /**
     * Verifies that filtering products by category returns
     * matching products with correct pagination information.
     */
    @Test
    void getProductsByCategory_whenMatchingProductsExist_returnsPagedProductResponses() {

        String category = "Electronics";
        Pageable pageable = PageRequest.of(0, 10);

        Product product1 = createTestProduct(
                1L,
                "Samsung Galaxy S26 Ultra",
                "Samsung flagship smartphone",
                new BigDecimal("1299.99"),
                15,
                "Electronics"
        );

        Product product2 = createTestProduct(
                2L,
                "Apple iPhone 17",
                "Apple flagship smartphone",
                new BigDecimal("1099.99"),
                20,
                "Electronics"
        );

        Page<Product> productPage = new PageImpl<>(
                List.of(product1, product2),
                pageable,
                2
        );

        when(productRepository.findByCategoryIgnoreCase(category, pageable))
                .thenReturn(productPage);

        Page<ProductResponse> response =
                productService.getProductsByCategory(category, pageable);

        assertNotNull(response);
        assertEquals(2, response.getContent().size());

        assertEquals("Samsung Galaxy S26 Ultra",
                response.getContent().get(0).getName());

        assertEquals("Apple iPhone 17",
                response.getContent().get(1).getName());

        assertEquals("Electronics",
                response.getContent().get(0).getCategory());

        assertEquals("Electronics",
                response.getContent().get(1).getCategory());

        assertEquals(2, response.getTotalElements());
        assertEquals(1, response.getTotalPages());
        assertEquals(0, response.getNumber());
        assertEquals(10, response.getSize());

        verify(productRepository)
                .findByCategoryIgnoreCase(category, pageable);
    }

    /**
     * Verifies that filtering by a category with no matching products
     * returns an empty page.
     */
    @Test
    void getProductsByCategory_whenNoMatchingProductsExist_returnsEmptyPage() {

        String category = "Furniture";
        Pageable pageable = PageRequest.of(0, 10);

        when(productRepository.findByCategoryIgnoreCase(category, pageable))
                .thenReturn(Page.empty(pageable));

        Page<ProductResponse> response =
                productService.getProductsByCategory(category, pageable);

        assertNotNull(response);
        assertTrue(response.isEmpty());
        assertEquals(0, response.getTotalElements());
        assertEquals(0, response.getTotalPages());
        assertEquals(0, response.getNumber());
        assertEquals(10, response.getSize());

        verify(productRepository).findByCategoryIgnoreCase(category, pageable);
    }

    /**
     * Creates a test product with the specified field values.
     *
     * @param id the product ID to assign
     * @param name the product name
     * @param description the product description
     * @param price the unit price of the product
     * @param quantity the available inventory quantity
     * @param category the product category
     * @return a product populated with the specified test values
     */
    private static Product createTestProduct(
            Long id,
            String name,
            String description,
            BigDecimal price,
            int quantity,
            String category) {

        Product product = new Product();

        ReflectionTestUtils.setField(product, "id", id);

        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setQuantity(quantity);
        product.setCategory(category);

        return product;
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