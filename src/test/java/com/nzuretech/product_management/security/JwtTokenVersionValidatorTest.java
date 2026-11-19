package com.nzuretech.product_management.security;

import com.nzuretech.product_management.entity.AppUser;
import com.nzuretech.product_management.repository.AppUserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JWT token-version validation.
 *
 * <p>Verifies that existing, enabled users with matching
 * token versions are accepted, while disabled accounts,
 * deleted accounts, and revoked tokens are rejected.</p>
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class JwtTokenVersionValidatorTest {

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private JwtTokenVersionValidator validator;

    private AppUser user;

    /**
     * Creates an enabled user with token version 2.
     */
    @BeforeEach
    void setUp() {

        user = new AppUser(
                "testuser",
                "encoded-password"
        );

        user.setEnabled(true);
        user.setTokenVersion(2L);
    }

    /**
     * Verifies that a valid JWT with a matching
     * token version is accepted.
     */
    @Test
    void validate_whenVersionMatches_returnsSuccess() {

        Jwt jwt = createJwt("testuser", 2L);

        when(appUserRepository
                .findByNormalizedUsernameForTokenValidation(
                        "testuser"
                ))
                .thenReturn(Optional.of(user));

        OAuth2TokenValidatorResult result =
                validator.validate(jwt);

        assertFalse(result.hasErrors());
    }

    /**
     * Verifies that an outdated JWT is rejected
     * after the database token version changes.
     */
    @Test
    void validate_whenVersionOutdated_returnsFailure() {

        Jwt jwt = createJwt("testuser", 1L);

        when(appUserRepository
                .findByNormalizedUsernameForTokenValidation(
                        "testuser"
                ))
                .thenReturn(Optional.of(user));

        OAuth2TokenValidatorResult result =
                validator.validate(jwt);

        assertTrue(result.hasErrors());
    }

    /**
     * Verifies that a disabled account cannot
     * use a previously issued JWT.
     */
    @Test
    void validate_whenAccountDisabled_returnsFailure() {

        user.setEnabled(false);

        Jwt jwt = createJwt("testuser", 2L);

        when(appUserRepository
                .findByNormalizedUsernameForTokenValidation(
                        "testuser"
                ))
                .thenReturn(Optional.of(user));

        OAuth2TokenValidatorResult result =
                validator.validate(jwt);

        assertTrue(result.hasErrors());
    }

    /**
     * Verifies that a deleted account cannot
     * use a previously issued JWT.
     */
    @Test
    void validate_whenAccountDeleted_returnsFailure() {

        Jwt jwt = createJwt("testuser", 2L);

        when(appUserRepository
                .findByNormalizedUsernameForTokenValidation(
                        "testuser"
                ))
                .thenReturn(Optional.empty());

        OAuth2TokenValidatorResult result =
                validator.validate(jwt);

        assertTrue(result.hasErrors());
    }

    /**
     * Verifies that a JWT without a token_version
     * claim is rejected.
     */
    @Test
    void validate_whenVersionMissing_returnsFailure() {

        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject("testuser")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(900))
                .build();

        OAuth2TokenValidatorResult result =
                validator.validate(jwt);

        assertTrue(result.hasErrors());

        verifyNoInteractions(appUserRepository);
    }

    /**
     * Verifies that a JWT containing a negative
     * token version is rejected.
     */
    @Test
    void validate_whenVersionNegative_returnsFailure() {

        Jwt jwt = createJwt("testuser", -1L);

        OAuth2TokenValidatorResult result =
                validator.validate(jwt);

        assertTrue(result.hasErrors());

        verifyNoInteractions(appUserRepository);
    }

    /**
     * Verifies that a JWT without a valid subject
     * is rejected.
     */
    @Test
    void validate_whenSubjectBlank_returnsFailure() {

        Jwt jwt = createJwt("", 2L);

        OAuth2TokenValidatorResult result =
                validator.validate(jwt);

        assertTrue(result.hasErrors());

        verifyNoInteractions(appUserRepository);
    }

    /**
     * Creates a JWT for testing token-version validation.
     *
     * @param username JWT subject
     * @param tokenVersion JWT token version
     * @return JWT with the supplied claims
     */
    private Jwt createJwt(
            String username,
            long tokenVersion) {

        Instant now = Instant.now();

        return Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject(username)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(900))
                .claim("token_version", tokenVersion)
                .build();
    }
}
