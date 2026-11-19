package com.nzuretech.product_management.controller;

import com.nzuretech.product_management.entity.AppUser;
import com.nzuretech.product_management.repository.AppUserRepository;
import com.nzuretech.product_management.security.JwtTokenService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for the JWT authentication controller.
 *
 * <p>Verifies successful login, database token-version
 * retrieval, and rejection of invalid credentials.</p>
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private AppUserRepository appUserRepository;

    /**
     * Verifies that valid credentials return a JWT containing
     * the current token version retrieved from the database.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void login_whenCredentialsValid_returnsJwt()
            throws Exception {

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        "admin",
                        null,
                        List.of()
                );

        AppUser admin = new AppUser(
                "admin",
                "encoded-password"
        );

        admin.setTokenVersion(3L);

        when(authenticationManager.authenticate(
                any(Authentication.class)))
                .thenReturn(authentication);

        when(appUserRepository
                .findByNormalizedUsernameWithRolesAndPermissions(
                        "admin"
                ))
                .thenReturn(Optional.of(admin));

        when(jwtTokenService.generateAccessToken(
                any(Authentication.class),
                eq(3L)))
                .thenReturn("test-jwt-token");

        String requestBody = """
                {
                    "username": "admin",
                    "password": "valid-password"
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

        verify(authenticationManager)
                .authenticate(any(Authentication.class));

        verify(appUserRepository)
                .findByNormalizedUsernameWithRolesAndPermissions(
                        "admin"
                );

        verify(jwtTokenService)
                .generateAccessToken(
                        authentication,
                        3L
                );
    }

    /**
     * Verifies that incorrect credentials return HTTP 401
     * without querying the user again or generating a token.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void login_whenCredentialsInvalid_returnsUnauthorized()
            throws Exception {

        when(authenticationManager.authenticate(
                any(Authentication.class)))
                .thenThrow(new BadCredentialsException(
                        "Invalid credentials"
                ));

        String requestBody = """
                {
                    "username": "admin",
                    "password": "wrong-password"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("Invalid username or password."));

        verifyNoInteractions(
                appUserRepository,
                jwtTokenService
        );
    }

    /**
     * Verifies that login requests with a blank username
     * are rejected before authentication is attempted.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void login_whenUsernameBlank_returnsBadRequest()
            throws Exception {

        String requestBody = """
                {
                    "username": "",
                    "password": "valid-password"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(
                authenticationManager,
                appUserRepository,
                jwtTokenService
        );
    }

    /**
     * Verifies that login requests with a blank password
     * are rejected before authentication is attempted.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void login_whenPasswordBlank_returnsBadRequest()
            throws Exception {

        String requestBody = """
                {
                    "username": "admin",
                    "password": ""
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(
                authenticationManager,
                appUserRepository,
                jwtTokenService
        );
    }
}
