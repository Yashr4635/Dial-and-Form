package dev.atelierwatch.auth.security;

import dev.atelierwatch.auth.repository.UserRepository;
import dev.atelierwatch.auth.service.SupabaseIdentityService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final SupabaseIdentityService supabaseIdentityService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository,
            SupabaseIdentityService supabaseIdentityService
    ) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.supabaseIdentityService = supabaseIdentityService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        try {
            userEmail = jwtService.extractUsername(jwt);
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                var userOpt = userRepository.findByEmail(userEmail);
                if (userOpt.isPresent() && jwtService.isTokenValid(jwt, userEmail)) {
                    var user = userOpt.get();
                    var authorities = List.of(new SimpleGrantedAuthority(user.getRole()));
                    var authToken = new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            authorities
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception ignored) {
            // Supabase access tokens use the provider's signing keys, not this app's local JWT key.
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null && supabaseIdentityService.isConfigured()) {
            try {
                var user = supabaseIdentityService.authenticate(jwt);
                var authorities = List.of(new SimpleGrantedAuthority(user.getRole()));
                var authToken = new UsernamePasswordAuthenticationToken(user, null, authorities);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } catch (Exception ignored) {
                // Invalid, expired, unverified, or unlinked provider tokens stay unauthenticated.
            }
        }

        filterChain.doFilter(request, response);
    }
}
