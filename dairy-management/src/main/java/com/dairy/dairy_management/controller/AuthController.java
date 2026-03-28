package com.dairy.dairy_management.controller;

import com.dairy.dairy_management.dto.AuthResponse;
import com.dairy.dairy_management.dto.LoginRequest;
import com.dairy.dairy_management.dto.LogoutRequest;
import com.dairy.dairy_management.dto.RefreshTokenRequest;
import com.dairy.dairy_management.dto.RegisterPartnerRequest;
import com.dairy.dairy_management.dto.RegisterRequest;
import com.dairy.dairy_management.service.AuthService;
import com.dairy.dairy_management.service.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final LoginAttemptService loginAttemptService;

    public AuthController(AuthService authService, LoginAttemptService loginAttemptService) {
        this.authService = authService;
        this.loginAttemptService = loginAttemptService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    /**
     * Login with rate limiting — max 5 failed attempts per IP before a 15-minute block.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) {
        String ip = resolveClientIp(httpRequest);

        if (loginAttemptService.isBlocked(ip)) {
            long minutes = loginAttemptService.minutesUntilUnblocked(ip);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed login attempts. Try again in " + minutes + " minute(s).");
        }

        try {
            AuthResponse response = authService.login(request);
            loginAttemptService.recordSuccess(ip);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            loginAttemptService.recordFailure(ip);
            // Re-throw so the global exception handler returns the appropriate HTTP status
            throw e;
        }
    }

    /**
     * Register a delivery partner — creates User (DELIVERY_PARTNER role) + DeliveryPartner profile in one step.
     */
    @PostMapping("/register-partner")
    public ResponseEntity<AuthResponse> registerPartner(@Valid @RequestBody RegisterPartnerRequest request) {
        return ResponseEntity.ok(authService.registerPartner(request));
    }

    /**
     * Exchange a valid refresh token for a new access + refresh token pair.
     *
     * Token rotation: the submitted refresh token is immediately invalidated
     * and a brand-new pair is returned. Each refresh token can only be used once.
     *
     * The Flutter app should call this when it receives a 401 response,
     * before prompting the user to log in again.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    /**
     * Invalidates the supplied refresh token so it can no longer be used.
     *
     * Access tokens issued before logout remain valid until they expire
     * (stateless by design — keep jwt.expiration short, e.g. 15 minutes).
     *
     * The Flutter app should call this on user-initiated logout and
     * discard both tokens from local storage.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    /**
     * Resolves the real client IP, honoring X-Forwarded-For when behind a proxy/load balancer.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
