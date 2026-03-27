package com.dairy.dairy_management.controller;

import com.dairy.dairy_management.dto.AuthResponse;
import com.dairy.dairy_management.dto.LoginRequest;
import com.dairy.dairy_management.dto.RegisterPartnerRequest;
import com.dairy.dairy_management.dto.RegisterRequest;
import com.dairy.dairy_management.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Register a delivery partner — creates User (DELIVERY_PARTNER role) + DeliveryPartner profile in one step.
     * Example: POST /auth/register-partner
     * { "username": "partner1", "password": "pass123", "name": "Raju", "phone": "9876543210" }
     */
    @PostMapping("/register-partner")
    public ResponseEntity<AuthResponse> registerPartner(@Valid @RequestBody RegisterPartnerRequest request) {
        return ResponseEntity.ok(authService.registerPartner(request));
    }
}
