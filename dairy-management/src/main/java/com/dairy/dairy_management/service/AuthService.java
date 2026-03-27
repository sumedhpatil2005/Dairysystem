package com.dairy.dairy_management.service;

import com.dairy.dairy_management.config.JwtUtil;
import com.dairy.dairy_management.dto.AuthResponse;
import com.dairy.dairy_management.dto.LoginRequest;
import com.dairy.dairy_management.dto.RegisterPartnerRequest;
import com.dairy.dairy_management.dto.RegisterRequest;
import com.dairy.dairy_management.entity.DeliveryPartner;
import com.dairy.dairy_management.entity.Role;
import com.dairy.dairy_management.entity.User;
import com.dairy.dairy_management.repository.DeliveryPartnerRepository;
import com.dairy.dairy_management.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final DeliveryPartnerRepository partnerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
                       DeliveryPartnerRepository partnerRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.partnerRepository = partnerRepository;
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

        String token = jwtUtil.generateToken(user);
        return new AuthResponse(token, user.getRole().name(), user.getUsername());
    }

    /**
     * Registers a delivery partner user + creates their profile in one step.
     * Eliminates the need to call POST /auth/register then POST /delivery-partners separately.
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

        String token = jwtUtil.generateToken(user);
        return new AuthResponse(token, user.getRole().name(), user.getUsername());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(user);
        return new AuthResponse(token, user.getRole().name(), user.getUsername());
    }
}
