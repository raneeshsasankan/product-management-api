package com.nzuretech.product_management.repository;

import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import com.nzuretech.product_management.entity.Role;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for RoleRepository using PostgreSQL.
 *
 * <p>Verifies that Flyway-seeded roles and their permissions
 * can be retrieved correctly through Spring Data JPA.</p>
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
class RoleRepositoryTest {

    @Autowired
    private RoleRepository roleRepository;

    /**
     * Verifies that the MANAGER role is retrieved
     * with all three assigned permissions.
     */
    @Test
    void findWithPermissionsByName_whenManagerExists_returnsPermissions() {

        Role role = roleRepository
                .findWithPermissionsByName("MANAGER")
                .orElseThrow();

        Set<String> permissionNames = role.getPermissions()
                .stream()
                .map(permission -> permission.getName())
                .collect(Collectors.toSet());

        assertEquals("MANAGER", role.getName());

        assertEquals(
                Set.of(
                        "PRODUCT_READ",
                        "PRODUCT_CREATE",
                        "PRODUCT_UPDATE"
                ),
                permissionNames
        );
    }

    /**
     * Verifies that an unknown role returns an empty Optional.
     */
    @Test
    void findWithPermissionsByName_whenRoleDoesNotExist_returnsEmpty() {

        assertTrue(
                roleRepository
                        .findWithPermissionsByName("UNKNOWN_ROLE")
                        .isEmpty()
        );
    }
}
