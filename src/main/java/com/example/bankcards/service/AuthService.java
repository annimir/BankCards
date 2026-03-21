package com.example.bankcards.service;

import com.example.bankcards.dto.AuthDTO;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.DuplicateResourceException;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthDTO.AuthResponse register(AuthDTO.RegisterRequest request) {
        log.info("Registration attempt for username='{}', email='{}'", request.getUsername(), request.getEmail());

        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration failed — username already taken: '{}'", request.getUsername());
            throw new DuplicateResourceException("Username already taken: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed — email already registered: '{}'", request.getEmail());
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .enabled(true)
                .build();

        userRepository.save(user);
        log.info("User registered successfully: username='{}', id={}", user.getUsername(), user.getId());

        String token = jwtUtil.generateToken(user);
        return new AuthDTO.AuthResponse(token, user.getUsername(), user.getRole().name());
    }

    public AuthDTO.AuthResponse login(AuthDTO.LoginRequest request) {
        log.info("Login attempt for username='{}'", request.getUsername());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        log.info("Login successful: username='{}', role={}", user.getUsername(), user.getRole());
        String token = jwtUtil.generateToken(user);
        return new AuthDTO.AuthResponse(token, user.getUsername(), user.getRole().name());
    }
}