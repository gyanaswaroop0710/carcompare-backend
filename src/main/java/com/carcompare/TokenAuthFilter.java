package com.carcompare;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/** Reads "Authorization: Bearer <token>", looks it up and puts the AppUser into the security context. */
public class TokenAuthFilter extends OncePerRequestFilter {
    private final AuthTokenRepository tokens;
    private final AppUserRepository users;

    public TokenAuthFilter(AuthTokenRepository tokens, AppUserRepository users) {
        this.tokens = tokens; this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String h = req.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ") && SecurityContextHolder.getContext().getAuthentication() == null) {
            String hash = AuthController.sha256(h.substring(7).trim());
            tokens.findById(hash)
                    .filter(t -> t.getExpiresAt().isAfter(Instant.now()))
                    .flatMap(t -> users.findById(t.getUserId()))
                    .ifPresent(u -> SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(u, null, List.of(new SimpleGrantedAuthority("ROLE_USER")))));
        }
        chain.doFilter(req, res);
    }
}
