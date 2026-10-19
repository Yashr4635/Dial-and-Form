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
                const message = (json && json.message) || `Request failed with status ${response.status}`;
                throw new Error(message);
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

    async compareWatches(ids) {
        return this.request(`/watches/compare?ids=${ids.join(",")}`);
    }

    async getCategories() {
        return this.request("/categories");
    }

    async getBrands() {
        return this.request("/watches/brands");
    }

    // Auth
    async login(email, password) {
        const data = await this.request("/auth/login", {
            method: "POST",
            body: JSON.stringify({ email, password })
        });
        this.setAuth(data);
        return data;
    }

    async register(displayName, email, password) {
        const data = await this.request("/auth/register", {
            method: "POST",
            body: JSON.stringify({ displayName, email, password })
        });
        this.setAuth(data);
        return data;
    }

    logout() {
        this.setAuth(null);
    }

    async getCurrentUser() {
        if (!this.token) return null;
        try {
            return await this.request("/users/me");
        } catch {
            this.logout();
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
            const items = guestCart.map(item => ({
                id: item.watchId,
                watchId: item.watchId,
                watchTitle: item.title,
                watchBrand: item.brand,
                watchImageUrl: item.imageUrl,
                unitPrice: item.price,
                quantity: item.quantity,
                lineTotal: (item.price * item.quantity).toFixed(2)
            }));
            const subtotal = items.reduce((sum, it) => sum + (it.unitPrice * it.quantity), 0);
            return {
                id: null,
                items: items,
                subtotal: subtotal.toFixed(2),
                totalItems: items.reduce((sum, it) => sum + it.quantity, 0)
            };
        }
        return this.request("/cart");
    }

    async addToCart(watch, quantity = 1) {
        if (!this.token) {
            const guestCart = JSON.parse(localStorage.getItem("atelier_guest_cart") || "[]");
            const existing = guestCart.find(i => i.watchId === watch.id);
            if (existing) {
                existing.quantity += quantity;
            } else {
                guestCart.push({
                    watchId: watch.id,
                    title: watch.title,
                    brand: watch.brand,
                    imageUrl: watch.imageUrl,
                    price: watch.retailPrice || watch.price,
                    quantity: quantity
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
        return this.request(`/cart/items/${itemId}`, {
            method: "PUT",
            body: JSON.stringify({ quantity })
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

    async createOrder(addressId) {
        return this.request("/checkout/orders", {
            method: "POST",
            body: JSON.stringify({ addressId })
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

    async updateAdminOrderStatus(orderReference, status) {
        return this.request(`/admin/orders/${encodeURIComponent(orderReference)}/status?status=${encodeURIComponent(status)}`, {
            method: "PUT"
        });
    }
}

window.atelierApi = new AtelierApiClient();

