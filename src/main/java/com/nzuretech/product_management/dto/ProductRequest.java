package com.nzuretech.product_management.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Represents the request payload for creating or updating a product.
 *
 * <p>Defines the product fields and validation constraints applied
 * to client-supplied data. Swagger schema annotations provide
 * additional API documentation for each field.</p>
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */
@Schema(description = "Request payload containing product details for creation or update")
public class ProductRequest {

    /**
     * Product name.
     */
    @Schema(
            description = "Name of the product",
            example = "Samsung Galaxy S26",
            requiredMode = Schema.RequiredMode.REQUIRED,
            maxLength = 150
    )
    @NotBlank(message = "Product name is required")
    @Size(max = 150, message = "Product name cannot exceed 150 characters")
    private String name;

    /**
     * Optional product description.
     */
    @Schema(
            description = "Detailed description of the product",
            example = "Samsung flagship smartphone - 512GB",
            maxLength = 1000
    )
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    /**
     * Product price.
     */
    @Schema(
            description = "Product price. Must be greater than zero with up to two decimal places",
            example = "1299.99",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minimum = "0.01",
            maximum = "99999999.99"
    )
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than zero")
    @Digits(integer = 8, fraction = 2,
            message = "Price must have at most 8 integer digits and 2 decimal places")
    private BigDecimal price;

    /**
     * Available product quantity.
     */
    @Schema(
            description = "Available quantity in stock. Cannot be negative",
            example = "25",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minimum = "0"
    )
    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    private Integer quantity;

    /**
     * Product category.
     */
    @Schema(
            description = "Category to which the product belongs",
            example = "Electronics",
            requiredMode = Schema.RequiredMode.REQUIRED,
            maxLength = 50
    )
    @NotBlank(message = "Category is required")
    @Size(max = 50, message = "Category cannot exceed 50 characters")
    private String category;

    /**
     * Default constructor required for request deserialization.
     */
    public ProductRequest() {
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
}
