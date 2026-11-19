package com.nzuretech.product_management.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Shared PostgreSQL Testcontainers configuration
 * for database integration tests.
 *
 * <p>Uses an isolated PostgreSQL container to execute
 * repository and security integration tests.</p>
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestConfig {

    /**
     * Creates a PostgreSQL Testcontainer for integration testing.
     *
     * @return PostgreSQL container instance
     */
    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:18-alpine");
    }
}
