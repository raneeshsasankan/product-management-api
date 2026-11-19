package com.nzuretech.product_management.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents an application user for authentication
 * and role-based access control.
 *
 * <p>Each user can be assigned multiple roles through
 * the user_roles join table.</p>
 *
 * <p>User passwords are stored as secure hashes.</p>
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */
@Entity
@Table(name = "users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false,
            insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false,
            insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(name = "token_version", nullable = false)
    private Long tokenVersion = 0L;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(
                    name = "user_id",
                    referencedColumnName = "id"
            ),
            inverseJoinColumns = @JoinColumn(
                    name = "role_id",
                    referencedColumnName = "id"
            )
    )
    private Set<Role> roles = new HashSet<>();

    /**
     * Default constructor required by JPA.
     */
    public AppUser() {
    }

    /**
     * Creates a user with authentication details.
     *
     * @param username the login username
     * @param passwordHash the encoded password
     */
    public AppUser(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    /**
     * Returns the unique user identifier.
     *
     * @return the user identifier
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns the user's login name.
     *
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the user's login name.
     *
     * @param username the username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Returns the stored password hash.
     *
     * @return the encoded password
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Sets the stored password hash.
     *
     * @param passwordHash the encoded password
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * Indicates whether the account is enabled.
     *
     * @return true if the account is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enables or disables the account.
     *
     * @param enabled the account status
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns the account creation timestamp.
     *
     * @return the creation timestamp
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Returns the last update timestamp.
     *
     * @return the update timestamp
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Returns the current security token version.
     *
     * @return the user's token version
     */
    public Long getTokenVersion() {
        return tokenVersion;
    }

    /**
     * Updates the user's security token version.
     *
     * @param tokenVersion the new token version
     */
    public void setTokenVersion(Long tokenVersion) {
        this.tokenVersion = tokenVersion;
    }

    /**
     * Returns the roles assigned to this user.
     *
     * @return the assigned roles
     */
    public Set<Role> getRoles() {
        return roles;
    }

    /**
     * Sets the roles assigned to this user.
     *
     * @param roles the roles to assign
     */
    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }
}
