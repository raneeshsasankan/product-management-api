package com.nzuretech.product_management.security;

import com.nzuretech.product_management.entity.AppUser;
import com.nzuretech.product_management.entity.Permission;
import com.nzuretech.product_management.entity.Role;
import com.nzuretech.product_management.repository.AppUserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DatabaseUserDetailsService.
 *
 * <p>Verifies that database users, roles, and permissions
 * are correctly converted into Spring Security user details.</p>
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class DatabaseUserDetailsServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private DatabaseUserDetailsService userDetailsService;

    private AppUser appUser;

    /**
     * Initializes test user data before each test.
     */
    @BeforeEach
    void setUp() {

        Permission readPermission = new Permission(
                "PRODUCT_READ",
                "Read products"
        );

        Permission createPermission = new Permission(
                "PRODUCT_CREATE",
                "Create products"
        );

        Permission updatePermission = new Permission(
                "PRODUCT_UPDATE",
                "Update products"
        );

        Role managerRole = new Role(
                "MANAGER",
                "Product manager"
        );

        managerRole.setPermissions(Set.of(
                readPermission,
                createPermission,
                updatePermission
        ));

        appUser = new AppUser(
                "testmanager",
                "stored-password-hash"
        );

        appUser.setRoles(Set.of(managerRole));
    }

    /**
     * Verifies that an existing user is loaded with
     * the correct role and permission authorities.
     */
    @Test
    void loadUserByUsername_whenUserExists_returnsUserDetails() {

        when(appUserRepository
                .findByNormalizedUsernameWithRolesAndPermissions("testmanager"))
                .thenReturn(Optional.of(appUser));

        UserDetails userDetails =
                userDetailsService.loadUserByUsername("testmanager");

        assertEquals("testmanager", userDetails.getUsername());
        assertEquals("stored-password-hash", userDetails.getPassword());
        assertTrue(userDetails.isEnabled());

        Set<String> authorities = userDetails.getAuthorities()
                .stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toSet());

        assertEquals(
                Set.of(
                        "ROLE_MANAGER",
                        "PRODUCT_READ",
                        "PRODUCT_CREATE",
                        "PRODUCT_UPDATE"
                ),
                authorities
        );

        verify(appUserRepository)
                .findByNormalizedUsernameWithRolesAndPermissions("testmanager");
    }

    /**
     * Verifies that an unknown username causes
     * UsernameNotFoundException.
     */
    @Test
    void loadUserByUsername_whenUserDoesNotExist_throwsException() {

        when(appUserRepository
                .findByNormalizedUsernameWithRolesAndPermissions("unknown"))
                .thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("unknown")
        );

        verify(appUserRepository)
                .findByNormalizedUsernameWithRolesAndPermissions("unknown");
    }

    /**
     * Verifies that a disabled database user is represented
     * as a disabled Spring Security UserDetails account.
     */
    @Test
    void loadUserByUsername_whenUserIsDisabled_returnsDisabledUserDetails() {

        appUser.setEnabled(false);

        when(appUserRepository
                .findByNormalizedUsernameWithRolesAndPermissions("testmanager"))
                .thenReturn(Optional.of(appUser));

        UserDetails userDetails =
                userDetailsService.loadUserByUsername("testmanager");

        assertFalse(userDetails.isEnabled());
        assertEquals("testmanager", userDetails.getUsername());

        verify(appUserRepository)
                .findByNormalizedUsernameWithRolesAndPermissions("testmanager");
    }

}
