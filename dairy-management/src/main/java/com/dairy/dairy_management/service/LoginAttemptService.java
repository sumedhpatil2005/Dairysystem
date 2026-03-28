package com.dairy.dairy_management.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory brute-force protection for the login endpoint.
 *
 * Rules:
 *   - After 5 consecutive failed login attempts from the same IP,
 *     that IP is blocked for 15 minutes.
 *   - A successful login resets the counter immediately.
 *   - The block automatically lifts after 15 minutes (no manual admin action needed).
 *
 * This is an in-memory solution — counters reset on app restart.
 * Sufficient for a single-instance dairy business deployment.
 * For multi-instance deployments, replace with Redis-backed counters.
 */
@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int BLOCK_DURATION_MINUTES = 15;

    private final ConcurrentHashMap<String, AttemptRecord> attempts = new ConcurrentHashMap<>();

    /** Returns true if this IP should be blocked from attempting login. */
    public boolean isBlocked(String ip) {
        AttemptRecord record = attempts.get(ip);
        if (record == null) return false;

        // Auto-lift block after BLOCK_DURATION_MINUTES
        if (record.blockedUntil != null && LocalDateTime.now().isAfter(record.blockedUntil)) {
            attempts.remove(ip);
            return false;
        }

        return record.blockedUntil != null && LocalDateTime.now().isBefore(record.blockedUntil);
    }

    /** Call this when a login attempt fails. */
    public void recordFailure(String ip) {
        AttemptRecord record = attempts.computeIfAbsent(ip, k -> new AttemptRecord());
        record.failureCount++;

        if (record.failureCount >= MAX_ATTEMPTS) {
            record.blockedUntil = LocalDateTime.now().plusMinutes(BLOCK_DURATION_MINUTES);
        }
    }

    /** Call this when a login succeeds — resets the counter. */
    public void recordSuccess(String ip) {
        attempts.remove(ip);
    }

    /** Returns how many minutes remain on the block (for error messages). */
    public long minutesUntilUnblocked(String ip) {
        AttemptRecord record = attempts.get(ip);
        if (record == null || record.blockedUntil == null) return 0;
        return java.time.Duration.between(LocalDateTime.now(), record.blockedUntil).toMinutes() + 1;
    }

    private static class AttemptRecord {
        int failureCount = 0;
        LocalDateTime blockedUntil = null;
    }
}
