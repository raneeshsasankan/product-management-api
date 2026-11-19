package com.nzuretech.product_management.security;

import com.nzuretech.product_management.entity.ApiPermission;
import com.nzuretech.product_management.entity.Permission;
import com.nzuretech.product_management.repository.ApiPermissionRepository;

import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Provides API permission mappings for security tests.
 *
 * <p>Simulates the four endpoint permission mappings
 * inserted into PostgreSQL by Flyway V4.</p>
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */
public final class ApiPermissionTestFixtures {

    /**
     * Prevents instantiation of this utility class.
     */
    private ApiPermissionTestFixtures() {
    }

    /**
     * Configures mocked product API permission mappings.
     *
     * @param repository mocked API permission repository
     */
    public static void stubProductPermissions(
            ApiPermissionRepository repository) {

        List<ApiPermission> mappings = List.of(
                mapping("GET", "/api/products/**", "PRODUCT_READ"),
                mapping("POST", "/api/products", "PRODUCT_CREATE"),
                mapping("PUT", "/api/products/*", "PRODUCT_UPDATE"),
                mapping("DELETE", "/api/products/*", "PRODUCT_DELETE")
        );

        lenient().when(repository.findAll())
                .thenReturn(mappings);
    }

    /**
     * Creates a mocked API permission mapping.
     *
     * @param method HTTP method
     * @param pattern endpoint URL pattern
     * @param permissionName required permission
     * @return mocked mapping
     */
    private static ApiPermission mapping(
            String method,
            String pattern,
            String permissionName) {

        ApiPermission mapping = mock(
                ApiPermission.class,
                withSettings().lenient()
        );

        Permission permission = mock(
                Permission.class,
                withSettings().lenient()
        );

        when(mapping.getHttpMethod()).thenReturn(method);
        when(mapping.getUrlPattern()).thenReturn(pattern);
        when(mapping.getPermission()).thenReturn(permission);
        when(permission.getName()).thenReturn(permissionName);

        return mapping;
    }
}
