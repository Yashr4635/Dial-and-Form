# Dial & Form

**A premium watch storefront prototype with a Rolex-focused catalog, a scroll-driven visual sequence, and a full-stack commerce API.**

[![Java 21](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 3.3.4](https://img.shields.io/badge/Spring%20Boot-3.3.4-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL 16](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![GitHub Actions](https://github.com/Yashr4635/Dial-and-Form/actions/workflows/ci.yml/badge.svg)](https://github.com/Yashr4635/Dial-and-Form/actions/workflows/ci.yml)

**Staging demo:** [dial-and-form-storefront.onrender.com](https://dial-and-form-storefront.onrender.com) · last checked 26 October 2026
**Source:** [github.com/Yashr4635/Dial-and-Form](https://github.com/Yashr4635/Dial-and-Form)
**Status:** Deployed staging prototype. It is not a production-ready store or an authorized Rolex retailer.

The hosted URLs were last checked on 2026-10-26. The homepage collection links and sign-in messaging have since been updated in the source; recheck staging to confirm the latest deployment.

## Contents

- [Overview](#overview)
- [Demo and links](#demo-and-links)
- [Features and current limits](#features-and-current-limits)
- [Technology stack](#technology-stack)
- [Architecture](#architecture)
- [User workflows](#user-workflows)
- [Repository structure](#repository-structure)
- [Getting started](#getting-started)
- [Environment configuration](#environment-configuration)
- [API documentation](#api-documentation)
- [Database](#database)
- [Authentication and security](#authentication-and-security)
- [Testing](#testing)
- [Deployment](#deployment)
- [Screenshots](#screenshots)
- [Roadmap](#roadmap)
- [Engineering notes](#engineering-notes)
- [License and contact](#license-and-contact)

## Overview

Dial & Form is a watch storefront prototype for people who want to browse premium watches, review reference-level specifications, and use account-based shopping features. Its homepage preserves a 240-frame scroll-controlled watch sequence. The catalog is seeded with 17 Rolex references and supports search, collection filtering, sorting, pagination, and product details.

The project pairs a native HTML/CSS/JavaScript storefront with a Python static server and proxy, a Java Spring Boot REST API, PostgreSQL persistence, and Supabase Auth integration. Price and stock are intentionally unavailable until verified. Catalog items therefore cannot be purchased. Brochure-derived product images remain private while redistribution rights are unresolved.

## Demo and links

| Resource | Link | Notes |
|---|---|---|
| Storefront | [Open staging site](https://dial-and-form-storefront.onrender.com) | Render staging; last read-only checked 2026-10-26 |
| API health | [API health endpoint](https://dial-and-form-api.onrender.com/actuator/health) | HTTP 200 / UP on 2026-10-26; health endpoint only |
| Source repository | [GitHub repository](https://github.com/Yashr4635/Dial-and-Form) | Remote verified from local Git configuration |
| API reference | Local Swagger UI at http://localhost:8084/swagger-ui.html after API startup | Public staging Swagger availability was not verified |
| Architecture | [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | System, request, auth, deployment and entity diagrams |
| API routes | [docs/API.md](docs/API.md) | Methods, access policy and DTO references |
| Current status | [docs/PROJECT_STATUS.md](docs/PROJECT_STATUS.md) | Verified and unverified capabilities |

Staging availability can change, and an HTTP response does not prove the full user journey is ready.

## Features and current limits

### Implemented

- 240 sequential WebP frames rendered on canvas in response to scroll progress, including reverse-scroll reassembly.
- Reduced-motion handling, responsive canvas resizing, and bounded nearby-frame loading in the existing animation code.
- Rolex-focused catalog with 17 verified references in the backend.
- Catalog API support for search, collection/category, movement/material/price filters, sorting and pagination.
- Product detail records with source brochure provenance.
- Supabase email OTP verification and password setup flow in frontend code.
- Backend identity mapping and APIs for cart, wishlist, addresses, orders, payment status and shipments.
- Price and stock validation that prevents products without configured values from entering purchasable checkout.

### Known limits

- OTP email delivery has not passed a fresh end-to-end test; prior user screenshots showed delivery errors.
- The prior homepage used three category filters that returned no products. The October 27 source update points those tiles to explicit Submariner, Cosmograph Daytona, and GMT-Master II collection routes; this navigation still needs browser verification in staging.
- Full mobile/browser behavior and authenticated shopping journeys have not been independently verified.
- No current verified selling prices or stock quantities are configured.
- No live payment processor, real transaction, or shipping carrier is configured. Payment simulation is not a real payment.
- Brochure images are private review candidates and are not cleared for public redistribution.
- Homepage animation target is Sky-Dweller reference 326935-0005. Catalog reference 336935 is different and is not a replacement.
- No separate animated watch-hand overlay is implemented.

## Technology stack

| Layer | Technology | Purpose |
|---|---|---|
| Frontend | HTML, CSS, native JavaScript, Canvas | Storefront and scroll-driven animation |
| Local server | Python standard library | Static file server, route fallback and API proxy |
| Backend | Java 21, Spring Boot 3.3.4 | REST API, validation, security, persistence and health |
| Data access | Spring Data JPA / Hibernate | Relational persistence |
| Database | PostgreSQL 16; H2 for dev/tests | Staging and local/test database |
| Migrations | Flyway | Versioned schema and catalog migrations V1–V6 |
| Authentication | Supabase Auth JavaScript SDK v2 | Email OTP and password flow; exact patch version is not pinned |
| Legacy auth support | Spring Security and JJWT 0.12.6 | API token authentication and compatibility paths |
| API description | Springdoc OpenAPI 2.6.0 | OpenAPI document and local Swagger UI |
| Build/test | Maven Wrapper 3.10.0, JUnit, Spring Boot Test | Backend build and tests |
| Runtime checks | Node.js 20 in CI | JavaScript syntax and animation asset-count checks |
| Containerization | Docker, Docker Compose | API image and optional local container setup |
| Hosting | Render | Staging storefront, API and PostgreSQL |
| CI | GitHub Actions | JavaScript syntax/frame checks and Maven tests |

No AI/ML model, analytics product, frontend framework/bundler, direct Resend API client, or real payment gateway was found in the repository. Resend can be configured as SMTP transport through Supabase, but the application does not call its API.

## Architecture

The browser loads the storefront and uses Supabase Auth for identity. Storefront API requests go to the Python service, which proxies them to Spring Boot. The API uses JPA and Flyway-managed PostgreSQL tables.

~~~mermaid
flowchart LR
    User[Visitor or signed-in customer] --> Browser[Browser storefront]
    Browser <-->|HTTP pages and same-origin API requests| Store[Render Python storefront and proxy]
    Store -->|HTTP REST| API[Render Spring Boot API]
    API -->|JPA / Flyway| DB[(Render PostgreSQL 16)]
    Browser -->|Supabase Auth SDK| Auth[Supabase Auth]
    API -->|verify provider identity| Auth
~~~

Detailed request, authentication, deployment and database diagrams are in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## User workflows

### Browse the catalog

1. The browser loads the storefront shell from the Python service.
2. The frontend requests catalog data through the storefront API proxy.
3. Spring Boot applies search/filter/sort/page parameters and queries PostgreSQL.
4. The API returns a response envelope and page data; frontend code renders product cards/details.

### Create an account and sign in

1. The browser Supabase client requests an email OTP.
2. The user verifies the code and sets a password.
3. The user signs in using email and password.
4. The frontend sends a Supabase bearer token on protected API requests; the backend verifies provider identity and maps it to a local user.

The code contains this flow, but email delivery remains unverified. See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) and [docs/PROJECT_STATUS.md](docs/PROJECT_STATUS.md).

### Cart and checkout

Authenticated API routes support cart, addresses, quotes and order creation. The backend validates price/stock and creates an order awaiting payment. Current catalog prices and stock are null, so checkout cannot create a purchasable order. Development-only payment simulation does not process funds.

## Repository structure

    .
    ├── index.html                 Storefront shell
    ├── styles.css                 Homepage styling
    ├── storefront.css             Storefront page styling
    ├── app.js                     Original 240-frame scroll animation
    ├── js/                        API client and storefront modules
    ├── assets/frames/             Original 240 WebP frames
    ├── assets/private-review/     Ignored, restricted brochure candidates
    ├── server.py                  Python static server and API proxy
    ├── backend/
    │   ├── src/main/java/         Spring controllers, services, entities and config
    │   ├── src/main/resources/    Runtime profiles and Flyway migrations
    │   ├── src/test/java/         Backend tests
    │   ├── Dockerfile             API container build
    │   └── mvnw                   Maven wrapper
    ├── docs/                      Architecture, API and runbooks
    ├── render.yaml                Render staging Blueprint
    ├── docker-compose.yml         Optional local containers; inspect env setup first
    ├── .env.example               Variable-name template; values not reproduced here
    └── .github/workflows/ci.yml   CI checks

The private-review directory is git-ignored and must not be copied into public assets without rights approval.

## Getting started

### Prerequisites

- Java 21
- Python 3
- Git
- Node.js 20 only if running the same JavaScript syntax checks as CI
- Docker Compose only if using optional containers

### Clone the repository

    git clone https://github.com/Yashr4635/Dial-and-Form.git
    cd Dial-and-Form

### 1. Start the API in development mode

The default Spring profile is dev and configures an in-memory H2 database with Flyway. From the repository root:

    cd backend
    SERVER_PORT=8084 sh ./mvnw spring-boot:run

The API listens at http://localhost:8084. Local API docs are configured at http://localhost:8084/swagger-ui.html; health is at http://localhost:8084/actuator/health.

### 2. Start the storefront

Open a second terminal from the repository root:

    python3 server.py --port 8004 --backend-port 8084

Then open http://localhost:8004. The Python service serves direct storefront routes and proxies API calls to the backend.

### 3. Configure Supabase for auth

For the Supabase email flow, set SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY in the environment of both the backend and storefront processes before starting them. The storefront injects public client settings into its page; the backend uses them to validate/link identities. Configure email and redirect behavior in Supabase Auth. Never put a service-role key in the browser or commit secrets.

Local catalog browsing can run independently of successful email delivery; authentication requires valid Supabase configuration.

### Build and tests

From repository root:

    cd backend
    sh ./mvnw package

Run tests without packaging using:

    sh ./mvnw test -B

Frontend/Python syntax checks from repository root:

    node --check app.js
    node --check js/api.js
    node --check js/ui.js
    node --check js/storefront.js
    python3 -m py_compile server.py

See [docs/TESTING.md](docs/TESTING.md) for recorded results and limitations.

## Environment configuration

Set values only in an ignored local environment file or secure process environment. This table documents names, not values.

| Variable | Purpose | Required when |
|---|---|---|
| SUPABASE_URL | Supabase project endpoint | Supabase auth; storefront and API |
| SUPABASE_PUBLISHABLE_KEY | Browser-safe Supabase client key | Supabase auth; storefront and API |
| FRONTEND_URL | Allowed frontend origin / API CORS | Deployed API |
| BACKEND_URL | API origin used by storefront proxy | Deployed storefront |
| BACKEND_HOSTPORT | Local backend host:port option for server.py | Optional local proxy |
| BACKEND_PORT | Local backend port option | Optional local proxy |
| PORT | Bind port for Render/runtime process | Hosting runtime |
| HOST | Bind host for Python storefront | Optional; Render binds 0.0.0.0 |
| SERVER_PORT | Spring Boot port override | Optional local API |
| SPRING_PROFILES_ACTIVE | Selects Spring profile; Render uses prod | Deployment |
| DATABASE_URL | Render-provided database URL parsed by backend | Render API |
| SPRING_DATASOURCE_URL | JDBC database URL | Compose or explicit Spring configuration |
| SPRING_DATASOURCE_USERNAME | Database username | PostgreSQL deployment |
| SPRING_DATASOURCE_PASSWORD | Database password | PostgreSQL deployment |
| JWT_SECRET | Backend token signing configuration | Configured API profile |
| JWT_EXPIRATION_MS | Present in .env.example; runtime usage is not wired as an override | Verify before relying on it |

Do not share keys, tokens, passwords or database URLs in issues or screenshots.

## API documentation

Springdoc/OpenAPI is configured at /v3/api-docs and /swagger-ui.html. The local Swagger UI is configured above. Public staging docs availability was not checked. Route details are in [docs/API.md](docs/API.md).

## Database

PostgreSQL is used for Render staging. Local dev and test profiles use H2 in PostgreSQL compatibility mode. Flyway migrations V1–V6 define schema and data. Main entities include categories, watches, users, wishlists, addresses, carts, orders, order-item snapshots, payments and shipments.

V5 archives prior published catalog rows and imports Rolex records without deleting old watch rows. V6 adds an optional Supabase identity link. The migration-preservation test uses H2, not PostgreSQL 16. Review [docs/DATABASE.md](docs/DATABASE.md) before a persistent migration.

## Authentication and security

Supabase Auth handles browser email OTP/password authentication. Spring Security protects customer and admin API operations and applies role checks to admin routes. Legacy API login/register routes remain. The API is stateless. Browser configuration includes only the Supabase publishable key; a service-role key must never be exposed.

The storefront gates `/watches`, `/collections`, and `/brands` routes behind a verified sign-in and preserves the requested route for return after login. This is a frontend route gate: the catalog API itself permits anonymous reads. Decide whether that API policy is acceptable for the project. OTP delivery, CORS from the deployed browser, and the full account lifecycle remain unverified.

## Testing

The repository has Spring Boot/JUnit tests for commerce flows, payment safety, PostgreSQL URL parsing, catalog filtering and migration preservation. GitHub Actions checks JavaScript syntax, confirms 240 animation frames exist, and runs Maven tests with Java 21. The last recorded local run on 2026-10-26 passed 17 tests with zero failures/errors/skips. This is not browser E2E coverage or proof of production readiness.

## Deployment

Render is configured for staging:

- Storefront: https://dial-and-form-storefront.onrender.com
- API: https://dial-and-form-api.onrender.com
- PostgreSQL 16 staging database, Singapore region
- GitHub Actions checks main pushes and pull requests; Render deploy is configured from main after checks pass

No production environment was found. Local edits do not update the hosted site; a change must be committed and pushed to the configured branch for auto-deploy. See [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md).

## Screenshots

No approved UI screenshots are currently stored in the repository. The 240 animation frames are application assets, not screenshots. Product brochure candidates are private. See [docs/assets/README.md](docs/assets/README.md) for screenshots to capture and their intended use.

## Roadmap

1. Browser-test collection navigation and the sign-in return flow in staging after the latest source changes deploy.
2. Verify Supabase OTP delivery and the complete password/login flow.
3. Test direct routes, back/forward navigation, responsive layouts, reduced motion and the original scroll animation in a browser.
4. Validate V5/V6 against an isolated PostgreSQL 16 database with representative historical data.
5. Clear image usage rights or replace candidates with approved images.
6. Configure verified prices, inventory, tax, delivery, return and warranty data.
7. Plan a real payment provider and production operations only after business details are ready.

## Engineering notes

The storefront remains framework-free. The API uses a relational domain model and versioned migrations. Product migration archives old published records, and the order model retains item snapshots. The UI and API keep products unpurchasable when price or stock is unknown. The original animation was preserved rather than overlaid with unverified watch hands.

## License and contact

No LICENSE file was found. The repository should not be described as MIT-licensed or as granting unrestricted reuse until a license is selected and added.

The GitHub repository owner is [Yashr4635](https://github.com/Yashr4635). No LinkedIn profile URL or public contact address was verified. Proposed LinkedIn wording is in [docs/LINKEDIN_PROJECT_UPDATE.md](docs/LINKEDIN_PROJECT_UPDATE.md).
