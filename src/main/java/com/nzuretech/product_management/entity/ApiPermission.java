package com.nzuretech.product_management.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Represents a permission required to access a specific
 * REST API endpoint.
 *
 * <p>Each API permission maps an HTTP method and URL pattern
 * to a permission defined in the permissions table.</p>
 *
 * <p>Examples include GET /api/products/** requiring
 * PRODUCT_READ and POST /api/products requiring
 * PRODUCT_CREATE.</p>
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */
@Entity
@Table(
        name = "api_permissions",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_api_permissions_method_pattern",
                columnNames = {"http_method", "url_pattern"}
        )
)
public class ApiPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(name = "url_pattern", nullable = false, length = 255)
    private String urlPattern;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;

    @Column(name = "created_at", nullable = false,
            insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false,
            insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    /**
     * Default constructor.
     */
    public ApiPermission() {
    }

    /**
     * Creates an API permission mapping.
     *
     * @param httpMethod the HTTP method, such as GET or POST
     * @param urlPattern the URL pattern to protect
     * @param permission the permission required for access
     */
    public ApiPermission(
            String httpMethod,
            String urlPattern,
            Permission permission) {

        this.httpMethod = httpMethod;
        this.urlPattern = urlPattern;
        this.permission = permission;
    }

    /**
     * Returns the unique API permission identifier.
     *
     * @return the identifier
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns the HTTP method.
     *
     * @return the HTTP method
     */
    public String getHttpMethod() {
        return httpMethod;
    }

    /**
     * Sets the HTTP method.
     *
     * @param httpMethod the HTTP method
     */
    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    /**
     * Returns the protected URL pattern.
     *
     * @return the URL pattern
     */
    public String getUrlPattern() {
        return urlPattern;
    }

    /**
     * Sets the protected URL pattern.
     *
     * @param urlPattern the URL pattern
     */
    public void setUrlPattern(String urlPattern) {
        this.urlPattern = urlPattern;
    }

    /**
     * Returns the permission required to access the endpoint.
     *
     * @return the required permission
     */
    public Permission getPermission() {
        return permission;
    }

    /**
     * Sets the permission required to access the endpoint.
     *
     * @param permission the required permission
     */
    public void setPermission(Permission permission) {
        this.permission = permission;
    }

    /**
     * Returns the timestamp when the mapping was created.
     *
     * @return the creation timestamp
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Returns the timestamp when the mapping was last updated.
     *
     * @return the last update timestamp
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
