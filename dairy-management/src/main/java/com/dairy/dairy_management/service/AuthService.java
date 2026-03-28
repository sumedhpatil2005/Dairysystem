package com.dairy.dairy_management.service;

import com.dairy.dairy_management.config.JwtUtil;
import com.dairy.dairy_management.dto.AuthResponse;
import com.dairy.dairy_management.dto.LoginRequest;
import com.dairy.dairy_management.dto.LogoutRequest;
import com.dairy.dairy_management.dto.RefreshTokenRequest;
import com.dairy.dairy_management.dto.RegisterPartnerRequest;
import com.dairy.dairy_management.dto.RegisterRequest;
import com.dairy.dairy_management.entity.DeliveryPartner;
import com.dairy.dairy_management.entity.RefreshToken;
import com.dairy.dairy_management.entity.Role;
import com.dairy.dairy_management.entity.User;
import com.dairy.dairy_management.exception.ConflictException;
import com.dairy.dairy_management.exception.NotFoundException;
import com.dairy.dairy_management.repository.DeliveryPartnerRepository;
import com.dairy.dairy_management.repository.RefreshTokenRepository;
import com.dairy.dairy_management.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

@Service
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final DeliveryPartnerRepository partnerRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
                       DeliveryPartnerRepository partnerRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.partnerRepository = partnerRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        userRepository.save(user);

        return buildAuthResponse(user);
    }

    /**
     * Registers a delivery partner user + creates their profile in one step.
     */
    @Transactional
    public AuthResponse registerPartner(RegisterPartnerRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        if (partnerRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Phone number already in use");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.DELIVERY_PARTNER);
        userRepository.save(user);

        DeliveryPartner partner = new DeliveryPartner();
        partner.setUser(user);
        partner.setName(request.getName());
        partner.setPhone(request.getPhone());
        partnerRepository.save(partner);

        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        // Block login for deactivated delivery partners
        if (user.getRole() == Role.DELIVERY_PARTNER) {
            partnerRepository.findByUserId(user.getId())
                    .filter(p -> !p.isActive())
                    .ifPresent(p -> {
                        throw new ConflictException(
                                "Your account has been deactivated. Please contact the admin.");
                    });
        }

        return buildAuthResponse(user);
    }

    /**
     * Validates the refresh token against the DB and issues a new access + refresh token pair.
     *
     * Token rotation: the old refresh token is deleted and a new one is saved.
     * This means each refresh token can only be used once — if an attacker steals
     * a refresh token and uses it, the legitimate user's next refresh will fail
     * (their old token was already rotated away).
     */
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        String incomingToken = request.getRefreshToken();

        // 1. Validate JWT signature and expiry
        String username;
        try {
            username = jwtUtil.extractUsername(incomingToken);
        } catch (Exception e) {
            throw new RuntimeException("Invalid or expired refresh token");
        }

        if (!jwtUtil.isRefreshToken(incomingToken)) {
            throw new RuntimeException("Token is not a refresh token");
        }

        // 2. Look up token in DB by its hash
        String tokenHash = sha256(incomingToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new RuntimeException("Refresh token not recognised — please log in again"));

        if (stored.isExpired()) {
            refreshTokenRepository.delete(stored);
            throw new RuntimeException("Refresh token has expired — please log in again");
        }

        // 3. Load user and issue new token pair (rotate)
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));

        // Delete the old token (rotation — old token is now invalid)
        refreshTokenRepository.delete(stored);

        return buildAuthResponse(user);
    }

    /**
     * Invalidates the refresh token so it can no longer be used.
     * Access tokens issued before logout remain valid until they expire
     * (they are short-lived and stateless — this is the standard JWT trade-off).
     */
    @Transactional
    public void logout(LogoutRequest request) {
        String tokenHash = sha256(request.getRefreshToken());
        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(refreshTokenRepository::delete);
        // Silently succeed even if token not found (already expired/used)
    }

    // ── Private helpers ───────────────────────────────────────────────────

    /**
     * Generates an access + refresh token pair, persists the refresh token in DB,
     * and returns the combined AuthResponse.
     */
    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtUtil.generateToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        // Persist the refresh token (hash only — not plaintext)
        persistRefreshToken(user.getUsername(), refreshToken);

        return new AuthResponse(
                accessToken,
                refreshToken,
                jwtUtil.getExpirationMs(),
                user.getRole().name(),
                user.getUsername()
        );
    }

    private void persistRefreshToken(String username, String rawToken) {
        RefreshToken entity = new RefreshToken();
        entity.setTokenHash(sha256(rawToken));
        entity.setUsername(username);
        // Mirror the JWT expiry so DB and token stay in sync
        long refreshExpiryMs = jwtUtil.getRefreshExpirationMs();
        entity.setExpiresAt(LocalDateTime.now().plusSeconds(refreshExpiryMs / 1000));
        refreshTokenRepository.save(entity);
    }

    /** SHA-256 hex digest — used to store token fingerprints without saving raw JWTs. */
    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
