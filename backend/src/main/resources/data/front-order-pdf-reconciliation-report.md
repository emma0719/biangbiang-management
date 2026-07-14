# Front Order PDF Reconciliation Report

Source: `docs/reference/Front Order List 前台订货.pdf`

Rendered pages inspected: 1-4

Extractor used: `pypdfium2` for page images, `pypdf` and `pdfplumber` for text/table extraction.

## Page 1

Section type: front order sheet.

Matched vendors and rows:

- Wellpack `(Wed.)`: 17 product rows imported. Visible codes captured for `L32B`, `Box #8`, `S16P240`, `Box #1`, `NP14162`, `NB-38`, `FORK`, `SPOON`, `WFTW4000`, and `S8P240`.
- JFC `(Wed)`: Sapporo Beer imported.
- CO-HO: 7 order rows imported. `Sho Chiku Bai 1L(case)` kept separate from page 2 `Sho Chiku Bai 1.5L`.
- Southern Glazer's `(Wed.)`: 10 order rows imported. PDF spellings retained for `Volka Smiroff`, `Rum Barcadi`, and `JAMESON IRISH WHISKEY`.
- Costco: 5 catalog products imported. The visible order cell `Coke / Diet Coke` maps to separate Coke and Diet Coke products because page 2 inventory separates them.
- GIC: 3 product rows imported.
- Sysco: 9 product rows imported. `Glove Black (S/M/L)` remains one product row; no separate size rows were created.
- No Delivery: 24 product rows imported as an explicit section.

Missing metadata:

- Quantity columns are blank.
- Most package specs, prices, and order units are not visible.

## Page 2

Section type: inventory count sheet.

Matched vendors and rows:

- JFC: Sapporo Beer with inventory unit `can`.
- CO-HO: 7 visible inventory rows. Units captured as `bt` for Sho Chiku Bai 1.5L and JINRO SOJU rows; `can` for Taiwan beers.
- Southern Glazer: 9 visible inventory rows. Units captured as `bt`.
- Costco: Coke, Diet Coke, and Sprite with inventory unit `can`.

Import decisions:

- Page 2 units are used as inventory units when the same product also appears on page 1.
- Page 2-only products were imported when not present on page 1: `Sho Chiku Bai 1.5L`, `Malibu Rum Coconut`, and `Cucumber Mint`.

Missing metadata:

- Quantity columns are blank.
- No prices or package specs are visible except `1.5L` in the Sho Chiku Bai name.

## Page 3

Section type: specialty vendor sheet.

Matched vendors and rows:

- ERNDC: Hendrick's Gin, Fernet Branca.
- AWS: Song Cai Gin, Green Tea Vodka.
- SG: 10 products mapped to canonical `Southern Glazer's`.
- Coho: 8 products mapped to canonical `CO-HO`.
- CD: Ming River retained separately from Southern Glazer's Ming River.
- Highside: Highside Gin, Highside Amaro.
- PMS: Faccia Burro Centerbie, Pineapple Rum.
- JFC: Matcha Powder.
- Asian Market: Spicy Peanut, Sweet Rice, Garlic Peas.
- Alison: Oolong, Hojicha, Jasmin Tea, Black Tea, Gardenia water.

Ambiguities:

- Unit columns are visible but blank for every row.
- `SG` is interpreted as Southern Glazer's based on surrounding alcohol catalog context and prior pages.
- `Coho` is interpreted as CO-HO based on prior pages.

## Page 4

Section type: priced order excerpt.

Matched rows:

- `27366` Pork Gyoza, package `10/21oz`, unit price `$33.00`, order quantity `1`, line total `$33.00`.
- `22130` WP SEAWEED SALAD, package `4/4.4`, unit price `$12.59`, order quantity `1`, line total `$12.59`.
- `25100` WP EDAMAME SOYBEANS, package `case`, unit price `$31.00`, no visible order quantity, line total `$0.00`.
- `27198` DENG GAO FOLDED BUN, package `10pc/Bag`, unit price `$5.00`, order quantity `22`, line total `$110.00`.
- TACOMA TOFU EXTRA FIRM, no visible code, package `12pc/case`, unit price `$12.84`, order quantity `4`, line total `$51.36`.
- `44488` Tonkotsu ramen base, package `35.27oz/Bag`, unit price `$15.12`, order quantity `3`, line total `$45.36`.
- `8745` Sesame ice cream, package `1.5gal`, unit price `$21.09`, no visible order quantity, line total `$0.00`.
- `17620` Mocha ice cream, package `1.5gal`, unit price `$21.09`, no visible order quantity, line total `$0.00`.

Import decisions:

- Products are assigned to `Unspecified Page 4 Supplier` because no vendor heading is visible.
- Historical order quantities and totals are kept in notes. They are not imported as default order quantities.

## Duplicates And Aliases

- `CO-HO` and `Coho`: one canonical vendor.
- `Southern Glazer's`, `Southern Glazer`, and `SG`: one canonical vendor.
- Southern Glazer's `Ming River` and CD `Ming River`: retained as separate vendor/product rows because both are visible.
- Prior placeholder `Page 20 Supplier`: replaced by `Unspecified Page 4 Supplier` with a legacy name for idempotent matching.

## Rows Excluded

- Blank quantity/date/header/total-only rows are excluded from product catalog import.
- Page 4 grand total `$252.31` is excluded from product import and documented here only.
- No OCR-only guesses were added.

## Open Questions

- Confirm the supplier for page 4 products.
- Confirm whether Southern Glazer's and SG should remain one canonical vendor operationally.
- Confirm whether CO-HO and Coho should remain one canonical vendor operationally.
- Confirm whether `Glove Black (S/M/L)` should become three managed SKUs when size-specific purchasing is required.
