ALTER TABLE vendors
  ADD COLUMN source_key VARCHAR(190) NULL AFTER vendor_code,
  ADD COLUMN source_version VARCHAR(64) NULL AFTER source_key,
  ADD COLUMN imported_from_reference BOOLEAN NOT NULL DEFAULT FALSE AFTER source_version,
  ADD UNIQUE KEY uk_vendors_location_source_key (location_code, source_key);

ALTER TABLE order_catalog_products
  ADD COLUMN source_key VARCHAR(190) NULL AFTER vendor_product_code,
  ADD COLUMN source_version VARCHAR(64) NULL AFTER source_key,
  ADD COLUMN imported_from_reference BOOLEAN NOT NULL DEFAULT FALSE AFTER source_version,
  ADD UNIQUE KEY uk_products_location_vendor_source_key (location_code, vendor_id, source_key);
