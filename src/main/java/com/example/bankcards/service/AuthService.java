package com.example.bankcards.service;

import com.example.bankcards.dto.AuthDTO;
import com.example.bankcards.entity.RefreshToken;
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
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthDTO.AuthResponse register(AuthDTO.RegisterRequest request) {
        log.info("Registration attempt: username='{}', email='{}'", request.getUsername(), request.getEmail());

        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration failed — username taken: '{}'", request.getUsername());
            throw new DuplicateResourceException("Username already taken: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed — email taken: '{}'", request.getEmail());
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
        log.info("User registered: id={}, username='{}'", user.getId(), user.getUsername());

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthDTO.AuthResponse login(AuthDTO.LoginRequest request) {
        log.info("Login attempt: username='{}'", request.getUsername());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        log.info("Login successful: username='{}', role={}", user.getUsername(), user.getRole());
        return buildAuthResponse(user);
    }

    /**
     * Issues new access + refresh tokens using a valid refresh token.
     * The old refresh token is revoked (rotation — prevents reuse).
     */
    @Transactional
    public AuthDTO.RefreshResponse refresh(AuthDTO.RefreshRequest request) {
        log.debug("Token refresh requested");

        RefreshToken oldToken = refreshTokenService.verifyRefreshToken(request.getRefreshToken());
        User user = oldToken.getUser();

        // Revoke the used token (rotation strategy)
        oldToken.setRevoked(true);

        // Issue new pair
        String newAccessToken = jwtUtil.generateToken(user);
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);

        log.info("Tokens rotated for userId={}", user.getId());
        return new AuthDTO.RefreshResponse(newAccessToken, newRefreshToken.getToken());
    }

    /**
     * Revokes all refresh tokens for the authenticated user (logout from all sessions).
     */
    @Transactional
    public void logout(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            refreshTokenService.revokeAll(user.getId());
            log.info("User '{}' logged out — all refresh tokens revoked", username);
        });
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private AuthDTO.AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtUtil.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        return new AuthDTO.AuthResponse(
                accessToken,
                refreshToken.getToken(),
                user.getUsername(),
                user.getRole().name()
        );
    }
}