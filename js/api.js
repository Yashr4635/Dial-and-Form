/**
 * Atelier Watch API Client
 * Connects vanilla frontend to Spring Boot 3.3 REST API
 */
const API_BASE = "http://localhost:8080/api/v1";

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
}

window.atelierApi = new AtelierApiClient();
