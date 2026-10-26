# Deployment Guide

The repository configures Render staging. No production environment is verified. This is an operational reference, not authorization to deploy or migrate.

## Render resources

| Resource | Region / plan | Build and start | Health |
|---|---|---|---|
| dial-and-form-storefront | Singapore / free in render.yaml | Build: python3 -m py_compile server.py; start: python3 server.py --host 0.0.0.0 --port $PORT | /healthz |
| dial-and-form-api | Singapore / free in render.yaml | Dockerfile under backend/ | /actuator/health |
| dial-and-form-staging-db | PostgreSQL 16 / free in render.yaml | Managed Render PostgreSQL | Private network |

- Storefront: https://dial-and-form-storefront.onrender.com
- API: https://dial-and-form-api.onrender.com
- Repository: https://github.com/Yashr4635/Dial-and-Form
- Branch: main; Render auto-deploy uses checksPass.
- Last independent read-only URL check: 2026-10-26.
- No production service, custom domain or payment gateway is verified.

## Configuration variable names

| Variable | Service | Purpose/source |
|---|---|---|
| PORT | Render services | Runtime bind port |
| HOST | Storefront | Bind host; Render uses 0.0.0.0 |
| BACKEND_URL | Storefront | API Render URL referenced from API service |
| SPRING_PROFILES_ACTIVE | API | Render sets prod |
| DATABASE_URL | API | Render database connection reference |
| JWT_SECRET | API | Render-generated signing secret |
| FRONTEND_URL | API | Storefront origin for API CORS |
| SUPABASE_URL | Storefront and API | Supabase project URL |
| SUPABASE_PUBLISHABLE_KEY | Storefront and API | Browser-safe Supabase client key |

Values are omitted. Never provide a Supabase service-role key to browser code. Render dashboard values were not inspected during the last audit. Storefront HTML config fields were previously observed as nonempty without printing their values.

Local configuration also references SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD, SERVER_PORT, BACKEND_HOSTPORT and BACKEND_PORT. JWT_EXPIRATION_MS appears in .env.example but is not wired as a runtime override.

## Local development

Requirements: Java 21 and Python 3. Node 20 is used by CI for syntax checks.

From repository root:

    cd backend
    SERVER_PORT=8084 sh ./mvnw spring-boot:run

In a second terminal from repository root:

    python3 server.py --port 8004 --backend-port 8084

Open http://localhost:8004. Backend is at http://localhost:8084. The default dev profile uses H2 and Flyway. Supabase auth requires SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY for both processes and email/redirect configuration in Supabase. Do not put secrets in source or commit them.

## Build, test and CI

Backend:

    cd backend
    sh ./mvnw test -B

Frontend and Python:

    node --check app.js
    node --check js/api.js
    node --check js/ui.js
    node --check js/storefront.js
    python3 -m py_compile server.py

CI in .github/workflows/ci.yml checks JS syntax, exactly 240 frame files, and backend Maven tests using Java 21. Render deploys main after checks pass. Last recorded local test run passed 17 tests on 2026-10-26; see TESTING.md.

## Release procedure

1. Check branch, commit and status; preserve local changes.
2. Review changes and ensure private-review images, source PDFs, env files and secrets are excluded.
3. Run relevant tests and inspect GitHub Actions results.
4. Push only an authorized change to main; Render auto-deploys after checks.
5. Review Render build/runtime logs and health.
6. Verify API catalog, storefront proxy and browser rendering.
7. Do not run persistent migrations or change plans without approval.

No deployment was performed for the documentation update.

## Migration and recovery

Flyway V1–V6 run with the backend. V5 archives old published product rows and imports the verified Rolex catalog; V6 adds nullable Supabase identity mapping. V5's unique reference index can fail if existing data contains duplicate refs. Preservation tests use H2 compatibility, not PostgreSQL 16.

Before a persistent staging migration, confirm database identity, take and test backup, inspect Flyway history and duplicate references, and validate exact migrations against disposable PostgreSQL 16 with representative customer/commerce data. Use Flyway sequencing only. Do not manually replay SQL, delete Flyway history, or assume application rollback reverses schema. Production migration requires explicit approval and a tested restore plan.

## Known deployment limitations

The first cold API/proxy requests in the prior audit timed out/returned 502; later warm checks succeeded. Render config is free tier and the dashboard showed spin-down notice. No production monitoring, backup/restore, payment, or shipping provider was verified. Do not change plan without approval.

Brochure candidates remain under assets/private-review and are not served publicly. Keep private until rights are documented.
