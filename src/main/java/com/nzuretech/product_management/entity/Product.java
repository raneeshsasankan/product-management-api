package com.nzuretech.product_management.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a product stored in the products database table.
 *
 * <p>Maintains product information, inventory details, and
 * creation and modification timestamps.</p>
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Entity lifecycle callback executed before persisting a new entity.
     * Sets both creation and last-updated timestamps.
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    /**
     * Entity lifecycle callback executed before updating an existing entity.
     * Updates the last-updated timestamp.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Default no-argument constructor required by JPA.
     */
    public Product() {
    }

    /**
     * Gets the unique product ID.
     *
     * @return the product ID
     */
    public Long getId() {
        return id;
    }

    /**
     * Gets the product name.
     *
     * @return the product name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the product name.
     *
     * @param name the product name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the product description.
     *
     * @return the product description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the product description.
     *
     * @param description the product description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the unit price of the product.
     *
     * @return the price
     */
    public BigDecimal getPrice() {
        return price;
    }

    /**
     * Sets the unit price of the product.
     *
     * @param price the price
     */
    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    /**
     * Gets the available inventory quantity.
     *
     * @return the stock quantity
     */
    public Integer getQuantity() {
        return quantity;
    }

    /**
     * Sets the available inventory quantity.
     *
     * @param quantity the stock quantity
     */
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    /**
     * Gets the category classification of the product.
     *
     * @return the category name
     */
    public String getCategory() {
        return category;
    }

    /**
     * Sets the category classification of the product.
     *
     * @param category the category name
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * Gets the timestamp when this entity record was created.
     *
     * @return the creation timestamp
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Gets the timestamp when this entity record was last updated.
     *
     * @return the last update timestamp
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
