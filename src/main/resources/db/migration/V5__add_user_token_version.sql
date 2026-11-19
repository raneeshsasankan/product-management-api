-- ============================================================
-- V5: Add JWT token versioning support.
--
-- The token_version column will allow the application
-- to invalidate previously issued access tokens when
-- the user's security state changes.
--
-- @author Raneesh Sasankan
-- ============================================================

ALTER TABLE users
    ADD COLUMN token_version BIGINT NOT NULL DEFAULT 0;

-- Prevent negative token version values.
ALTER TABLE users
    ADD CONSTRAINT chk_users_token_version_nonnegative
        CHECK (token_version >= 0);
