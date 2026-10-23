/**
 * Atelier Watch API Client
 * Connects vanilla frontend to Spring Boot 3.3 REST API
 */
const API_BASE = window.ATELIER_API_URL || 
    (window.location.hostname === "localhost" || window.location.hostname === "127.0.0.1" 
        ? "http://localhost:8080/api/v1" 
        : "/api/v1");

class AtelierApiClient {
    constructor() {
        this.token = localStorage.getItem("atelier_token") || null;
        this.user = JSON.parse(localStorage.getItem("atelier_user") || "null");
        this.supabase = null;
        this.authStartupError = null;
        const config = window.DIAL_FORM_SUPABASE_CONFIG || {};
        this.supabaseConfig = config;
        this.authReady = this.initializeSupabaseSession().catch(error => {
            this.authStartupError = error.message || "Could not restore the Supabase session.";
        });
    }

    async initializeSupabaseSession() {
        const hasSupabaseConfig = Boolean(this.supabaseConfig.url || this.supabaseConfig.publishableKey);
        if (!hasSupabaseConfig) return;
        if (!this.supabaseConfig.url || !this.supabaseConfig.publishableKey) {
            this.authStartupError = "Supabase setup is incomplete. Check the project URL and publishable key.";
            return;
        }
        await this.loadSupabaseSdk();
        this.supabase = window.supabase.createClient(this.supabaseConfig.url, this.supabaseConfig.publishableKey, {
            auth: { autoRefreshToken: true, persistSession: true, detectSessionInUrl: true }
        });
        this.supabase.auth.onAuthStateChange((_event, session) => this.applySupabaseSession(session));
        const { data, error } = await this.supabase.auth.getSession();
        if (error) throw error;
        this.applySupabaseSession(data.session);
    }

    loadSupabaseSdk() {
        if (window.supabase?.createClient) return Promise.resolve();
        return new Promise((resolve, reject) => {
            const script = document.createElement("script");
            script.src = "https://cdn.jsdelivr.net/npm/@supabase/supabase-js@2";
            script.async = true;
            script.onload = () => window.supabase?.createClient
                ? resolve()
                : reject(new Error("Supabase client library did not load."));
            script.onerror = () => reject(new Error("Could not load Supabase authentication. Check your network connection."));
            document.head.appendChild(script);
        });
    }

    applySupabaseSession(session) {
        this.token = session?.access_token || null;
        if (!session) {
            this.user = null;
            localStorage.removeItem("atelier_token");
            localStorage.removeItem("atelier_user");
            return;
        }
        const sessionEmail = session.user?.email?.toLowerCase();
        if (this.user?.email && sessionEmail && this.user.email.toLowerCase() !== sessionEmail) this.user = null;
        localStorage.setItem("atelier_token", this.token);
        if (this.user) localStorage.setItem("atelier_user", JSON.stringify(this.user));
    }

    setAuth(authData) {
        if (authData) {
            this.token = authData.token;
            this.user = authData.user;
            localStorage.setItem("atelier_token", this.token);
            localStorage.setItem("atelier_user", JSON.stringify(this.user));
        } else {
            this.token = null;
            this.user = null;
            localStorage.removeItem("atelier_token");
            localStorage.removeItem("atelier_user");
        }
    }

    getHeaders() {
        const headers = {
            "Content-Type": "application/json",
            "Accept": "application/json"
        };
        if (this.token) {
            headers["Authorization"] = `Bearer ${this.token}`;
        }
        return headers;
    }

    async request(endpoint, options = {}) {
        await this.authReady;
        if (this.authStartupError) throw new Error(this.authStartupError);
        if (this.supabase) {
            const { data, error } = await this.supabase.auth.getSession();
            if (error) throw error;
            this.applySupabaseSession(data.session);
        }
        const url = `${API_BASE}${endpoint}`;
        const config = {
            ...options,
            headers: {
                ...this.getHeaders(),
                ...(options.headers || {})
            }
        };

        try {
            const response = await fetch(url, config);
            const json = await response.json().catch(() => null);

            if (!response.ok) {
                const error = new Error((json && json.message) || `Request failed with status ${response.status}`);
                error.status = response.status;
                error.validationErrors = json && json.data && typeof json.data === "object" ? json.data : null;
                throw error;
            }
            return json.data;
        } catch (error) {
            console.error(`API Error [${endpoint}]:`, error);
            throw error;
        }
    }

    // Watches
    async getWatches(params = {}) {
        const query = new URLSearchParams();
        if (params.search) query.append("search", params.search);
        if (params.brand) query.append("brand", params.brand);
        if (params.collection) query.append("collection", params.collection);
        if (params.category) query.append("category", params.category);
        if (params.movement) query.append("movement", params.movement);
        if (params.material) query.append("material", params.material);
        if (params.minPrice) query.append("minPrice", params.minPrice);
        if (params.maxPrice) query.append("maxPrice", params.maxPrice);
        if (params.sort) query.append("sort", params.sort);
        if (params.page !== undefined) query.append("page", params.page);
        if (params.size !== undefined) query.append("size", params.size);

        const qs = query.toString();
        return this.request(`/watches${qs ? "?" + qs : ""}`);
    }

    async getFeaturedWatches() {
        return this.request("/watches/featured");
    }

    async getWatchBySlug(slug) {
        return this.request(`/watches/${encodeURIComponent(slug)}`);
    }

    async getCategories() {
        return this.request("/categories");
    }

    async getBrands() {
        return this.request("/watches/brands");
    }

    async getCollections() {
        return this.request("/watches/collections");
    }

    async getCaseMaterials() {
        return this.request("/watches/materials");
    }

    async getMovementTypes() {
        return this.request("/watches/movements");
    }

    // Auth
    async login(email, password) {
        await this.authReady;
        if (this.authStartupError) throw new Error(this.authStartupError);
        if (this.supabase) {
            const { data, error } = await this.supabase.auth.signInWithPassword({ email, password });
            if (error) throw error;
            if (!data.session) throw new Error("Sign-in did not return a session. Confirm your email address and try again.");
            this.applySupabaseSession(data.session);
            this.user = await this.request("/users/me");
            localStorage.setItem("atelier_user", JSON.stringify(this.user));
            return { token: this.token, user: this.user };
        }
        const data = await this.request("/auth/login", {
            method: "POST",
            body: JSON.stringify({ email, password })
        });
        this.setAuth(data);
        return data;
    }

    async register(displayName, email, password) {
        await this.authReady;
        if (this.authStartupError) throw new Error(this.authStartupError);
        if (this.supabase) {
            const { data, error } = await this.supabase.auth.signUp({
                email,
                password,
                options: {
                    emailRedirectTo: `${window.location.origin}/confirm-email`,
                    data: { display_name: displayName }
                }
            });
            if (error) throw error;
            if (!data.session) return { confirmationRequired: true, user: { displayName, email } };
            this.applySupabaseSession(data.session);
            this.user = await this.request("/users/me");
            localStorage.setItem("atelier_user", JSON.stringify(this.user));
            return { token: this.token, user: this.user };
        }
        const data = await this.request("/auth/register", {
            method: "POST",
            body: JSON.stringify({ displayName, email, password })
        });
        this.setAuth(data);
        return data;
    }

    async confirmSignup(email, token) {
        await this.authReady;
        if (this.authStartupError) throw new Error(this.authStartupError);
        if (!this.supabase) throw new Error("Email confirmation is not configured for this preview.");

        const { data, error } = await this.supabase.auth.verifyOtp({
            email: email.trim(),
            token: token.trim(),
            type: "email"
        });
        if (error) throw error;
        if (!data.session) throw new Error("The code was accepted, but no sign-in session was returned. Please sign in.");

        this.applySupabaseSession(data.session);
        this.user = await this.request("/users/me");
        localStorage.setItem("atelier_user", JSON.stringify(this.user));
        return { token: this.token, user: this.user };
    }

    async resendSignupConfirmation(email) {
        await this.authReady;
        if (this.authStartupError) throw new Error(this.authStartupError);
        if (!this.supabase) throw new Error("Email confirmation is not configured for this preview.");

        const { error } = await this.supabase.auth.resend({
            type: "signup",
            email: email.trim(),
            options: { emailRedirectTo: `${window.location.origin}/confirm-email` }
        });
        if (error) throw error;
    }

    async logout() {
        if (this.supabase) {
            try { await this.supabase.auth.signOut(); }
            catch (error) { console.warn("Supabase sign-out failed:", error); }
        }
        this.setAuth(null);
    }

    async getCurrentUser() {
        if (!this.token) return null;
        try {
            return await this.request("/users/me");
        } catch {
            await this.logout();
            return null;
        }
    }

    // Wishlist
    async getWishlist() {
        if (!this.token) return [];
        return this.request("/me/wishlist");
    }

    async addToWishlist(watchId) {
        return this.request(`/me/wishlist/${watchId}`, { method: "PUT" });
    }

    async removeFromWishlist(watchId) {
        return this.request(`/me/wishlist/${watchId}`, { method: "DELETE" });
    }

    // E-Commerce: Cart
    async getCart() {
        if (!this.token) {
            // LocalStorage fallback for guests
            const guestCart = JSON.parse(localStorage.getItem("atelier_guest_cart") || "[]");
            const items = guestCart.map(item => {
                const purchasable = item.purchasable === true && Number(item.price) > 0;
                return {
                id: item.watchId,
                watchId: item.watchId,
                watchTitle: item.title,
                watchBrand: item.brand,
                watchImageUrl: item.imageUrl,
                referenceNumber: item.referenceNumber,
                unitPrice: purchasable ? item.price : null,
                quantity: item.quantity,
                lineTotal: purchasable ? (item.price * item.quantity).toFixed(2) : null,
                purchasable
                };
            });
            const checkoutAvailable = items.length > 0 && items.every(item => item.purchasable);
            const subtotal = checkoutAvailable ? items.reduce((sum, it) => sum + (it.unitPrice * it.quantity), 0).toFixed(2) : null;
            return {
                id: null,
                items: items,
                subtotal,
                totalItems: items.reduce((sum, it) => sum + it.quantity, 0),
                checkoutAvailable
            };
        }
        return this.request("/cart");
    }

    async addToCart(watch, quantity = 1) {
        if (!watch || watch.purchasable !== true || Number(watch.price) <= 0 || Number(watch.stockQuantity) <= 0) {
            throw new Error("This Rolex has no verified price and available inventory yet.");
        }
        if (!this.token) {
            const guestCart = JSON.parse(localStorage.getItem("atelier_guest_cart") || "[]");
            const existing = guestCart.find(i => i.watchId === watch.id);
            if (existing) {
                existing.quantity += quantity;
            } else {
                guestCart.push({
                    watchId: watch.id,
                    title: watch.name,
                    brand: watch.brand,
                    imageUrl: watch.imageUrl,
                    referenceNumber: watch.referenceNumber,
                    price: watch.price,
                    quantity: quantity,
                    purchasable: true
                });
            }
            localStorage.setItem("atelier_guest_cart", JSON.stringify(guestCart));
            return this.getCart();
        }
        return this.request("/cart/items", {
            method: "POST",
            body: JSON.stringify({ watchId: watch.id, quantity })
        });
    }

    async updateCartItem(itemId, quantity) {
        if (!this.token) {
            let guestCart = JSON.parse(localStorage.getItem("atelier_guest_cart") || "[]");
            if (quantity <= 0) {
                guestCart = guestCart.filter(i => i.watchId !== itemId);
            } else {
                const item = guestCart.find(i => i.watchId === itemId);
                if (item) item.quantity = quantity;
            }
            localStorage.setItem("atelier_guest_cart", JSON.stringify(guestCart));
            return this.getCart();
        }
        return this.request(`/cart/items/${itemId}?quantity=${encodeURIComponent(quantity)}`, {
            method: "PATCH"
        });
    }

    async removeFromCart(itemId) {
        if (!this.token) {
            let guestCart = JSON.parse(localStorage.getItem("atelier_guest_cart") || "[]");
            guestCart = guestCart.filter(i => i.watchId !== itemId);
            localStorage.setItem("atelier_guest_cart", JSON.stringify(guestCart));
            return this.getCart();
        }
        return this.request(`/cart/items/${itemId}`, {
            method: "DELETE"
        });
    }

    async clearCart() {
        if (!this.token) {
            localStorage.removeItem("atelier_guest_cart");
            return { items: [], subtotal: 0, totalItems: 0 };
        }
        return this.request("/cart", { method: "DELETE" });
    }

    async mergeGuestCart() {
        const guestCart = JSON.parse(localStorage.getItem("atelier_guest_cart") || "[]");
        if (!this.token || guestCart.length === 0) return this.getCart();
        const cart = await this.request("/cart/merge", {
            method: "POST",
            body: JSON.stringify(guestCart.map(item => ({ watchId: item.watchId, quantity: item.quantity })))
        });
        localStorage.removeItem("atelier_guest_cart");
        return cart;
    }

    // E-Commerce: Addresses
    async getAddresses() {
        if (!this.token) return [];
        return this.request("/me/addresses");
    }

    async saveAddress(addressData) {
        return this.request("/me/addresses", {
            method: "POST",
            body: JSON.stringify(addressData)
        });
    }

    // E-Commerce: Checkout & Orders
    async getCheckoutQuote(addressId) {
        return this.request(`/checkout/quote?addressId=${addressId}`);
    }

    async createOrder(shippingAddressId) {
        return this.request("/checkout/orders", {
            method: "POST",
            body: JSON.stringify({ shippingAddressId })
        });
    }

    async createRazorpayOrder(orderReference) {
        return this.request(`/payments/razorpay/create-order?orderReference=${encodeURIComponent(orderReference)}`, {
            method: "POST"
        });
    }

    async verifyPayment(verificationData) {
        return this.request("/payments/razorpay/verify", {
            method: "POST",
            body: JSON.stringify(verificationData)
        });
    }

    async getTestPaymentSignature(orderReference, razorpayPaymentId) {
        const query = new URLSearchParams({ orderReference, razorpayPaymentId });
        return this.request(`/payments/razorpay/simulate-signature?${query.toString()}`);
    }

    async getUserOrders() {
        if (!this.token) return [];
        return this.request("/me/orders");
    }

    async getOrderByReference(orderReference) {
        return this.request(`/me/orders/${encodeURIComponent(orderReference)}`);
    }

    async getAdminOrders(page = 0, size = 20) {
        return this.request(`/admin/orders?page=${page}&size=${size}`);
    }

    async getAdminWatches(page = 0, size = 100) {
        return this.request(`/admin/watches?page=${page}&size=${size}`);
    }

    async getAdminCategories() {
        return this.request("/admin/categories");
    }

    async createAdminCategory(category) {
        return this.request("/admin/categories", { method: "POST", body: JSON.stringify(category) });
    }

    async updateAdminCategory(categoryId, category) {
        return this.request(`/admin/categories/${encodeURIComponent(categoryId)}`, { method: "PATCH", body: JSON.stringify(category) });
    }

    async deleteAdminCategory(categoryId) {
        return this.request(`/admin/categories/${encodeURIComponent(categoryId)}`, { method: "DELETE" });
    }

    async updateAdminOrderStatus(orderId, status) {
        return this.request(`/admin/orders/${encodeURIComponent(orderId)}/status?status=${encodeURIComponent(status)}`, {
            method: "PATCH"
        });
    }

    async updateAdminShipment(orderId, status) {
        return this.request(`/admin/orders/${encodeURIComponent(orderId)}/shipment?status=${encodeURIComponent(status)}`, {
            method: "PATCH"
        });
    }

    async createAdminWatch(watchData) {
        return this.request("/admin/watches", { method: "POST", body: JSON.stringify(watchData) });
    }

    async updateAdminWatch(watchId, watchData) {
        return this.request(`/admin/watches/${encodeURIComponent(watchId)}`, { method: "PATCH", body: JSON.stringify(watchData) });
    }

    async archiveAdminWatch(watchId) {
        return this.request(`/admin/watches/${encodeURIComponent(watchId)}`, { method: "DELETE" });
    }
}

window.atelierApi = new AtelierApiClient();

