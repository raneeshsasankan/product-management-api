package com.nzuretech.product_management.repository;

import com.nzuretech.product_management.entity.AppUser;
import com.nzuretech.product_management.entity.Role;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PostgreSQL integration tests for AppUserRepository.
 *
 * <p>Verifies normalized username lookup and retrieval
 * of assigned roles and permissions.</p>
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
class AppUserRepositoryTest {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private RoleRepository roleRepository;

    /**
     * Verifies that a user can be retrieved by normalized
     * username with assigned roles and permissions.
     */
    @Test
    void findByNormalizedUsername_whenUserExists_returnsRolesAndPermissions() {

        Role managerRole = roleRepository.findByName("MANAGER")
                .orElseThrow();

        AppUser user = new AppUser(
                "testmanager",
                "$2a$10$testPasswordHash"
        );

        user.setRoles(Set.of(managerRole));

        appUserRepository.saveAndFlush(user);

        AppUser foundUser = appUserRepository
                .findByNormalizedUsernameWithRolesAndPermissions(
                        " TESTMANAGER "
                )
                .orElseThrow();

        assertEquals("testmanager", foundUser.getUsername());
        assertTrue(foundUser.isEnabled());

        assertEquals(1, foundUser.getRoles().size());

        Set<String> permissionNames = foundUser.getRoles()
                .stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getName())
                .collect(Collectors.toSet());

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
     * Verifies that an unknown username returns
     * an empty Optional.
     */
    @Test
    void findByNormalizedUsername_whenUserDoesNotExist_returnsEmpty() {

        assertTrue(
                appUserRepository
                        .findByNormalizedUsernameWithRolesAndPermissions(
                                "unknownuser"
                        )
                        .isEmpty()
        );
    }

    /**
     * Verifies that newly created users have an initial
     * token version of zero and that JPA correctly
     * persists and retrieves the token version.
     */
    @Test
    void findByNormalizedUsername_whenUserExists_returnsTokenVersion() {

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow();

        AppUser user = new AppUser(
                "tokenversionuser",
                "test-password-hash"
        );

        user.setRoles(Set.of(userRole));

        appUserRepository.saveAndFlush(user);

        AppUser foundUser = appUserRepository
                .findByNormalizedUsernameWithRolesAndPermissions(
                        "tokenversionuser"
                )
                .orElseThrow();

        assertEquals(0L, foundUser.getTokenVersion());
    }

    /**
     * Verifies that the lightweight JWT validation query
     * retrieves the correct user account and token version.
     *
     * <p>This test uses PostgreSQL through Testcontainers.</p>
     */
    @Test
    void findByNormalizedUsernameForTokenValidation_returnsUser() {

        AppUser user = new AppUser(
                "JwtValidationUser",
                "encoded-test-password"
        );

        user.setEnabled(true);
        user.setTokenVersion(5L);

        appUserRepository.saveAndFlush(user);

        Optional<AppUser> result = appUserRepository
                .findByNormalizedUsernameForTokenValidation(
                        "  jwtvalidationuser  "
                );

        assertTrue(result.isPresent());
        assertEquals("JwtValidationUser", result.get().getUsername());
        assertTrue(result.get().isEnabled());
        assertEquals(5L, result.get().getTokenVersion());
    }

}
