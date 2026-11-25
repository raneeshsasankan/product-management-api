package com.nzuretech.product_management.config;

import com.nzuretech.product_management.entity.AppUser;
import com.nzuretech.product_management.entity.Role;
import com.nzuretech.product_management.repository.AppUserRepository;
import com.nzuretech.product_management.repository.RoleRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

/**
 * Creates the initial administrator when the bootstrap-admin
 * profile is explicitly enabled.
 *
 * <p>Normal application startup never activates this configuration.
 * The Docker setup service can exit after bootstrap completes.</p>
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */
@Configuration
@Profile("bootstrap-admin")
public class AdminBootstrap {

    /**
     * Creates the administrator if it does not already exist.
     *
     * @param appUserRepository repository for application users
     * @param roleRepository repository for security roles
     * @param passwordEncoder password encoder
     * @param password initial administrator password
     * @param exitAfterRun whether to exit after initialization
     * @param context Spring application context
     * @return administrator bootstrap runner
     */
    @Bean
    public ApplicationRunner bootstrapAdministrator(
            AppUserRepository appUserRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            @Value("${APP_BOOTSTRAP_ADMIN_PASSWORD}") String password,
            @Value("${APP_BOOTSTRAP_EXIT_AFTER_RUN:false}")
            boolean exitAfterRun,
            ConfigurableApplicationContext context) {

        return args -> {
            String username = "admin";

            boolean adminExists = appUserRepository
                    .findByNormalizedUsernameWithRolesAndPermissions(username)
                    .isPresent();

            if (!adminExists) {
                if (password.length() < 12) {
                    throw new IllegalArgumentException(
                            "Bootstrap password must be at least 12 characters."
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

                System.out.println("Initial administrator created.");
            } else {
                System.out.println("Administrator already exists. Skipping bootstrap.");
            }

            if (exitAfterRun) {
                int exitCode = SpringApplication.exit(context, () -> 0);
                System.exit(exitCode);
            }
        };
    }
}
