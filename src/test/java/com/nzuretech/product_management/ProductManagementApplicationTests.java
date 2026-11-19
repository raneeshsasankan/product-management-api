
package com.nzuretech.product_management;

import com.nzuretech.product_management.config.PostgresTestConfig;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration test verifying that the Spring Boot
 * application context initializes successfully.
 *
 * <p>Uses an isolated PostgreSQL Testcontainer
 * with Flyway migrations applied during startup.</p>
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */
@SpringBootTest
@Import(PostgresTestConfig.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "APP_JWT_SECRET=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
})
class ProductManagementApplicationTests {

    /**
     * Verifies that the Spring application context
     * initializes without configuration errors.
     */
    @Test
    void contextLoads() {
        // Successful context initialization completes the test.
    }
}
