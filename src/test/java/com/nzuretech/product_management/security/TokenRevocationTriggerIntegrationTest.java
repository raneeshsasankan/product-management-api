package com.nzuretech.product_management.security;

import com.nzuretech.product_management.config.PostgresTestConfig;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for PostgreSQL token-revocation
 * triggers introduced by Flyway migration V6.
 *
 * <p>Uses a real PostgreSQL Testcontainer to verify
 * database-trigger behavior.</p>
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */
@DataJpaTest
@Import(PostgresTestConfig.class)
@ActiveProfiles("test")
class TokenRevocationTriggerIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Verifies that assigning a role to a user
     * automatically increments token_version.
     *
     * <p>The test transaction rolls back after execution,
     * preserving database isolation.</p>
     */
    @Test
    void assigningRole_incrementsUserTokenVersion() {

        Long userId = jdbcTemplate.queryForObject(
                """
                INSERT INTO users (
                    username,
                    password_hash
                )
                VALUES (
                    'trigger_test_user',
                    'test-password-hash'
                )
                RETURNING id
                """,
                Long.class
        );

        Long viewerRoleId = jdbcTemplate.queryForObject(
                """
                SELECT id
                FROM roles
                WHERE name = 'VIEWER'
                """,
                Long.class
        );

        Long initialVersion = jdbcTemplate.queryForObject(
                """
                SELECT token_version
                FROM users
                WHERE id = ?
                """,
                Long.class,
                userId
        );

        assertEquals(0L, initialVersion);

        jdbcTemplate.update(
                """
                INSERT INTO user_roles (
                    user_id,
                    role_id
                )
                VALUES (?, ?)
                """,
                userId,
                viewerRoleId
        );

        Long updatedVersion = jdbcTemplate.queryForObject(
                """
                SELECT token_version
                FROM users
                WHERE id = ?
                """,
                Long.class,
                userId
        );

        assertEquals(
                initialVersion + 1,
                updatedVersion
        );
    }

    /**
     * Verifies that removing a user's role
     * invalidates previously issued JWTs.
     */
    @Test
    void removingRole_incrementsUserTokenVersion() {

        Long userId = jdbcTemplate.queryForObject(
                """
                INSERT INTO users (username, password_hash)
                VALUES ('role_removal_user', 'test-password-hash')
                RETURNING id
                """,
                Long.class
        );

        Long roleId = jdbcTemplate.queryForObject(
                "SELECT id FROM roles WHERE name = 'VIEWER'",
                Long.class
        );

        jdbcTemplate.update(
                "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)",
                userId,
                roleId
        );

        long versionBeforeRemoval = getTokenVersion(userId);

        jdbcTemplate.update(
                "DELETE FROM user_roles WHERE user_id = ? AND role_id = ?",
                userId,
                roleId
        );

        assertEquals(
                versionBeforeRemoval + 1,
                getTokenVersion(userId)
        );
    }

    /**
     * Verifies that granting a new permission to a role
     * increments the token version of users holding that role.
     */
    @Test
    void addingRolePermission_incrementsAffectedUserTokenVersion() {

        Long userId = jdbcTemplate.queryForObject(
                """
                INSERT INTO users (username, password_hash)
                VALUES ('permission_change_user', 'test-password-hash')
                RETURNING id
                """,
                Long.class
        );

        Long viewerRoleId = jdbcTemplate.queryForObject(
                "SELECT id FROM roles WHERE name = 'VIEWER'",
                Long.class
        );

        Long deletePermissionId = jdbcTemplate.queryForObject(
                "SELECT id FROM permissions WHERE name = 'PRODUCT_DELETE'",
                Long.class
        );

        jdbcTemplate.update(
                "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)",
                userId,
                viewerRoleId
        );

        long versionBeforePermissionChange = getTokenVersion(userId);

        jdbcTemplate.update(
                """
                INSERT INTO role_permissions (role_id, permission_id)
                VALUES (?, ?)
                """,
                viewerRoleId,
                deletePermissionId
        );

        assertEquals(
                versionBeforePermissionChange + 1,
                getTokenVersion(userId)
        );
    }

    /**
     * Verifies that updating non-security metadata
     * does not unnecessarily revoke a user's JWTs.
     */
    @Test
    void noOpUserRoleUpdate_doesNotIncrementTokenVersion() {

        Long userId = jdbcTemplate.queryForObject(
                """
                INSERT INTO users (username, password_hash)
                VALUES ('noop_role_user', 'test-password-hash')
                RETURNING id
                """,
                Long.class
        );

        Long roleId = jdbcTemplate.queryForObject(
                "SELECT id FROM roles WHERE name = 'VIEWER'",
                Long.class
        );

        jdbcTemplate.update(
                "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)",
                userId,
                roleId
        );

        long versionBeforeUpdate = getTokenVersion(userId);

        jdbcTemplate.update(
                """
                UPDATE user_roles
                SET created_at = created_at
                WHERE user_id = ? AND role_id = ?
                """,
                userId,
                roleId
        );

        assertEquals(
                versionBeforeUpdate,
                getTokenVersion(userId)
        );
    }

    /**
     * Verifies that a no-op role-permission update
     * does not unnecessarily revoke JWTs.
     */
    @Test
    void noOpRolePermissionUpdate_doesNotIncrementTokenVersion() {

        Long userId = jdbcTemplate.queryForObject(
                """
                INSERT INTO users (username, password_hash)
                VALUES ('noop_permission_user', 'test-password-hash')
                RETURNING id
                """,
                Long.class
        );

        Long viewerRoleId = jdbcTemplate.queryForObject(
                "SELECT id FROM roles WHERE name = 'VIEWER'",
                Long.class
        );

        Long readPermissionId = jdbcTemplate.queryForObject(
                "SELECT id FROM permissions WHERE name = 'PRODUCT_READ'",
                Long.class
        );

        jdbcTemplate.update(
                "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)",
                userId,
                viewerRoleId
        );

        long versionBeforeUpdate = getTokenVersion(userId);

        jdbcTemplate.update(
                """
                UPDATE role_permissions
                SET created_at = created_at
                WHERE role_id = ? AND permission_id = ?
                """,
                viewerRoleId,
                readPermissionId
        );

        assertEquals(
                versionBeforeUpdate,
                getTokenVersion(userId)
        );
    }


    /**
     * Verifies that removing a permission from a role
     * increments the token version of affected users.
     *
     * <p>This ensures previously issued JWTs are revoked
     * when a user's permissions are reduced.</p>
     *
     * @author Raneesh Sasankan
     */
    @Test
    void removingRolePermission_incrementsAffectedUserTokenVersion() {

        Long userId = jdbcTemplate.queryForObject(
                """
                INSERT INTO users (username, password_hash)
                VALUES ('permission_removal_user', 'test-password-hash')
                RETURNING id
                """,
                Long.class
        );

        Long viewerRoleId = jdbcTemplate.queryForObject(
                "SELECT id FROM roles WHERE name = 'VIEWER'",
                Long.class
        );

        Long readPermissionId = jdbcTemplate.queryForObject(
                "SELECT id FROM permissions WHERE name = 'PRODUCT_READ'",
                Long.class
        );

        jdbcTemplate.update(
                """
                INSERT INTO user_roles (user_id, role_id)
                VALUES (?, ?)
                """,
                userId,
                viewerRoleId
        );

        long versionBeforeRemoval = getTokenVersion(userId);

        int affectedRows = jdbcTemplate.update(
                """
                DELETE FROM role_permissions
                WHERE role_id = ?
                  AND permission_id = ?
                """,
                viewerRoleId,
                readPermissionId
        );

        assertEquals(1, affectedRows);

        assertEquals(
                versionBeforeRemoval + 1,
                getTokenVersion(userId)
        );
    }

    /**
     * Retrieves the current token version of a user.
     *
     * @param userId database user identifier
     * @return current token version
     */
    private long getTokenVersion(Long userId) {

        Long version = jdbcTemplate.queryForObject(
                """
                SELECT token_version
                FROM users
                WHERE id = ?
                """,
                Long.class,
                userId
        );

        return java.util.Objects.requireNonNull(version);
    }

}
