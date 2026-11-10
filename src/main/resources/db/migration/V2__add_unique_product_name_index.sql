-- V2: Prevent duplicate product names.
-- Comparison ignores capitalization and surrounding whitespace.

CREATE UNIQUE INDEX ux_products_normalized_name
    ON products (LOWER(TRIM(name)));
