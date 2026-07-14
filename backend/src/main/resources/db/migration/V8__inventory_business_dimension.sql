ALTER TABLE order_catalog_products
  ADD COLUMN inventory_business VARCHAR(64) NULL AFTER category,
  ADD KEY ix_order_catalog_inventory_business (location_code, inventory_business, active);

ALTER TABLE inventory_count_sessions
  ADD COLUMN inventory_business VARCHAR(64) NOT NULL DEFAULT 'BIANGBIANG_FRONT' AFTER location_code,
  ADD KEY ix_inventory_sessions_location_business_date (location_code, inventory_business, business_date);

UPDATE order_catalog_products
SET inventory_business = 'BIANGBIANG_FRONT'
WHERE source_key LIKE 'p2-%'
   OR source_key LIKE 'p1-p2-%'
   OR source_key LIKE '%-p2-%';

UPDATE order_catalog_products
SET inventory_business = 'PAPER_FAN'
WHERE source_key LIKE 'p3-%';
