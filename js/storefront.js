/* Dedicated storefront routes for the existing Vanilla JS application. */
(() => {
    const app = document.getElementById("route-app");
    const home = document.querySelector("body > main");
    const api = window.atelierApi;
    const watchCache = new Map();
    const placeholderImage = "/assets/catalog/placeholder-watch.svg";
    let privateReviewImageMap = {};
    const privateReviewImagesReady = fetch("/assets/private-review/image-map.json", { cache: "no-store" })
        .then(response => response.ok ? response.json() : {})
        .then(value => { privateReviewImageMap = value && typeof value === "object" ? value : {}; })
        .catch(() => {});
    let renderVersion = 0;

    const esc = (value = "") => String(value).replace(/[&<>"']/g, char => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[char]));
    const slugify = value => String(value || "").toLowerCase().trim().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
    const money = (value, currency = "USD") => {
        if (value == null || value === "") return "Price on request";
        try { return new Intl.NumberFormat("en-US", { style: "currency", currency: currency || "USD", maximumFractionDigits: 0 }).format(Number(value || 0)); }
        catch { return `${currency || "USD"} ${Number(value || 0).toLocaleString()}`; }
    };
    const productImage = watch => privateReviewImageMap[watch?.referenceNumber]
        || (String(watch?.imageUrl || "").startsWith("catalog:") ? placeholderImage : (watch?.imageUrl || placeholderImage));
    const pathNow = () => window.location.pathname.replace(/\/+$/, "") || "/";
    const queryNow = () => new URLSearchParams(window.location.search);
    const internal = href => href.startsWith("/") && !href.startsWith("//");

    function toast(message, kind = "info") {
        const container = document.getElementById("toast-container");
        if (!container) return;
        const node = document.createElement("div");
        node.className = `toast toast-${kind}`;
        node.textContent = message;
        container.appendChild(node);
        requestAnimationFrame(() => node.classList.add("is-visible"));
        setTimeout(() => node.remove(), 4200);
    }

    function navigate(href, replace = false) {
        if (replace) history.replaceState({}, "", href);
        else history.pushState({}, "", href);
        window.scrollTo(0, 0);
        renderRoute();
    }
    window.atelierNavigate = navigate;

    function shell(content, active = "") {
        const user = api.user;
        return `
          <div class="route-site">
            <header class="route-header">
              <a class="route-wordmark" href="/" aria-label="Dial & Form home">DIAL <span>&amp; FORM</span></a>
              <nav class="route-nav" aria-label="Store navigation">
                <a href="/cart" ${active === "cart" ? 'aria-current="page"' : ""}>Bag <span class="route-cart-count" data-cart-count>0</span></a>
                <a class="route-account-link" href="${user ? "/account" : "/login"}" id="route-auth-link">${user ? esc(user.displayName.split(" ")[0]) : "Sign in"}</a>
              </nav>
            </header>
            ${content}
            <footer class="route-footer">
              <a class="route-wordmark" href="/">DIAL <span>&amp; FORM</span></a>
              <p>An independent boutique for exploring fine watches.</p>
              <div class="route-footer-links"><a href="/watches">Watches</a><a href="/collections">Collections</a><a href="/account">Account</a><a href="mailto:hello@dialandform.example">Contact</a></div>
              <small>Dial &amp; Form is an independent storefront and is not affiliated with or authorized by Rolex. © 2026.</small>
            </footer>
          </div>`;
    }

    function pageHeading(eyebrow, title, description = "", crumb = "") {
        return `<div class="route-page-heading">${crumb ? `<p class="route-breadcrumb"><a href="/">Home</a><span>/</span>${esc(crumb)}</p>` : ""}<p class="eyebrow dark-eyebrow">${esc(eyebrow)}</p><h1>${title}</h1>${description ? `<p class="route-lede">${esc(description)}</p>` : ""}</div>`;
    }

    function statePanel(title, detail, kind = "info") {
        return `<div class="route-state route-state-${kind}" role="status"><strong>${esc(title)}</strong><p>${esc(detail)}</p></div>`;
    }

    function remember(watch) {
        if (watch && watch.id != null) watchCache.set(String(watch.id), watch);
        return watch;
    }

    function productCard(watch, options = {}) {
        remember(watch);
        const purchasable = watch.purchasable === true;
        const availability = watch.price == null
            ? "Price on request · checkout unavailable"
            : watch.stockQuantity == null
                ? "Availability not configured"
                : Number(watch.stockQuantity) <= 0
                    ? "Currently unavailable"
                    : `${Number(watch.stockQuantity)} available`;
        const was = watch.price != null && Number(watch.retailPrice) > Number(watch.price) ? `<del>${money(watch.retailPrice, watch.currency)}</del>` : "";
        return `<article class="watch-card ${options.compact ? "watch-card-compact" : ""}">
          <a class="watch-card-image" href="/watches/${encodeURIComponent(watch.slug)}" aria-label="View ${esc(watch.brand)} ${esc(watch.name)}">
            <img src="${esc(productImage(watch))}" onerror="this.onerror=null;this.src='${placeholderImage}'" alt="${esc(watch.brand)} ${esc(watch.name)}" loading="lazy" />
            <span class="watch-card-image-index">${esc(watch.collectionName || watch.category?.name || "ROLEX")}</span>
          </a>
          <div class="watch-card-copy"><span class="watch-card-brand">${esc(watch.brand)}</span><h2><a href="/watches/${encodeURIComponent(watch.slug)}">${esc(watch.name)}</a></h2>
            <p class="watch-card-spec">${esc(watch.movementType || "Mechanical")} <span>·</span> ${esc(watch.caseDiameterText || (watch.caseDiameterMm ? `${watch.caseDiameterMm} mm` : ""))} <span>·</span> ${esc(watch.caseMaterial || "")}</p>
            <p class="watch-card-reference">Reference ${esc(watch.referenceNumber || "—")}</p>
            <div class="watch-card-price">${money(watch.price, watch.currency)} ${was}</div>
            <p class="watch-availability ${purchasable ? "" : "is-unavailable"}">${esc(availability)}</p>
            <div class="watch-card-actions">
              <button type="button" class="button button-dark js-add-cart" data-watch-id="${esc(watch.id)}" data-watch-slug="${esc(watch.slug)}" ${purchasable ? "" : "disabled"}>${purchasable ? "Add to bag" : "Price on request"}</button>
              <button type="button" class="icon-button js-toggle-wishlist" data-watch-id="${esc(watch.id)}" data-watch-slug="${esc(watch.slug)}" aria-label="Save ${esc(watch.name)} to wishlist">♡</button>
            </div>
          </div>
        </article>`;
    }

    function breadcrumbBack(label, href) {
        return `<p class="route-breadcrumb"><a href="/">Home</a><span>/</span><a href="${href}">${esc(label)}</a></p>`;
    }

    function authRequired() {
        if (api.token) return false;
        const returnTo = `${pathNow()}${window.location.search}`;
        navigate(`/login?returnTo=${encodeURIComponent(returnTo)}`, true);
        return true;
    }

    async function verifiedUser() {
        if (!api.token) return null;
        return await api.getCurrentUser();
    }

    async function renderCatalogPage(brandOverride = "", collectionOverride = "") {
        await privateReviewImagesReady;
        const params = queryNow();
        const collection = collectionOverride || params.get("collection") || "";
        const [collections, materials, movements] = await Promise.all([
            api.getCollections(), api.getCaseMaterials(), api.getMovementTypes()
        ]);
        const filter = {
            search: params.get("search") || "",
            collection,
            movement: params.get("movement") || "",
            material: params.get("material") || "",
            sort: params.get("sort") || "referenceNumber,asc",
            page: Number(params.get("page") || 0)
        };
        const result = await api.getWatches({ ...filter, page: filter.page, size: 12 });
        const watches = result.content || [];
        const selectedCollection = collections.find(name => slugify(name) === collection);
        const title = collectionOverride ? esc(selectedCollection || "Collection") : (brandOverride ? esc(brandOverride) : "Rolex watches");
        const clearHref = collectionOverride ? `/collections/${encodeURIComponent(collectionOverride)}` : (brandOverride ? `/brands/${slugify(brandOverride)}` : "/watches");
        return `<main class="route-main route-catalog">
          ${pageHeading(collectionOverride ? "Rolex collection" : "The verified Rolex catalog", title, "Search verified model names and references, then filter by collection, movement, or material.", selectedCollection || "Watches")}
          <form id="catalog-filters" class="catalog-filter-panel">
            <label class="catalog-search">Search by model, reference, or caliber<input name="search" type="search" value="${esc(filter.search)}" placeholder="Datejust 126334" /></label>
            <label>Collection<select name="collection"><option value="">All collections</option>${collections.map(name => `<option value="${esc(slugify(name))}" ${slugify(name) === collection ? "selected" : ""}>${esc(name)}</option>`).join("")}</select></label>
            <label>Movement<select name="movement"><option value="">All movements</option>${movements.map(item => `<option value="${esc(item)}" ${item === filter.movement ? "selected" : ""}>${esc(item)}</option>`).join("")}</select></label>
            <label>Case material<select name="material"><option value="">All materials</option>${materials.map(item => `<option value="${esc(item)}" ${item === filter.material ? "selected" : ""}>${esc(item)}</option>`).join("")}</select></label>
            <label>Sort by<select name="sort">${[["referenceNumber,asc", "Reference: A to Z"], ["name,asc", "Model: A to Z"], ["collectionName,asc", "Collection: A to Z"]].map(([value, label]) => `<option value="${value}" ${value === filter.sort ? "selected" : ""}>${label}</option>`).join("")}</select></label>
            <button class="button button-dark" type="submit">Apply filters</button><a class="button button-light" href="${clearHref}">Clear</a>
          </form>
          <div class="catalog-result-heading"><span>${Number(result.totalElements || 0)} timepieces</span><span>Page ${Number(result.page || 0) + 1} of ${Math.max(1, Number(result.totalPages || 0))}</span></div>
          ${watches.length ? `<div class="watch-grid">${watches.map(w => productCard(w)).join("")}</div>` : statePanel("No timepieces found", "Try adjusting the search or filters, or clear them to see the complete collection.", "empty")}
          <nav class="pagination" aria-label="Catalog pages"><button class="button button-light" data-page="${Math.max(0, filter.page - 1)}" ${filter.page <= 0 ? "disabled" : ""}>Previous</button><span>Page ${filter.page + 1} / ${Math.max(1, result.totalPages || 1)}</span><button class="button button-light" data-page="${filter.page + 1}" ${result.last || !result.totalPages ? "disabled" : ""}>Next</button></nav>
          </main>`;
    }

    async function renderBrands() {
        const brands = await api.getBrands();
        return `<main class="route-main route-brands">${pageHeading("The maisons", "Tradition, interpreted.", "Discover the independent signatures represented in our curated collection.", "Brands")}
          ${brands.length ? `<div class="brand-grid">${brands.map((brand, index) => `<a class="brand-card" href="/brands/${encodeURIComponent(slugify(brand))}"><span>MAISON ${String(index + 1).padStart(2, "0")}</span><strong>${esc(brand)}</strong><i>Explore watches ↗</i></a>`).join("")}</div>` : statePanel("No brands available", "The brand directory could not be loaded.", "error")}</main>`;
    }

    async function renderCollections() {
        const collections = await api.getCollections();
        return `<main class="route-main route-brands">${pageHeading("Rolex collections", "Explore by collection.", "Browse the verified references by the collection names printed in their brochures.", "Collections")}
          ${collections.length ? `<div class="brand-grid">${collections.map((name, index) => `<a class="brand-card" href="/collections/${encodeURIComponent(slugify(name))}"><span>COLLECTION ${String(index + 1).padStart(2, "0")}</span><strong>${esc(name)}</strong><i>Explore watches ↗</i></a>`).join("")}</div>` : statePanel("Collections unavailable", "The catalog service returned no collection names.", "empty")}</main>`;
    }

    async function renderWatchDetail(slug) {
        await privateReviewImagesReady;
        const watch = remember(await api.getWatchBySlug(decodeURIComponent(slug)));
        const facts = [
            ["Collection", watch.collectionName], ["Case", watch.caseTypeDescription], ["Case material", watch.caseMaterial],
            ["Bezel", watch.bezelDescription], ["Case back", watch.caseBackDescription], ["Winding crown", watch.windingCrownDescription],
            ["Diameter", watch.caseDiameterText], ["Case thickness", watch.caseThicknessMm == null ? null : `${watch.caseThicknessMm} mm`],
            ["Water resistance", watch.waterResistanceText], ["Movement", watch.movementType], ["Calibre", watch.caliber],
            ["Precision", watch.precision], ["Power reserve", watch.powerReserveText], ["Oscillator", watch.oscillator], ["Winding", watch.winding],
            ["Crystal", watch.crystalMaterial], ["Dial", watch.dialColor], ["Dial details", watch.dialDetails],
            ["Bracelet", watch.braceletType], ["Bracelet material", watch.strapMaterial], ["Clasp", watch.claspDescription],
            ["Functions", watch.functions], ["Certification", watch.certification]
        ].filter(([, value]) => value);
        const availability = watch.price == null ? "Price on request · checkout unavailable"
            : watch.stockQuantity == null ? "Availability not configured"
                : watch.stockQuantity <= 0 ? "Currently unavailable" : `${watch.stockQuantity} available`;
        const source = watch.sourcePdf ? `Source: ${watch.sourcePdf}, cover p. ${watch.sourceCoverPage}, description p. ${watch.sourceDescriptionPage}, specifications pp. ${watch.sourceSpecificationPages}. Image source p. ${watch.sourceImagePage}, ${watch.sourceImageObject}.` : "";
        return `<main class="route-main route-product">${breadcrumbBack("Collection", "/watches")}
          <div class="product-dossier">
            <div class="product-visual"><img src="${esc(productImage(watch))}" onerror="this.onerror=null;this.src='${placeholderImage}'" alt="${esc(watch.brand)} ${esc(watch.name)}" /><span>${esc(watch.collectionName || "Rolex")}</span></div>
            <article class="product-information"><p class="eyebrow dark-eyebrow">${esc(watch.brand)}${watch.referenceNumber ? ` · Ref. ${esc(watch.referenceNumber)}` : ""}</p><h1>${esc(watch.name)}</h1><p class="product-tagline">${esc(watch.tagline || "Official brochure specifications")}</p><p class="product-description">${esc(watch.description || "Detailed specifications from the catalog.")}</p>
              <div class="product-price-large">${money(watch.price, watch.currency)}${watch.price != null && Number(watch.retailPrice) > Number(watch.price) ? `<del>${money(watch.retailPrice, watch.currency)}</del>` : ""}</div><p class="watch-availability ${watch.purchasable ? "" : "is-unavailable"}">${esc(availability)}</p>
              <div class="product-actions"><button type="button" class="button button-dark js-add-cart" data-watch-id="${esc(watch.id)}" data-watch-slug="${esc(watch.slug)}" ${watch.purchasable ? "" : "disabled"}>${watch.purchasable ? "Add to bag" : "Price on request"}</button><button type="button" class="button button-light js-toggle-wishlist" data-watch-id="${esc(watch.id)}" data-watch-slug="${esc(watch.slug)}">Save to wishlist</button></div>
              <section class="specification-dossier"><p class="eyebrow dark-eyebrow">Technical dossier</p><dl>${facts.map(([label, value]) => `<div><dt>${esc(label)}</dt><dd>${esc(value)}</dd></div>`).join("")}</dl></section>
              ${watch.provenanceNotes ? `<p class="product-provenance"><strong>Catalog notes</strong>${esc(watch.provenanceNotes)}</p>` : ""}${source ? `<p class="product-provenance"><strong>Source provenance</strong>${esc(source)}</p>` : ""}<p class="independent-retailer-note">Brochure image rights have not been cleared for public redistribution. Specifications are shown for independent discovery and should be confirmed with the manufacturer or seller before purchase.</p>
            </article>
          </div></main>`;
    }

    function renderAuth(register = false) {
        const params = queryNow();
        const safeReturn = params.get("returnTo") && params.get("returnTo").startsWith("/") && !params.get("returnTo").startsWith("//") ? params.get("returnTo") : "/account";
        const registeredMessage = !register && params.get("registered") === "1";
        const isCatalogGate = safeReturn.startsWith("/watches") || safeReturn.startsWith("/collections") || safeReturn.startsWith("/brands");
        const eyebrow = isCatalogGate ? "Collector Access Required" : (register ? "Collector account" : "Welcome back");
        const title = register
            ? (isCatalogGate ? "Register to access the collection." : "Begin your collection.")
            : (isCatalogGate ? "Sign in to access the collection." : "Your collection awaits.");
        const description = register
            ? (isCatalogGate ? "Create and verify your collector account first to unlock the Rolex collection and technical dossiers." : "Verify your email first. You’ll create your password in the next step.")
            : (isCatalogGate ? "Sign in with your email and password to browse verified Rolex models and explore technical dossiers." : "Sign in with the email address and password you created.");
        return `<main class="route-main route-auth">${pageHeading(eyebrow, title, description, register ? "Register" : "Sign in")}
          <form class="editorial-form" id="auth-form" data-auth-mode="${register ? "register" : "login"}" novalidate>
            ${register ? `<label>Full name<input name="displayName" autocomplete="name" minlength="2" maxlength="100" required /></label><p class="field-error" data-error-for="displayName"></p>` : ""}
            <label>Email address<input name="email" type="email" autocomplete="email" required /></label><p class="field-error" data-error-for="email"></p>
            ${register ? "" : `<label>Password<span class="password-control"><input name="password" type="password" autocomplete="current-password" minlength="8" required /><button type="button" class="password-toggle" aria-label="Show password">Show</button></span></label><p class="field-error" data-error-for="password"></p>`}
            <div class="form-alert" role="alert" aria-live="polite">${registeredMessage ? "Your email is verified and your password is set. Sign in to continue." : (isCatalogGate && !register ? "Collector sign-in is required to view timepieces and technical specifications." : "")}</div>
            <button class="button button-dark form-submit" type="submit">${register ? "Send verification code" : "Sign in"}</button>
            ${register ? `<button class="button button-light" id="auth-resend-confirmation" type="button" hidden>Send confirmation code</button>` : ""}
            <p class="form-switch">${register ? `Already registered? <a href="/login?returnTo=${encodeURIComponent(safeReturn)}">Sign in</a>` : `New here? <a href="/register?returnTo=${encodeURIComponent(safeReturn)}">Create an account</a>`}</p>
            <p class="form-privacy-note">${register ? "We’ll verify that you own this email before asking you to create a password." : "Your email and password are checked by Supabase. Email confirmation is required for new accounts."}</p>
          </form></main>`;
    }

    function renderConfirmEmail() {
        const pendingEmail = sessionStorage.getItem("atelier_pending_confirmation_email") || "";
        const codeRequestEmail = sessionStorage.getItem("atelier_confirmation_code_request_email") || "";
        const codeRequestSent = pendingEmail && codeRequestEmail.toLowerCase() === pendingEmail.toLowerCase();
        const params = queryNow();
        const returnTo = params.get("returnTo");
        const safeReturn = returnTo && internal(returnTo) ? returnTo : "/account";
        return `<main class="route-main route-auth">${pageHeading("Email confirmation", "Check your inbox.", "Enter the six-digit code from your signup email. After verification, you’ll create your password.", "Confirm email")}
          <form class="editorial-form" id="confirmation-form" novalidate>
            <label>Email address<input name="email" type="email" autocomplete="email" value="${esc(pendingEmail)}" required /></label>
            <label>Six-digit code<input name="token" type="text" inputmode="numeric" autocomplete="one-time-code" pattern="[0-9]{6}" minlength="6" maxlength="6" placeholder="000000" required /></label>
            <p class="form-privacy-note">The code is sent by Supabase Auth. Never share it with anyone. If the email contains a confirmation link, open it to continue automatically.</p>
            <div class="form-alert" role="alert" aria-live="polite">${codeRequestSent ? "Verification code requested. Check your inbox, spam, and promotions folders. Email delivery depends on the configured Supabase email provider." : ""}</div>
            <button class="button button-dark form-submit" type="submit">Verify email and continue</button>
            <button class="button button-light" id="resend-confirmation" type="button">${codeRequestSent ? "Resend verification code" : "Get verification code"}</button>
            <p class="form-switch"><a href="/register?returnTo=${encodeURIComponent(safeReturn)}">Use a different email</a> · <a href="/login?returnTo=${encodeURIComponent(safeReturn)}">Back to sign in</a></p>
          </form></main>`;
    }

    function renderCreatePassword() {
        const email = sessionStorage.getItem("atelier_pending_confirmation_email") || "";
        return `<main class="route-main route-auth">${pageHeading("Finish account setup", "Create your password.", "Your email is verified. Choose a password to finish creating your account, then sign in with your email and password.", "New password")}
          <form class="editorial-form" id="password-setup-form" novalidate>
            <label>Email address<input type="email" value="${esc(email)}" autocomplete="email" readonly /></label>
            <label>Password<span class="password-control"><input name="password" type="password" autocomplete="new-password" minlength="8" required /><button type="button" class="password-toggle" aria-label="Show password">Show</button></span></label><p class="field-error" data-error-for="password"></p>
            <label>Confirm password<input name="passwordConfirm" type="password" autocomplete="new-password" minlength="8" required /></label><p class="field-error" data-error-for="passwordConfirm"></p>
            <div class="form-alert" role="alert" aria-live="polite"></div>
            <button class="button button-dark form-submit" type="submit">Create password</button>
          </form></main>`;
    }

    async function renderAccount() {
        if (authRequired()) return "";
        const user = await verifiedUser();
        if (!user) { authRequired(); return ""; }
        const [orders, wishlist, cart] = await Promise.all([api.getUserOrders(), api.getWishlist(), api.getCart()]);
        return `<main class="route-main route-account">${pageHeading("Collector account", `Good to see you,<br /><em>${esc(user.displayName.split(" ")[0])}.</em>`, "Your collection, orders, and account details in one place.", "Account")}
          <section class="account-profile-card"><div><p class="eyebrow dark-eyebrow">Account details</p><h2>${esc(user.displayName)}</h2><p>${esc(user.email)}</p></div><div class="account-role">${user.role === "ROLE_ADMIN" ? "Administrator" : "Collector"}</div><button type="button" class="text-action" id="signout-button">Sign out</button></section>
          <div class="account-shortcuts"><a href="/orders"><span>${orders.length}</span><strong>Orders</strong><small>Track status and shipment</small></a><a href="/account/wishlist"><span>${wishlist.length}</span><strong>Wishlist</strong><small>Your saved timepieces</small></a><a href="/cart"><span>${Number(cart.totalItems || 0)}</span><strong>Shopping bag</strong><small>Continue your selection</small></a>${user.role === "ROLE_ADMIN" ? `<a href="/admin"><span>→</span><strong>Admin desk</strong><small>Catalog and fulfillment</small></a>` : ""}</div>
          <section class="account-recent"><div class="account-section-heading"><div><p class="eyebrow dark-eyebrow">The latest</p><h2>Recent orders</h2></div><a href="/orders">Order history ↗</a></div>${orders.length ? orderCards(orders.slice(0, 3)) : statePanel("No orders yet", "When you place an order, its details and shipment status will appear here.", "empty")}</section></main>`;
    }

    function orderCards(orders) {
        return `<div class="order-list">${orders.map(order => `<a class="order-card" href="/orders/${encodeURIComponent(order.orderReference)}"><div><span>ORDER ${esc(order.orderReference)}</span><strong>${esc(order.status.replaceAll("_", " "))}</strong></div><div><span>${new Date(order.createdAt).toLocaleDateString()}</span><span>Payment · ${esc(order.paymentStatus || "NOT_STARTED")}</span></div><div class="order-card-total">${money(order.totalAmount, order.currency)}<b>View details ↗</b></div></a>`).join("")}</div>`;
    }

    async function renderWishlist() {
        if (authRequired()) return "";
        await privateReviewImagesReady;
        const items = await api.getWishlist();
        return `<main class="route-main">${pageHeading("Collector account", "Saved for later.", "Your wishlist is attached to your account and follows you between sessions.", "Wishlist")}${items.length ? `<div class="watch-grid">${items.map(w => productCard(w)).join("")}</div>` : `${statePanel("Your wishlist is empty", "Save any timepiece in the catalog to keep it close.", "empty")}<a class="button button-dark" href="/watches">Explore the collection</a>`}</main>`;
    }

    async function renderCart() {
        await privateReviewImagesReady;
        const cart = await api.getCart();
        const items = cart.items || [];
        const count = Number(cart.totalItems || cart.totalQuantity || items.reduce((sum, item) => sum + item.quantity, 0));
        return `<main class="route-main route-cart">${pageHeading("Your selection", "The shopping bag.", "Review each timepiece before proceeding to secure checkout.", "Bag")}
          ${items.length ? `<div class="cart-layout"><section class="cart-line-items" aria-label="Items in your bag">${items.map(item => `<article class="cart-line" data-cart-line="${esc(item.id || item.watchId)}"><a href="/watches/${encodeURIComponent(item.slug || "")}"><img src="${esc(productImage(item))}" onerror="this.onerror=null;this.src='${placeholderImage}'" alt="${esc(item.brand || item.watchBrand)} ${esc(item.name || item.watchTitle)}" /></a><div class="cart-line-main"><span>${esc(item.brand || item.watchBrand)}</span><a href="/watches/${encodeURIComponent(item.slug || "")}"><strong>${esc(item.name || item.watchTitle)}</strong></a><small>${money(item.unitPrice, item.currency)} each</small></div><div class="cart-quantity"><button type="button" class="js-cart-quantity" data-cart-id="${esc(item.id || item.watchId)}" data-quantity="${item.quantity - 1}" aria-label="Decrease quantity">−</button><span>${Number(item.quantity)}</span><button type="button" class="js-cart-quantity" data-cart-id="${esc(item.id || item.watchId)}" data-quantity="${item.quantity + 1}" aria-label="Increase quantity">+</button></div><div class="cart-line-total">${money(item.lineTotal ?? (item.unitPrice == null ? null : item.unitPrice * item.quantity), item.currency)}<button type="button" class="js-remove-cart" data-cart-id="${esc(item.id || item.watchId)}">Remove</button></div></article>`).join("")}</section>
            <aside class="cart-summary"><p class="eyebrow dark-eyebrow">At a glance</p><div><span>${count} item${count === 1 ? "" : "s"}</span><strong>${money(cart.subtotal)}</strong></div><div><span>Delivery</span><strong>Quoted at checkout</strong></div><div class="cart-total"><span>Subtotal</span><strong>${money(cart.subtotal)}</strong></div>${cart.checkoutAvailable ? `<p>Final delivery charges and totals are confirmed by the server during checkout.</p><a class="button button-dark" href="/checkout">Continue to checkout</a>` : `<p class="demo-note">Price and inventory are not configured for every item. Checkout is disabled.</p>`}<a class="text-action" href="/watches">Continue exploring</a></aside></div>` : `${statePanel("Your bag is empty", "Discover the collection and add a timepiece to begin.", "empty")}<a class="button button-dark" href="/watches">Explore the collection</a>`}</main>`;
    }

    function addressForm() {
        return `<form class="editorial-form checkout-address-form" id="checkout-address-form" novalidate>
          <div class="form-field-grid"><label>Recipient name<input name="recipientName" autocomplete="name" required maxlength="150" /></label><p class="field-error" data-error-for="recipientName"></p><label>Phone number<input name="phone" type="tel" autocomplete="tel" pattern="^\\+?[0-9][0-9 ()-]{6,28}$" placeholder="+91 98765 43210" required /></label><p class="field-error" data-error-for="phone"></p></div>
          <label>Street address<input name="addressLine1" autocomplete="address-line1" required maxlength="255" /></label><p class="field-error" data-error-for="addressLine1"></p>
          <label>Apartment, suite, or landmark <span class="optional-label">Optional</span><input name="addressLine2" autocomplete="address-line2" maxlength="255" /></label>
          <div class="form-field-grid"><label>City<input name="city" autocomplete="address-level2" required maxlength="100" /></label><p class="field-error" data-error-for="city"></p><label>State / province<input name="state" autocomplete="address-level1" required maxlength="100" /></label><p class="field-error" data-error-for="state"></p></div>
          <div class="form-field-grid"><label>PIN / postal code<input name="postalCode" autocomplete="postal-code" inputmode="numeric" required maxlength="30" /></label><p class="field-error" data-error-for="postalCode"></p><label>Country<select name="country" autocomplete="country-name" required><option value="India" selected>India</option><option>United States</option><option>United Kingdom</option><option>United Arab Emirates</option><option>Other</option></select></label><p class="field-error" data-error-for="country"></p></div>
          <div class="form-alert" role="alert" aria-live="polite"></div>
        </form>`;
    }

    async function renderCheckout() {
        if (authRequired()) return "";
        const [cart, addresses] = await Promise.all([api.getCart(), api.getAddresses()]);
        const items = cart.items || [];
        if (!items.length) return `<main class="route-main">${pageHeading("Secure checkout", "Nothing to check out yet.", "Your bag is ready when you are.", "Checkout")}${statePanel("Your bag is empty", "Add a timepiece to the bag before checkout.", "empty")}<a class="button button-dark" href="/watches">Explore the collection</a></main>`;
        if (cart.checkoutAvailable !== true) return `<main class="route-main">${pageHeading("Checkout unavailable", "Price on request.", "The catalog is not configured for purchases yet.", "Checkout")}${statePanel("No purchase can be started", "Every item needs a verified selling price and configured inventory before the server can create a quote or order.", "info")}<a class="button button-light" href="/cart">Return to your bag</a><a class="button button-dark" href="/watches">Browse watches</a></main>`;
        const preferred = addresses.find(address => address.defaultAddress) || addresses[0];
        return `<main class="route-main route-checkout">${pageHeading("Secure checkout", "Delivery, then confirmation.", "Enter an Indian shipping address or choose a saved destination. The backend calculates the final total.", "Checkout")}
          <div class="checkout-layout"><section class="checkout-details"><section id="checkout-choice" class="saved-addresses"><h2>1. Delivery address</h2>
            ${addresses.length ? `<div class="saved-address-options">${addresses.map(address => `<label><input type="radio" name="addressChoice" value="${esc(address.id)}" ${preferred.id === address.id ? "checked" : ""} /><span><strong>${esc(address.recipientName)}</strong><small>${esc(address.addressLine1)}${address.addressLine2 ? `, ${esc(address.addressLine2)}` : ""}<br />${esc(address.city)}, ${esc(address.state)} ${esc(address.postalCode)}, ${esc(address.country)}</small><small>${esc(address.phone)}</small></span></label>`).join("")}<label><input type="radio" name="addressChoice" value="new" /><span><strong>Use a new address</strong><small>Enter another secure delivery destination.</small></span></label></div>` : statePanel("Add a delivery address", "Complete the fields below to continue.", "info")}
            <div id="checkout-address-fields" ${preferred && addresses.length ? "hidden" : ""}>${addressForm()}</div></section>
            <section class="checkout-payment-card"><h2>2. Demo payment</h2><p>Development demo only. This simulates a payment result; it does not contact Razorpay, charge a card, move funds, or create a shipment.</p><div class="checkout-status" id="checkout-feedback" role="status" aria-live="polite"></div><button class="button button-dark" type="button" id="checkout-pay-button">Create demo order and simulate payment</button></section>
          </section><aside class="checkout-summary"><p class="eyebrow dark-eyebrow">Order summary</p><div class="checkout-items">${items.map(item => `<div><span>${esc(item.brand || item.watchBrand)} · ${esc(item.name || item.watchTitle)} <small>× ${Number(item.quantity)}</small></span><strong>${money(item.lineTotal || item.unitPrice * item.quantity)}</strong></div>`).join("")}</div><div><span>Items subtotal</span><strong id="checkout-subtotal">${money(cart.subtotal)}</strong></div><div><span>Delivery</span><strong>Quoted by the store</strong></div><div class="cart-total"><span>Server quote total</span><strong id="checkout-total">${preferred ? "Loading quote…" : "Enter address"}</strong></div><p id="checkout-quote-note">${preferred ? "Fetching a verified quote…" : "A verified quote will appear after your address is saved."}</p></aside></div></main>`;
    }

    async function renderOrders() {
        if (authRequired()) return "";
        const orders = await api.getUserOrders();
        return `<main class="route-main">${pageHeading("Collector account", "Order history.", "Every order and its current payment and shipment status.", "Orders")}${orders.length ? orderCards(orders) : `${statePanel("No orders yet", "Your confirmed acquisitions will be listed here.", "empty")}<a class="button button-dark" href="/watches">Explore watches</a>`}</main>`;
    }

    async function renderOrderDetail(reference) {
        if (authRequired()) return "";
        await privateReviewImagesReady;
        const order = await api.getOrderByReference(decodeURIComponent(reference));
        const statusLabel = String(order.status || "").replaceAll("_", " ");
        return `<main class="route-main">${breadcrumbBack("Orders", "/orders")}<section class="order-detail-heading"><p class="eyebrow dark-eyebrow">Order ${esc(order.orderReference)}</p><h1>Order details.</h1><p>Placed ${new Date(order.createdAt).toLocaleString()}</p><div class="order-status-pills"><span>Order · ${esc(statusLabel)}</span><span>Payment · ${esc(order.paymentStatus || "NOT_STARTED")}</span>${order.shipmentStatus ? `<span>Shipment · ${esc(order.shipmentStatus.replaceAll("_", " "))}</span>` : ""}</div>${order.paymentStatus === "SIMULATED" ? `<p class="demo-note" role="status">Demo only: no payment was processed and no shipment was created.</p>` : ""}</section>
          <div class="order-detail-layout"><section class="order-item-list">${(order.items || []).map(item => `<article class="cart-line"><img src="${esc(productImage(item))}" onerror="this.onerror=null;this.src='${placeholderImage}'" alt="${esc(item.brand)} ${esc(item.name)}" /><div class="cart-line-main"><span>${esc(item.brand)}</span><strong>${esc(item.name)}</strong><small>Quantity ${Number(item.quantity)} · ${money(item.unitPrice, order.currency)} each</small></div><strong>${money(item.totalPrice, order.currency)}</strong></article>`).join("")}</section><aside class="cart-summary"><p class="eyebrow dark-eyebrow">Delivery destination</p><p><strong>${esc(order.shippingName)}</strong><br />${esc(order.shippingAddressLine1)}${order.shippingAddressLine2 ? `<br />${esc(order.shippingAddressLine2)}` : ""}<br />${esc(order.shippingCity)}, ${esc(order.shippingState)} ${esc(order.shippingPostalCode)}<br />${esc(order.shippingCountry)}</p><div><span>Items subtotal</span><strong>${money(order.subtotal, order.currency)}</strong></div><div><span>Delivery</span><strong>${money(order.shippingFee, order.currency)}</strong></div><div class="cart-total"><span>Order total</span><strong>${money(order.totalAmount, order.currency)}</strong></div>${order.trackingNumber ? `<p><strong>Tracking ${esc(order.trackingNumber)}</strong><br />${esc(order.shipmentStatus || "Order processed")}</p><p class="demo-note">Shipment status is demonstration data; no live carrier is connected.</p>` : ""}${order.status === "PENDING_PAYMENT" ? `<button type="button" class="button button-dark" id="retry-payment" data-order-reference="${esc(order.orderReference)}">Retry test payment</button>` : ""}</aside></div></main>`;
    }

    const watchForm = (watch, categories, editing = false) => {
        const w = watch || {};
        const value = key => esc(w[key] == null ? "" : w[key]);
        const categoryId = w.category?.id ?? "";
        const input = (key, label, type = "text", options = {}) => `<label>${label}<input name="${key}" type="${type}" value="${value(key)}" ${options.required === false ? "" : "required"} ${options.min != null ? `min="${options.min}"` : ""} ${options.step ? `step="${options.step}"` : ""} /></label>`;
        return `<form class="admin-watch-form" ${editing ? `data-watch-edit="${esc(w.id)}"` : 'id="admin-watch-create"'}>
          <div class="form-field-grid">${input("name", "Model name")}${input("brand", "Brand")}${!editing ? input("slug", "URL slug") : ""}${input("price", "Price", "number", { min: "0.01", step: "0.01" })}</div>
          <label>Tagline<input name="tagline" value="${value("tagline")}" /></label><label>Description<textarea name="description" required>${value("description")}</textarea></label>
          <div class="form-field-grid"><label>Category<select name="categoryId" required><option value="">Choose category</option>${categories.map(c => `<option value="${c.id}" ${c.id === categoryId ? "selected" : ""}>${esc(c.name)}</option>`).join("")}</select></label>${input("currency", "Currency (ISO)", "text", { required: false })}</div>
          <div class="form-field-grid">${input("stockQuantity", "Stock quantity", "number", { min: 0 })}${input("movementType", "Movement type")}${input("caliber", "Caliber", "text", { required: false })}${input("caseMaterial", "Case material")}</div>
          <div class="form-field-grid">${input("caseDiameterMm", "Case diameter (mm)", "number", { min: 10, step: "0.1" })}${input("caseThicknessMm", "Case thickness (mm)", "number", { min: 0, step: "0.1", required: false })}${input("waterResistanceM", "Water resistance (m)", "number", { min: 0 })}${input("powerReserveHours", "Power reserve (hours)", "number", { min: 0 })}</div>
          <div class="form-field-grid">${input("crystalMaterial", "Crystal material")}${input("dialColor", "Dial color")}${input("strapMaterial", "Bracelet / strap", "text", { required: false })}${input("imageUrl", "Image URL")}</div>
          <label>Functions<input name="functions" value="${value("functions")}" /></label>
          <div class="form-field-grid"><label>Publication status<select name="status"><option value="PUBLISHED" ${w.status === "PUBLISHED" || !w.status ? "selected" : ""}>Published</option><option value="DRAFT" ${w.status === "DRAFT" ? "selected" : ""}>Draft</option><option value="ARCHIVED" ${w.status === "ARCHIVED" ? "selected" : ""}>Archived</option></select></label><label class="admin-check"><input name="featured" type="checkbox" ${w.featured ? "checked" : ""} /> Featured on storefront</label></div>
          <div class="form-alert" role="alert"></div><button class="button button-dark" type="submit">${editing ? "Save watch details" : "Add watch"}</button>
        </form>`;
    };

    async function renderAdmin() {
        if (authRequired()) return "";
        const user = await verifiedUser();
        if (!user) { authRequired(); return ""; }
        if (user.role !== "ROLE_ADMIN") return `<main class="route-main">${pageHeading("Access restricted", "Administrator access required.", "Sign in with an administrator account to manage the catalog.", "Admin")}${statePanel("This account does not have administrator access", "Catalog and order management is enforced by the backend role system.", "error")}</main>`;
        const [watchPage, categories, orderPage] = await Promise.all([api.getAdminWatches(), api.getAdminCategories(), api.getAdminOrders()]);
        const watches = watchPage.content || [];
        const orders = orderPage.content || [];
        return `<main class="route-main route-admin">${pageHeading("Operations", "The operations desk.", "Manage the catalog, inventory, categories, orders, and demonstration shipments.", "Admin")}
          <div class="admin-tabs"><a href="#admin-products">Products</a><a href="#admin-categories">Categories</a><a href="#admin-orders">Orders</a></div>
          <section class="admin-section" id="admin-products"><div class="admin-section-heading"><div><p class="eyebrow dark-eyebrow">Catalog</p><h2>Watch inventory</h2></div><label>Search products<input id="admin-watch-search" type="search" placeholder="Model or brand" /></label></div>
            <details class="admin-create-panel"><summary>Add a watch</summary>${watchForm(null, categories)}</details>
            <div class="admin-watch-list">${watches.map(w => `<details class="admin-watch-row" data-admin-search="${esc(`${w.brand} ${w.name}`.toLowerCase())}"><summary><span><strong>${esc(w.brand)} · ${esc(w.name)}</strong><small>${esc(w.status || "PUBLISHED")} · ${Number(w.stockQuantity || 0)} in stock · ${money(w.price, w.currency)}</small></span><span>Edit</span></summary>${watchForm(w, categories, true)}</details>`).join("") || statePanel("No catalog items", "Add the first watch to the catalog.", "empty")}</div>
          </section>
          <section class="admin-section" id="admin-categories"><div class="admin-section-heading"><div><p class="eyebrow dark-eyebrow">Taxonomy</p><h2>Categories</h2></div></div><form id="admin-category-create" class="admin-category-create"><label>Name<input name="name" required /></label><label>Slug<input name="slug" pattern="[a-z0-9-]+" required /></label><label>Description<input name="description" /></label><button class="button button-dark" type="submit">Add category</button><div class="form-alert" role="alert"></div></form><div class="admin-category-list">${categories.map(c => `<form class="admin-category-row" data-category-edit="${c.id}"><label>Name<input name="name" value="${esc(c.name)}" required /></label><label>Slug<input name="slug" value="${esc(c.slug)}" pattern="[a-z0-9-]+" required /></label><label>Description<input name="description" value="${esc(c.description || "")}" /></label><button class="text-action" type="submit">Save</button><button class="text-action danger-action" type="button" data-category-delete="${c.id}">Remove</button></form>`).join("")}</div></section>
          <section class="admin-section" id="admin-orders"><div class="admin-section-heading"><div><p class="eyebrow dark-eyebrow">Fulfillment</p><h2>Orders & shipments</h2></div></div><div class="admin-orders-list">${orders.length ? orders.map(order => `<article class="admin-order-row"><div><strong>${esc(order.orderReference)}</strong><span>${esc(order.shippingName)} · ${money(order.totalAmount, order.currency)}</span></div><label>Order status<select data-admin-order-status="${order.id}">${["PENDING_PAYMENT", "DEMO_COMPLETED", "CONFIRMED", "PROCESSING", "CANCELLED", "REFUNDED"].map(status => `<option value="${status}" ${order.status === status ? "selected" : ""}>${status.replaceAll("_", " ")}</option>`).join("")}</select></label><div>Payment <strong>${esc(order.paymentStatus || "NOT_STARTED")}</strong></div><label>Demonstration shipment<select data-admin-shipment-status="${order.id}">${["ORDER_PROCESSED", "IN_TRANSIT", "DELIVERED"].map(status => `<option value="${status}" ${order.shipmentStatus === status ? "selected" : ""}>${status.replaceAll("_", " ")}</option>`).join("")}</select></label><a href="/orders/${encodeURIComponent(order.orderReference)}">Open order ↗</a></article>`).join("") : statePanel("No orders to manage", "New orders will appear here.", "empty")}</div><p class="demo-note">Shipment updates are demonstration data. No carrier integration is configured.</p></section>
          </main>`;
    }

    async function routeContent() {
        const path = pathNow();
        const catalogRoute = path === "/watches" || path.startsWith("/watches/")
            || path === "/collections" || path.startsWith("/collections/")
            || path === "/brands" || path.startsWith("/brands/");
        if (catalogRoute && !(await verifiedUser())) {
            const returnTo = `${path}${window.location.search}`;
            navigate(`/login?returnTo=${encodeURIComponent(returnTo)}`, true);
            return null;
        }
        if (path === "/watches") return { html: await renderCatalogPage(), active: "watches" };
        if (path.startsWith("/watches/")) return { html: await renderWatchDetail(path.slice("/watches/".length)), active: "watches" };
        if (path === "/collections") return { html: await renderCollections(), active: "collections" };
        if (path.startsWith("/collections/")) {
            const slug = decodeURIComponent(path.slice("/collections/".length));
            const collections = await api.getCollections();
            const collection = collections.find(name => slugify(name) === slug);
            if (!collection) throw new Error("That collection could not be found in the verified catalog.");
            return { html: await renderCatalogPage("", slug), active: "collections" };
        }
        if (path === "/brands") return { html: await renderBrands(), active: "brands" };
        if (path.startsWith("/brands/")) {
            const slug = path.slice("/brands/".length);
            const brands = await api.getBrands();
            const brand = brands.find(name => slugify(name) === decodeURIComponent(slug));
            if (!brand) throw new Error("That brand could not be found in the catalog.");
            return { html: await renderCatalogPage(brand), active: "brands" };
        }
        if (path === "/login") return { html: renderAuth(false) };
        if (path === "/register") return { html: renderAuth(true) };
        if (path === "/create-password") {
            await api.authReady;
            if (!api.token || sessionStorage.getItem("atelier_password_setup_pending") !== "true") {
                navigate("/register", true);
                return null;
            }
            return { html: renderCreatePassword() };
        }
        if (path === "/confirm-email") {
            await api.authReady;
            if (api.token) {
                const user = await verifiedUser();
                if (user) {
                    const destination = queryNow().get("returnTo");
                    if (queryNow().get("flow") === "register") {
                        sessionStorage.setItem("atelier_password_setup_pending", "true");
                        sessionStorage.setItem("atelier_pending_confirmation_email", user.email || "");
                        sessionStorage.setItem("atelier_pending_signup_name", user.displayName || "");
                        navigate(`/create-password${destination && internal(destination) ? `?returnTo=${encodeURIComponent(destination)}` : ""}`, true);
                        return null;
                    }
                    sessionStorage.removeItem("atelier_pending_confirmation_email");
                    navigate(destination && internal(destination) ? destination : "/account", true);
                    return null;
                }
            }
            return { html: renderConfirmEmail() };
        }
        if (path === "/account") return { html: await renderAccount() };
        if (path === "/account/wishlist") return { html: await renderWishlist() };
        if (path === "/cart") return { html: await renderCart(), active: "cart" };
        if (path === "/checkout") return { html: await renderCheckout() };
        if (path === "/orders") return { html: await renderOrders() };
        if (path.startsWith("/orders/")) return { html: await renderOrderDetail(path.slice("/orders/".length)) };
        if (path === "/admin") return { html: await renderAdmin() };
        return { html: `<main class="route-main">${pageHeading("Not found", "This page has moved.", "The route is not part of the Dial & Form storefront.")}${statePanel("Page not found", "Return to the collection or go back to the landing page.", "empty")}<a class="button button-dark" href="/">Return home</a></main>` };
    }

    async function renderRoute() {
        const version = ++renderVersion;
        if (pathNow() === "/") {
            document.body.classList.remove("route-active");
            app.hidden = true;
            document.title = "Dial & Form — Horology in Motion";
            return;
        }
        document.body.classList.add("route-active");
        app.hidden = false;
        document.title = `Dial & Form — ${pathNow().split("/").filter(Boolean).at(-1)?.replaceAll("-", " ") || "Collection"}`;
        app.innerHTML = shell(`<main class="route-main route-loading"><div class="route-spinner" aria-hidden="true"></div><p>Preparing the collection…</p></main>`);
        updateCartBadges();
        try {
            const result = await routeContent();
            if (!result || version !== renderVersion || pathNow() === "/") return;
            app.innerHTML = shell(result.html, result.active || "");
            bindRouteActions();
            updateCartBadges();
            if (pathNow() === "/checkout") await bindCheckout();
        } catch (error) {
            if (version !== renderVersion) return;
            app.innerHTML = shell(`<main class="route-main">${pageHeading("Dial & Form", "We couldn't load this view.", "The request did not complete.")}${statePanel("Please try again", error.message || "The catalog service could not be reached.", "error")}<button class="button button-dark" type="button" id="route-retry">Retry</button></main>`);
            document.getElementById("route-retry")?.addEventListener("click", renderRoute);
        }
    }

    async function updateCartBadges() {
        const badges = [...document.querySelectorAll("[data-cart-count], #cart-nav-count")];
        if (!badges.length) return;
        try {
            const cart = await api.getCart();
            const count = Number(cart.totalItems || cart.totalQuantity || (cart.items || []).reduce((sum, item) => sum + item.quantity, 0));
            badges.forEach(node => node.textContent = count);
        } catch { badges.forEach(node => node.textContent = "0"); }
    }

    function showApiErrors(form, error) {
        form.querySelectorAll(".field-error").forEach(node => { node.textContent = ""; });
        const errors = error.validationErrors;
        if (errors && typeof errors === "object") {
            Object.entries(errors).forEach(([field, message]) => {
                const target = form.querySelector(`[data-error-for="${CSS.escape(field)}"]`);
                if (target) target.textContent = message;
            });
        }
        const alert = form.querySelector(".form-alert");
        if (alert) alert.textContent = errors ? "Please correct the highlighted fields." : (error.message || "Please check the submitted details.");
    }

    function isConfirmationDeliveryError(error) {
        return /error sending (?:confirmation|magic link|email)|failed to send|email address not authorized|smtp/i.test(String(error?.message || ""));
    }

    function showConfirmationDeliveryError(form, error) {
        const alert = form.querySelector(".form-alert");
        if (alert) alert.textContent = "Supabase could not send the verification email. Your account is not verified. You can retry below; if it fails again, check the Auth SMTP sender, verified domain, and Magic Link template.";
        const retryButton = form.querySelector("#auth-resend-confirmation");
        if (retryButton) retryButton.hidden = false;
        console.warn("Supabase confirmation email send failed:", error?.message || error);
    }

    function clearFormErrors(form) {
        form.querySelectorAll(".field-error").forEach(node => { node.textContent = ""; });
        const alert = form.querySelector(".form-alert");
        if (alert) alert.textContent = "";
    }

    async function findWatch(id, slug) {
        const known = watchCache.get(String(id));
        if (known) return known;
        if (slug) return remember(await api.getWatchBySlug(slug));
        const page = await api.getWatches({ page: 0, size: 50 });
        return (page.content || []).map(remember).find(w => String(w.id) === String(id));
    }

    async function handleCartAdd(button) {
        const watch = await findWatch(button.dataset.watchId, button.dataset.watchSlug);
        if (!watch) throw new Error("This watch is no longer available in the catalog.");
        await api.addToCart(watch, 1);
        toast(`${watch.brand} ${watch.name} added to your bag.`);
        navigate("/cart");
    }

    async function toggleWishlist(button) {
        if (!api.token) {
            navigate(`/login?returnTo=${encodeURIComponent(`${pathNow()}${window.location.search}`)}`);
            return;
        }
        const id = Number(button.dataset.watchId);
        const current = await api.getWishlist();
        const exists = current.some(item => Number(item.id) === id);
        if (exists) {
            await api.removeFromWishlist(id);
            toast("Removed from your wishlist.");
        } else {
            await api.addToWishlist(id);
            toast("Saved to your wishlist.");
        }
        if (pathNow() === "/account/wishlist") await renderRoute();
        else button.setAttribute("aria-pressed", String(!exists));
    }

    async function startTestPayment(orderReference, button) {
        const previous = button.textContent;
        button.disabled = true;
        button.textContent = "Preparing demo result…";
        try {
            const gatewayOrder = await api.createRazorpayOrder(orderReference);
            const paymentId = `pay_test_${Date.now()}`;
            const signed = await api.getTestPaymentSignature(orderReference, paymentId);
            button.textContent = "Recording demo result…";
            await api.verifyPayment({ orderReference, razorpayOrderId: gatewayOrder.razorpayOrderId, razorpayPaymentId: paymentId, razorpaySignature: signed.signature });
            localStorage.removeItem("atelier_pending_order");
            toast("Demo payment simulated; no payment or shipment was created.");
            navigate(`/orders/${encodeURIComponent(orderReference)}`);
        } catch (error) {
            button.disabled = false;
            button.textContent = previous;
            toast(error.message || "Test payment could not be verified.", "error");
            throw error;
        } finally {
            if (button.isConnected && button.disabled) {
                button.disabled = false;
                button.textContent = previous;
            }
        }
    }

    async function bindCheckout() {
        const addressRadios = [...document.querySelectorAll("input[name='addressChoice']")];
        const addressFields = document.getElementById("checkout-address-fields");
        const form = document.getElementById("checkout-address-form");
        const selectedSaved = () => document.querySelector("input[name='addressChoice']:checked")?.value || "";
        const quoteNote = document.getElementById("checkout-quote-note");
        const quoteTotal = document.getElementById("checkout-total");
        const quoteSubtotal = document.getElementById("checkout-subtotal");

        async function refreshQuote(addressId) {
            if (!addressId) return;
            quoteNote.textContent = "Fetching a verified quote…";
            quoteTotal.textContent = "…";
            try {
                const quote = await api.getCheckoutQuote(addressId);
                quoteSubtotal.textContent = money(quote.subtotal, quote.currency);
                quoteTotal.textContent = money(quote.totalAmount, quote.currency);
                quoteNote.textContent = `Server-calculated total · ${Number(quote.totalItems)} item${Number(quote.totalItems) === 1 ? "" : "s"}.`;
            } catch (error) {
                quoteNote.textContent = error.message;
                quoteTotal.textContent = "Quote unavailable";
            }
        }

        addressRadios.forEach(radio => radio.addEventListener("change", () => {
            const isNew = radio.value === "new";
            addressFields.hidden = !isNew;
            if (!isNew) refreshQuote(Number(radio.value));
            else { quoteNote.textContent = "A verified quote will appear after the address is saved."; quoteTotal.textContent = "Enter address"; }
        }));
        const initial = selectedSaved();
        if (initial && initial !== "new") await refreshQuote(Number(initial));

        const payButton = document.getElementById("checkout-pay-button");
        payButton?.addEventListener("click", async () => {
            payButton.disabled = true;
            const feedback = document.getElementById("checkout-feedback");
            feedback.textContent = "Validating delivery details…";
            let shippingAddressId = Number(selectedSaved()) || null;
            try {
                if (!shippingAddressId) {
                    clearFormErrors(form);
                    if (!form.reportValidity()) { feedback.textContent = "Complete the required address fields to continue."; return; }
                    const formData = new FormData(form);
                    const country = String(formData.get("country") || "").trim();
                    const postalCode = String(formData.get("postalCode") || "").trim();
                    if (country.toLowerCase() === "india" && !/^\d{6}$/.test(postalCode)) {
                        form.querySelector("[name='postalCode']").setCustomValidity("Enter a six-digit Indian PIN code.");
                        form.querySelector("[name='postalCode']").reportValidity();
                        form.querySelector("[name='postalCode']").setCustomValidity("");
                        return;
                    }
                    const address = await api.saveAddress({
                        recipientName: String(formData.get("recipientName")).trim(),
                        phone: String(formData.get("phone")).trim(),
                        addressLine1: String(formData.get("addressLine1")).trim(),
                        addressLine2: String(formData.get("addressLine2") || "").trim(),
                        city: String(formData.get("city")).trim(), state: String(formData.get("state")).trim(),
                        postalCode, country, defaultAddress: true
                    });
                    shippingAddressId = Number(address.id);
                }
                feedback.textContent = "Confirming the server-calculated quote…";
                const quote = await api.getCheckoutQuote(shippingAddressId);
                quoteSubtotal.textContent = money(quote.subtotal, quote.currency);
                quoteTotal.textContent = money(quote.totalAmount, quote.currency);
                quoteNote.textContent = "Final total verified by the server.";
                feedback.textContent = "Creating your order…";
                const order = await api.createOrder(shippingAddressId);
                localStorage.setItem("atelier_pending_order", order.orderReference);
                feedback.textContent = `Demo order ${order.orderReference} reserved. Starting the simulation…`;
                await startTestPayment(order.orderReference, payButton);
            } catch (error) {
                if (error.validationErrors) showApiErrors(form, error);
                const pending = localStorage.getItem("atelier_pending_order");
                feedback.innerHTML = `${esc(error.message || "Checkout could not be completed.")}${pending ? ` <a href="/orders/${encodeURIComponent(pending)}">Open pending order ${esc(pending)} to retry payment.</a>` : ""}`;
                payButton.disabled = false;
            } finally {
                if (payButton.isConnected && payButton.textContent === "Create demo order and simulate payment") payButton.disabled = false;
            }
        });
    }

    function formPayload(form) {
        const data = new FormData(form);
        const payload = Object.fromEntries(data.entries());
        for (const key of ["price", "caseDiameterMm", "caseThicknessMm"]) if (payload[key] !== undefined && payload[key] !== "") payload[key] = Number(payload[key]);
        for (const key of ["categoryId", "stockQuantity", "waterResistanceM", "powerReserveHours"]) if (payload[key] !== undefined && payload[key] !== "") payload[key] = Number(payload[key]);
        payload.featured = form.elements.featured?.checked || false;
        if (payload.currency) payload.currency = payload.currency.toUpperCase();
        return payload;
    }

    async function bindRouteActions() {
        app.querySelectorAll(".js-add-cart").forEach(button => button.addEventListener("click", async () => {
            button.disabled = true;
            try { await handleCartAdd(button); }
            catch (error) { toast(error.message || "Could not add this watch to the bag.", "error"); button.disabled = false; }
        }));
        app.querySelectorAll(".js-toggle-wishlist").forEach(button => button.addEventListener("click", () => toggleWishlist(button).catch(error => toast(error.message || "Wishlist update failed.", "error"))));
        app.querySelectorAll(".js-cart-quantity").forEach(button => button.addEventListener("click", async () => {
            try { await api.updateCartItem(Number(button.dataset.cartId), Number(button.dataset.quantity)); await renderRoute(); }
            catch (error) { toast(error.message || "Quantity could not be updated.", "error"); }
        }));
        app.querySelectorAll(".js-remove-cart").forEach(button => button.addEventListener("click", async () => {
            try { await api.removeFromCart(Number(button.dataset.cartId)); await renderRoute(); }
            catch (error) { toast(error.message || "Item could not be removed.", "error"); }
        }));
        app.querySelector("#catalog-filters")?.addEventListener("submit", event => {
            event.preventDefault();
            const data = new FormData(event.currentTarget);
            const query = new URLSearchParams();
            for (const [key, value] of data.entries()) if (String(value).trim()) query.set(key, String(value).trim());
            let path = pathNow().startsWith("/brands/") ? pathNow() : "/watches";
            if (pathNow().startsWith("/collections/")) {
                const selectedCollection = String(data.get("collection") || "");
                path = selectedCollection ? `/collections/${encodeURIComponent(selectedCollection)}` : "/watches";
                query.delete("collection");
            }
            navigate(`${path}${query.toString() ? `?${query}` : ""}`);
        });
        app.querySelectorAll(".pagination [data-page]").forEach(button => button.addEventListener("click", () => {
            const params = queryNow(); params.set("page", button.dataset.page); navigate(`${pathNow()}?${params}`);
        }));
        app.querySelector("#auth-form")?.addEventListener("submit", handleAuthSubmit);
        app.querySelector("#auth-resend-confirmation")?.addEventListener("click", handleSignupConfirmationRetry);
        app.querySelector("#confirmation-form")?.addEventListener("submit", handleConfirmationSubmit);
        app.querySelector("#resend-confirmation")?.addEventListener("click", handleResendConfirmation);
        app.querySelector("#password-setup-form")?.addEventListener("submit", handlePasswordSetupSubmit);
        app.querySelectorAll(".password-toggle").forEach(button => button.addEventListener("click", () => {
            const input = button.parentElement.querySelector("input");
            const reveal = input.type === "password";
            input.type = reveal ? "text" : "password";
            button.textContent = reveal ? "Hide" : "Show";
            button.setAttribute("aria-label", reveal ? "Hide password" : "Show password");
        }));
        app.querySelector("#signout-button")?.addEventListener("click", async () => {
            await api.logout();
            toast("Signed out.");
            navigate("/");
        });
        app.querySelector("#retry-payment")?.addEventListener("click", event => startTestPayment(event.currentTarget.dataset.orderReference, event.currentTarget).catch(() => {}));
        app.querySelector("#admin-watch-search")?.addEventListener("input", event => {
            const query = event.currentTarget.value.toLowerCase().trim();
            app.querySelectorAll(".admin-watch-row").forEach(row => row.hidden = !row.dataset.adminSearch.includes(query));
        });
        app.querySelector("#admin-watch-create")?.addEventListener("submit", async event => {
            event.preventDefault();
            const form = event.currentTarget;
            const submit = form.querySelector("button[type=submit]"); submit.disabled = true; clearFormErrors(form);
            try { await api.createAdminWatch(formPayload(form)); toast("Watch added to the catalog."); await renderRoute(); }
            catch (error) { showApiErrors(form, error); submit.disabled = false; }
        });
        app.querySelectorAll("[data-watch-edit]").forEach(form => form.addEventListener("submit", async event => {
            event.preventDefault(); const form = event.currentTarget; const submit = form.querySelector("button[type=submit]"); submit.disabled = true; clearFormErrors(form);
            try { await api.updateAdminWatch(Number(form.dataset.watchEdit), formPayload(form)); toast("Watch details saved."); await renderRoute(); }
            catch (error) { showApiErrors(form, error); submit.disabled = false; }
        }));
        app.querySelector("#admin-category-create")?.addEventListener("submit", async event => {
            event.preventDefault(); const form = event.currentTarget;
            try { await api.createAdminCategory(Object.fromEntries(new FormData(form).entries())); toast("Category added."); await renderRoute(); }
            catch (error) { showApiErrors(form, error); }
        });
        app.querySelectorAll("[data-category-edit]").forEach(form => form.addEventListener("submit", async event => {
            event.preventDefault(); const form = event.currentTarget;
            try { await api.updateAdminCategory(Number(form.dataset.categoryEdit), Object.fromEntries(new FormData(form).entries())); toast("Category saved."); }
            catch (error) { toast(error.message || "Category update failed.", "error"); }
        }));
        app.querySelectorAll("[data-category-delete]").forEach(button => button.addEventListener("click", async () => {
            try { await api.deleteAdminCategory(Number(button.dataset.categoryDelete)); toast("Category removed."); await renderRoute(); }
            catch (error) { toast(error.message || "Category could not be removed.", "error"); }
        }));
        app.querySelectorAll("[data-admin-order-status]").forEach(select => select.addEventListener("change", async () => {
            try { await api.updateAdminOrderStatus(Number(select.dataset.adminOrderStatus), select.value); toast("Order status updated."); }
            catch (error) { toast(error.message || "Order status update failed.", "error"); await renderRoute(); }
        }));
        app.querySelectorAll("[data-admin-shipment-status]").forEach(select => select.addEventListener("change", async () => {
            try { await api.updateAdminShipment(Number(select.dataset.adminShipmentStatus), select.value); toast("Demonstration shipment status updated."); }
            catch (error) { toast(error.message || "Shipment status update failed.", "error"); await renderRoute(); }
        }));
    }

    async function handleAuthSubmit(event) {
        event.preventDefault();
        const form = event.currentTarget;
        clearFormErrors(form);
        if (!form.reportValidity()) return;
        const button = form.querySelector("button[type=submit]");
        const mode = form.dataset.authMode;
        button.disabled = true;
        button.textContent = mode === "register" ? "Sending verification code…" : "Signing in…";
        try {
            const values = new FormData(form);
            const email = String(values.get("email")).trim();
            if (mode === "login") {
                const result = await api.login(email, String(values.get("password")));
                await api.mergeGuestCart().catch(error => toast(`Signed in, but some bag items could not be merged: ${error.message}`, "error"));
                toast(`Welcome back, ${result.user.displayName}.`);
                const destination = queryNow().get("returnTo");
                navigate(destination && internal(destination) ? destination : "/account", true);
                return;
            }
            const displayName = String(values.get("displayName")).trim();
            sessionStorage.setItem("atelier_pending_signup_name", displayName);
            await api.register(displayName, email);
            sessionStorage.setItem("atelier_pending_confirmation_email", email);
            sessionStorage.setItem("atelier_confirmation_code_request_email", email);
            form.reset();
            const returnTo = queryNow().get("returnTo");
            navigate(`/confirm-email?flow=register${returnTo && internal(returnTo) ? `&returnTo=${encodeURIComponent(returnTo)}` : ""}`, true);
        } catch (error) {
            if (mode === "register" && isConfirmationDeliveryError(error)) {
                const values = new FormData(form);
                const email = String(values.get("email") || "").trim();
                const displayName = String(values.get("displayName") || "").trim();
                if (email) sessionStorage.setItem("atelier_pending_confirmation_email", email);
                if (displayName) sessionStorage.setItem("atelier_pending_signup_name", displayName);
                showConfirmationDeliveryError(form, error);
            } else {
                showApiErrors(form, error);
            }
            button.disabled = false;
            button.textContent = mode === "register" ? "Send verification code" : "Sign in";
        }
    }

    async function handleSignupConfirmationRetry(event) {
        const button = event.currentTarget;
        const form = app.querySelector("#auth-form");
        const emailInput = form?.elements.email;
        const alert = form?.querySelector(".form-alert");
        if (!emailInput?.reportValidity()) return;
        button.disabled = true;
        button.textContent = "Requesting code…";
        try {
            const email = String(emailInput.value).trim();
            await api.resendSignupConfirmation(email);
            sessionStorage.setItem("atelier_pending_confirmation_email", email);
            sessionStorage.setItem("atelier_confirmation_code_request_email", email);
            const returnTo = queryNow().get("returnTo");
            navigate(`/confirm-email?flow=register${returnTo && internal(returnTo) ? `&returnTo=${encodeURIComponent(returnTo)}` : ""}`, true);
        } catch (error) {
            if (alert) alert.textContent = isConfirmationDeliveryError(error)
                ? "Supabase still cannot send the email. This needs a working Supabase Auth SMTP configuration and verified sender domain; no code was delivered."
                : (error.message || "The code could not be requested. Check the email address and try again.");
            button.disabled = false;
            button.textContent = "Retry sending confirmation code";
        }
    }

    async function handleConfirmationSubmit(event) {
        event.preventDefault();
        const form = event.currentTarget;
        clearFormErrors(form);
        if (!form.reportValidity()) return;
        const button = form.querySelector("button[type=submit]");
        const values = new FormData(form);
        const registerFlow = queryNow().get("flow") === "register";
        button.disabled = true;
        button.textContent = "Verifying code…";
        try {
            const email = String(values.get("email")).trim();
            const code = String(values.get("token")).trim();
            await api.confirmSignup(email, code);
            sessionStorage.removeItem("atelier_confirmation_code_request_email");
            if (registerFlow) {
                sessionStorage.setItem("atelier_password_setup_pending", "true");
                const returnTo = queryNow().get("returnTo");
                navigate(`/create-password${returnTo && internal(returnTo) ? `?returnTo=${encodeURIComponent(returnTo)}` : ""}`, true);
                return;
            }
            sessionStorage.removeItem("atelier_pending_confirmation_email");
            await api.mergeGuestCart().catch(error => toast(`Email confirmed, but some bag items could not be merged: ${error.message}`, "error"));
            toast("Email confirmed. You’re signed in.");
            const destination = queryNow().get("returnTo");
            navigate(destination && internal(destination) ? destination : "/account", true);
        } catch (error) {
            showApiErrors(form, error);
            button.disabled = false;
            button.textContent = "Verify email and continue";
        }
    }

    async function handleResendConfirmation(event) {
        const button = event.currentTarget;
        const form = app.querySelector("#confirmation-form");
        const email = form?.elements.email;
        clearFormErrors(form);
        if (!email?.reportValidity()) return;
        button.disabled = true;
        button.textContent = "Requesting code…";
        try {
            const normalizedEmail = String(email.value).trim();
            await api.resendSignupConfirmation(normalizedEmail);
            sessionStorage.setItem("atelier_confirmation_code_request_email", normalizedEmail);
            form.querySelector(".form-alert").textContent = "Confirmation code requested. Check your inbox, spam, and promotions folders. If no email arrives, check the configured email provider’s delivery logs.";
            button.textContent = "Resend verification code";
        } catch (error) {
            form.querySelector(".form-alert").textContent = isConfirmationDeliveryError(error)
                ? "Supabase could not send the email. No code was delivered. Check the Auth SMTP sender, verified domain, and Magic Link email template."
                : (error.message || "The confirmation email could not be requested.");
            button.textContent = "Get verification code";
        } finally {
            button.disabled = false;
        }
    }

    async function handlePasswordSetupSubmit(event) {
        event.preventDefault();
        const form = event.currentTarget;
        clearFormErrors(form);
        if (!form.reportValidity()) return;
        const values = new FormData(form);
        const password = String(values.get("password"));
        const passwordConfirm = String(values.get("passwordConfirm"));
        if (password !== passwordConfirm) {
            form.querySelector('[data-error-for="passwordConfirm"]').textContent = "Passwords do not match.";
            return;
        }
        const button = form.querySelector("button[type=submit]");
        button.disabled = true;
        button.textContent = "Saving password…";
        try {
            await api.setAccountPassword(password);
            sessionStorage.removeItem("atelier_password_setup_pending");
            sessionStorage.removeItem("atelier_pending_confirmation_email");
            sessionStorage.removeItem("atelier_confirmation_code_request_email");
            sessionStorage.removeItem("atelier_pending_signup_name");
            toast("Account ready. Sign in with your email and password.");
            const destination = queryNow().get("returnTo");
            navigate(`/login?registered=1${destination && internal(destination) ? `&returnTo=${encodeURIComponent(destination)}` : ""}`, true);
        } catch (error) {
            showApiErrors(form, error);
            button.disabled = false;
            button.textContent = "Create password";
        }
    }

    document.addEventListener("click", event => {
        const link = event.target.closest("a[href]");
        if (!link || link.target || link.hasAttribute("download")) return;
        const url = new URL(link.href, window.location.href);
        if (url.origin !== window.location.origin || !internal(url.pathname)) return;
        if (url.hash && url.pathname === window.location.pathname && url.search === window.location.search) return;
        event.preventDefault();
        navigate(`${url.pathname}${url.search}${url.hash}`);
    });
    document.addEventListener("click", event => {
        if (app.contains(event.target)) return;
        const add = event.target.closest(".js-add-cart");
        if (add) {
            add.disabled = true;
            handleCartAdd(add).catch(error => { toast(error.message || "Could not add this watch to the bag.", "error"); add.disabled = false; });
            return;
        }
        const save = event.target.closest(".js-toggle-wishlist");
        if (save) toggleWishlist(save).catch(error => toast(error.message || "Wishlist update failed.", "error"));
    });
    window.addEventListener("popstate", renderRoute);

    renderRoute();
})();
