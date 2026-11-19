package com.nzuretech.product_management.security;

import com.nzuretech.product_management.entity.ApiPermission;
import com.nzuretech.product_management.repository.ApiPermissionRepository;

import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Authorizes API requests using permission mappings
 * stored in PostgreSQL.
 *
 * <p>Matches incoming HTTP methods and URL paths against
 * the api_permissions table and verifies that the
 * authenticated user has the required permission.</p>
 *
 * <p>Requests without matching database rules are denied.</p>
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */
@Component
public class DatabasePermissionAuthorizationManager
        implements AuthorizationManager<RequestAuthorizationContext> {

    private final ApiPermissionRepository apiPermissionRepository;

    /**
     * Creates the database-driven authorization manager.
     *
     * @param apiPermissionRepository repository containing
     *                                endpoint permission mappings
     */
    public DatabasePermissionAuthorizationManager(
            ApiPermissionRepository apiPermissionRepository) {

        this.apiPermissionRepository = apiPermissionRepository;
    }

    /**
     * Determines whether an authenticated user is authorized
     * to access the requested endpoint.
     *
     * @param authentication supplier of the current authentication
     * @param context the incoming HTTP request context
     * @return authorization decision
     */
    @Override
    public AuthorizationResult authorize(
            Supplier<? extends Authentication> authentication,
            RequestAuthorizationContext context) {

        Authentication currentAuthentication = authentication.get();

        if (currentAuthentication == null
                || !currentAuthentication.isAuthenticated()) {
            return new AuthorizationDecision(false);
        }

        Set<String> userAuthorities =
                currentAuthentication.getAuthorities()
                        .stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toSet());

        List<ApiPermission> mappings =
                apiPermissionRepository.findAll();

        boolean matched = false;

        for (ApiPermission mapping : mappings) {

            HttpMethod method =
                    HttpMethod.valueOf(mapping.getHttpMethod());

            PathPatternRequestMatcher matcher =
                    PathPatternRequestMatcher.pathPattern(
                            method,
                            mapping.getUrlPattern()
                    );

            if (!matcher.matches(context.getRequest())) {
                continue;
            }

            matched = true;

            String requiredPermission =
                    mapping.getPermission().getName();

            if (!userAuthorities.contains(requiredPermission)) {
                return new AuthorizationDecision(false);
            }
        }

        return new AuthorizationDecision(matched);
    }
}
