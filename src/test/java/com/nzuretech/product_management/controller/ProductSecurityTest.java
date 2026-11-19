package com.nzuretech.product_management.controller;

import com.nzuretech.product_management.config.JwtConfig;
import com.nzuretech.product_management.config.SecurityConfig;
import com.nzuretech.product_management.dto.ProductRequest;
import com.nzuretech.product_management.dto.ProductResponse;
import com.nzuretech.product_management.entity.AppUser;
import com.nzuretech.product_management.repository.ApiPermissionRepository;
import com.nzuretech.product_management.repository.AppUserRepository;
import com.nzuretech.product_management.security.ApiPermissionTestFixtures;
import com.nzuretech.product_management.security.DatabasePermissionAuthorizationManager;
import com.nzuretech.product_management.security.JwtTokenService;
import com.nzuretech.product_management.security.JwtTokenVersionValidator;
import com.nzuretech.product_management.service.ProductService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for JWT authentication and
 * database-driven product authorization.
 *
 * <p>Verifies that product endpoints enforce permissions
 * using signed JWT Bearer tokens and the database-backed
 * authorization manager.</p>
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */

@WebMvcTest(ProductController.class)
@Import({
        SecurityConfig.class,
        JwtConfig.class,
        JwtTokenService.class,
        JwtTokenVersionValidator.class,
        DatabasePermissionAuthorizationManager.class
})
@AutoConfigureMockMvc(addFilters = true)
@TestPropertySource(properties = {
        "APP_JWT_SECRET=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
})
class ProductSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private ApiPermissionRepository apiPermissionRepository;

    @MockitoBean
    private AppUserRepository appUserRepository;

    /**
     * Prepares API permission mappings for authorization tests.
     */
    @BeforeEach
    void setUp() {

        ApiPermissionTestFixtures.stubProductPermissions(
                apiPermissionRepository
        );

        lenient().when(appUserRepository
                        .findByNormalizedUsernameForTokenValidation(
                                anyString()
                        ))
                .thenAnswer(invocation -> {

                    String username = invocation.getArgument(
                            0,
                            String.class
                    );

                    AppUser user = new AppUser(
                            username,
                            "encoded-test-password"
                    );

                    user.setEnabled(true);
                    user.setTokenVersion(0L);

                    return Optional.of(user);
                });
    }


    /**
     * Generates a signed JWT for a test user with the
     * permissions associated with the specified role.
     *
     * @param username authenticated username
     * @param roleName role assigned to the user
     * @return signed JWT access token
     */
    private String generateTokenForRole(
            String username,
            String roleName) {

        List<String> permissions = switch (roleName) {

            case "ADMIN" -> List.of(
                    "ROLE_ADMIN",
                    "PRODUCT_READ",
                    "PRODUCT_CREATE",
                    "PRODUCT_UPDATE",
                    "PRODUCT_DELETE"
            );

            case "MANAGER" -> List.of(
                    "ROLE_MANAGER",
                    "PRODUCT_READ",
                    "PRODUCT_CREATE",
                    "PRODUCT_UPDATE"
            );

            case "USER" -> List.of(
                    "ROLE_USER",
                    "PRODUCT_READ"
            );

            case "VIEWER" -> List.of(
                    "ROLE_VIEWER",
                    "PRODUCT_READ"
            );

            default -> throw new IllegalArgumentException(
                    "Unknown test role: " + roleName
            );
        };

        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken(
                        username,
                        null,
                        permissions.stream()
                                .map(SimpleGrantedAuthority::new)
                                .toList()
                );

        authentication.setAuthenticated(true);

        return jwtTokenService.generateAccessToken(authentication, 0L);
    }

    /**
     * Verifies that unauthenticated requests cannot
     * access the protected products endpoint.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    @WithAnonymousUser
    void getAllProducts_whenUnauthenticated_returnsUnauthorized()
            throws Exception {

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(productService);
    }

    /**
     * Verifies that a USER with PRODUCT_READ permission
     * can retrieve products using JWT authentication.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void getAllProducts_whenUserAuthenticated_returnsOk()
            throws Exception {

        String token = generateTokenForRole("user", "USER");

        Page<ProductResponse> products =
                new PageImpl<>(Collections.emptyList());

        when(productService.getAllProducts(any(Pageable.class)))
                .thenReturn(products);

        mockMvc.perform(get("/api/products")
                        .header(
                                "Authorization",
                                "Bearer " + token
                        ))
                .andExpect(status().isOk());

        verify(productService)
                .getAllProducts(any(Pageable.class));
    }

    /**
     * Verifies that a USER without PRODUCT_CREATE
     * permission cannot create products.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void createProduct_whenUserRole_returnsForbidden()
            throws Exception {

        String token = generateTokenForRole("user", "USER");

        String requestBody = """
                {
                    "name": "Security Test Product",
                    "description": "Testing permission authorization",
                    "price": 99.99,
                    "quantity": 10,
                    "category": "Electronics"
                }
                """;

        mockMvc.perform(post("/api/products")
                        .header(
                                "Authorization",
                                "Bearer " + token
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());

        verifyNoInteractions(productService);
    }

    /**
     * Verifies that an ADMIN with PRODUCT_CREATE
     * permission can successfully create a product.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void createProduct_whenAdminRole_returnsCreated()
            throws Exception {

        String token = generateTokenForRole("admin", "ADMIN");

        String requestBody = """
                {
                    "name": "Security Admin Test Product",
                    "description": "Testing ADMIN authorization",
                    "price": 149.99,
                    "quantity": 10,
                    "category": "Electronics"
                }
                """;

        ProductResponse response = new ProductResponse();
        response.setName("Security Admin Test Product");
        response.setDescription("Testing ADMIN authorization");
        response.setPrice(new BigDecimal("149.99"));
        response.setQuantity(10);
        response.setCategory("Electronics");

        when(productService.createProduct(any(ProductRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/products")
                        .header(
                                "Authorization",
                                "Bearer " + token
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name")
                        .value("Security Admin Test Product"))
                .andExpect(jsonPath("$.price")
                        .value(149.99));

        verify(productService)
                .createProduct(any(ProductRequest.class));
    }

    /**
     * Verifies that a MANAGER with PRODUCT_CREATE
     * permission can create products without the ADMIN role.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void createProduct_whenManagerHasCreatePermission_returnsCreated()
            throws Exception {

        String token = generateTokenForRole("manager", "MANAGER");

        String requestBody = """
                {
                    "name": "Manager Test Product",
                    "description": "Created by a manager",
                    "price": 199.99,
                    "quantity": 5,
                    "category": "Electronics"
                }
                """;

        ProductResponse response = new ProductResponse();
        response.setName("Manager Test Product");
        response.setDescription("Created by a manager");
        response.setPrice(new BigDecimal("199.99"));
        response.setQuantity(5);
        response.setCategory("Electronics");

        when(productService.createProduct(any(ProductRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/products")
                        .header(
                                "Authorization",
                                "Bearer " + token
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name")
                        .value("Manager Test Product"));

        verify(productService)
                .createProduct(any(ProductRequest.class));
    }

    /**
     * Verifies that a MANAGER with PRODUCT_UPDATE
     * permission can update an existing product.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void updateProduct_whenManagerHasUpdatePermission_returnsOk()
            throws Exception {

        String token = generateTokenForRole("manager", "MANAGER");

        String requestBody = """
                {
                    "name": "Updated Manager Product",
                    "description": "Updated by manager",
                    "price": 249.99,
                    "quantity": 8,
                    "category": "Electronics"
                }
                """;

        ProductResponse response = new ProductResponse();
        response.setName("Updated Manager Product");
        response.setDescription("Updated by manager");
        response.setPrice(new BigDecimal("249.99"));
        response.setQuantity(8);
        response.setCategory("Electronics");

        when(productService.updateProduct(
                eq(1L),
                any(ProductRequest.class)
        )).thenReturn(response);

        mockMvc.perform(put("/api/products/1")
                        .header(
                                "Authorization",
                                "Bearer " + token
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name")
                        .value("Updated Manager Product"))
                .andExpect(jsonPath("$.price")
                        .value(249.99));

        verify(productService).updateProduct(
                eq(1L),
                any(ProductRequest.class)
        );
    }

    /**
     * Verifies that a USER without PRODUCT_UPDATE
     * permission cannot update an existing product.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void updateProduct_whenUserLacksUpdatePermission_returnsForbidden()
            throws Exception {

        String token = generateTokenForRole("user", "USER");

        String requestBody = """
                {
                    "name": "Unauthorized Update",
                    "description": "Should not be updated",
                    "price": 99.99,
                    "quantity": 10,
                    "category": "Electronics"
                }
                """;

        mockMvc.perform(put("/api/products/1")
                        .header(
                                "Authorization",
                                "Bearer " + token
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());

        verifyNoInteractions(productService);
    }

    /**
     * Verifies that an ADMIN with PRODUCT_DELETE
     * permission can successfully delete a product.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void deleteProduct_whenAdminHasDeletePermission_returnsNoContent()
            throws Exception {

        String token = generateTokenForRole("admin", "ADMIN");

        mockMvc.perform(delete("/api/products/1")
                        .header(
                                "Authorization",
                                "Bearer " + token
                        ))
                .andExpect(status().isNoContent());

        verify(productService).deleteProduct(1L);
    }

    /**
     * Verifies that a MANAGER without PRODUCT_DELETE
     * permission cannot delete products.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void deleteProduct_whenManagerLacksDeletePermission_returnsForbidden()
            throws Exception {

        String token = generateTokenForRole("manager", "MANAGER");

        mockMvc.perform(delete("/api/products/1")
                        .header(
                                "Authorization",
                                "Bearer " + token
                        ))
                .andExpect(status().isForbidden());

        verifyNoInteractions(productService);
    }
}
