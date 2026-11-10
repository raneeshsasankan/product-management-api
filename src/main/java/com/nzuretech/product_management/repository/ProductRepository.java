package com.nzuretech.product_management.repository;

import com.nzuretech.product_management.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


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
     * ignoring case sensitivity, using pagination and sorting.
     *
     * @param name the search text
     * @param pageable the pagination and sorting information
     * @return a page containing matching products
     */
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Finds products belonging to the specified category,
     * ignoring case sensitivity, using pagination and sorting.
     *
     * @param category the category name
     * @param pageable the pagination and sorting information
     * @return a page containing matching products
     */
    Page<Product> findByCategoryIgnoreCase(String category, Pageable pageable);


    /**
     * Checks whether a product exists with the specified name,
     * ignoring case differences and surrounding whitespace.
     *
     * @param name the product name to check
     * @return true if a matching product exists, otherwise false
     */
    @Query("""
       SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
       FROM Product p
       WHERE LOWER(TRIM(p.name)) = LOWER(TRIM(:name))
       """)
    boolean existsByNormalizedName(@Param("name") String name);

}
