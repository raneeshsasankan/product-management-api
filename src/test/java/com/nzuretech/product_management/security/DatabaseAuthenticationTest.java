package com.nzuretech.product_management.security;

import com.nzuretech.product_management.controller.AuthController;
import com.nzuretech.product_management.entity.AppUser;
import com.nzuretech.product_management.entity.Role;
import com.nzuretech.product_management.repository.AppUserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests database-backed login authentication using
 * the real Spring Security authentication provider.
 *
 * <p>Verifies password validation, account status,
 * database token-version retrieval, and JWT generation
 * eligibility.</p>
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class DatabaseAuthenticationTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private JwtTokenService jwtTokenService;

    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    /**
     * Configures the real database-backed authentication
     * service and Spring Security authentication provider.
     */
    @BeforeEach
    void setUp() {

        passwordEncoder = new BCryptPasswordEncoder();

        DatabaseUserDetailsService userDetailsService =
                new DatabaseUserDetailsService(
                        appUserRepository
                );

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(
                        userDetailsService
                );

        provider.setPasswordEncoder(passwordEncoder);

        AuthenticationManager authenticationManager =
                new ProviderManager(provider);

        AuthController authController =
                new AuthController(
                        authenticationManager,
                        jwtTokenService,
                        appUserRepository
                );

        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .build();
    }

    /**
     * Verifies that a disabled database user cannot
     * obtain a JWT, even with the correct password.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void login_whenDatabaseUserDisabled_returnsUnauthorized()
            throws Exception {

        Role userRole = new Role(
                "USER",
                "Standard user"
        );

        AppUser disabledUser = new AppUser(
                "disableduser",
                passwordEncoder.encode("correct-password")
        );

        disabledUser.setRoles(Set.of(userRole));
        disabledUser.setEnabled(false);

        when(appUserRepository
                .findByNormalizedUsernameWithRolesAndPermissions(
                        "disableduser"
                ))
                .thenReturn(Optional.of(disabledUser));

        String requestBody = """
                {
                    "username": "disableduser",
                    "password": "correct-password"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("Invalid username or password."));

        verify(appUserRepository)
                .findByNormalizedUsernameWithRolesAndPermissions(
                        "disableduser"
                );

        verifyNoInteractions(jwtTokenService);
    }

    /**
     * Verifies that an enabled database user with valid
     * credentials can authenticate and obtain a JWT
     * using the current database token version.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void login_whenCredentialsValid_returnsJwt()
            throws Exception {

        Role adminRole = new Role(
                "ADMIN",
                "Administrator"
        );

        AppUser admin = new AppUser(
                "admin",
                passwordEncoder.encode("correct-password")
        );

        admin.setRoles(Set.of(adminRole));
        admin.setTokenVersion(5L);

        when(appUserRepository
                .findByNormalizedUsernameWithRolesAndPermissions(
                        "admin"
                ))
                .thenReturn(Optional.of(admin));

        when(jwtTokenService.generateAccessToken(
                any(Authentication.class),
                eq(5L)))
                .thenReturn("test-jwt-token");

        String requestBody = """
                {
                    "username": "admin",
                    "password": "correct-password"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken")
                        .value("test-jwt-token"))
                .andExpect(jsonPath("$.tokenType")
                        .value("Bearer"))
                .andExpect(jsonPath("$.expiresIn")
                        .value(900));


        verify(appUserRepository, times(2))
                .findByNormalizedUsernameWithRolesAndPermissions(
                        "admin"
                );

        verify(jwtTokenService)
                .generateAccessToken(
                        any(Authentication.class),
                        eq(5L)
                );
    }

    /**
     * Verifies that a valid database user cannot
     * authenticate with an incorrect password.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void login_whenPasswordIncorrect_returnsUnauthorized()
            throws Exception {

        Role userRole = new Role(
                "USER",
                "Standard user"
        );

        AppUser user = new AppUser(
                "testuser",
                passwordEncoder.encode("correct-password")
        );

        user.setRoles(Set.of(userRole));

        when(appUserRepository
                .findByNormalizedUsernameWithRolesAndPermissions(
                        "testuser"
                ))
                .thenReturn(Optional.of(user));

        String requestBody = """
                {
                    "username": "testuser",
                    "password": "wrong-password"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("Invalid username or password."));

        verifyNoInteractions(jwtTokenService);
    }

    /**
     * Verifies that a username not found in the database
     * cannot authenticate or obtain a JWT.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void login_whenUsernameDoesNotExist_returnsUnauthorized()
            throws Exception {

        when(appUserRepository
                .findByNormalizedUsernameWithRolesAndPermissions(
                        "unknown"
                ))
                .thenReturn(Optional.empty());

        String requestBody = """
                {
                    "username": "unknown",
                    "password": "some-password"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("Invalid username or password."));

        verifyNoInteractions(jwtTokenService);
    }
}
