package com.nzuretech.product_management.dto;

/**
 * Represents the access token returned after
 * successful authentication.
 *
 * @param accessToken signed JWT access token
 * @param tokenType authentication scheme
 * @param expiresIn token validity in seconds
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
