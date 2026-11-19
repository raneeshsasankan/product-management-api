package com.nzuretech.product_management.security;

import com.nzuretech.product_management.entity.ApiPermission;
import com.nzuretech.product_management.entity.Permission;
import com.nzuretech.product_management.repository.ApiPermissionRepository;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for database-driven API permission authorization.
 *
 * <p>Verifies request matching, permission checking,
 * and default-deny authorization behavior.</p>
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class DatabasePermissionAuthorizationManagerTest {

    @Mock
    private ApiPermissionRepository apiPermissionRepository;

    private DatabasePermissionAuthorizationManager authorizationManager;

    /**
     * Initializes the authorization manager before each test.
     */
    @BeforeEach
    void setUp() {
        authorizationManager =
                new DatabasePermissionAuthorizationManager(
                        apiPermissionRepository
                );
    }

    /**
     * Verifies that ADMIN can delete products.
     */
    @Test
    void authorize_whenAdminDeletesProduct_returnsAllowed() {

        mockMapping(
                "DELETE",
                "/api/products/*",
                "PRODUCT_DELETE"
        );

        boolean granted = authorize(
                authentication("admin", "PRODUCT_DELETE"),
                "DELETE",
                "/api/products/1"
        );

        assertTrue(granted);
    }

    /**
     * Verifies that MANAGER can update products.
     */
    @Test
    void authorize_whenManagerUpdatesProduct_returnsAllowed() {

        mockMapping(
                "PUT",
                "/api/products/*",
                "PRODUCT_UPDATE"
        );

        boolean granted = authorize(
                authentication("manager", "PRODUCT_UPDATE"),
                "PUT",
                "/api/products/1"
        );

        assertTrue(granted);
    }

    /**
     * Verifies that MANAGER cannot delete products
     * without PRODUCT_DELETE permission.
     */
    @Test
    void authorize_whenManagerDeletesProduct_returnsDenied() {

        mockMapping(
                "DELETE",
                "/api/products/*",
                "PRODUCT_DELETE"
        );

        boolean granted = authorize(
                authentication("manager", "PRODUCT_UPDATE"),
                "DELETE",
                "/api/products/1"
        );

        assertFalse(granted);
    }

    /**
     * Verifies that VIEWER can read products.
     */
    @Test
    void authorize_whenViewerReadsProducts_returnsAllowed() {

        mockMapping(
                "GET",
                "/api/products/**",
                "PRODUCT_READ"
        );

        boolean granted = authorize(
                authentication("viewer", "PRODUCT_READ"),
                "GET",
                "/api/products"
        );

        assertTrue(granted);
    }

    /**
     * Verifies that VIEWER cannot create products.
     */
    @Test
    void authorize_whenViewerCreatesProduct_returnsDenied() {

        mockMapping(
                "POST",
                "/api/products",
                "PRODUCT_CREATE"
        );

        boolean granted = authorize(
                authentication("viewer", "PRODUCT_READ"),
                "POST",
                "/api/products"
        );

        assertFalse(granted);
    }

    /**
     * Verifies that requests without a matching
     * permission mapping are denied.
     */
    @Test
    void authorize_whenNoMappingMatches_returnsDenied() {

        when(apiPermissionRepository.findAll())
                .thenReturn(List.of());

        boolean granted = authorize(
                authentication("admin", "PRODUCT_READ"),
                "GET",
                "/api/unknown"
        );

        assertFalse(granted);
    }

    /**
     * Verifies that unauthenticated requests
     * are denied without querying permissions.
     */
    @Test
    void authorize_whenUnauthenticated_returnsDenied() {

        boolean granted = authorize(
                null,
                "GET",
                "/api/products"
        );

        assertFalse(granted);

        verifyNoInteractions(apiPermissionRepository);
    }

    /**
     * Verifies that a permission mapping for one HTTP method
     * cannot authorize a request using a different method.
     */
    @Test
    void authorize_whenHttpMethodDoesNotMatch_returnsDenied() {

        ApiPermission mapping = mock(ApiPermission.class);

        when(mapping.getHttpMethod()).thenReturn("GET");
        when(mapping.getUrlPattern())
                .thenReturn("/api/products/**");

        when(apiPermissionRepository.findAll())
                .thenReturn(List.of(mapping));

        boolean granted = authorize(
                authentication("viewer", "PRODUCT_READ"),
                "POST",
                "/api/products"
        );

        assertFalse(granted);
    }

    /**
     * Verifies that a database lookup failure does not
     * produce a successful authorization decision.
     *
     * <p>The current implementation propagates database
     * exceptions rather than granting access.</p>
     */
    @Test
    void authorize_whenDatabaseLookupFails_doesNotGrantAccess() {

        when(apiPermissionRepository.findAll())
                .thenThrow(new IllegalStateException(
                        "Database unavailable"
                ));

        assertThrows(
                IllegalStateException.class,
                () -> authorize(
                        authentication("admin", "PRODUCT_DELETE"),
                        "DELETE",
                        "/api/products/1"
                )
        );
    }

    /**
     * Configures a mocked API permission mapping.
     *
     * @param method HTTP method
     * @param path URL pattern
     * @param permissionName required permission
     */
    private void mockMapping(
            String method,
            String path,
            String permissionName) {

        ApiPermission mapping = mock(ApiPermission.class);
        Permission permission = mock(Permission.class);

        when(mapping.getHttpMethod()).thenReturn(method);
        when(mapping.getUrlPattern()).thenReturn(path);
        when(mapping.getPermission()).thenReturn(permission);
        when(permission.getName()).thenReturn(permissionName);

        when(apiPermissionRepository.findAll())
                .thenReturn(List.of(mapping));
    }

    /**
     * Creates an authenticated user with the specified authorities.
     *
     * @param username authenticated username
     * @param permissions granted permission names
     * @return authenticated user
     */
    private Authentication authentication(
            String username,
            String... permissions) {

        return UsernamePasswordAuthenticationToken.authenticated(
                username,
                null,
                Arrays.stream(permissions)
                        .map(SimpleGrantedAuthority::new)
                        .toList()
        );
    }

    /**
     * Evaluates authorization for an HTTP request.
     *
     * @param authentication current user authentication
     * @param method HTTP method
     * @param path request path
     * @return whether access is granted
     */
    private boolean authorize(
            Authentication authentication,
            String method,
            String path) {

        MockHttpServletRequest request =
                new MockHttpServletRequest(method, path);

        request.setServletPath(path);

        RequestAuthorizationContext context =
                new RequestAuthorizationContext(request);

        return authorizationManager.authorize(
                () -> authentication,
                context
        ).isGranted();
    }
}
