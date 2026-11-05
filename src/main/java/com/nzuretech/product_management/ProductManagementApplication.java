package com.nzuretech.product_management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Product Management REST API.
 *
 * <p>Bootstraps the Spring application context, auto-configuration,
 * and embedded servlet container.</p>
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */
@SpringBootApplication
public class ProductManagementApplication {

	/**
	 * Launches the Product Management application.
	 *
	 * @param args command-line arguments passed during startup
	 */
	public static void main(String[] args) {
		SpringApplication.run(ProductManagementApplication.class, args);
	}
}
