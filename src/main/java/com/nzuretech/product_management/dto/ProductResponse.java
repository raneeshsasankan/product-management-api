package com.nzuretech.product_management.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents product details returned by the REST API.
 *
 * <p>Includes product information, the system-generated identifier,
 * and audit timestamps.</p>
 */
public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer quantity;
    private String category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Default constructor. */
    public ProductResponse() {
    }

    /**
     * Creates a product response with all product details.
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

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
