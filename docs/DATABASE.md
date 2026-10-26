# Database Design

## Engines and migrations

- Staging: Render-managed PostgreSQL 16 in Singapore per render.yaml.
- Development/tests: H2 in PostgreSQL compatibility mode.
- Persistence: Spring Data JPA/Hibernate.
- Schema management: Flyway V1–V6 under backend/src/main/resources/db/migration.
- Hibernate validates schema; migrations define schema evolution.

The live database was not queried in this audit. A user screenshot reported six migrations applied. Preservation tests use H2, not PostgreSQL 16.

## Main tables

| Table | Purpose |
|---|---|
| categories | Catalog category/collection grouping |
| watches | Product, specification, price/stock state and provenance |
| users | Local account identity and authorization; V6 adds optional Supabase identity |
| wishlist_items | Unique user/watch saved-item relation |
| shipping_addresses | Customer delivery addresses |
| carts | One cart per user |
| cart_items | Watch and quantity in a cart |
| orders | Order lifecycle and unique public order reference |
| order_items | Order items with historical product/price snapshots |
| payments | Payment status and references; one row per order |
| shipments | Shipment state; one row per order |

V1 creates catalog/user/wishlist foundations. V3 adds commerce tables and fields. V5 allows unavailable price/stock, adds specifications and image provenance, constrains published catalog to Rolex, archives prior published rows and imports 17 products. V6 adds nullable unique supabase_user_id.

## Relationships

~~~mermaid
erDiagram
    CATEGORIES ||--o{ WATCHES : classifies
    USERS ||--o{ WISHLIST_ITEMS : saves
    WATCHES ||--o{ WISHLIST_ITEMS : saved_as
    USERS ||--o{ SHIPPING_ADDRESSES : owns
    USERS ||--o| CARTS : owns
    CARTS ||--o{ CART_ITEMS : contains
    WATCHES ||--o{ CART_ITEMS : added_to
    USERS ||--o{ ORDERS : places
    ORDERS ||--o{ ORDER_ITEMS : includes
    WATCHES ||--o{ ORDER_ITEMS : referenced_by
    ORDERS ||--o| PAYMENTS : payment_record
    ORDERS ||--o| SHIPMENTS : shipment_record
~~~

A cart has a unique user_id. Payments and shipments each have unique order_id. Wishlist user/watch and cart/watch pairs have uniqueness constraints. Orders use unique order_reference. Order-item rows preserve product snapshots when product details later change.

## Integrity and checkout rules

- V5 requires published catalog rows to use brand Rolex.
- Price may be null or positive; stock may be null or nonnegative.
- V5 removes default stock so unknown availability is not fabricated.
- Checkout requires configured price and stock. Current imports remain unavailable for purchase.
- A unique index covers watch references and may fail if legacy rows duplicate a reference.
- Supabase identity is nullable and unique to allow gradual account linking.

## Archive and preservation

V5 archives previously published watches rather than deleting or recreating watch tables. Existing user IDs and commerce references are preserved. Order item snapshots remain. Some cart/wishlist/watch foreign keys use delete cascade, but V5 does not delete watches.

CatalogMigrationPreservationTest seeds a user, a non-Rolex watch, cart, wishlist, order/item, failed payment and shipment in H2 PostgreSQL mode, runs migrations and checks preservation. It is not a PostgreSQL 16 migration test.

## Safe migration procedure

1. Confirm database identity and Flyway version.
2. Take a backup and verify restoration.
3. Check for duplicate watch refs before V5.
4. Run exact migrations against disposable PostgreSQL 16 with representative legacy data.
5. Verify archived products, IDs, foreign keys, cart/wishlist, snapshots, payment and shipment.
6. Apply through Flyway only after explicit approval.
7. For failure use a reviewed forward correction or verified backup. Do not delete Flyway history or replay SQL manually.

No migration was run for documentation work.
