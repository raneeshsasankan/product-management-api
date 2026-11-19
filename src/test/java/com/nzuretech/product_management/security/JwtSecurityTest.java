package com.nzuretech.product_management.security;

import com.nzuretech.product_management.config.JwtConfig;
import com.nzuretech.product_management.config.SecurityConfig;
import com.nzuretech.product_management.controller.ProductController;
import com.nzuretech.product_management.dto.ProductRequest;
import com.nzuretech.product_management.dto.ProductResponse;
import com.nzuretech.product_management.entity.AppUser;
import com.nzuretech.product_management.repository.ApiPermissionRepository;
import com.nzuretech.product_management.repository.AppUserRepository;
import com.nzuretech.product_management.service.ProductService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security integration tests for JWT Bearer authentication.
 *
 * <p>Verifies access to protected product endpoints
 * using JWT authentication and authorization.</p>
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
class JwtSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @MockitoBean
    private ApiPermissionRepository apiPermissionRepository;

    /**
     * Prepares database permission mappings for
     * JWT authorization integration tests.
     */
    @BeforeEach
    void setUpApiPermissions() {

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
     * Verifies that a user without authentication
     * cannot access the product endpoint.
     */
    @Test
    void getAllProducts_withoutJwt_returnsUnauthorized()
            throws Exception {

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(productService);
    }

    /**
     * Verifies that an invalid Bearer token
     * is rejected by Spring Security.
     */
    @Test
    void getAllProducts_withInvalidJwt_returnsUnauthorized()
            throws Exception {

        mockMvc.perform(get("/api/products")
                        .header("Authorization",
                                "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(productService);
    }

    /**
     * Verifies that a correctly signed JWT containing
     * the ADMIN role can access the protected products endpoint.
     */
    @Test
    void getAllProducts_withValidAdminJwt_returnsOk()
            throws Exception {

        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken(
                        "admin",
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_ADMIN"),
                                new SimpleGrantedAuthority("PRODUCT_READ")
                        )
                );

        authentication.setAuthenticated(true);

        String accessToken =
                jwtTokenService.generateAccessToken(authentication, 0L);

        when(productService.getAllProducts(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/products")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    /**
     * Verifies that a correctly signed but expired JWT
     * cannot access the protected product endpoint.
     */
    @Test
    void getAllProducts_withExpiredJwt_returnsUnauthorized()
            throws Exception {

        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("nzuretech-product-management")
                .subject("admin")
                .issuedAt(now.minusSeconds(3600))
                .expiresAt(now.minusSeconds(1800))
                .claim(
                        "authorities",
                        List.of("ROLE_ADMIN", "PRODUCT_READ")
                )
                .claim("token_version", 0L)
                .build();

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        String expiredToken = jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();

        mockMvc.perform(get("/api/products")
                        .header(
                                "Authorization",
                                "Bearer " + expiredToken
                        ))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(productService);
    }

    /**
     * Verifies that a VIEWER with PRODUCT_READ
     * permission can access the product list.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void getAllProducts_withViewerJwt_returnsOk()
            throws Exception {

        String token = generateJwt(
                "viewer",
                List.of(
                        "ROLE_VIEWER",
                        "PRODUCT_READ"
                )
        );

        when(productService.getAllProducts(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/products")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        verify(productService)
                .getAllProducts(any(Pageable.class));
    }

    /**
     * Verifies that a correctly signed JWT containing
     * an unexpected issuer is rejected.
     *
     * <p>The token contains valid ADMIN authorities
     * and has not expired, but its issuer does not
     * match the configured application issuer.</p>
     *
     * @throws Exception if the HTTP request fails
     */

    @Test
    void getAllProducts_withInvalidIssuer_returnsUnauthorized()
            throws Exception {

        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("untrusted-application")
                .subject("admin")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(900))
                .claim(
                        "authorities",
                        List.of(
                                "ROLE_ADMIN",
                                "PRODUCT_READ",
                                "PRODUCT_CREATE",
                                "PRODUCT_UPDATE",
                                "PRODUCT_DELETE"
                        )
                )
                .claim("token_version", 0L)
                .build();

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        String token = jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();

        mockMvc.perform(get("/api/products")
                        .header(
                                "Authorization",
                                "Bearer " + token
                        ))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(productService);
    }

    /**
     * Verifies that a JWT is rejected when its token version
     * no longer matches the user's database token version.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void getAllProducts_withRevokedJwt_returnsUnauthorized()
            throws Exception {

        AppUser admin = new AppUser(
                "admin",
                "encoded-password"
        );

        admin.setEnabled(true);
        admin.setTokenVersion(1L);

        when(appUserRepository
                .findByNormalizedUsernameForTokenValidation("admin"))
                .thenReturn(Optional.of(admin));

        String token = generateJwt(
                "admin",
                List.of("ROLE_ADMIN", "PRODUCT_READ")
        );

        mockMvc.perform(get("/api/products")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(productService);
    }

    /**
     * Verifies that a disabled account cannot use
     * a previously issued JWT with a matching token version.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void getAllProducts_withDisabledAccount_returnsUnauthorized()
            throws Exception {

        AppUser disabledAdmin = new AppUser(
                "admin",
                "encoded-password"
        );

        disabledAdmin.setEnabled(false);
        disabledAdmin.setTokenVersion(0L);

        when(appUserRepository
                .findByNormalizedUsernameForTokenValidation("admin"))
                .thenReturn(Optional.of(disabledAdmin));

        String token = generateJwt(
                "admin",
                List.of("ROLE_ADMIN", "PRODUCT_READ")
        );

        mockMvc.perform(get("/api/products")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(productService);
    }

    /**
     * Verifies that a JWT is rejected when the associated
     * database user no longer exists.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void getAllProducts_withDeletedAccount_returnsUnauthorized()
            throws Exception {

        when(appUserRepository
                .findByNormalizedUsernameForTokenValidation("admin"))
                .thenReturn(Optional.empty());

        String token = generateJwt(
                "admin",
                List.of("ROLE_ADMIN", "PRODUCT_READ")
        );

        mockMvc.perform(get("/api/products")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(productService);
    }

    /**
     * Verifies that a JWT containing the USER role
     * cannot create products because ADMIN authorization
     * is required.
     */
    @Test
    void createProduct_withUserJwt_returnsForbidden()
            throws Exception {

        String accessToken = generateJwt(
                "testuser",
                List.of("ROLE_USER", "PRODUCT_READ")
        );

        String requestBody = """
            {
                "name": "JWT User Test Product",
                "description": "Testing USER authorization",
                "price": 99.99,
                "quantity": 10,
                "category": "Electronics"
            }
            """;

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());

        verifyNoInteractions(productService);
    }

    /**
     * Verifies that a JWT containing the ADMIN role
     * can successfully create a product.
     */
    @Test
    void createProduct_withAdminJwt_returnsCreated()
            throws Exception {

        String accessToken = generateJwt(
                "admin",
                List.of(
                        "ROLE_ADMIN",
                        "PRODUCT_READ",
                        "PRODUCT_CREATE"
                )
        );

        String requestBody = """
            {
                "name": "JWT Admin Test Product",
                "description": "Testing JWT ADMIN authorization",
                "price": 149.99,
                "quantity": 10,
                "category": "Electronics"
            }
            """;

        ProductResponse response = new ProductResponse();
        response.setName("JWT Admin Test Product");
        response.setDescription("Testing JWT ADMIN authorization");
        response.setPrice(new BigDecimal("149.99"));
        response.setQuantity(10);
        response.setCategory("Electronics");

        when(productService.createProduct(any(ProductRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name")
                        .value("JWT Admin Test Product"))
                .andExpect(jsonPath("$.price").value(149.99));

        verify(productService)
                .createProduct(any(ProductRequest.class));
    }

    /**
     * Verifies that a MANAGER JWT with PRODUCT_CREATE
     * permission can create products.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void createProduct_withManagerJwt_returnsCreated()
            throws Exception {

        String token = generateJwt(
                "manager",
                List.of(
                        "ROLE_MANAGER",
                        "PRODUCT_READ",
                        "PRODUCT_CREATE",
                        "PRODUCT_UPDATE"
                )
        );

        String requestBody = """
            {
                "name": "Manager JWT Product",
                "description": "Created using manager JWT",
                "price": 199.99,
                "quantity": 10,
                "category": "Electronics"
            }
            """;

        ProductResponse response = new ProductResponse();
        response.setName("Manager JWT Product");
        response.setDescription("Created using manager JWT");
        response.setPrice(new BigDecimal("199.99"));
        response.setQuantity(10);
        response.setCategory("Electronics");

        when(productService.createProduct(any(ProductRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name")
                        .value("Manager JWT Product"));

        verify(productService)
                .createProduct(any(ProductRequest.class));
    }

    /**
     * Verifies that a VIEWER without PRODUCT_CREATE
     * permission cannot create products.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void createProduct_withViewerJwt_returnsForbidden()
            throws Exception {

        String token = generateJwt(
                "viewer",
                List.of(
                        "ROLE_VIEWER",
                        "PRODUCT_READ"
                )
        );

        String requestBody = """
            {
                "name": "Viewer Test Product",
                "description": "Unauthorized creation attempt",
                "price": 99.99,
                "quantity": 10,
                "category": "Electronics"
            }
            """;

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());

        verifyNoInteractions(productService);
    }

    /**
     * Verifies that a MANAGER JWT with PRODUCT_UPDATE
     * permission can update products.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void updateProduct_withManagerJwt_returnsOk()
            throws Exception {

        String token = generateJwt(
                "manager",
                List.of(
                        "ROLE_MANAGER",
                        "PRODUCT_READ",
                        "PRODUCT_CREATE",
                        "PRODUCT_UPDATE"
                )
        );

        String requestBody = """
            {
                "name": "Updated Manager JWT Product",
                "description": "Updated using manager JWT",
                "price": 299.99,
                "quantity": 15,
                "category": "Electronics"
            }
            """;

        ProductResponse response = new ProductResponse();
        response.setName("Updated Manager JWT Product");
        response.setDescription("Updated using manager JWT");
        response.setPrice(new BigDecimal("299.99"));
        response.setQuantity(15);
        response.setCategory("Electronics");

        when(productService.updateProduct(
                eq(1L),
                any(ProductRequest.class)
        )).thenReturn(response);

        mockMvc.perform(put("/api/products/1")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name")
                        .value("Updated Manager JWT Product"));

        verify(productService).updateProduct(
                eq(1L),
                any(ProductRequest.class)
        );
    }

    /**
     * Verifies that MANAGER cannot delete products
     * without PRODUCT_DELETE permission.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void deleteProduct_withManagerJwt_returnsForbidden()
            throws Exception {

        String token = generateJwt(
                "manager",
                List.of(
                        "ROLE_MANAGER",
                        "PRODUCT_READ",
                        "PRODUCT_CREATE",
                        "PRODUCT_UPDATE"
                )
        );

        mockMvc.perform(delete("/api/products/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        verifyNoInteractions(productService);
    }

    /**
     * Verifies that a VIEWER without PRODUCT_DELETE
     * permission cannot delete products.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void deleteProduct_withViewerJwt_returnsForbidden()
            throws Exception {

        String token = generateJwt(
                "viewer",
                List.of(
                        "ROLE_VIEWER",
                        "PRODUCT_READ"
                )
        );

        mockMvc.perform(delete("/api/products/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        verifyNoInteractions(productService);
    }


    /**
     * Verifies that authenticated users cannot access
     * endpoints without an explicit authorization rule.
     *
     * <p>Even an ADMIN JWT must be denied when an endpoint
     * is not included in the security configuration.</p>
     *
     * @throws Exception if the HTTP request fails
     */

    @Test
    void accessUnknownEndpoint_withAdminJwt_returnsForbidden()
            throws Exception {

        String token = generateJwt(
                "admin",
                List.of(
                        "ROLE_ADMIN",
                        "PRODUCT_READ",
                        "PRODUCT_CREATE",
                        "PRODUCT_UPDATE",
                        "PRODUCT_DELETE"
                )
        );

        mockMvc.perform(get("/api/admin/reports")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        verifyNoInteractions(productService);
    }

    /**
     * Generates a valid, signed JWT containing the specified
     * Spring Security authorities.
     *
     * @param username the authenticated username
     * @param authorities roles and permissions to include
     * @return signed JWT access token
     */
    private String generateJwt(
            String username,
            List<String> authorities) {

        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("nzuretech-product-management")
                .subject(username)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(900))
                .claim("authorities", authorities)
                .claim("token_version", 0L)
                .build();

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }

}
