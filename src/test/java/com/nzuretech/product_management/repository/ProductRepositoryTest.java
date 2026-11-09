package com.nzuretech.product_management.repository;

import com.nzuretech.product_management.entity.Product;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PostgreSQL integration tests for ProductRepository.
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */
@DataJpaTest
@Import(ProductRepositoryTest.ContainerConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    /**
     * Configures a Spring-managed PostgreSQL test container.
     */
    @TestConfiguration(proxyBeanMethods = false)
    static class ContainerConfig {

        @Bean
        @ServiceConnection
        PostgreSQLContainer postgresContainer() {
            return new PostgreSQLContainer("postgres:18-alpine");
        }
    }

    /**
     * Verifies that a product is persisted with a generated ID
     * and populated audit timestamps.
     */
    @Test
    void save_whenValidProduct_persistsProductWithTimestamps() {

        Product product = createTestProduct("Samsung Galaxy S26 Ultra",
                "Samsung flagship smartphone", new BigDecimal("1299.99"),
                15, "Electronics");

        Product savedProduct = productRepository.saveAndFlush(product);

        assertNotNull(savedProduct.getId());
        assertEquals("Samsung Galaxy S26 Ultra", savedProduct.getName());
        assertNotNull(savedProduct.getCreatedAt());
        assertNotNull(savedProduct.getUpdatedAt());
    }

    /**
     * Verifies that updating a product preserves its creation timestamp
     * and updates its modification timestamp.
     */
    @Test
    void update_whenProductExists_updatesOnlyModifiedTimestamp() {

        // Arrange
        Product product = createTestProduct("Samsung Galaxy S25",
                "Samsung smartphone", new BigDecimal("999.99"),
                10, "Electronics");

        Product savedProduct = productRepository.saveAndFlush(product);

        Long productId = savedProduct.getId();

        entityManager.clear();

        Product persistedProduct = productRepository.findById(productId)
                .orElseThrow();

        LocalDateTime originalCreatedAt = persistedProduct.getCreatedAt();
        LocalDateTime originalUpdatedAt = persistedProduct.getUpdatedAt();

        entityManager.clear();

        Product existingProduct = productRepository.findById(productId)
                .orElseThrow();

        existingProduct.setPrice(new BigDecimal("1299.99"));

        productRepository.saveAndFlush(existingProduct);

        entityManager.clear();

        Product updatedProduct = productRepository.findById(productId)
                .orElseThrow();

        assertEquals(originalCreatedAt, updatedProduct.getCreatedAt());
        assertTrue(updatedProduct.getUpdatedAt().isAfter(originalUpdatedAt));
        assertEquals(new BigDecimal("1299.99"), updatedProduct.getPrice());
    }

    /**
     * Verifies that products can be searched by name
     * without case sensitivity.
     */
    @Test
    void findByNameContainingIgnoreCase_whenMatchingProductsExist_returnsProducts() {

        // Arrange
        Product product1 = createTestProduct("Samsung Galaxy S26 Ultra",
                "Samsung smartphone", new BigDecimal("1299.99"),
                15, "Electronics");

        Product product2 = createTestProduct("Apple iPhone 17",
                "Apple smartphone", new BigDecimal("1099.99"),
                20, "Electronics");

        productRepository.saveAllAndFlush(List.of(product1, product2));

        Pageable pageable = PageRequest.of(0, 10);

        Page<Product> result =
                productRepository.findByNameContainingIgnoreCase(
                        "sAmSuNg", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals("Samsung Galaxy S26 Ultra",
                result.getContent().get(0).getName());
    }

    /**
     * Verifies that products are filtered by category
     * without case sensitivity.
     */
    @Test
    void findByCategoryIgnoreCase_whenMatchingProductsExist_returnsProducts() {

        Product product1 = createTestProduct("Samsung Galaxy S26 Ultra",
                "Samsung smartphone", new BigDecimal("1299.99"),
                15, "Electronics");

        Product product2 = createTestProduct("Office Chair",
                "Office Chair", new BigDecimal("199.99"),
                10, "Furniture");

        productRepository.saveAllAndFlush(List.of(product1, product2));

        Pageable pageable = PageRequest.of(0, 10);

        Page<Product> result =
                productRepository.findByCategoryIgnoreCase(
                        "eLeCtRoNiCs", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals("Samsung Galaxy S26 Ultra",
                result.getContent().get(0).getName());
        assertEquals("Electronics",
                result.getContent().get(0).getCategory());
    }

    /**
     * Verifies that products are returned in the correct
     * order when pagination and sorting are applied.
     */
    @Test
    void findAll_whenPaginationAndSortingApplied_returnsCorrectPage() {

        Product product1 = createTestProduct("Samsung Galaxy S26 Ultra",
                "Samsung smartphone", new BigDecimal("1299.99"),
                15, "Electronics");

        Product product2 = createTestProduct("Apple iPhone 17",
                "Apple smartphone", new BigDecimal("1099.99"),
                20, "Electronics");

        Product product3 = createTestProduct("Google Pixel 10",
                "Google smartphone", new BigDecimal("899.99"),
                12, "Electronics");

        productRepository.saveAllAndFlush(
                List.of(product1, product2, product3)
        );

        Pageable pageable = PageRequest.of(
                0,
                2,
                Sort.by(Sort.Direction.ASC, "price")
        );

        Page<Product> result = productRepository.findAll(pageable);

        assertEquals(2, result.getContent().size());
        assertEquals(3, result.getTotalElements());
        assertEquals(2, result.getTotalPages());
        assertEquals(0, result.getNumber());
        assertEquals(2, result.getSize());

        assertEquals("Google Pixel 10",
                result.getContent().get(0).getName());

        assertEquals("Apple iPhone 17",
                result.getContent().get(1).getName());
    }

    /**
     * Creates a product entity with the specified test values.
     *
     * @param name the product name
     * @param description the product description
     * @param price the product price
     * @param quantity the available quantity
     * @param category the product category
     * @return a product entity ready for persistence
     */
    private static Product createTestProduct(
            String name,
            String description,
            BigDecimal price,
            int quantity,
            String category) {

        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setQuantity(quantity);
        product.setCategory(category);

        return product;
    }
}