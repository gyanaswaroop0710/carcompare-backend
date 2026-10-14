package com.carcompare;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    public record RegisterRequest(String name, String email, String password) {}
    public record LoginRequest(String email, String password) {}

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Duration TOKEN_TTL = Duration.ofDays(7);
    private static final SecureRandom RNG = new SecureRandom();

    private final AppUserRepository users;
    private final AuthTokenRepository tokens;
    private final PasswordEncoder encoder;

    public AuthController(AppUserRepository users, AuthTokenRepository tokens, PasswordEncoder encoder) {
        this.users = users; this.tokens = tokens; this.encoder = encoder;
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody RegisterRequest r) {
        String name = r.name() == null ? "" : r.name().trim();
        String email = normalize(r.email());
        String pw = r.password() == null ? "" : r.password();
        if (name.isEmpty() || name.length() > 80) throw bad("Please enter your name");
        if (!EMAIL.matcher(email).matches() || email.length() > 150) throw bad("Please enter a valid email");
        if (pw.length() < 6) throw bad("Password must be at least 6 characters");
        if (pw.length() > 72) throw bad("Password must be at most 72 characters");
        if (users.existsByEmail(email)) throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        AppUser u;
        try {
            u = users.save(new AppUser(name, email, encoder.encode(pw)));
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        return session(u);
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest r) {
        AppUser u = users.findByEmail(normalize(r.email()))
                .filter(x -> r.password() != null && encoder.matches(r.password(), x.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return session(u);
    }

    @PostMapping("/logout")
    public void logout(@RequestHeader("Authorization") String header) {
        if (header.startsWith("Bearer ")) tokens.deleteById(sha256(header.substring(7).trim()));
    }

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal AppUser u) { return userJson(u); }

    // ── helpers ──────────────────────────────────────────────────────────
    private Map<String, Object> session(AppUser u) {
        byte[] raw = new byte[32];
        RNG.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        tokens.save(new AuthToken(sha256(token), u.getId(), Instant.now().plus(TOKEN_TTL)));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("token", token);
        m.put("user", userJson(u));
        return m;
    }

    private static Map<String, Object> userJson(AppUser u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId()); m.put("name", u.getName()); m.put("email", u.getEmail());
        return m;
    }

    private static String normalize(String email) { return email == null ? "" : email.trim().toLowerCase(); }
    private static ResponseStatusException bad(String msg) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg); }

    static String sha256(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
}
