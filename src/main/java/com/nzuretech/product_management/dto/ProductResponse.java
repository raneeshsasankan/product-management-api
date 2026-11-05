
package com.nzuretech.product_management.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents product details returned by the REST API.
 *
 * <p>Includes product information, the system-generated identifier,
 * and audit timestamps. Swagger schema annotations provide
 * documentation for the API response fields.</p>
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */
@Schema(description = "Product details returned by the Product Management API")
public class ProductResponse {

    /**
     * Unique system-generated product identifier.
     */
    @Schema(
            description = "Unique identifier assigned to the product",
            example = "1",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private Long id;

    /**
     * Product name.
     */
    @Schema(
            description = "Name of the product",
            example = "Samsung Galaxy S26"
    )
    private String name;

    /**
     * Product description.
     */
    @Schema(
            description = "Detailed description of the product",
            example = "Samsung flagship smartphone - 512GB"
    )
    private String description;

    /**
     * Product price.
     */
    @Schema(
            description = "Selling price of the product",
            example = "1299.99"
    )
    private BigDecimal price;

    /**
     * Available product quantity.
     */
    @Schema(
            description = "Available quantity in stock",
            example = "25"
    )
    private Integer quantity;

    /**
     * Product category.
     */
    @Schema(
            description = "Category to which the product belongs",
            example = "Electronics"
    )
    private String category;

    /**
     * Timestamp when the product was created.
     */
    @Schema(
            description = "Date and time when the product was created",
            example = "2026-11-04T10:30:00",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private LocalDateTime createdAt;

    /**
     * Timestamp when the product was last updated.
     */
    @Schema(
            description = "Date and time when the product was last updated",
            example = "2026-11-04T14:45:00",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private LocalDateTime updatedAt;

    /**
     * Default constructor.
     */
    public ProductResponse() {
    }

    /**
     * Creates a product response with all product details.
     *
     * @param id unique product identifier
     * @param name product name
     * @param description product description
     * @param price product price
     * @param quantity available quantity
     * @param category product category
     * @param createdAt product creation timestamp
     * @param updatedAt product last update timestamp
     */
    public ProductResponse(Long id, String name, String description,
                           BigDecimal price, Integer quantity, String category,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.quantity = quantity;
        this.category = category;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Gets the product identifier.
     *
     * @return unique product identifier
     */
    public Long getId() {
        return id;
    }

    /**
     * Gets the product name.
     *
     * @return product name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the product name.
     *
     * @param name product name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the product description.
     *
     * @return product description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the product description.
     *
     * @param description product description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the product price.
     *
     * @return product price
     */
    public BigDecimal getPrice() {
        return price;
    }

    /**
     * Sets the product price.
     *
     * @param price product price
     */
    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    /**
     * Gets the available product quantity.
     *
     * @return available quantity
     */
    public Integer getQuantity() {
        return quantity;
    }

    /**
     * Sets the available product quantity.
     *
     * @param quantity available quantity
     */
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    /**
     * Gets the product category.
     *
     * @return product category
     */
    public String getCategory() {
        return category;
    }

    /**
     * Sets the product category.
     *
     * @param category product category
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * Gets the product creation timestamp.
     *
     * @return product creation timestamp
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Gets the product last update timestamp.
     *
     * @return product last update timestamp
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
