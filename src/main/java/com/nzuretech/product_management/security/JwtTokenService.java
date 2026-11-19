package com.nzuretech.product_management.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Generates signed JWT access tokens for authenticated users.
 *
 * <p>Access tokens use HS256 signing and expire after
 * 15 minutes. The user's granted authorities are included
 * as claims for authorization.</p>
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */
@Service
public class JwtTokenService {

    private static final String ISSUER =
            "nzuretech-product-management";

    private static final Duration ACCESS_TOKEN_VALIDITY =
            Duration.ofMinutes(15);

    private final JwtEncoder jwtEncoder;

    /**
     * Creates the JWT token service.
     *
     * @param jwtEncoder encoder used to sign JWTs
     */
    public JwtTokenService(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    /**
     * Generates an access token containing the user's
     * current database token version.
     *
     * @param authentication successfully authenticated user
     * @param tokenVersion current token version from PostgreSQL
     * @return signed JWT access token
     * @throws IllegalArgumentException if authentication is invalid
     *         or tokenVersion is negative
     */
    public String generateAccessToken(
            Authentication authentication,
            long tokenVersion) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException(
                    "An authenticated user is required."
            );
        }

        if (tokenVersion < 0) {
            throw new IllegalArgumentException(
                    "Token version cannot be negative."
            );
        }

        Instant now = Instant.now();

        List<String> authorities = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .sorted()
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(authentication.getName())
                .issuedAt(now)
                .expiresAt(now.plus(ACCESS_TOKEN_VALIDITY))
                .claim("authorities", authorities)
                .claim("token_version", tokenVersion)
                .build();

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }

}
