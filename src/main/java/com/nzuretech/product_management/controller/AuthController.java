package com.nzuretech.product_management.controller;

import com.nzuretech.product_management.dto.LoginRequest;
import com.nzuretech.product_management.dto.LoginResponse;
import com.nzuretech.product_management.entity.AppUser;
import com.nzuretech.product_management.repository.AppUserRepository;
import com.nzuretech.product_management.security.JwtTokenService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller responsible for user authentication
 * and JWT access token issuance.
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;

    private final JwtTokenService jwtTokenService;

    private final AppUserRepository appUserRepository;


    /**
     * Creates the authentication controller.
     *
     * @param authenticationManager authenticates user credentials
     * @param jwtTokenService generates JWT access tokens
     * @param appUserRepository retrieves the authenticated user
     */
    public AuthController(
            AuthenticationManager authenticationManager,
            JwtTokenService jwtTokenService,
            AppUserRepository appUserRepository) {

        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
        this.appUserRepository = appUserRepository;
    }


    /**
     * Authenticates a user and issues a signed access token.
     *
     * @param request login credentials
     * @return JWT access token and expiration information
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.username(),
                                request.password()
                        )
                );

        // Retrieve the current token version.
        AppUser appUser = appUserRepository
                .findByNormalizedUsernameWithRolesAndPermissions(
                        authentication.getName()
                )
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user no longer exists."
                ));

        String token = jwtTokenService.generateAccessToken(
                authentication,
                appUser.getTokenVersion()
        );

        return ResponseEntity.ok(
                new LoginResponse(token, "Bearer", 900)
        );
    }

    /**
     * Returns a generic authentication error without
     * exposing whether the username or password was incorrect.
     *
     * @param exception authentication failure
     * @return unauthorized response
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthenticationFailure(
            AuthenticationException exception) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                        "error", "Unauthorized",
                        "message", "Invalid username or password."
                ));
    }
}
