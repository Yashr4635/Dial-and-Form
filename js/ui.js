/**
 * Atelier Watch UI & Integration Layer
 */
(() => {
    // State
    const state = {
        categories: [],
        watches: [],
        comparisonList: JSON.parse(localStorage.getItem("atelier_compare") || "[]"),
        wishlistIds: new Set(),
        currentFilter: {
            search: "",
            category: "",
            movement: "",
            sort: "id,desc",
            page: 0
        },
        activeWatch: null
    };

    // DOM Elements
    const elements = {
        catalogModal: document.getElementById("catalog-modal"),
        detailModal: document.getElementById("detail-modal"),
        compareModal: document.getElementById("compare-modal"),
        authModal: document.getElementById("auth-modal"),
        toastContainer: document.getElementById("toast-container"),

        // Catalog Controls
        catalogGrid: document.getElementById("catalog-grid"),
        categoryFilters: document.getElementById("category-filters"),
        searchInput: document.getElementById("catalog-search"),
        sortSelect: document.getElementById("catalog-sort"),
        catalogLoading: document.getElementById("catalog-loading"),
        catalogEmpty: document.getElementById("catalog-empty"),

        // Triggers
        openCatalogBtns: document.querySelectorAll("[data-open-catalog]"),
        openAuthBtns: document.querySelectorAll("[data-open-auth]"),
        compareTray: document.getElementById("compare-tray"),
        compareCountBadge: document.getElementById("compare-count"),
        openCompareBtn: document.getElementById("open-compare-btn"),

        // Auth
        authNavBtn: document.getElementById("auth-nav-btn"),
        loginForm: document.getElementById("login-form"),
        registerForm: document.getElementById("register-form"),
        authTabs: document.querySelectorAll(".auth-tab"),
        loginTabContent: document.getElementById("login-tab-content"),
        registerTabContent: document.getElementById("register-tab-content"),
        wishlistTabContent: document.getElementById("wishlist-tab-content"),
        wishlistGrid: document.getElementById("wishlist-grid"),

        // E-Commerce Elements
        cartModal: document.getElementById("cart-modal"),
        checkoutModal: document.getElementById("checkout-modal"),
        cartNavBtn: document.getElementById("cart-nav-btn"),
        cartNavCount: document.getElementById("cart-nav-count"),
        cartItemsContainer: document.getElementById("cart-items-container"),
        cartSubtotal: document.getElementById("cart-subtotal"),
        btnProceedCheckout: document.getElementById("btn-proceed-checkout"),
        btnClearCart: document.getElementById("btn-clear-cart"),
        ordersTabContent: document.getElementById("orders-tab-content"),
        ordersList: document.getElementById("orders-list"),
        refreshOrdersBtn: document.getElementById("refresh-orders-btn"),
        savedAddressesList: document.getElementById("saved-addresses-list"),
        checkoutAddressForm: document.getElementById("checkout-address-form"),
        checkoutSubtotal: document.getElementById("checkout-subtotal"),
        checkoutTotal: document.getElementById("checkout-total"),
        btnPayRazorpay: document.getElementById("btn-pay-razorpay")
    };

    // Helper: Toast Notifications
    function showToast(message, type = "info") {
        if (!elements.toastContainer) return;
        const toast = document.createElement("div");
        toast.className = `toast toast-${type}`;
        toast.innerHTML = `<span>${message}</span>`;
        elements.toastContainer.appendChild(toast);
        setTimeout(() => toast.classList.add("is-visible"), 10);
        setTimeout(() => {
            toast.classList.remove("is-visible");
            setTimeout(() => toast.remove(), 300);
        }, 3200);
    }

    // Helper: Format Price
    function formatCurrency(amount, currency = "USD") {
        return new Intl.NumberFormat("en-US", {
            style: "currency",
            currency: currency,
            maximumFractionDigits: 0
        }).format(amount);
    }

    // Modal Manager
    function openModal(modal) {
        if (!modal) return;
        modal.classList.add("is-active");
        document.body.classList.add("modal-open");
    }

    function closeModal(modal) {
        if (!modal) return;
        modal.classList.remove("is-active");
        if (!document.querySelector(".atelier-modal.is-active")) {
            document.body.classList.remove("modal-open");
        }
    }

    // Setup Modals Dismissal
    document.querySelectorAll(".modal-backdrop, .modal-close").forEach(el => {
        el.addEventListener("click", () => {
            document.querySelectorAll(".atelier-modal.is-active").forEach(closeModal);
        });
    });

    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape") {
            document.querySelectorAll(".atelier-modal.is-active").forEach(closeModal);
        }
    });

    // -------------------------------------------------------------------------
    // 1. Catalog & Discovery
    // -------------------------------------------------------------------------
    async function loadCategories() {
        try {
            state.categories = await window.atelierApi.getCategories();
            if (!elements.categoryFilters) return;

            elements.categoryFilters.innerHTML = `
                <button class="filter-pill active" data-category="">All Collections</button>
                ${state.categories.map(c => `
                    <button class="filter-pill" data-category="${c.slug}">${c.name}</button>
                `).join("")}
            `;

            elements.categoryFilters.querySelectorAll(".filter-pill").forEach(btn => {
                btn.addEventListener("click", () => {
                    elements.categoryFilters.querySelectorAll(".filter-pill").forEach(b => b.classList.remove("active"));
                    btn.classList.add("active");
                    state.currentFilter.category = btn.getAttribute("data-category");
                    fetchWatches();
                });
            });
        } catch (err) {
            console.error("Failed to load categories:", err);
        }
    }

    async function fetchWatches() {
        if (!elements.catalogGrid) return;
        elements.catalogLoading.style.display = "block";
        elements.catalogEmpty.style.display = "none";
        elements.catalogGrid.innerHTML = "";

        try {
            const result = await window.atelierApi.getWatches({
                search: state.currentFilter.search,
                category: state.currentFilter.category,
                sort: state.currentFilter.sort,
                size: 24
            });

            state.watches = result.content;
            renderWatchGrid(state.watches);
        } catch (err) {
            showToast("Failed to load catalog timepieces", "error");
        } finally {
            elements.catalogLoading.style.display = "none";
        }
    }

    function renderWatchGrid(watches) {
        if (!watches || watches.length === 0) {
            elements.catalogEmpty.style.display = "block";
            return;
        }

        elements.catalogGrid.innerHTML = watches.map(watch => {
            const isCompared = state.comparisonList.includes(watch.id);
            const isSaved = state.wishlistIds.has(watch.id);

            return `
                <article class="watch-card" data-slug="${watch.slug}">
                    <div class="watch-card-media" data-open-detail="${watch.slug}">
                        <img src="${watch.imageUrl}" alt="${watch.name}" loading="lazy" />
                        ${watch.featured ? '<span class="watch-badge">Featured</span>' : ''}
                        <button class="wishlist-btn ${isSaved ? 'is-saved' : ''}" data-wishlist-id="${watch.id}" aria-label="Toggle Wishlist">
                            <svg width="18" height="18" viewBox="0 0 24 24" fill="${isSaved ? '#c9a66c' : 'none'}" stroke="currentColor" stroke-width="1.6">
                                <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
                            </svg>
                        </button>
                    </div>
                    <div class="watch-card-body">
                        <span class="watch-card-brand">${watch.brand}</span>
                        <h4 class="watch-card-title" data-open-detail="${watch.slug}">${watch.name}</h4>
                        <div class="watch-card-meta">
                            <span>${watch.movementType}</span>
                            <span>•</span>
                            <span>${watch.caseDiameterMm} mm</span>
                            <span>•</span>
                            <span>${watch.caseMaterial}</span>
                        </div>
                        <div class="watch-card-footer">
                            <span class="watch-card-price">${formatCurrency(watch.price, watch.currency)}</span>
                            <div class="watch-card-actions">
                                <button class="btn-text" data-open-detail="${watch.slug}">Specs</button>
                                <button class="btn-compare ${isCompared ? 'is-active' : ''}" data-compare-id="${watch.id}">
                                    ${isCompared ? 'Compared' : '+ Compare'}
                                </button>
                                <button class="btn-primary" style="padding: 6px 10px; font-size: 8px;" data-add-cart="${watch.id}">+ Bag</button>
                            </div>
                        </div>
                    </div>
                </article>
            `;
        }).join("");

        // Attach Card Event Listeners
        elements.catalogGrid.querySelectorAll("[data-open-detail]").forEach(el => {
            el.addEventListener("click", () => {
                const slug = el.getAttribute("data-open-detail");
                openWatchDetail(slug);
            });
        });

        elements.catalogGrid.querySelectorAll("[data-add-cart]").forEach(btn => {
            btn.addEventListener("click", (e) => {
                e.stopPropagation();
                const id = parseInt(btn.getAttribute("data-add-cart"));
                const watch = state.watches.find(w => w.id === id);
                if (watch) handleAddToCart(watch, 1);
            });
        });

        elements.catalogGrid.querySelectorAll("[data-compare-id]").forEach(btn => {
            btn.addEventListener("click", (e) => {
                e.stopPropagation();
                const id = parseInt(btn.getAttribute("data-compare-id"));
                toggleComparison(id);
            });
        });

        elements.catalogGrid.querySelectorAll("[data-wishlist-id]").forEach(btn => {
            btn.addEventListener("click", (e) => {
                e.stopPropagation();
                const id = parseInt(btn.getAttribute("data-wishlist-id"));
                toggleWishlist(id);
            });
        });
    }

    // Debounced Search
    let searchTimeout = null;
    if (elements.searchInput) {
        elements.searchInput.addEventListener("input", (e) => {
            clearTimeout(searchTimeout);
            searchTimeout = setTimeout(() => {
                state.currentFilter.search = e.target.value.trim();
                fetchWatches();
            }, 250);
        });
    }

    if (elements.sortSelect) {
        elements.sortSelect.addEventListener("change", (e) => {
            state.currentFilter.sort = e.target.value;
            fetchWatches();
        });
    }

    // -------------------------------------------------------------------------
    // 2. Watch Detail View
    // -------------------------------------------------------------------------
    async function openWatchDetail(slug) {
        openModal(elements.detailModal);
        const container = document.getElementById("detail-content");
        container.innerHTML = `
            <div class="modal-loading-state">
                <span class="spinner"></span>
                <p>Retrieving technical horological specifications...</p>
            </div>
        `;

        try {
            const watch = await window.atelierApi.getWatchBySlug(slug);
            state.activeWatch = watch;
            const isCompared = state.comparisonList.includes(watch.id);
            const isSaved = state.wishlistIds.has(watch.id);

            container.innerHTML = `
                <div class="detail-layout">
                    <div class="detail-gallery">
                        <img src="${watch.imageUrl}" alt="${watch.name}" />
                    </div>
                    <div class="detail-info">
                        <div class="detail-header">
                            <span class="detail-brand">${watch.brand}</span>
                            <h2 class="detail-title">${watch.name}</h2>
                            <p class="detail-tagline">${watch.tagline || ''}</p>
                            <div class="detail-price-row">
                                <span class="detail-price">${formatCurrency(watch.price, watch.currency)}</span>
                                <span class="detail-category-badge">${watch.category ? watch.category.name : 'Luxury'}</span>
                            </div>
                        </div>

                        <div class="detail-description">
                            <p>${watch.description}</p>
                        </div>

                        <div class="detail-specs-table">
                            <h4 class="specs-heading">Technical Specifications</h4>
                            <div class="spec-row">
                                <span class="spec-label">Caliber</span>
                                <span class="spec-value">${watch.caliber || 'Manufacture'}</span>
                            </div>
                            <div class="spec-row">
                                <span class="spec-label">Movement</span>
                                <span class="spec-value">${watch.movementType}</span>
                            </div>
                            <div class="spec-row">
                                <span class="spec-label">Power Reserve</span>
                                <span class="spec-value">${watch.powerReserveHours} Hours</span>
                            </div>
                            <div class="spec-row">
                                <span class="spec-label">Case Material</span>
                                <span class="spec-value">${watch.caseMaterial}</span>
                            </div>
                            <div class="spec-row">
                                <span class="spec-label">Diameter / Thickness</span>
                                <span class="spec-value">${watch.caseDiameterMm} mm / ${watch.caseThicknessMm || '—'} mm</span>
                            </div>
                            <div class="spec-row">
                                <span class="spec-label">Water Resistance</span>
                                <span class="spec-value">${watch.waterResistanceM} m (${Math.round(watch.waterResistanceM * 3.28)} ft)</span>
                            </div>
                            <div class="spec-row">
                                <span class="spec-label">Dial</span>
                                <span class="spec-value">${watch.dialColor}</span>
                            </div>
                            <div class="spec-row">
                                <span class="spec-label">Crystal</span>
                                <span class="spec-value">${watch.crystalMaterial}</span>
                            </div>
                            <div class="spec-row">
                                <span class="spec-label">Bracelet / Strap</span>
                                <span class="spec-value">${watch.strapMaterial || 'Matching case'}</span>
                            </div>
                            ${watch.functions ? `
                            <div class="spec-row">
                                <span class="spec-label">Complications</span>
                                <span class="spec-value">${watch.functions}</span>
                            </div>` : ''}
                        </div>

                        <div class="detail-actions">
                            <button class="btn-primary" id="detail-cart-btn">
                                Add to Bag (${formatCurrency(watch.price, watch.currency)})
                            </button>
                            <button class="btn-secondary" id="detail-wishlist-btn">
                                ${isSaved ? 'In Your Wishlist' : 'Add to Wishlist'}
                            </button>
                            <button class="btn-secondary" id="detail-compare-btn">
                                ${isCompared ? 'Remove from Compare' : 'Add to Compare'}
                            </button>
                        </div>
                    </div>
                </div>
            `;

            document.getElementById("detail-cart-btn").addEventListener("click", () => {
                handleAddToCart(watch, 1);
            });

            document.getElementById("detail-wishlist-btn").addEventListener("click", () => {
                toggleWishlist(watch.id);
                openWatchDetail(slug);
            });

            document.getElementById("detail-compare-btn").addEventListener("click", () => {
                toggleComparison(watch.id);
                openWatchDetail(slug);
            });
        } catch (err) {
            container.innerHTML = `
                <div class="modal-error-state">
                    <p>Unable to load timepiece details.</p>
                </div>
            `;
        }
    }

    // -------------------------------------------------------------------------
    // 3. Side-by-Side Comparison Matrix
    // -------------------------------------------------------------------------
    function updateCompareTray() {
        const count = state.comparisonList.length;
        if (elements.compareCountBadge) elements.compareCountBadge.textContent = count;
        if (elements.compareTray) {
            if (count > 0) elements.compareTray.classList.add("is-visible");
            else elements.compareTray.classList.remove("is-visible");
        }
        localStorage.setItem("atelier_compare", JSON.stringify(state.comparisonList));
    }

    function toggleComparison(watchId) {
        const index = state.comparisonList.indexOf(watchId);
        if (index > -1) {
            state.comparisonList.splice(index, 1);
            showToast("Watch removed from comparison");
        } else {
            if (state.comparisonList.length >= 4) {
                showToast("Maximum 4 watches can be compared side-by-side", "error");
                return;
            }
            state.comparisonList.push(watchId);
            showToast("Added to comparison");
        }
        updateCompareTray();
        renderWatchGrid(state.watches);
    }

    async function openComparisonMatrix() {
        if (state.comparisonList.length < 2) {
            showToast("Select at least 2 watches to compare", "info");
            return;
        }

        openModal(elements.compareModal);
        const container = document.getElementById("compare-content");
        container.innerHTML = `
            <div class="modal-loading-state">
                <span class="spinner"></span>
                <p>Generating technical comparison matrix...</p>
            </div>
        `;

        try {
            const data = await window.atelierApi.compareWatches(state.comparisonList);
            const watches = data.watches;

            container.innerHTML = `
                <div class="compare-table-wrapper">
                    <table class="compare-table">
                        <thead>
                            <tr>
                                <th class="spec-col-header">Attribute</th>
                                ${watches.map(w => `
                                    <th class="watch-col-header">
                                        <button class="remove-compare-btn" data-remove-compare="${w.id}" title="Remove">✕</button>
                                        <img src="${w.imageUrl}" alt="${w.name}" class="compare-thumb" />
                                        <div class="compare-watch-brand">${w.brand}</div>
                                        <div class="compare-watch-name">${w.name}</div>
                                        <div class="compare-watch-price">${formatCurrency(w.price, w.currency)}</div>
                                    </th>
                                `).join("")}
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td class="spec-label">Category</td>
                                ${watches.map(w => `<td>${w.category ? w.category.name : '—'}</td>`).join("")}
                            </tr>
                            <tr>
                                <td class="spec-label">Movement Type</td>
                                ${watches.map(w => `<td><strong>${w.movementType}</strong></td>`).join("")}
                            </tr>
                            <tr>
                                <td class="spec-label">Caliber</td>
                                ${watches.map(w => `<td>${w.caliber || 'In-House'}</td>`).join("")}
                            </tr>
                            <tr>
                                <td class="spec-label">Case Material</td>
                                ${watches.map(w => `<td>${w.caseMaterial}</td>`).join("")}
                            </tr>
                            <tr>
                                <td class="spec-label">Diameter</td>
                                ${watches.map(w => `<td>${w.caseDiameterMm} mm</td>`).join("")}
                            </tr>
                            <tr>
                                <td class="spec-label">Thickness</td>
                                ${watches.map(w => `<td>${w.caseThicknessMm ? w.caseThicknessMm + ' mm' : '—'}</td>`).join("")}
                            </tr>
                            <tr>
                                <td class="spec-label">Water Resistance</td>
                                ${watches.map(w => `<td><strong>${w.waterResistanceM} meters</strong></td>`).join("")}
                            </tr>
                            <tr>
                                <td class="spec-label">Power Reserve</td>
                                ${watches.map(w => `<td><strong>${w.powerReserveHours} Hours</strong></td>`).join("")}
                            </tr>
                            <tr>
                                <td class="spec-label">Dial Color</td>
                                ${watches.map(w => `<td>${w.dialColor}</td>`).join("")}
                            </tr>
                            <tr>
                                <td class="spec-label">Crystal</td>
                                ${watches.map(w => `<td>${w.crystalMaterial}</td>`).join("")}
                            </tr>
                            <tr>
                                <td class="spec-label">Complications</td>
                                ${watches.map(w => `<td>${w.functions || 'Standard Hours, Minutes, Seconds'}</td>`).join("")}
                            </tr>
                        </tbody>
                    </table>
                </div>
            `;

            container.querySelectorAll("[data-remove-compare]").forEach(btn => {
                btn.addEventListener("click", () => {
                    const id = parseInt(btn.getAttribute("data-remove-compare"));
                    toggleComparison(id);
                    if (state.comparisonList.length >= 2) openComparisonMatrix();
                    else closeModal(elements.compareModal);
                });
            });
        } catch (err) {
            container.innerHTML = `
                <div class="modal-error-state">
                    <p>Failed to generate comparison.</p>
                </div>
            `;
        }
    }

    if (elements.openCompareBtn) {
        elements.openCompareBtn.addEventListener("click", openComparisonMatrix);
    }

    // -------------------------------------------------------------------------
    // 4. User Authentication & Wishlist
    // -------------------------------------------------------------------------
    async function initUserSession() {
        const user = await window.atelierApi.getCurrentUser();
        updateAuthDisplay(user);
        if (user) {
            syncWishlist();
        }
    }

    function updateAuthDisplay(user) {
        if (!elements.authNavBtn) return;
        if (user) {
            elements.authNavBtn.textContent = user.displayName.split(" ")[0];
            elements.authNavBtn.classList.add("is-authenticated");
        } else {
            elements.authNavBtn.textContent = "Sign In";
            elements.authNavBtn.classList.remove("is-authenticated");
        }
    }

    async function syncWishlist() {
        if (!window.atelierApi.token) return;
        try {
            const items = await window.atelierApi.getWishlist();
            state.wishlistIds = new Set(items.map(w => w.id));
            renderWishlistTab(items);
            if (state.watches.length) renderWatchGrid(state.watches);
        } catch (err) {
            console.error("Failed to sync wishlist:", err);
        }
    }

    async function toggleWishlist(watchId) {
        if (!window.atelierApi.token) {
            showToast("Please sign in to save timepieces to your wishlist", "info");
            openModal(elements.authModal);
            return;
        }

        const isSaved = state.wishlistIds.has(watchId);
        try {
            if (isSaved) {
                await window.atelierApi.removeFromWishlist(watchId);
                state.wishlistIds.delete(watchId);
                showToast("Removed from wishlist");
            } else {
                await window.atelierApi.addToWishlist(watchId);
                state.wishlistIds.add(watchId);
                showToast("Saved to your wishlist");
            }
            syncWishlist();
        } catch (err) {
            showToast(err.message, "error");
        }
    }

    function renderWishlistTab(items) {
        if (!elements.wishlistGrid) return;
        if (!items || items.length === 0) {
            elements.wishlistGrid.innerHTML = `
                <div class="wishlist-empty-state">
                    <p>Your wishlist is currently empty.</p>
                    <button class="btn-primary" data-open-catalog>Explore Collection</button>
                </div>
            `;
            elements.wishlistGrid.querySelector("[data-open-catalog]")?.addEventListener("click", () => {
                closeModal(elements.authModal);
                openModal(elements.catalogModal);
            });
            return;
        }

        elements.wishlistGrid.innerHTML = items.map(w => `
            <div class="wishlist-card">
                <img src="${w.imageUrl}" alt="${w.name}" />
                <div class="wishlist-card-details">
                    <span class="wishlist-card-brand">${w.brand}</span>
                    <h4 class="wishlist-card-title">${w.name}</h4>
                    <span class="wishlist-card-price">${formatCurrency(w.price, w.currency)}</span>
                </div>
                <div class="wishlist-card-actions">
                    <button class="btn-text" data-open-detail="${w.slug}">View Specs</button>
                    <button class="btn-remove-wishlist" data-remove-wishlist="${w.id}">Remove</button>
                </div>
            </div>
        `).join("");

        elements.wishlistGrid.querySelectorAll("[data-open-detail]").forEach(btn => {
            btn.addEventListener("click", () => {
                closeModal(elements.authModal);
                openWatchDetail(btn.getAttribute("data-open-detail"));
            });
        });

        elements.wishlistGrid.querySelectorAll("[data-remove-wishlist]").forEach(btn => {
            btn.addEventListener("click", () => {
                const id = parseInt(btn.getAttribute("data-remove-wishlist"));
                toggleWishlist(id);
            });
        });
    }

    // Auth Tabs Switching
    elements.authTabs.forEach(tab => {
        tab.addEventListener("click", () => {
            elements.authTabs.forEach(t => t.classList.remove("active"));
            tab.classList.add("active");
            const target = tab.getAttribute("data-tab");

            elements.loginTabContent.style.display = target === "login" ? "block" : "none";
            elements.registerTabContent.style.display = target === "register" ? "block" : "none";
            elements.wishlistTabContent.style.display = target === "wishlist" ? "block" : "none";
        });
    });

    // Login Form Submit
    if (elements.loginForm) {
        elements.loginForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            const email = elements.loginForm.querySelector("#login-email").value;
            const password = elements.loginForm.querySelector("#login-password").value;
            const submitBtn = elements.loginForm.querySelector("button[type='submit']");
            const errorMsg = elements.loginForm.querySelector(".form-error");

            errorMsg.style.display = "none";
            submitBtn.disabled = true;
            submitBtn.textContent = "Authenticating...";

            try {
                const res = await window.atelierApi.login(email, password);
                showToast(`Welcome back, ${res.user.displayName}`);
                updateAuthDisplay(res.user);
                syncWishlist();
                closeModal(elements.authModal);
                elements.loginForm.reset();
            } catch (err) {
                errorMsg.textContent = err.message || "Invalid email or password";
                errorMsg.style.display = "block";
            } finally {
                submitBtn.disabled = false;
                submitBtn.textContent = "Sign In";
            }
        });
    }

    // Register Form Submit
    if (elements.registerForm) {
        elements.registerForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            const displayName = elements.registerForm.querySelector("#reg-name").value;
            const email = elements.registerForm.querySelector("#reg-email").value;
            const password = elements.registerForm.querySelector("#reg-password").value;
            const submitBtn = elements.registerForm.querySelector("button[type='submit']");
            const errorMsg = elements.registerForm.querySelector(".form-error");

            errorMsg.style.display = "none";
            submitBtn.disabled = true;
            submitBtn.textContent = "Creating Account...";

            try {
                const res = await window.atelierApi.register(displayName, email, password);
                showToast(`Welcome to Atelier Watch, ${res.user.displayName}`);
                updateAuthDisplay(res.user);
                closeModal(elements.authModal);
                elements.registerForm.reset();
            } catch (err) {
                errorMsg.textContent = err.message || "Registration failed";
                errorMsg.style.display = "block";
            } finally {
                submitBtn.disabled = false;
                submitBtn.textContent = "Create Account";
            }
        });
    }

    // Sign Out Button
    const signOutBtn = document.getElementById("signout-btn");
    if (signOutBtn) {
        signOutBtn.addEventListener("click", () => {
            window.atelierApi.logout();
            state.wishlistIds.clear();
            updateAuthDisplay(null);
            showToast("Signed out successfully");
            closeModal(elements.authModal);
            if (state.watches.length) renderWatchGrid(state.watches);
        });
    }

    // -------------------------------------------------------------------------
    // 5. E-Commerce: Cart, Checkout, Razorpay & Order Tracking
    // -------------------------------------------------------------------------
    let currentCart = null;
    let selectedAddressId = null;

    async function syncCart() {
        try {
            currentCart = await window.atelierApi.getCart();
            renderCart(currentCart);
        } catch (err) {
            console.error("Cart sync error:", err);
        }
    }

    function renderCart(cart) {
        if (!cart) return;
        const totalItems = cart.totalQuantity || cart.totalItems || (cart.items ? cart.items.reduce((s, i) => s + i.quantity, 0) : 0);
        if (elements.cartNavCount) elements.cartNavCount.textContent = totalItems;

        if (!elements.cartItemsContainer) return;

        if (!cart.items || cart.items.length === 0) {
            elements.cartItemsContainer.innerHTML = `
                <div style="text-align: center; padding: 40px 0;">
                    <p style="font-family: var(--serif); font-size: 18px; color: var(--gold); margin-bottom: 8px;">Your Shopping Bag is Empty</p>
                    <p style="font-size: 12px; color: rgba(244, 239, 224, 0.5); margin-bottom: 20px;">Explore our curated collection of luxury timepieces.</p>
                    <button class="btn-primary" id="btn-browse-from-cart" style="font-size: 9px; padding: 10px 18px;">Browse Collection</button>
                </div>
            `;
            if (elements.cartSubtotal) elements.cartSubtotal.textContent = "$0";
            if (elements.btnProceedCheckout) elements.btnProceedCheckout.disabled = true;

            document.getElementById("btn-browse-from-cart")?.addEventListener("click", () => {
                closeModal(elements.cartModal);
                openModal(elements.catalogModal);
                if (state.watches.length === 0) fetchWatches();
            });
            return;
        }

        if (elements.btnProceedCheckout) elements.btnProceedCheckout.disabled = false;
        if (elements.cartSubtotal) elements.cartSubtotal.textContent = formatCurrency(cart.subtotal);

        elements.cartItemsContainer.innerHTML = cart.items.map(item => `
            <div class="cart-item-row" data-cart-item-id="${item.id || item.watchId}">
                <img src="${item.imageUrl || item.watchImageUrl || 'https://images.unsplash.com/photo-1522335789203-aabd1fc54bc9?auto=format&fit=crop&w=400&q=80'}" alt="${item.name || item.watchTitle || ''}" class="cart-item-img" />
                <div>
                    <span class="cart-item-brand">${item.brand || item.watchBrand || ''}</span>
                    <h5 class="cart-item-title">${item.name || item.watchTitle || 'Luxury Watch'}</h5>
                    <span style="font-size: 11px; color: rgba(244, 239, 224, 0.6);">${formatCurrency(item.unitPrice)} each</span>
                </div>
                <div class="cart-qty-control">
                    <button class="cart-qty-btn btn-qty-dec" data-id="${item.id || item.watchId}" data-qty="${item.quantity - 1}">−</button>
                    <span class="cart-qty-val">${item.quantity}</span>
                    <button class="cart-qty-btn btn-qty-inc" data-id="${item.id || item.watchId}" data-qty="${item.quantity + 1}">+</button>
                </div>
                <div style="display: flex; align-items: center; gap: 12px;">
                    <span class="cart-item-total">${formatCurrency(item.lineTotal || (item.unitPrice * item.quantity))}</span>
                    <button class="cart-item-remove" data-remove-id="${item.id || item.watchId}" title="Remove">✕</button>
                </div>
            </div>
        `).join("");

        // Attach listeners
        elements.cartItemsContainer.querySelectorAll(".btn-qty-dec, .btn-qty-inc").forEach(btn => {
            btn.addEventListener("click", async () => {
                const id = parseInt(btn.getAttribute("data-id"));
                const qty = parseInt(btn.getAttribute("data-qty"));
                try {
                    currentCart = await window.atelierApi.updateCartItem(id, qty);
                    renderCart(currentCart);
                } catch (e) {
                    showToast(e.message || "Failed to update quantity", "error");
                }
            });
        });

        elements.cartItemsContainer.querySelectorAll(".cart-item-remove").forEach(btn => {
            btn.addEventListener("click", async () => {
                const id = parseInt(btn.getAttribute("data-remove-id"));
                try {
                    currentCart = await window.atelierApi.removeFromCart(id);
                    renderCart(currentCart);
                    showToast("Item removed from bag");
                } catch (e) {
                    showToast(e.message || "Failed to remove item", "error");
                }
            });
        });
    }

    async function handleAddToCart(watch, quantity = 1) {
        try {
            currentCart = await window.atelierApi.addToCart(watch, quantity);
            renderCart(currentCart);
            showToast(`Added ${watch.brand || ''} ${watch.name || watch.title} to Shopping Bag`);
            openModal(elements.cartModal);
        } catch (err) {
            showToast(err.message || "Could not add to bag", "error");
        }
    }

    if (elements.btnClearCart) {
        elements.btnClearCart.addEventListener("click", async () => {
            try {
                currentCart = await window.atelierApi.clearCart();
                renderCart(currentCart);
                showToast("Shopping bag cleared");
            } catch (err) {
                showToast(err.message || "Could not clear bag", "error");
            }
        });
    }

    // Checkout Flow
    if (elements.btnProceedCheckout) {
        elements.btnProceedCheckout.addEventListener("click", async () => {
            if (!window.atelierApi.token) {
                showToast("Please sign in or create an account to proceed with secure vault checkout", "info");
                closeModal(elements.cartModal);
                openModal(elements.authModal);
                document.querySelector(".auth-tab[data-tab='login']")?.click();
                return;
            }

            closeModal(elements.cartModal);
            openModal(elements.checkoutModal);
            await setupCheckoutModal();
        });
    }

    async function setupCheckoutModal() {
        try {
            const addresses = await window.atelierApi.getAddresses();
            if (elements.savedAddressesList) {
                if (addresses && addresses.length > 0) {
                    selectedAddressId = addresses[0].id;
                    elements.savedAddressesList.innerHTML = `
                        <div style="font-size: 11px; color: var(--gold); margin-bottom: 6px; text-transform: uppercase; letter-spacing: 0.1em;">Saved Shipping Destinations:</div>
                        ${addresses.map((a, idx) => `
                            <label style="display: flex; align-items: flex-start; gap: 8px; margin-bottom: 8px; font-size: 12px; cursor: pointer;">
                                <input type="radio" name="selected_addr" value="${a.id}" ${idx === 0 ? 'checked' : ''} style="margin-top: 3px;" />
                                <div>
                                    <strong style="color: #fff;">${a.recipientName}</strong> (${a.phone})<br/>
                                    <span style="color: rgba(244, 239, 224, 0.7);">${a.streetAddress}${a.apartmentSuite ? ', ' + a.apartmentSuite : ''}, ${a.city}, ${a.state} ${a.postalCode}</span>
                                </div>
                            </label>
                        `).join("")}
                    `;
                    elements.savedAddressesList.querySelectorAll("input[name='selected_addr']").forEach(radio => {
                        radio.addEventListener("change", (e) => {
                            selectedAddressId = parseInt(e.target.value);
                            updateCheckoutQuote(selectedAddressId);
                        });
                    });
                } else {
                    elements.savedAddressesList.innerHTML = `<p style="font-size: 11px; color: rgba(244, 239, 224, 0.5); margin-bottom: 10px;">Please enter your delivery destination details below:</p>`;
                }
            }

            if (selectedAddressId) {
                await updateCheckoutQuote(selectedAddressId);
            } else if (currentCart) {
                if (elements.checkoutSubtotal) elements.checkoutSubtotal.textContent = formatCurrency(currentCart.subtotal);
                if (elements.checkoutTotal) elements.checkoutTotal.textContent = formatCurrency(currentCart.subtotal);
            }
        } catch (e) {
            console.error("Setup checkout error:", e);
        }
    }

    async function updateCheckoutQuote(addressId) {
        try {
            const quote = await window.atelierApi.getCheckoutQuote(addressId);
            if (elements.checkoutSubtotal) elements.checkoutSubtotal.textContent = formatCurrency(quote.subtotal);
            if (elements.checkoutTotal) elements.checkoutTotal.textContent = formatCurrency(quote.totalAmount);
        } catch (e) {
            console.error("Quote error:", e);
        }
    }

    // Razorpay Payment Action
    if (elements.btnPayRazorpay) {
        elements.btnPayRazorpay.addEventListener("click", async () => {
            elements.btnPayRazorpay.disabled = true;
            elements.btnPayRazorpay.textContent = "Securing Escrow Vault Order...";

            try {
                let addressId = selectedAddressId;

                // If no saved address selected, save from form
                if (!addressId) {
                    const recipientName = document.getElementById("ship-name").value.trim() || "Alexander Thorne";
                    const phone = document.getElementById("ship-phone").value.trim() || "+1-555-019-2834";
                    const street = document.getElementById("ship-street").value.trim() || "742 Evergreen Terrace";
                    const city = document.getElementById("ship-city").value.trim() || "New York";
                    const stateName = document.getElementById("ship-state").value.trim() || "NY";
                    const postal = document.getElementById("ship-postal").value.trim() || "10021";

                    const savedAddr = await window.atelierApi.saveAddress({
                        recipientName,
                        phone,
                        streetAddress: street,
                        apartmentSuite: "Penthouse Suite",
                        city,
                        state: stateName,
                        postalCode: postal,
                        country: "United States",
                        isDefault: true
                    });
                    addressId = savedAddr.id;
                }

                // 1. Create Order on Backend (Atomic Stock Check & Deduction)
                elements.btnPayRazorpay.textContent = "Reserving Inventory...";
                const orderData = await window.atelierApi.createOrder(addressId);
                const orderRef = orderData.orderReference;

                // 2. Initialize Razorpay Payment Gateway Order
                elements.btnPayRazorpay.textContent = "Connecting Razorpay Gateway...";
                const rzpOrder = await window.atelierApi.createRazorpayOrder(orderRef);

                // 3. Simulated/Test Razorpay Payment Verification
                elements.btnPayRazorpay.textContent = "Verifying HMAC-SHA256 Signature...";
                const simulatedPaymentId = "pay_test_" + Date.now();
                
                // Call verification endpoint
                await window.atelierApi.verifyPayment({
                    orderReference: orderRef,
                    razorpayOrderId: rzpOrder.razorpayOrderId,
                    razorpayPaymentId: simulatedPaymentId,
                    razorpaySignature: "simulated_valid_test_signature"
                });

                showToast(`Order ${orderRef} Confirmed! Armored courier tracking assigned.`, "info");
                await window.atelierApi.clearCart();
                await syncCart();

                closeModal(elements.checkoutModal);
                openModal(elements.authModal);
                document.querySelector(".auth-tab[data-tab='orders']")?.click();
            } catch (err) {
                showToast(err.message || "Payment authorization failed", "error");
            } finally {
                elements.btnPayRazorpay.disabled = false;
                elements.btnPayRazorpay.textContent = "Authorize Payment with Razorpay (Test Mode)";
            }
        });
    }

    // Orders History & Tracking
    async function loadUserOrders() {
        if (!elements.ordersList) return;
        elements.ordersList.innerHTML = `<div style="text-align: center; padding: 20px; color: var(--gold); font-size: 11px;">Loading horological acquisitions...</div>`;

        try {
            const orders = await window.atelierApi.getUserOrders();
            renderUserOrders(orders);
        } catch (e) {
            elements.ordersList.innerHTML = `<div style="text-align: center; padding: 20px; color: #e06c75; font-size: 12px;">Failed to load order history.</div>`;
        }
    }

    function renderUserOrders(orders) {
        if (!elements.ordersList) return;
        if (!orders || orders.length === 0) {
            elements.ordersList.innerHTML = `
                <div style="text-align: center; padding: 30px 0;">
                    <p style="font-family: var(--serif); font-size: 16px; margin-bottom: 6px;">No Private Acquisitions Yet</p>
                    <p style="font-size: 11px; color: rgba(244, 239, 224, 0.5);">Your confirmed luxury orders and white-glove shipments will appear here.</p>
                </div>
            `;
            return;
        }

        elements.ordersList.innerHTML = orders.map(ord => `
            <div class="order-card">
                <div class="order-header-row">
                    <div>
                        <span class="order-ref">${ord.orderReference}</span>
                        <div style="font-size: 10px; color: rgba(244, 239, 224, 0.5); margin-top: 2px;">
                            ${new Date(ord.createdAt).toLocaleDateString("en-US", { year: "numeric", month: "short", day: "numeric" })}
                        </div>
                    </div>
                    <div>
                        <span class="order-status-badge status-${ord.status}">${ord.status.replace("_", " ")}</span>
                    </div>
                </div>

                <div style="margin: 12px 0;">
                    ${ord.items ? ord.items.map(it => `
                        <div style="display: flex; justify-content: space-between; font-size: 12px; margin-bottom: 4px;">
                            <span>${it.watchBrand || ''} <strong>${it.watchTitle}</strong> × ${it.quantity}</span>
                            <span style="color: var(--gold);">${formatCurrency(it.lineTotal)}</span>
                        </div>
                    `).join("") : ''}
                </div>

                <div style="display: flex; justify-content: space-between; border-top: 1px solid rgba(244, 239, 224, 0.08); padding-top: 8px; font-size: 13px;">
                    <span style="color: rgba(244, 239, 224, 0.6);">Total Amount</span>
                    <strong style="color: #fff; font-family: var(--serif);">${formatCurrency(ord.totalAmount)}</strong>
                </div>

                ${ord.trackingNumber ? `
                    <div class="order-tracking-box">
                        <div>
                            <span style="color: var(--gold); text-transform: uppercase; letter-spacing: 0.1em; font-size: 9px; display: block;">Insured Armored Dispatch</span>
                            <span style="font-family: monospace; font-size: 12px; color: #fff;">${ord.trackingNumber}</span>
                        </div>
                        <span style="font-size: 10px; color: rgba(244, 239, 224, 0.7);">${ord.shipmentStatus || 'In Transit via Atelier Express'}</span>
                    </div>
                ` : ''}
            </div>
        `).join("");
    }

    if (elements.refreshOrdersBtn) {
        elements.refreshOrdersBtn.addEventListener("click", loadUserOrders);
    }

    // -------------------------------------------------------------------------
    // 6. Connect Main Page Buttons
    // -------------------------------------------------------------------------
    elements.openCatalogBtns.forEach(btn => {
        btn.addEventListener("click", (e) => {
            e.preventDefault();
            openModal(elements.catalogModal);
            if (state.watches.length === 0) fetchWatches();
        });
    });

    elements.openAuthBtns.forEach(btn => {
        btn.addEventListener("click", (e) => {
            e.preventDefault();
            openModal(elements.authModal);
            if (window.atelierApi.token) {
                document.querySelector(".auth-tab[data-tab='wishlist']")?.click();
            } else {
                document.querySelector(".auth-tab[data-tab='login']")?.click();
            }
        });
    });

    document.querySelectorAll("[data-open-cart]").forEach(btn => {
        btn.addEventListener("click", (e) => {
            e.preventDefault();
            openModal(elements.cartModal);
            syncCart();
        });
    });

    // Auth Tabs Switching
    elements.authTabs.forEach(tab => {
        tab.addEventListener("click", () => {
            elements.authTabs.forEach(t => t.classList.remove("active"));
            tab.classList.add("active");
            const target = tab.getAttribute("data-tab");

            if (elements.loginTabContent) elements.loginTabContent.style.display = target === "login" ? "block" : "none";
            if (elements.registerTabContent) elements.registerTabContent.style.display = target === "register" ? "block" : "none";
            if (elements.wishlistTabContent) elements.wishlistTabContent.style.display = target === "wishlist" ? "block" : "none";
            if (elements.ordersTabContent) {
                elements.ordersTabContent.style.display = target === "orders" ? "block" : "none";
                if (target === "orders") loadUserOrders();
            }
        });
    });

    // Init Lifecycle
    loadCategories();
    fetchWatches();
    updateCompareTray();
    initUserSession();
    syncCart();
})();

