package com.nzuretech.product_management.repository;

import com.nzuretech.product_management.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for managing application permissions.
 *
 * <p>Provides database operations for permissions used
 * by the role-based access control system.</p>
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */
public interface PermissionRepository
        extends JpaRepository<Permission, Long> {

    /**
     * Finds a permission by its unique name.
     *
     * @param name the permission name
     * @return an Optional containing the matching permission,
     *         or empty if no permission exists
     */
    Optional<Permission> findByName(String name);
}
