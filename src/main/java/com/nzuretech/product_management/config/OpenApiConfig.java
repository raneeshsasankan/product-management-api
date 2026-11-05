package com.nzuretech.product_management.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI configuration for the Product Management API.
 *
 * <p>Defines the API metadata displayed in Swagger UI, including
 * the API title, description, version, and contact information.</p>
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */
@Configuration
public class OpenApiConfig {

    /**
     * Creates the OpenAPI documentation metadata for this application.
     *
     * @return configured OpenAPI definition
     */
    @Bean
    public OpenAPI productManagementOpenAPI() {

        // Define API information displayed in Swagger UI.
        return new OpenAPI()
                .info(new Info()
                        .title("Nzure Tech - Product Management API")
                        .description("""
                                REST API for managing products.

                                Features include:
                                - Product creation and management
                                - Product search by name
                                - Product filtering by category
                                - Input validation
                                - Centralized exception handling
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Nzure Tech")
                                .email("contact@nzuretech.com")
                                .url("https://nzuretech.com")));
    }
}
