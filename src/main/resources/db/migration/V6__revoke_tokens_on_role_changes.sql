-- ============================================================
-- V6: Automatically revoke JWTs when role assignments
--     or role-permission assignments change.
--
-- Token versions are incremented for affected users.
-- No-op updates do not revoke tokens.
--
-- @author Raneesh Sasankan
-- ============================================================


-- ============================================================
-- 1. User role assignment changes
-- ============================================================

CREATE OR REPLACE FUNCTION revoke_tokens_on_user_role_change()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN

    -- A new role was assigned to a user.
    IF TG_OP = 'INSERT' THEN

        UPDATE users
        SET token_version = token_version + 1
        WHERE id = NEW.user_id;

    -- A role was removed from a user.
    ELSIF TG_OP = 'DELETE' THEN

        UPDATE users
        SET token_version = token_version + 1
        WHERE id = OLD.user_id;

    -- An existing role assignment was updated.
    ELSIF TG_OP = 'UPDATE' THEN

        -- Only revoke when the user or role actually changes.
        IF OLD.user_id IS DISTINCT FROM NEW.user_id
           OR OLD.role_id IS DISTINCT FROM NEW.role_id THEN

            UPDATE users
            SET token_version = token_version + 1
            WHERE id IN (
                         OLD.user_id,
                         NEW.user_id
                );

        END IF;

    END IF;

RETURN NULL;

END;
$$;


-- Create the user-role revocation trigger.
CREATE TRIGGER trg_revoke_tokens_user_roles
AFTER INSERT OR UPDATE OR DELETE
ON user_roles
FOR EACH ROW
EXECUTE FUNCTION revoke_tokens_on_user_role_change();


-- ============================================================
-- 2. Role permission assignment changes
-- ============================================================

CREATE OR REPLACE FUNCTION revoke_tokens_on_role_permission_change()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN

    -- A permission was assigned to a role.
    IF TG_OP = 'INSERT' THEN

        UPDATE users
        SET token_version = token_version + 1
        WHERE id IN (
            SELECT ur.user_id
            FROM user_roles ur
            WHERE ur.role_id = NEW.role_id
        );

    -- A permission was removed from a role.
    ELSIF TG_OP = 'DELETE' THEN

        UPDATE users
        SET token_version = token_version + 1
        WHERE id IN (
            SELECT ur.user_id
            FROM user_roles ur
            WHERE ur.role_id = OLD.role_id
        );

    -- An existing role-permission assignment changed.
    ELSIF TG_OP = 'UPDATE' THEN

        -- Avoid revocation for no-op updates.
        IF OLD.role_id IS DISTINCT FROM NEW.role_id
           OR OLD.permission_id IS DISTINCT
              FROM NEW.permission_id THEN

            -- Invalidate tokens for users of either role.
            UPDATE users
            SET token_version = token_version + 1
            WHERE id IN (
                SELECT ur.user_id
                FROM user_roles ur
                WHERE ur.role_id IN (
                                     OLD.role_id,
                                     NEW.role_id
                    )
            );

        END IF;

    END IF;

RETURN NULL;

END;
$$;


-- Create the role-permission revocation trigger.
CREATE TRIGGER trg_revoke_tokens_role_permissions
    AFTER INSERT OR UPDATE OR DELETE
                    ON role_permissions
                        FOR EACH ROW
                        EXECUTE FUNCTION revoke_tokens_on_role_permission_change();
