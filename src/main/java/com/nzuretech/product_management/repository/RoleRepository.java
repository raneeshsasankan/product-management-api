package com.nzuretech.product_management.repository;

import com.nzuretech.product_management.entity.Role;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for managing application roles.
 *
 * <p>Provides database operations for roles and supports
 * retrieving their associated permissions for role-based
 * access control.</p>
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Finds a role by its unique name.
     *
     * @param name the role name
     * @return the matching role, or an empty Optional
     */
    Optional<Role> findByName(String name);

    /**
     * Finds a role by name and loads its assigned permissions.
     *
     * <p>The entity graph ensures that permissions are loaded
     * with the role, avoiding lazy-loading problems when the
     * permissions are accessed later.</p>
     *
     * @param name the role name
     * @return the matching role with permissions,
     *         or an empty Optional
     */
    @EntityGraph(attributePaths = {"permissions"})
    Optional<Role> findWithPermissionsByName(String name);
}
