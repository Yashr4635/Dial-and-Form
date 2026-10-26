# API Reference

Base path: /api/v1. Routes and access labels are derived from controller mappings and SecurityConfig. Request/response bodies are represented by DTOs in backend/src/main/java; this document does not invent JSON payloads.

Local Swagger UI is configured at http://localhost:8084/swagger-ui.html after backend startup. OpenAPI JSON is configured at /v3/api-docs. Public staging docs availability was not verified.

## Public catalog

| Method | Route | Access | Purpose / query parameters |
|---|---|---|---|
| GET | /watches | Public | Search/filter/page. Optional search, brand, collection, category, movement, material, minPrice, maxPrice; page default 0, size default 12 and capped at 50, sort default id,desc. |
| GET | /watches/featured | Public | Published featured watches. |
| GET | /watches/brands | Public | Distinct brands present in catalog. |
| GET | /watches/collections | Public | Public Rolex collections. |
| GET | /watches/materials | Public | Distinct case materials. |
| GET | /watches/movements | Public | Distinct movement types. |
| GET | /watches/compare?ids=… | Public | Compare watch IDs; controller describes a maximum of four. |
| GET | /watches/{slug} | Public | Watch details by slug. |
| GET | /categories | Public | Catalog categories. |

Example read-only request:

    GET https://dial-and-form-api.onrender.com/api/v1/watches?search=126613LB&page=0&size=12

Catalog endpoints use ApiResponse and PageResponse wrappers. API results were last checked on 2026-10-26.

## Account and identity

| Method | Route | Access | Purpose |
|---|---|---|---|
| POST | /auth/register | Public | Backend registration compatibility endpoint. Current browser signup uses Supabase OTP flow. |
| POST | /auth/login | Public | Backend login compatibility endpoint. |
| POST | /auth/logout | Public route pattern | Logout endpoint; verify behavior for selected identity flow. |
| GET | /users/me | Authenticated | Current local user profile. |

Browser Supabase SDK calls for OTP request, OTP verification, password setup and email/password login are provider calls, not API routes. Email delivery is unverified.

## Wishlist and addresses

| Method | Route | Access | Purpose |
|---|---|---|---|
| GET | /me/wishlist | Authenticated | Current user's saved watches. |
| PUT | /me/wishlist/{watchId} | Authenticated | Add a watch. |
| DELETE | /me/wishlist/{watchId} | Authenticated | Remove a watch. |
| GET | /me/addresses | Authenticated | List shipping addresses. |
| POST | /me/addresses | Authenticated | Create address; DTO AddressRequest. |
| DELETE | /me/addresses/{id} | Authenticated | Delete account address. |

## Cart and orders

| Method | Route | Access | Purpose |
|---|---|---|---|
| GET | /cart | Authenticated | Read current user's cart. |
| POST | /cart/items | Authenticated | Add item; DTO AddCartItemRequest. |
| PATCH | /cart/items/{itemId} | Authenticated | Update quantity; DTO UpdateCartItemRequest. |
| DELETE | /cart/items/{itemId} | Authenticated | Remove cart line. |
| POST | /cart/merge | Authenticated | Merge client cart; DTO CartMergeRequest. |
| GET | /checkout/quote?addressId={id} | Authenticated | Calculate checkout quote server-side. |
| POST | /checkout/orders | Authenticated | Initialize order awaiting payment authorization. |
| GET | /me/orders | Authenticated | List current user's orders. |
| GET | /me/orders/{orderRef} | Authenticated | Read order by public reference. |

Price and stock must be configured before a product is purchasable. Current imported product values are unavailable and should not proceed to purchase.

## Admin

Admin routes require ADMIN role through Spring Security and/or method-level checks.

| Method | Route | Purpose |
|---|---|---|
| GET | /admin/watches | List published, draft and archived watches. |
| POST | /admin/watches | Create watch; DTO WatchCreateRequest. |
| PATCH | /admin/watches/{id} | Update watch; DTO WatchUpdateRequest. |
| DELETE | /admin/watches/{id} | Archive watch. |
| GET | /admin/categories | List categories. |
| POST | /admin/categories | Create category. |
| PATCH | /admin/categories/{id} | Update category. |
| DELETE | /admin/categories/{id} | Delete category. |
| GET | /admin/orders | Paginated order listing. |
| PATCH | /admin/orders/{id}/status | Update order status. |
| PATCH | /admin/orders/{id}/shipment | Update shipment status. |

## Payment

These routes are demo-oriented, not evidence of a live gateway. Simulation routes return 503 when disabled. The webhook is unconfigured and returns service unavailable.

| Method | Route | Access | Purpose |
|---|---|---|---|
| POST | /payments/razorpay/create-order | Authenticated | Create demo payment record only when simulation is enabled; no gateway order or charge. |
| POST | /payments/razorpay/verify | Authenticated | Record simulated outcome only; no payment processed. |
| POST | /payments/webhook | Public route | Live gateway webhooks disabled; returns service unavailable. |

A development signature helper exists but is omitted from this route summary. Never treat simulation as successful payment.

## Health and API docs

- GET /actuator/health — health endpoint used by Render.
- /swagger-ui.html and /v3/api-docs — Springdoc paths configured in application.yml.
- The API security config permits Swagger and management paths; staging public docs availability was not checked.
