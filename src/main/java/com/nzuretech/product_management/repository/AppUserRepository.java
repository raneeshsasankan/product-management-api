package com.nzuretech.product_management.repository;

import com.nzuretech.product_management.entity.AppUser;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Repository for managing application users.
 *
 * <p>Provides database operations for user accounts,
 * authentication, and JWT token-version validation.</p>
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */
public interface AppUserRepository
        extends JpaRepository<AppUser, Long> {

    /**
     * Finds a user by username, ignoring capitalization
     * and surrounding whitespace.
     *
     * <p>Loads the user's roles and their permissions
     * to support Spring Security authentication.</p>
     *
     * @param username the username to search for
     * @return the matching user with roles and permissions,
     *         or an empty Optional if no user exists
     */
    @EntityGraph(attributePaths = {
            "roles",
            "roles.permissions"
    })
    @Query("""
            SELECT DISTINCT u
            FROM AppUser u
            WHERE LOWER(TRIM(u.username)) =
                  LOWER(TRIM(:username))
            """)
    Optional<AppUser> findByNormalizedUsernameWithRolesAndPermissions(
            @Param("username") String username
    );

    /**
     * Finds a user for JWT token-version validation.
     *
     * <p>Does not explicitly fetch the user's roles
     * or permissions. Those relationships are not
     * required for token-version validation.</p>
     *
     * @param username the JWT subject username
     * @return the matching user or an empty Optional
     */
    @Query("""
            SELECT u
            FROM AppUser u
            WHERE LOWER(TRIM(u.username)) =
                  LOWER(TRIM(:username))
            """)
    Optional<AppUser> findByNormalizedUsernameForTokenValidation(
            @Param("username") String username
    );
}
