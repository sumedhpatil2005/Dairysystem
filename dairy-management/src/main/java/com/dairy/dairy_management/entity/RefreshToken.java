package com.dairy.dairy_management.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Persisted refresh token record.
 *
 * We store the SHA-256 hash of the token (not the plaintext) so that even
 * if the database is compromised, raw tokens cannot be replayed.
 *
 * Lifecycle:
 *   - Created on login / register
 *   - Rotated on every /auth/refresh (old row deleted, new row inserted)
 *   - Deleted on /auth/logout
 *   - Expired rows cleaned up weekly by ScheduledJobService
 */
@Data
@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_rt_token_hash", columnList = "tokenHash", unique = true),
        @Index(name = "idx_rt_username",   columnList = "username")
})
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** SHA-256 hex digest of the actual refresh token JWT. */
    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false, length = 80)
    private String username;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
