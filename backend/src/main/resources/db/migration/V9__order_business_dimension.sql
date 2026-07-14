ALTER TABLE order_catalog_products
  ADD COLUMN order_business VARCHAR(64) NULL AFTER inventory_business,
  ADD KEY ix_order_catalog_products_order_business (location_code, order_business, active);

UPDATE order_catalog_products
SET order_business = inventory_business
WHERE inventory_business IS NOT NULL;

ALTER TABLE purchase_orders
  ADD COLUMN order_business VARCHAR(64) NOT NULL DEFAULT 'BIANGBIANG_FRONT' AFTER location_code,
  ADD KEY ix_purchase_orders_order_business (location_code, order_business, business_date);
