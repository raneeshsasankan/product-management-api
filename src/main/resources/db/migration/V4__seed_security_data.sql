-- ==========================================
-- V4: Seed initial RBAC security data
--
-- @author Raneesh Sasankan
-- ============================================================

-- 1. Create roles
INSERT INTO roles (name, description) VALUES
                                          ('ADMIN', 'Full application access'),
                                          ('MANAGER', 'Manage products without delete access'),
                                          ('USER', 'Standard user with product read access'),
                                          ('VIEWER', 'Read-only access');


-- 2. Create permissions
INSERT INTO permissions (name, description) VALUES
                                                ('PRODUCT_READ', 'View and search products'),
                                                ('PRODUCT_CREATE', 'Create new products'),
                                                ('PRODUCT_UPDATE', 'Update existing products'),
                                                ('PRODUCT_DELETE', 'Delete products');


-- 3. Assign permissions to roles
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         CROSS JOIN permissions p
WHERE
    r.name = 'ADMIN'

   OR (r.name = 'MANAGER'
    AND p.name IN (
                   'PRODUCT_READ',
                   'PRODUCT_CREATE',
                   'PRODUCT_UPDATE'
        ))

   OR (r.name IN ('USER', 'VIEWER')
    AND p.name = 'PRODUCT_READ');


-- 4. Map API endpoints to permissions
INSERT INTO api_permissions (
    http_method,
    url_pattern,
    permission_id
)
SELECT mapping.http_method,
       mapping.url_pattern,
       p.id
FROM (
         VALUES
             ('GET', '/api/products/**', 'PRODUCT_READ'),
             ('POST', '/api/products', 'PRODUCT_CREATE'),
             ('PUT', '/api/products/*', 'PRODUCT_UPDATE'),
             ('DELETE', '/api/products/*', 'PRODUCT_DELETE')
     ) AS mapping(http_method, url_pattern, permission_name)
         JOIN permissions p
              ON p.name = mapping.permission_name;
