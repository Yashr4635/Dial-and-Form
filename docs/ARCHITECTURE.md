# Architecture

This describes components found in current source and Render Blueprint. Staging URLs were last checked 2026-10-26. No production environment was found.

## Components and trust boundaries

- **Browser:** HTML/CSS/JavaScript storefront, canvas animation, Supabase Auth client and API calls.
- **Python storefront:** allowlisted static files, route fallback, public Supabase client config injection and API proxy.
- **Spring Boot API:** request validation, Spring Security, controllers/services/repositories, Supabase identity mapping and PostgreSQL operations.
- **Supabase Auth:** external identity and email OTP/password provider. Delivery settings are outside this repo.
- **PostgreSQL:** Render staging data store, schema managed by Flyway.
- **GitHub Actions/Render:** CI checks source; Render deploys configured services from main after checks pass.

The Supabase publishable key is browser client configuration, not a service-role secret. Protected API requests are checked by backend auth. The Render database connection is private in the Blueprint. There is no browser-to-database connection.

## High-level architecture

~~~mermaid
flowchart LR
    User[Visitor or signed-in customer] --> Browser[Browser storefront]
    Browser <-->|HTTP pages and same-origin API requests| Storefront[Render Python storefront and proxy]
    Browser -->|Supabase Auth SDK| Supabase[Supabase Auth]
    Storefront -->|HTTP REST| API[Render Spring Boot API]
    API -->|JPA and Flyway| DB[(Render PostgreSQL 16)]
    API -->|Provider identity verification| Supabase
~~~

## Catalog request lifecycle

~~~mermaid
sequenceDiagram
    actor Visitor
    participant Browser as Storefront JavaScript
    participant Proxy as Python server
    participant API as Spring Boot API
    participant DB as PostgreSQL
    Visitor->>Browser: Search or filter catalog
    Browser->>Proxy: GET /api/v1/watches with query parameters
    Proxy->>API: Forward request
    API->>DB: Query published catalog page
    DB-->>API: Matching rows and page metadata
    API-->>Proxy: API response envelope
    Proxy-->>Browser: Same-origin response
    Browser-->>Visitor: Render cards or empty/error state
~~~

## Authentication lifecycle

The browser flow uses Supabase Auth. The API also retains backend login/register endpoints for compatibility and development. Email delivery has not been freshly verified.

~~~mermaid
sequenceDiagram
    actor Customer
    participant UI as Browser
    participant Auth as Supabase Auth
    participant API as Spring Boot API
    participant DB as PostgreSQL
    Customer->>UI: Submit email for verification
    UI->>Auth: Request signup OTP
    Auth-->>Customer: Send email code
    Customer->>UI: Enter code
    UI->>Auth: Verify OTP
    Auth-->>UI: Verified session
    Customer->>UI: Create password
    UI->>Auth: Update account password
    UI->>Auth: Sign out, then sign in with email/password
    Auth-->>UI: Authenticated session token
    UI->>API: Protected request with bearer token
    API->>Auth: Verify provider identity
    API->>DB: Link/read local user and process request
    DB-->>API: Result
    API-->>UI: API response
~~~

Current code implements OTP request, verification, password setup and password sign-in. Email delivery and full E2E behavior remain unverified.

## Deployment architecture

~~~mermaid
flowchart TD
    Repo[GitHub main branch] --> CI[GitHub Actions]
    CI -->|checks pass| Render[Render Blueprint]
    Render --> Store[Python storefront service]
    Render --> API[Docker Spring Boot service]
    Render --> DB[(PostgreSQL 16 staging database)]
    Store -->|BACKEND_URL| API
    API -->|private database connection| DB
    Store -->|configured project URL and publishable key| Supabase[Supabase Auth]
    API -->|configured provider validation| Supabase
~~~

## Data relationships

~~~mermaid
erDiagram
    CATEGORIES ||--o{ WATCHES : classifies
    USERS ||--o{ WISHLIST_ITEMS : saves
    WATCHES ||--o{ WISHLIST_ITEMS : appears_in
    USERS ||--o{ SHIPPING_ADDRESSES : owns
    USERS ||--o| CARTS : has
    CARTS ||--o{ CART_ITEMS : contains
    WATCHES ||--o{ CART_ITEMS : selected
    USERS ||--o{ ORDERS : places
    ORDERS ||--o{ ORDER_ITEMS : snapshots
    WATCHES ||--o{ ORDER_ITEMS : referenced_by
    ORDERS ||--o| PAYMENTS : has
    ORDERS ||--o| SHIPMENTS : has
~~~

A cart is unique per user. Payment and shipment each have a unique order_id. Order items store product snapshots. Foreign-key deletion behavior differs by relation; do not assume all relations cascade. See [DATABASE.md](DATABASE.md).

## Runtime sequence

1. GitHub Actions runs JavaScript syntax/frame checks and Maven tests for pushes and pull requests to main.
2. Render is configured to deploy main after checks pass.
3. Render runs the Python storefront, Spring Boot API and PostgreSQL in Singapore.
4. Storefront proxies API calls using BACKEND_URL.
5. Supabase Auth handles browser identity and the backend verifies provider identity for protected API requests.

Springdoc paths /swagger-ui.html and /v3/api-docs are configured for local use. Public staging Swagger availability was not verified. This is staging, not production.
