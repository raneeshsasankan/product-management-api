package com.nzuretech.product_management.config;

import com.nzuretech.product_management.entity.AppUser;
import com.nzuretech.product_management.entity.Role;
import com.nzuretech.product_management.repository.AppUserRepository;
import com.nzuretech.product_management.repository.RoleRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

/**
 * Provides an explicitly enabled bootstrap mechanism
 * for creating the initial local administrator.
 *
 * <p>This configuration must never be enabled for
 * ordinary production application startup.</p>
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */
@Configuration
@Profile("bootstrap-admin")
public class AdminBootstrap {

    /**
     * Creates the initial administrator if one does not exist.
     *
     * @param appUserRepository user repository
     * @param roleRepository role repository
     * @param passwordEncoder BCrypt password encoder
     * @param password initial administrator password
     * @return application bootstrap runner
     */
    @Bean
    public ApplicationRunner bootstrapAdministrator(
            AppUserRepository appUserRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            @Value("${APP_BOOTSTRAP_ADMIN_PASSWORD}") String password) {

        return args -> {

            String username = "admin";

            if (appUserRepository
                    .findByNormalizedUsernameWithRolesAndPermissions(username)
                    .isPresent()) {
                return;
            }

            if (password.length() < 12) {
                throw new IllegalArgumentException(
                        "Bootstrap administrator password must be at least 12 characters."
                );
            }

            Role adminRole = roleRepository.findByName("ADMIN")
                    .orElseThrow(() -> new IllegalStateException(
                            "ADMIN role has not been initialized."
                    ));

            AppUser admin = new AppUser(
                    username,
                    passwordEncoder.encode(password)
            );

            admin.setRoles(Set.of(adminRole));

            appUserRepository.saveAndFlush(admin);
        };
    }
}
