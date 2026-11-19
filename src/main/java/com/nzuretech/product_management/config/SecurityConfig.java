package com.nzuretech.product_management.config;

import com.nzuretech.product_management.repository.AppUserRepository;
import com.nzuretech.product_management.security.DatabasePermissionAuthorizationManager;
import com.nzuretech.product_management.security.DatabaseUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configures authentication and authorization for the REST API.
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {


    /**
     * Configures authentication and database-driven authorization.
     *
     * <p>Product endpoints are authorized using permission mappings
     * stored in PostgreSQL with JWT authentication.</p>
     *
     * @param http Spring Security HTTP configuration
     * @param permissionAuthorizationManager database authorization manager
     * @return configured Spring Security filter chain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            DatabasePermissionAuthorizationManager permissionAuthorizationManager)
            throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // Public authentication endpoint
                        .requestMatchers(
                                HttpMethod.POST, "/api/auth/login"
                        ).permitAll()

                        // Public Swagger documentation
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // Database-driven product authorization
                        .requestMatchers(
                                "/api/products",
                                "/api/products/**"
                        ).access(permissionAuthorizationManager)

                        // Deny access to endpoints without an explicit security rule
                        .anyRequest().denyAll()

                )

                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(
                                        jwtAuthenticationConverter()
                                )
                        )
                )
                .httpBasic(basic -> basic.disable());


        return http.build();
    }

    /**
     * Configures database-backed user authentication.
     *
     * <p>Loads users, roles, and permissions from PostgreSQL
     * instead of maintaining users in application memory.</p>
     *
     * @param appUserRepository repository for retrieving users
     * @return the database-backed user details service
     */
    @Bean
    public UserDetailsService userDetailsService(
            AppUserRepository appUserRepository) {

        return new DatabaseUserDetailsService(appUserRepository);
    }

    /**
     * Configures BCrypt password hashing.
     *
     * @return the password encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configures username/password authentication
     * using database-backed user details and BCrypt.
     *
     * @param userDetailsService database-backed user service
     * @param passwordEncoder BCrypt password encoder
     * @return configured authentication manager
     */
    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder);

        return new ProviderManager(provider);
    }

    /**
     * Converts JWT authorities into Spring Security authorities.
     *
     * <p>Reads the custom "authorities" claim without adding
     * a prefix, preserving ROLE_ADMIN and product permissions.</p>
     *
     * @return the configured JWT authentication converter
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter authoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        authoritiesConverter.setAuthoritiesClaimName("authorities");
        authoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter authenticationConverter =
                new JwtAuthenticationConverter();

        authenticationConverter.setJwtGrantedAuthoritiesConverter(
                authoritiesConverter
        );

        return authenticationConverter;
    }

}
