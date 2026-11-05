package com.nzuretech.product_management.repository;

import com.nzuretech.product_management.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository for Product entities.
 *
 * <p>Provides standard CRUD operations and custom derived queries
 * for product name searches and category filtering.</p>
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Finds products whose names contain the specified text,
     * ignoring case sensitivity.
     *
     * @param name the search text
     * @return matching products
     */
    List<Product> findByNameContainingIgnoreCase(String name);

    /**
     * Finds products belonging to the specified category,
     * ignoring case sensitivity.
     *
     * @param category the category name
     * @return matching products
     */
    List<Product> findByCategoryIgnoreCase(String category);
}
