CREATE TEMPORARY TABLE v12_order_catalog_vendors (
  vendor_name VARCHAR(190) NOT NULL PRIMARY KEY,
  normalized_name VARCHAR(190) NOT NULL,
  source_key VARCHAR(190) NOT NULL,
  display_order INT NOT NULL,
  notes TEXT NULL
);

INSERT INTO v12_order_catalog_vendors (vendor_name, normalized_name, source_key, display_order, notes) VALUES
  ('Wellpack', 'wellpack', 'p1-wellpack', 0, 'Latest Order List vendor. Do not include delivery-day labels such as (Wed.) in the vendor name.'),
  ('JFC', 'jfc', 'p1-p3-jfc', 1, 'Latest Order List vendor.'),
  ('CO-HO', 'co-ho', 'p1-p3-coho', 2, 'Latest Order List vendor. CO-HO and Coho remain one canonical vendor.'),
  ('Southern Glazer''s', 'southern glazer''s', 'p1-p3-southern-glazers', 3, 'Latest Order List vendor. Southern Glazer, Southern Glazer''s, and SG remain one canonical vendor.'),
  ('Costco', 'costco', 'p1-p2-costco', 4, 'Latest Order List vendor. Coke / Diet Coke remains represented by existing Coke and Diet Coke catalog rows to avoid duplicates.'),
  ('GIC', 'gic', 'p1-gic', 5, 'Latest Order List vendor.'),
  ('Sysco', 'sysco', 'p1-sysco', 6, 'Latest Order List vendor.'),
  ('No Delivery', 'no delivery', 'p1-no-delivery', 7, 'Latest Order List vendor.');

INSERT INTO vendors (
  location_code,
  name,
  normalized_name,
  source_key,
  source_version,
  imported_from_reference,
  notes,
  active,
  display_order,
  created_at,
  updated_at
)
SELECT
  'SEATTLE',
  target.vendor_name,
  target.normalized_name,
  target.source_key,
  'latest-order-list-2026-07-15',
  TRUE,
  target.notes,
  TRUE,
  target.display_order,
  CURRENT_TIMESTAMP(6),
  CURRENT_TIMESTAMP(6)
FROM v12_order_catalog_vendors target
WHERE NOT EXISTS (
  SELECT 1
  FROM vendors existing
  WHERE existing.location_code = 'SEATTLE'
    AND existing.normalized_name = target.normalized_name
);

UPDATE vendors existing
JOIN v12_order_catalog_vendors target
  ON existing.location_code = 'SEATTLE'
 AND existing.normalized_name = target.normalized_name
SET
  existing.source_key = COALESCE(existing.source_key, target.source_key),
  existing.source_version = 'latest-order-list-2026-07-15',
  existing.imported_from_reference = TRUE,
  existing.active = TRUE,
  existing.display_order = target.display_order,
  existing.updated_at = CURRENT_TIMESTAMP(6);

CREATE TEMPORARY TABLE v12_order_catalog_products (
  vendor_name VARCHAR(190) NOT NULL,
  source_key VARCHAR(190) NOT NULL,
  vendor_product_code VARCHAR(64) NULL,
  product_name VARCHAR(190) NOT NULL,
  normalized_name VARCHAR(190) NOT NULL,
  category VARCHAR(64) NOT NULL,
  order_unit VARCHAR(32) NOT NULL,
  order_unit_label VARCHAR(80) NOT NULL,
  display_order INT NOT NULL,
  notes TEXT NULL,
  PRIMARY KEY (vendor_name, source_key)
);

INSERT INTO v12_order_catalog_products (vendor_name, source_key, vendor_product_code, product_name, normalized_name, category, order_unit, order_unit_label, display_order, notes) VALUES
  ('Wellpack', 'p1-wellpack-l32b-black-dry-mix', 'L32B', 'Black (Dry mix)', 'l32b black (dry mix)', 'PACKAGING', 'CASE', 'case', 0, 'Latest Order List: L32B, Black (Dry mix).'),
  ('Wellpack', 'p1-wellpack-box8-white-wings', 'Box #8', 'White (Wings)', 'box #8 white (wings)', 'PACKAGING', 'CASE', 'case', 1, 'Latest Order List: Box #8, White (Wings).'),
  ('Wellpack', 'p1-wellpack-s16p240-round-16oz-soup', 'S16P240', 'Round 16oz. (Soup)', 's16p240 round 16oz. (soup)', 'PACKAGING', 'CASE', 'case', 2, 'Latest Order List: S16P240, Round 16oz. (Soup).'),
  ('Wellpack', 'p1-wellpack-milk-tea-cup-iced', NULL, 'Milk Tea Cup (Iced)', 'milk tea cup (iced)', 'TEA_AND_MILK_TEA', 'CASE', 'case', 3, 'Latest Order List: Milk Tea Cup (Iced).'),
  ('Wellpack', 'p1-wellpack-box1-white-bao', 'Box #1', 'White (Bao)', 'box #1 white (bao)', 'PACKAGING', 'CASE', 'case', 4, 'Latest Order List: Box #1, White (Bao).'),
  ('Wellpack', 'p1-wellpack-np14162-napkin-2-ply', 'NP14162', 'Napkin 2 ply', 'np14162 napkin 2 ply', 'DISPOSABLES', 'CASE', 'case', 5, 'Latest Order List: NP14162, Napkin 2 ply.'),
  ('Wellpack', 'p1-wellpack-nb38-black-soup-noodle', 'NB-38', 'Black (Soup noodle)', 'nb-38 black (soup noodle)', 'PACKAGING', 'CASE', 'case', 6, 'Latest Order List: NB-38, Black (Soup noodle).'),
  ('Wellpack', 'p1-wellpack-milk-tea-lid-iced', NULL, 'Milk Tea Lid (Iced)', 'milk tea lid (iced)', 'TEA_AND_MILK_TEA', 'CASE', 'case', 7, 'Latest Order List: Milk Tea Lid (Iced).'),
  ('Wellpack', 'p1-wellpack-bamboo-chopstick-8', NULL, 'Bamboo Chopstick 8"', 'bamboo chopstick 8"', 'DISPOSABLES', 'CASE', 'case', 8, 'Latest Order List: Bamboo Chopstick 8".'),
  ('Wellpack', 'p1-wellpack-fork-black', 'FORK', 'black', 'fork black', 'DISPOSABLES', 'CASE', 'case', 9, 'Latest Order List: FORK, black.'),
  ('Wellpack', 'p1-wellpack-tshirt-compostable-bag', NULL, 'T-Shirt compostable bag', 't-shirt compostable bag', 'DISPOSABLES', 'CASE', 'case', 10, 'Latest Order List: T-Shirt compostable bag.'),
  ('Wellpack', 'p1-wellpack-paper-cup-16oz-hot', NULL, 'Paper Cup 16oz (Hot)', 'paper cup 16oz (hot)', 'DISPOSABLES', 'CASE', 'case', 11, 'Latest Order List: Paper Cup 16oz (Hot).'),
  ('Wellpack', 'p1-wellpack-spoon-black', 'SPOON', 'black', 'spoon black', 'DISPOSABLES', 'CASE', 'case', 12, 'Latest Order List: SPOON, black.'),
  ('Wellpack', 'p1-wellpack-straw-black', NULL, 'Straw (Black)', 'straw (black)', 'DISPOSABLES', 'CASE', 'case', 13, 'Latest Order List: Straw (Black).'),
  ('Wellpack', 'p1-wellpack-wftw4000-hand-tissue', 'WFTW4000', 'Hand tissue', 'wftw4000 hand tissue', 'RESTROOM_SUPPLIES', 'CASE', 'case', 14, 'Latest Order List: WFTW4000, Hand tissue.'),
  ('Wellpack', 'p1-wellpack-paper-cup-lid-hot', NULL, 'Paper Cup Lid (Hot)', 'paper cup lid (hot)', 'DISPOSABLES', 'CASE', 'case', 15, 'Latest Order List: Paper Cup Lid (Hot).'),
  ('Wellpack', 'p1-wellpack-s8p240-round-8oz-rice', 'SBP240', 'Round 8oz. (Rice)', 'sbp240 round 8oz. (rice)', 'PACKAGING', 'CASE', 'case', 16, 'Latest Order List: SBP240, Round 8oz. (Rice). Existing source key retained to avoid duplicate S8P240/SBP240 rows.'),
  ('JFC', 'p1-p2-jfc-sapporo-beer', NULL, 'Sapporo Beer', 'sapporo beer', 'BEER', 'CASE', 'case', 0, 'Latest Order List: Sapporo Beer.'),
  ('CO-HO', 'p1-coho-sho-chiku-bai-1l-case', NULL, 'Sho Chiku Bai 1L', 'sho chiku bai 1l', 'SAKE', 'CASE', 'case', 0, 'Latest Order List: Sho Chiku Bai 1L.'),
  ('CO-HO', 'p1-p2-coho-jinro-greengrape', NULL, 'JINRO SOJU Green Grape', 'jinro soju green grape', 'SOJU', 'BOTTLE', 'bt', 1, 'Latest Order List: JINRO SOJU Green Grape. Existing Greengrape row reused.'),
  ('CO-HO', 'p1-p2-coho-taiwan-gold-medal-beer', NULL, 'Taiwan Gold Medal Beer', 'taiwan gold medal beer', 'BEER', 'CASE', 'case', 2, 'Latest Order List: Taiwan Gold Medal Beer.'),
  ('CO-HO', 'p1-p2-coho-jinro-strawberry', NULL, 'JINRO SOJU Strawberry', 'jinro soju strawberry', 'SOJU', 'BOTTLE', 'bt', 3, 'Latest Order List: JINRO SOJU Strawberry.'),
  ('CO-HO', 'p1-p2-coho-taiwan-lychee-beer', NULL, 'Taiwan Lychee Beer', 'taiwan lychee beer', 'BEER', 'CASE', 'case', 4, 'Latest Order List: Taiwan Lychee Beer.'),
  ('CO-HO', 'p1-p2-coho-jinro-original', NULL, 'JINRO SOJU Original', 'jinro soju original', 'SOJU', 'BOTTLE', 'bt', 5, 'Latest Order List: JINRO SOJU Original.'),
  ('CO-HO', 'p1-p2-coho-jinro-peach', NULL, 'JINRO SOJU Peach', 'jinro soju peach', 'SOJU', 'BOTTLE', 'bt', 6, 'Latest Order List: JINRO SOJU Peach.'),
  ('Southern Glazer''s', 'p1-p2-sg-volka-smiroff', NULL, 'Vodka Smirnoff', 'vodka smirnoff', 'SPIRITS', 'CASE', 'case', 0, 'Latest Order List: Vodka Smirnoff. Existing Volka Smiroff row reused.'),
  ('Southern Glazer''s', 'p1-p2-sg-tequila-sauza', NULL, 'Tequila Sauza', 'tequila sauza', 'SPIRITS', 'CASE', 'case', 1, 'Latest Order List: Tequila Sauza.'),
  ('Southern Glazer''s', 'p1-p2-sg-rum-barcadi', NULL, 'Rum Bacardi', 'rum bacardi', 'SPIRITS', 'CASE', 'case', 2, 'Latest Order List: Rum Bacardi. Existing Rum Barcadi row reused.'),
  ('Southern Glazer''s', 'p1-p2-sg-captain-rum', NULL, 'Captain Rum', 'captain rum', 'SPIRITS', 'CASE', 'case', 3, 'Latest Order List: Captain Rum.'),
  ('Southern Glazer''s', 'p1-p2-sg-yuzu-sparkling-sake', NULL, 'Yuzu Sparkling Sake', 'yuzu sparkling sake', 'SAKE', 'CASE', 'case', 4, 'Latest Order List: Yuzu Sparkling Sake.'),
  ('Southern Glazer''s', 'p1-p2-sg-whiskey-evan-williams', NULL, 'Whiskey Evan Williams', 'whiskey evan williams', 'SPIRITS', 'CASE', 'case', 5, 'Latest Order List: Whiskey Evan Williams.'),
  ('Southern Glazer''s', 'p1-p2-sg-gin-beefeater', NULL, 'Gin Beefeater', 'gin beefeater', 'SPIRITS', 'CASE', 'case', 6, 'Latest Order List: Gin Beefeater.'),
  ('Southern Glazer''s', 'p1-p2-sg-ming-river', NULL, 'Ming River', 'ming river', 'SPIRITS', 'CASE', 'case', 7, 'Latest Order List: Ming River.'),
  ('Southern Glazer''s', 'p1-p2-sg-vodka-titos', NULL, 'Vodka Tito''s', 'vodka tito''s', 'SPIRITS', 'CASE', 'case', 8, 'Latest Order List: Vodka Tito''s.'),
  ('Southern Glazer''s', 'p1-sg-jameson-irish-whiskey', NULL, 'JAMESON IRISH WHISKEY', 'jameson irish whiskey', 'SPIRITS', 'CASE', 'case', 9, 'Latest Order List: JAMESON IRISH WHISKEY.'),
  ('Costco', 'p1-p2-costco-coke', NULL, 'Coke', 'coke', 'NON_ALCOHOLIC_BEVERAGES', 'CASE', 'case', 0, 'Latest Order List: Coke / Diet Coke. Existing Coke row retained to avoid duplicate combined rows.'),
  ('Costco', 'p1-p2-costco-diet-coke', NULL, 'Diet Coke', 'diet coke', 'NON_ALCOHOLIC_BEVERAGES', 'CASE', 'case', 1, 'Latest Order List: Coke / Diet Coke. Existing Diet Coke row retained to avoid duplicate combined rows.'),
  ('Costco', 'p1-p2-costco-sprite', NULL, 'Sprite', 'sprite', 'NON_ALCOHOLIC_BEVERAGES', 'CASE', 'case', 2, 'Latest Order List: Sprite.'),
  ('Costco', 'p1-costco-perrier-sparkling-water', NULL, 'PERRIER Sparkling Water', 'perrier sparkling water', 'NON_ALCOHOLIC_BEVERAGES', 'CASE', 'case', 3, 'Latest Order List: PERRIER Sparkling Water.'),
  ('Costco', 'p1-costco-ginger-beer', NULL, 'Ginger Beer', 'ginger beer', 'NON_ALCOHOLIC_BEVERAGES', 'CASE', 'case', 4, 'Latest Order List: Ginger Beer.'),
  ('GIC', 'p1-gic-lychee-fruit-can', NULL, 'Lychee Fruit Can', 'lychee fruit can', 'COCKTAIL_INGREDIENTS', 'CASE', 'case', 0, 'Latest Order List: Lychee Fruit Can.'),
  ('GIC', 'p1-gic-coconut-milk-can', NULL, 'Coconut Milk Can', 'coconut milk can', 'COCKTAIL_INGREDIENTS', 'CASE', 'case', 1, 'Latest Order List: Coconut Milk Can.'),
  ('GIC', 'p1-gic-thai-tea', NULL, 'Thai Tea', 'thai tea', 'TEA_AND_MILK_TEA', 'CASE', 'case', 2, 'Latest Order List: Thai Tea.'),
  ('Sysco', 'p1-sysco-mint-leaf', NULL, 'Mint Leaf', 'mint leaf', 'PRODUCE', 'CASE', 'case', 0, 'Latest Order List: Mint Leaf.'),
  ('Sysco', 'p1-sysco-glove-black-sml', NULL, 'Glove Black (S, M, L)', 'glove black (s, m, l)', 'DISPOSABLES', 'CASE', 'case', 1, 'Latest Order List: Glove Black (S, M, L). Existing Glove Black (S/M/L) row reused.'),
  ('Sysco', 'p1-sysco-lemon-fruit-ct', NULL, 'Lemon Fruit', 'lemon fruit', 'PRODUCE', 'COUNT', 'ct', 2, 'Latest Order List: Lemon Fruit. Unit marker (ct) removed from display name.'),
  ('Sysco', 'p1-sysco-evaporated-milk', NULL, 'Evaporated Milk', 'evaporated milk', 'COCKTAIL_INGREDIENTS', 'CASE', 'case', 3, 'Latest Order List: Evaporated Milk.'),
  ('Sysco', 'p1-sysco-square-ice', NULL, 'SQUARE ICE', 'square ice', 'OTHER', 'CASE', 'case', 4, 'Latest Order List: SQUARE ICE.'),
  ('Sysco', 'p1-sysco-grenadine', NULL, 'Grenadine', 'grenadine', 'COCKTAIL_INGREDIENTS', 'CASE', 'case', 5, 'Latest Order List: Grenadine.'),
  ('Sysco', 'p1-sysco-almond-syrup', NULL, 'Almond Syrup', 'almond syrup', 'COCKTAIL_INGREDIENTS', 'CASE', 'case', 6, 'Latest Order List: Almond Syrup.'),
  ('Sysco', 'p1-sysco-lime-juice-bt', NULL, 'Lime Juice', 'lime juice', 'COCKTAIL_INGREDIENTS', 'BOTTLE', 'bt', 7, 'Latest Order List: Lime Juice. Unit marker (bt) removed from display name.'),
  ('Sysco', 'p1-sysco-lemon-juice-bt', NULL, 'Lemon Juice', 'lemon juice', 'COCKTAIL_INGREDIENTS', 'BOTTLE', 'bt', 8, 'Latest Order List: Lemon Juice. Unit marker (bt) removed from display name.'),
  ('No Delivery', 'p1-no-delivery-mango-puree-can', NULL, 'Mango Puree Can', 'mango puree can', 'COCKTAIL_INGREDIENTS', 'CASE', 'case', 0, 'Latest Order List: Mango Puree Can.'),
  ('No Delivery', 'p1-no-delivery-cocktail-cherry', NULL, 'Cocktail Cherry', 'cocktail cherry', 'COCKTAIL_INGREDIENTS', 'CASE', 'case', 1, 'Latest Order List: Cocktail Cherry.'),
  ('No Delivery', 'p1-no-delivery-pineapple-juice', NULL, 'Pineapple Juice', 'pineapple juice', 'COCKTAIL_INGREDIENTS', 'CASE', 'case', 2, 'Latest Order List: Pineapple Juice.'),
  ('No Delivery', 'p1-no-delivery-tonic-water', NULL, 'Tonic Water', 'tonic water', 'NON_ALCOHOLIC_BEVERAGES', 'CASE', 'case', 3, 'Latest Order List: Tonic Water.'),
  ('No Delivery', 'p1-no-delivery-pos-thermal-paper', NULL, 'POS Thermal Paper', 'pos thermal paper', 'OFFICE_AND_POS', 'CASE', 'case', 4, 'Latest Order List: POS Thermal Paper.'),
  ('No Delivery', 'p1-no-delivery-clorox-lysol-wipes', NULL, 'Clorox / Lysol Wipes', 'clorox / lysol wipes', 'CLEANING', 'CASE', 'case', 5, 'Latest Order List: Clorox / Lysol Wipes. Existing Clorox/ Lysol Wipes row reused.'),
  ('No Delivery', 'p1-no-delivery-toilet-paper', NULL, 'Toilet Paper', 'toilet paper', 'RESTROOM_SUPPLIES', 'CASE', 'case', 6, 'Latest Order List: Toilet Paper.'),
  ('No Delivery', 'p1-no-delivery-toilet-seat-cover', NULL, 'Toilet Seat Cover', 'toilet seat cover', 'RESTROOM_SUPPLIES', 'CASE', 'case', 7, 'Latest Order List: Toilet Seat Cover.'),
  ('No Delivery', 'p1-no-delivery-bamboo-skewers', NULL, 'BAMBOO Skewers', 'bamboo skewers', 'DISPOSABLES', 'CASE', 'case', 8, 'Latest Order List: BAMBOO Skewers.'),
  ('No Delivery', 'p1-no-delivery-straw-5-25-cocktails', NULL, 'Straw 5.25 inch (Cocktails)', 'straw 5.25 inch (cocktails)', 'DISPOSABLES', 'CASE', 'case', 9, 'Latest Order List: Straw 5.25 inch (Cocktails).'),
  ('No Delivery', 'p1-no-delivery-mask', NULL, 'Mask', 'mask', 'DISPOSABLES', 'CASE', 'case', 10, 'Latest Order List: Mask.'),
  ('No Delivery', 'p1-no-delivery-simple-green', NULL, 'Simple Green', 'simple green', 'CLEANING', 'CASE', 'case', 11, 'Latest Order List: Simple Green.'),
  ('No Delivery', 'p1-no-delivery-hand-soap', NULL, 'Hand Soap', 'hand soap', 'RESTROOM_SUPPLIES', 'CASE', 'case', 12, 'Latest Order List: Hand Soap.'),
  ('No Delivery', 'p1-no-delivery-tape', NULL, 'Tape', 'tape', 'OFFICE_AND_POS', 'CASE', 'case', 13, 'Latest Order List: Tape.'),
  ('No Delivery', 'p1-no-delivery-markers', NULL, 'Markers', 'markers', 'OFFICE_AND_POS', 'CASE', 'case', 14, 'Latest Order List: Markers.'),
  ('No Delivery', 'p1-no-delivery-milk-tea-creama', NULL, 'Milk Tea Crema', 'milk tea crema', 'TEA_AND_MILK_TEA', 'CASE', 'case', 15, 'Latest Order List: Milk Tea Crema. Existing Milk tea Creama row reused.'),
  ('No Delivery', 'p1-no-delivery-green-tea-leaf', NULL, 'Green Tea Leaf', 'green tea leaf', 'TEA_AND_MILK_TEA', 'CASE', 'case', 16, 'Latest Order List: Green Tea Leaf.'),
  ('No Delivery', 'p1-no-delivery-black-tea-leaf-milk-tea', NULL, 'Black Tea Leaf (Milk Tea)', 'black tea leaf (milk tea)', 'TEA_AND_MILK_TEA', 'CASE', 'case', 17, 'Latest Order List: Black Tea Leaf (Milk Tea).'),
  ('No Delivery', 'p1-no-delivery-oolong-tea-leaf-hot-tea', NULL, 'Oolong Tea Leaf (Hot Tea)', 'oolong tea leaf (hot tea)', 'TEA_AND_MILK_TEA', 'CASE', 'case', 18, 'Latest Order List: Oolong Tea Leaf (Hot Tea).'),
  ('No Delivery', 'p1-no-delivery-topochico', NULL, 'Topo Chico', 'topo chico', 'NON_ALCOHOLIC_BEVERAGES', 'CASE', 'case', 19, 'Latest Order List: Topo Chico. Existing Topochico row reused.'),
  ('No Delivery', 'p1-no-delivery-lychee-syrup', NULL, 'Lychee Syrup', 'lychee syrup', 'COCKTAIL_INGREDIENTS', 'CASE', 'case', 20, 'Latest Order List: Lychee Syrup.'),
  ('No Delivery', 'p1-no-delivery-passion-fruit-syrup', NULL, 'Passion Fruit Syrup', 'passion fruit syrup', 'COCKTAIL_INGREDIENTS', 'CASE', 'case', 21, 'Latest Order List: Passion Fruit Syrup.'),
  ('No Delivery', 'p1-no-delivery-triple-sec', NULL, 'Triple Sec', 'triple sec', 'SPIRITS', 'CASE', 'case', 22, 'Latest Order List: Triple Sec.'),
  ('No Delivery', 'p1-no-delivery-monin-rose-syrup', NULL, 'Monin Rose Syrup', 'monin rose syrup', 'COCKTAIL_INGREDIENTS', 'CASE', 'case', 23, 'Latest Order List: Monin Rose Syrup.');

CREATE TEMPORARY TABLE v12_order_catalog_product_aliases (
  vendor_name VARCHAR(190) NOT NULL,
  source_key VARCHAR(190) NOT NULL,
  alias_normalized_name VARCHAR(190) NOT NULL,
  PRIMARY KEY (vendor_name, source_key, alias_normalized_name)
);

INSERT INTO v12_order_catalog_product_aliases (vendor_name, source_key, alias_normalized_name) VALUES
  ('Wellpack', 'p1-wellpack-s16p240-round-16oz-soup', 's16p240 round 16oz (soup)'),
  ('Wellpack', 'p1-wellpack-bamboo-chopstick-8', 'bamboo chopstick, 8 inch'),
  ('Wellpack', 'p1-wellpack-bamboo-chopstick-8', 'bamboo chopstick, 8"'),
  ('Wellpack', 'p1-wellpack-s8p240-round-8oz-rice', 's8p240 round 8oz (rice)'),
  ('Wellpack', 'p1-wellpack-s8p240-round-8oz-rice', 's8p240 round 8oz. (rice)'),
  ('CO-HO', 'p1-p2-coho-jinro-greengrape', 'jinro soju greengrape'),
  ('Southern Glazer''s', 'p1-p2-sg-volka-smiroff', 'volka smiroff'),
  ('Southern Glazer''s', 'p1-p2-sg-volka-smiroff', 'smirnoff vodka'),
  ('Southern Glazer''s', 'p1-p2-sg-rum-barcadi', 'rum barcadi'),
  ('Southern Glazer''s', 'p1-p2-sg-rum-barcadi', 'bacardi rum'),
  ('Sysco', 'p1-sysco-glove-black-sml', 'glove black (s/m/l)'),
  ('Sysco', 'p1-sysco-lemon-fruit-ct', 'lemon fruit (ct)'),
  ('No Delivery', 'p1-no-delivery-clorox-lysol-wipes', 'clorox/ lysol wipes'),
  ('No Delivery', 'p1-no-delivery-clorox-lysol-wipes', 'clorox/lysol wipes'),
  ('No Delivery', 'p1-no-delivery-milk-tea-creama', 'milk tea creama'),
  ('No Delivery', 'p1-no-delivery-milk-tea-creama', 'milk tea creamer'),
  ('No Delivery', 'p1-no-delivery-topochico', 'topochico');

UPDATE order_catalog_products existing
JOIN vendors vendor
  ON vendor.id = existing.vendor_id
JOIN v12_order_catalog_products target
  ON target.vendor_name = vendor.name
LEFT JOIN v12_order_catalog_product_aliases alias
  ON alias.vendor_name = target.vendor_name
 AND alias.source_key = target.source_key
 AND alias.alias_normalized_name = existing.normalized_name
SET
  existing.vendor_product_code = target.vendor_product_code,
  existing.name = target.product_name,
  existing.normalized_name = target.normalized_name,
  existing.category = target.category,
  existing.order_unit = target.order_unit,
  existing.order_unit_label = target.order_unit_label,
  existing.order_business = 'BIANGBIANG_FRONT',
  existing.source_key = COALESCE(existing.source_key, target.source_key),
  existing.source_version = 'latest-order-list-2026-07-15',
  existing.imported_from_reference = TRUE,
  existing.active = TRUE,
  existing.display_order = target.display_order,
  existing.notes = COALESCE(existing.notes, target.notes),
  existing.updated_at = CURRENT_TIMESTAMP(6)
WHERE existing.location_code = 'SEATTLE'
  AND (
    existing.source_key = target.source_key
    OR existing.normalized_name = target.normalized_name
    OR alias.alias_normalized_name IS NOT NULL
  );

INSERT INTO order_catalog_products (
  location_code,
  vendor_id,
  vendor_product_code,
  source_key,
  source_version,
  imported_from_reference,
  name,
  normalized_name,
  category,
  inventory_business,
  order_business,
  inventory_unit,
  inventory_unit_label,
  order_unit,
  order_unit_label,
  currency,
  active,
  display_order,
  notes,
  created_at,
  updated_at
)
SELECT
  'SEATTLE',
  vendor.id,
  target.vendor_product_code,
  target.source_key,
  'latest-order-list-2026-07-15',
  TRUE,
  target.product_name,
  target.normalized_name,
  target.category,
  NULL,
  'BIANGBIANG_FRONT',
  target.order_unit,
  target.order_unit_label,
  target.order_unit,
  target.order_unit_label,
  'USD',
  TRUE,
  target.display_order,
  target.notes,
  CURRENT_TIMESTAMP(6),
  CURRENT_TIMESTAMP(6)
FROM v12_order_catalog_products target
JOIN vendors vendor
  ON vendor.location_code = 'SEATTLE'
 AND vendor.name = target.vendor_name
WHERE NOT EXISTS (
  SELECT 1
  FROM order_catalog_products existing
  LEFT JOIN v12_order_catalog_product_aliases alias
    ON alias.vendor_name = target.vendor_name
   AND alias.source_key = target.source_key
   AND alias.alias_normalized_name = existing.normalized_name
  WHERE existing.location_code = 'SEATTLE'
    AND existing.vendor_id = vendor.id
    AND (
      existing.source_key = target.source_key
      OR existing.normalized_name = target.normalized_name
      OR alias.alias_normalized_name IS NOT NULL
    )
);

DROP TEMPORARY TABLE v12_order_catalog_product_aliases;
DROP TEMPORARY TABLE v12_order_catalog_products;
DROP TEMPORARY TABLE v12_order_catalog_vendors;
