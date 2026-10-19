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
        wishlistGrid: document.getElementById("wishlist-grid")
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
                            <button class="btn-primary" id="detail-wishlist-btn">
                                ${isSaved ? 'In Your Wishlist' : 'Add to Wishlist'}
                            </button>
                            <button class="btn-secondary" id="detail-compare-btn">
                                ${isCompared ? 'Remove from Compare' : 'Add to Compare'}
                            </button>
                        </div>
                    </div>
                </div>
            `;

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
    // 5. Connect Main Page Buttons
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

    // Init Lifecycle
    loadCategories();
    fetchWatches();
    updateCompareTray();
    initUserSession();
})();
