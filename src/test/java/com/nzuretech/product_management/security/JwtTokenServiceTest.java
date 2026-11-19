package com.nzuretech.product_management.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtTokenService.
 *
 * <p>Verifies JWT signing, decoding, claims, token
 * expiration, and authentication requirements.</p>
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */
class JwtTokenServiceTest {

    private JwtTokenService jwtTokenService;

    private NimbusJwtDecoder jwtDecoder;

    /**
     * Creates isolated signing and verification components
     * using a test-only HMAC key.
     */
    @BeforeEach
    void setUp() {

        byte[] keyBytes = new byte[32];

        java.util.Arrays.fill(keyBytes, (byte) 7);

        SecretKey secretKey =
                new SecretKeySpec(keyBytes, "HmacSHA256");

        JwtEncoder jwtEncoder = NimbusJwtEncoder
                .withSecretKey(secretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();

        jwtDecoder = NimbusJwtDecoder
                .withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        jwtDecoder.setJwtValidator(
                JwtValidators.createDefaultWithIssuer(
                        "nzuretech-product-management"
                )
        );

        jwtTokenService = new JwtTokenService(jwtEncoder);
    }

    /**
     * Verifies that a generated JWT can be decoded and
     * contains the expected username and authorities.
     */
    @Test
    void generateAccessToken_whenAuthenticated_containsExpectedClaims() {

        Authentication authentication = authenticatedAdmin();

        String token =
                jwtTokenService.generateAccessToken(authentication, 0L);

        Jwt decodedToken = jwtDecoder.decode(token);

        assertEquals(
                "nzuretech-product-management",
                decodedToken.getClaimAsString("iss")
        );

        assertEquals("admin", decodedToken.getSubject());

        List<String> authorities =
                decodedToken.getClaimAsStringList("authorities");

        assertEquals(
                Set.of(
                        "ROLE_ADMIN",
                        "PRODUCT_READ",
                        "PRODUCT_CREATE"
                ),
                Set.copyOf(authorities)
        );
    }

    /**
     * Verifies that generated tokens expire after
     * approximately 15 minutes.
     */
    @Test
    void generateAccessToken_whenAuthenticated_expiresAfter15Minutes() {

        String token = jwtTokenService.generateAccessToken(authenticatedAdmin(), 0L);

        Jwt decodedToken = jwtDecoder.decode(token);

        Instant issuedAt = decodedToken.getIssuedAt();
        Instant expiresAt = decodedToken.getExpiresAt();

        assertNotNull(issuedAt);
        assertNotNull(expiresAt);

        long validitySeconds =
                Duration.between(issuedAt, expiresAt).getSeconds();

        assertEquals(900, validitySeconds);
    }

    /**
     * Verifies that unauthenticated principals cannot
     * generate JWT access tokens.
     */
    @Test
    void generateAccessToken_whenUnauthenticated_throwsException() {

        Authentication authentication =
                new TestingAuthenticationToken("admin", null);

        assertFalse(authentication.isAuthenticated());

        assertThrows(
                IllegalArgumentException.class,
                () -> jwtTokenService.generateAccessToken(authentication, 0L)
        );
    }


    /**
     * Verifies that generated JWTs contain the token version
     * supplied by the application.
     */
    @Test
    void generateAccessToken_withTokenVersion_containsVersionClaim() {

        Authentication authentication = authenticatedAdmin();

        long tokenVersion = 3L;

        String token = jwtTokenService.generateAccessToken(
                authentication,
                tokenVersion
        );

        Jwt decodedToken = jwtDecoder.decode(token);

        assertEquals(
                3L,
                ((Number) decodedToken.getClaim("token_version")).longValue()
        );
    }


    /**
     * Creates an authenticated administrator for testing.
     *
     * @return authenticated administrator token
     */
    private Authentication authenticatedAdmin() {

        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken(
                        "admin",
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_ADMIN"),
                                new SimpleGrantedAuthority("PRODUCT_READ"),
                                new SimpleGrantedAuthority("PRODUCT_CREATE")
                        )
                );

        authentication.setAuthenticated(true);

        return authentication;
    }
}
