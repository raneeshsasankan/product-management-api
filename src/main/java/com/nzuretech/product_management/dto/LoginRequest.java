package com.nzuretech.product_management.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Represents username and password credentials
 * submitted to the authentication endpoint.
 *
 * @param username application username
 * @param password user password
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */
public record LoginRequest(

        @NotBlank(message = "Username is required.")
        String username,

        @NotBlank(message = "Password is required.")
        String password

) {
}
