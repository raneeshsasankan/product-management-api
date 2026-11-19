package com.nzuretech.product_management.repository;

import com.nzuretech.product_management.entity.ApiPermission;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for managing API endpoint permission mappings.
 *
 * <p>Provides access to endpoint authorization rules
 * stored in the database, including the permissions
 * required to access each endpoint.</p>
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */
public interface ApiPermissionRepository
        extends JpaRepository<ApiPermission, Long> {

    /**
     * Retrieves all API permission mappings with
     * their associated permission details.
     *
     * <p>Uses an entity graph to load the permission
     * associated with each API endpoint mapping.</p>
     *
     * @return all API permission mappings with permissions
     */
    @Override
    @EntityGraph(attributePaths = {"permission"})
    List<ApiPermission> findAll();
}
