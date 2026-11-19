package com.nzuretech.product_management.config;

import com.nzuretech.product_management.security.JwtTokenVersionValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.util.Base64;

/**
 * Configures JWT signing and verification using HS256.
 *
 * <p>The signing key is loaded from an environment variable
 * and is never stored in the application's source code.</p>
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */
@Configuration
public class JwtConfig {

    /**
     * Creates the secret key used for JWT signing.
     *
     * @param encodedSecret Base64-encoded JWT signing key
     * @return the HMAC signing key
     */
    @Bean
    public SecretKey jwtSecretKey(
            @Value("${APP_JWT_SECRET}") String encodedSecret) {

        byte[] keyBytes;

        try {
            keyBytes = Base64.getDecoder().decode(encodedSecret);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "APP_JWT_SECRET must be valid Base64.",
                    exception
            );
        }

        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT signing key must contain at least 256 bits."
            );
        }

        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    /**
     * Creates the JWT encoder used to sign access tokens.
     *
     * @param secretKey the HMAC signing key
     * @return configured JWT encoder
     */
    @Bean
    public JwtEncoder jwtEncoder(SecretKey secretKey) {

        return NimbusJwtEncoder.withSecretKey(secretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();
    }


    /**
     * Configures JWT decoding and validation.
     *
     * <p>Validates token signature, expiration, issuer,
     * account status, and database token version.</p>
     *
     * @param secretKey JWT signing and verification key
     * @param tokenVersionValidator validates the user account
     *                              and token version
     * @return configured JWT decoder
     */
    @Bean
    public JwtDecoder jwtDecoder(
            SecretKey secretKey,
            JwtTokenVersionValidator tokenVersionValidator) {

        NimbusJwtDecoder decoder =
                NimbusJwtDecoder
                        .withSecretKey(secretKey)
                        .macAlgorithm(MacAlgorithm.HS256)
                        .build();

        OAuth2TokenValidator<Jwt> defaultValidator =
                JwtValidators.createDefaultWithIssuer(
                        "nzuretech-product-management"
                );

        OAuth2TokenValidator<Jwt> combinedValidator =
                new DelegatingOAuth2TokenValidator<>(
                        defaultValidator,
                        tokenVersionValidator
                );

        decoder.setJwtValidator(combinedValidator);

        return decoder;
    }

}
