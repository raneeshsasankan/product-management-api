package com.nzuretech.product_management.security;

import com.nzuretech.product_management.config.PostgresTestConfig;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PostgreSQL integration tests for dynamic API permissions.
 *
 * <p>Verifies that modifying an endpoint permission
 * mapping changes authorization decisions without
 * restarting the authorization manager.</p>
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */
@DataJpaTest
@Import({
        PostgresTestConfig.class,
        DatabasePermissionAuthorizationManager.class
})
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
class DatabasePermissionIntegrationTest {

    @Autowired
    private DatabasePermissionAuthorizationManager authorizationManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    /**
     * Verifies that changing the required database
     * permission immediately changes authorization.
     *
     * <p>The test runs inside a rollback transaction,
     * so the original permission mapping is preserved.</p>
     */
    @Test
    void authorize_whenDatabasePermissionChanges_updatesDecision() {

        Authentication viewer =
                UsernamePasswordAuthenticationToken.authenticated(
                        "viewer",
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_VIEWER"),
                                new SimpleGrantedAuthority("PRODUCT_READ")
                        )
                );

        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        "GET",
                        "/api/products"
                );

        request.setServletPath("/api/products");

        RequestAuthorizationContext context =
                new RequestAuthorizationContext(request);

        assertTrue(
                authorizationManager.authorize(
                        () -> viewer,
                        context
                ).isGranted()
        );

        int updated = jdbcTemplate.update("""
                UPDATE api_permissions
                SET permission_id = (
                    SELECT id
                    FROM permissions
                    WHERE name = 'PRODUCT_DELETE'
                )
                WHERE http_method = 'GET'
                  AND url_pattern = '/api/products/**'
                """);

        assertEquals(1, updated);

        entityManager.clear();

        assertFalse(
                authorizationManager.authorize(
                        () -> viewer,
                        context
                ).isGranted()
        );
    }
}
