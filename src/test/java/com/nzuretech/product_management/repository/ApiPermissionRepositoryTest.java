package com.nzuretech.product_management.repository;

import com.nzuretech.product_management.entity.ApiPermission;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PostgreSQL integration tests for ApiPermissionRepository.
 *
 * <p>Verifies that the API endpoint permissions seeded
 * by Flyway can be retrieved with their associated
 * permission details.</p>
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */
@DataJpaTest
@Import(ProductRepositoryTest.ContainerConfig.class)
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
class ApiPermissionRepositoryTest {

    @Autowired
    private ApiPermissionRepository apiPermissionRepository;

    /**
     * Verifies that all four product API permission mappings
     * are retrieved with their associated permissions.
     */
    @Test
    void findAll_returnsProductApiPermissionMappings() {

        List<ApiPermission> mappings =
                apiPermissionRepository.findAll();

        Map<String, String> permissionsByEndpoint = mappings.stream()
                .collect(Collectors.toMap(
                        mapping -> mapping.getHttpMethod()
                                + " " + mapping.getUrlPattern(),
                        mapping -> mapping.getPermission().getName()
                ));

        assertEquals(4, mappings.size());

        assertEquals(
                Map.of(
                        "GET /api/products/**", "PRODUCT_READ",
                        "POST /api/products", "PRODUCT_CREATE",
                        "PUT /api/products/*", "PRODUCT_UPDATE",
                        "DELETE /api/products/*", "PRODUCT_DELETE"
                ),
                permissionsByEndpoint
        );
    }
}
