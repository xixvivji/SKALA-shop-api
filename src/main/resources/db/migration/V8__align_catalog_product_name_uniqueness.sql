ALTER TABLE catalog.products
    DROP CONSTRAINT IF EXISTS products_name_key;

DO $$
BEGIN
    IF EXISTS (
        SELECT LOWER(BTRIM(name))
        FROM catalog.products
        WHERE status <> 'DELETED'
        GROUP BY LOWER(BTRIM(name))
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Normalize duplicate active product names before V8';
    END IF;
END
$$;

CREATE UNIQUE INDEX uk_catalog_products_active_name_ci
    ON catalog.products (LOWER(BTRIM(name)))
    WHERE status <> 'DELETED';
