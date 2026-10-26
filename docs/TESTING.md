# Testing and Quality

## Automated checks

GitHub Actions workflow .github/workflows/ci.yml runs for pushes and pull requests to main.

| Check | Behavior |
|---|---|
| JavaScript syntax | node --check on app.js, js/api.js, js/ui.js and js/storefront.js with Node 20 |
| Animation assets | Confirms exactly 240 WebP frames are present |
| Backend tests | Maven Wrapper clean test with Java 21 |
| Python syntax | Not in checked-in CI; run python3 -m py_compile server.py locally |

Backend test classes cover Render database URL parsing, commerce flow, payment safety, migration preservation and watch catalog behavior. Tests use Spring Boot Test, Spring Security Test, JUnit and H2. No browser E2E, accessibility, or performance suite was found.

## Latest recorded local results

Executed 2026-10-26:

- JavaScript syntax checks: PASS for four files.
- Python syntax compilation: PASS.
- Maven: sh ./mvnw -Dmaven.repo.local=/private/tmp/dial-form-m2 clean test -B, from backend.
- Result: BUILD SUCCESS; 17 tests, 0 failures, 0 errors, 0 skipped; Java 21.0.12.1; H2 test database.
- Frame sequence: 240 continuous files, frame_0001.webp through frame_0240.webp.
- Candidate mapping: 17/17 image hashes matched V5 hashes. This verifies file bytes, not a fresh visual comparison to original PDFs.

The first Maven attempt without isolated repository stopped before tests due local Maven lock state. The retry above succeeded. The command included clean, which removed ignored backend/target outputs before Maven recreated test/build output. Tracked source was not changed. Flyway gave a nonfatal warning that H2 2.2.224 was newer than its tested version.

## Read-only staging checks

On 2026-10-26 around 23:50 IST:

- Storefront homepage, login, watches, one detail route and collections returned HTTP 200 HTML shell.
- API health returned HTTP 200 / UP.
- API and storefront proxy catalog returned 17 Rolex rows with price and stock null.
- Datejust detail returned source m126334-0033.pdf and purchasable false.
- Search 126613LB and collection submariner each returned one result.
- Sort/pagination returned two distinct four-item pages.
- Public frame and placeholder returned HTTP 200; private-review path returned 404.
- Three homepage filter values returned empty results.
- A first cold API/proxy attempt timed out/returned 502; warm retries succeeded.

HTTP checks do not validate browser hydration, authentication, writes, email, payment or complete commerce.

## Recommended acceptance coverage

- Browser route hydration, direct refresh, back/forward and responsive behavior.
- Homepage collection links returning intended non-empty data.
- Supabase OTP delivery, verification, password setup and password sign-in.
- CORS from actual storefront origin.
- Authenticated cart, wishlist, addresses and order snapshots.
- Unpriced/out-of-stock checkout rejection.
- Isolated PostgreSQL 16 migration preservation.
- Reduced-motion and animation forward/reverse visual checks.
- No simulated payment presented as a real paid transaction.

No application tests were run specifically for this documentation change.
