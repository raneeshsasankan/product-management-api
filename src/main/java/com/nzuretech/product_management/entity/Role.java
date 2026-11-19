package com.nzuretech.product_management.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents an application role used for role-based access control.
 *
 * <p>A role groups permissions that determine which
 * operations a user is authorized to perform.</p>
 *
 * <p>Examples include ADMIN, MANAGER, USER, and VIEWER.</p>
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */
@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false,
            unique = true, length = 50)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "created_at", nullable = false,
            insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false,
            insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(
                    name = "role_id",
                    referencedColumnName = "id"
            ),
            inverseJoinColumns = @JoinColumn(
                    name = "permission_id",
                    referencedColumnName = "id"
            )
    )

    private Set<Permission> permissions = new HashSet<>();

    /**
     * Default no-argument constructor.
     */
    public Role() {
    }

    /**
     * Creates a role with a name and description.
     *
     * @param name the unique role name
     * @param description the role description
     */
    public Role(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /**
     * Returns the unique identifier of the role.
     *
     * @return the role identifier
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns the role name.
     *
     * @return the role name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the role name.
     *
     * @param name the role name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the role description.
     *
     * @return the role description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the role description.
     *
     * @param description the role description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the timestamp when the role was created.
     *
     * @return the creation timestamp
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Returns the timestamp when the role was last updated.
     *
     * @return the last update timestamp
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Returns the permissions assigned to this role.
     *
     * @return the set of assigned permissions
     */
    public Set<Permission> getPermissions() {
        return permissions;
    }

    /**
     * Sets the permissions assigned to this role.
     *
     * @param permissions the permissions to assign
     */
    public void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions;
    }
}
