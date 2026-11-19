package com.nzuretech.product_management.security;

import com.nzuretech.product_management.entity.AppUser;
import com.nzuretech.product_management.repository.AppUserRepository;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Validates JWT access tokens against the current
 * security state of the database user.
 *
 * <p>Rejects tokens belonging to deleted or disabled
 * users and tokens containing an outdated token version.</p>
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */
@Component
public class JwtTokenVersionValidator
        implements OAuth2TokenValidator<Jwt> {

    private final AppUserRepository appUserRepository;

    /**
     * Creates the JWT token-version validator.
     *
     * @param appUserRepository repository for user account data
     */
    public JwtTokenVersionValidator(
            AppUserRepository appUserRepository) {

        this.appUserRepository = appUserRepository;
    }

    /**
     * Validates a JWT against the user's current account
     * status and token version.
     *
     * @param jwt the decoded JWT
     * @return successful or failed validation result
     */
    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {

        String username = jwt.getSubject();

        if (username == null || username.isBlank()) {
            return invalidToken();
        }

        Object versionClaim = jwt.getClaim("token_version");

        if (!(versionClaim instanceof Number number)) {
            return invalidToken();
        }

        long jwtTokenVersion = number.longValue();

        if (jwtTokenVersion < 0) {
            return invalidToken();
        }

        Optional<AppUser> userOptional =
                appUserRepository
                        .findByNormalizedUsernameForTokenValidation(
                                username
                        );

        if (userOptional.isEmpty()) {
            return invalidToken();
        }

        AppUser user = userOptional.get();

        if (!user.isEnabled()) {
            return invalidToken();
        }

        if (user.getTokenVersion() == null ||
                user.getTokenVersion() != jwtTokenVersion) {
            return invalidToken();
        }

        return OAuth2TokenValidatorResult.success();
    }

    /**
     * Returns a generic invalid-token result without
     * exposing sensitive account information.
     *
     * @return failed JWT validation result
     */
    private OAuth2TokenValidatorResult invalidToken() {

        OAuth2Error error = new OAuth2Error(
                "invalid_token",
                "Invalid or revoked access token.",
                null
        );

        return OAuth2TokenValidatorResult.failure(error);
    }
}
