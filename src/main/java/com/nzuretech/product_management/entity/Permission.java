package com.nzuretech.product_management.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Represents an application permission used for
 * role-based access control.
 *
 * Examples include PRODUCT_READ, PRODUCT_CREATE,
 * PRODUCT_UPDATE, and PRODUCT_DELETE.
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */
@Entity
@Table(name = "permissions")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false,
            unique = true, length = 100)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "created_at", nullable = false,
            insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false,
            insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    /**
     * Default no-argument constructor.
     */
    public Permission() {
    }

    /**
     * Creates a permission with the specified name and description.
     *
     * @param name the unique permission name
     * @param description the permission description
     */
    public Permission(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /**
     * Gets the unique permission ID.
     *
     * @return the permission ID
     */
    public Long getId() {
        return id;
    }

    /**
     * Gets the permission name.
     *
     * @return the permission name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the permission name.
     *
     * @param name the permission name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the permission description.
     *
     * @return the permission description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of this permission.
     *
     * @param description the permission description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the timestamp when the permission was created.
     *
     * @return the creation timestamp
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Returns the timestamp when the permission was last updated.
     *
     * @return the last update timestamp
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
