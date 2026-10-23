package dev.atelierwatch.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import dev.atelierwatch.auth.model.User;
import dev.atelierwatch.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Verifies Supabase access tokens with Auth and maps their stable subject to the existing local user row. */
@Service
public class SupabaseIdentityService {

    private static final long CACHE_TTL_MILLIS = 45_000;
    private static final int CACHE_MAX_ENTRIES = 4_000;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RestClient restClient;
    private final Map<String, CachedIdentity> identityCache = new ConcurrentHashMap<>();

    @Value("${app.supabase.url:}")
    private String projectUrl;

    @Value("${app.supabase.publishable-key:}")
    private String publishableKey;

    public SupabaseIdentityService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            RestClient.Builder restClientBuilder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        var httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(3));
        this.restClient = restClientBuilder.requestFactory(requestFactory).build();
    }

    public boolean isConfigured() {
        return projectUrl != null && !projectUrl.isBlank()
                && publishableKey != null && !publishableKey.isBlank();
    }

    public User authenticate(String accessToken) {
        if (!isConfigured()) throw new BadCredentialsException("Supabase sign-in is not configured");

        long now = System.currentTimeMillis();
        String cacheKey = tokenFingerprint(accessToken);
        CachedIdentity identity = identityCache.get(cacheKey);
        if (identity == null || identity.expiresAtMillis() <= now) {
            identity = verifyWithAuth(accessToken);
            if (identityCache.size() >= CACHE_MAX_ENTRIES) {
                identityCache.entrySet().removeIf(entry -> entry.getValue().expiresAtMillis() <= now);
                if (identityCache.size() >= CACHE_MAX_ENTRIES) identityCache.clear();
            }
            identityCache.put(cacheKey, identity);
        }

        return mapToLocalUser(identity);
    }

    private CachedIdentity verifyWithAuth(String accessToken) {
        try {
            String baseUrl = projectUrl.replaceAll("/+$", "");
            JsonNode profile = restClient.get()
                    .uri(baseUrl + "/auth/v1/user")
                    .header("apikey", publishableKey)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(JsonNode.class);

            if (profile == null) throw new BadCredentialsException("Supabase returned no user profile");
            String subject = profile.path("id").asText("");
            String email = profile.path("email").asText("").trim().toLowerCase();
            String confirmedAt = profile.path("email_confirmed_at").asText("");
            if (subject.isBlank() || email.isBlank() || !email.contains("@") || confirmedAt.isBlank()) {
                throw new BadCredentialsException("A verified Supabase email account is required");
            }

            JsonNode metadata = profile.path("user_metadata");
            String displayName = firstNonBlank(
                    metadata.path("display_name").asText(""),
                    metadata.path("full_name").asText(""),
                    metadata.path("name").asText(""),
                    email.substring(0, email.indexOf('@'))
            );
            return new CachedIdentity(subject, email, displayName, System.currentTimeMillis() + CACHE_TTL_MILLIS);
        } catch (BadCredentialsException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadCredentialsException("Supabase access token could not be verified", ex);
        }
    }

    private User mapToLocalUser(CachedIdentity identity) {
        final String subject;
        try {
            subject = UUID.fromString(identity.subject()).toString();
        } catch (IllegalArgumentException ex) {
            throw new BadCredentialsException("Supabase returned an invalid user identifier");
        }

        return userRepository.findBySupabaseUserId(subject).orElseGet(() -> {
            User local = userRepository.findByEmail(identity.email()).orElse(null);
            if (local != null) {
                if (local.getSupabaseUserId() != null && !local.getSupabaseUserId().equals(subject)) {
                    throw new BadCredentialsException("This email is linked to a different Supabase account");
                }
                local.setSupabaseUserId(subject);
                return userRepository.save(local);
            }

            // Supabase owns this password. A random hash retains the existing non-null
            // schema while making local-password authentication unavailable to this account.
            byte[] random = new byte[32];
            SECURE_RANDOM.nextBytes(random);
            String providerOnlyPassword = Base64.getEncoder().encodeToString(random);
            User created = new User(identity.displayName(), identity.email(),
                    passwordEncoder.encode(providerOnlyPassword), "ROLE_USER");
            created.setSupabaseUserId(subject);
            return userRepository.save(created);
        });
    }

    private static String tokenFingerprint(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value.trim();
        return "Collector";
    }

    private record CachedIdentity(String subject, String email, String displayName, long expiresAtMillis) {}
}
