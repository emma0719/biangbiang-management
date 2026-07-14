# Front Order Catalog Import Report

Source: `docs/reference/Front Order List 前台订货.pdf`

Inspection date: 2026-06-20

## Import Summary

- PDF pages inspected: 4
- Vendors imported: 16
- Products imported: 123
- Location: `SEATTLE`
- Source keys: included for every imported vendor and product
- Importer behavior: idempotent create/backfill. It creates missing PDF rows, assigns source keys, and preserves existing source-managed fields that have already been edited after import.

## Confirmed Vendor Decisions

- `CO-HO` is the canonical vendor for PDF labels `CO-HO` and `Coho`.
- `Southern Glazer's` is the canonical vendor for PDF labels `Southern Glazer's`, `Southern Glazer`, and `SG`.
- `No Delivery` is an explicit PDF section on page 1, not an unknown supplier.
- `Unspecified Page 4 Supplier` replaces the old placeholder `Page 20 Supplier`; page 4 has no visible vendor heading.

## Confirmed Page 4 Product Metadata

- `27366` Pork Gyoza, `10/21oz`, unit price `$33.00`, order quantity `1`, total `$33.00`.
- `22130` WP SEAWEED SALAD, `4/4.4`, unit price `$12.59`, order quantity `1`, total `$12.59`.
- `25100` WP EDAMAME SOYBEANS, `case`, unit price `$31.00`, no visible order quantity, total `$0.00`.
- `27198` DENG GAO FOLDED BUN, `10pc/Bag`, unit price `$5.00`, order quantity `22`, total `$110.00`.
- TACOMA TOFU EXTRA FIRM, no visible code, `12pc/case`, unit price `$12.84`, order quantity `4`, total `$51.36`.
- `44488` Tonkotsu ramen base, `35.27oz/Bag`, unit price `$15.12`, order quantity `3`, total `$45.36`.
- `8745` Sesame ice cream, `1.5gal`, unit price `$21.09`, no visible order quantity, total `$0.00`.
- `17620` Mocha ice cream, `1.5gal`, unit price `$21.09`, no visible order quantity, total `$0.00`.

## Ambiguities Kept Out Of Structured Fields

- Page 4 supplier name is not visible.
- Page 3 unit columns are present but blank for every visible row.
- Page 1 `Coke / Diet Coke` is one order cell while page 2 separates the inventory rows.
- `Glove Black (S/M/L)` is one visible row; no separate size products were invented.
- Historical order quantities from page 4 are retained in notes, not default order quantities.

## Corrected From Previous Placeholder Import

- The catalog source status no longer says the PDF was absent.
- The catalog no longer refers to a 20-page PDF.
- `Page 20 Supplier` is retained only as a legacy name for idempotent matching.
- PDF-visible spelling is preserved, with prior normalized spellings recorded as `legacyNames` where needed.
