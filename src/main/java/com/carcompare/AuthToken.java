package com.carcompare;

import jakarta.persistence.*;
import java.time.Instant;

/** A login session. Only the SHA-256 hash of the token is stored, never the token itself. */
@Entity
@Table(name = "auth_token")
public class AuthToken {
    @Id @Column(name = "token_hash", length = 64) private String tokenHash;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;

    public AuthToken() {}
    public AuthToken(String tokenHash, Long userId, Instant expiresAt) {
        this.tokenHash = tokenHash; this.userId = userId; this.expiresAt = expiresAt;
    }
    public String getTokenHash() { return tokenHash; }
    public Long getUserId() { return userId; }
    public Instant getExpiresAt() { return expiresAt; }
}
