package com.example.bankcards.service;

import com.example.bankcards.dto.AuthDTO;
import com.example.bankcards.entity.RefreshToken;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardOperationException;
import com.example.bankcards.exception.DuplicateResourceException;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService unit tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private RefreshTokenService refreshTokenService; // добавлен после введения Refresh Token

    @InjectMocks private AuthService authService;

    private User savedUser;
    private RefreshToken stubRefreshToken;

    @BeforeEach
    void setUp() {
        savedUser = User.builder()
                .id(1L)
                .username("newuser")
                .email("new@test.com")
                .password("encoded")
                .role(Role.USER)
                .enabled(true)
                .build();

        stubRefreshToken = RefreshToken.builder()
                .id(1L)
                .token("refresh-uuid-stub")
                .user(savedUser)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();
    }

    // ─── register ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register: should return access token and refresh token on success")
    void register_Success() {
        AuthDTO.RegisterRequest request = new AuthDTO.RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@test.com");
        request.setPassword("Password1!");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any())).thenReturn(savedUser);
        when(jwtUtil.generateToken(any(User.class))).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(any())).thenReturn(stubRefreshToken);

        AuthDTO.AuthResponse response = authService.register(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-uuid-stub");
        assertThat(response.getUsername()).isEqualTo("newuser");
        assertThat(response.getRole()).isEqualTo("USER");
        verify(userRepository).save(any(User.class));
        verify(refreshTokenService).createRefreshToken(any(User.class));
    }

    @Test
    @DisplayName("register: should throw DuplicateResourceException when username taken")
    void register_DuplicateUsername() {
        AuthDTO.RegisterRequest request = new AuthDTO.RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@test.com");
        request.setPassword("Password1!");

        when(userRepository.existsByUsername("newuser")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username");
    }

    @Test
    @DisplayName("register: should throw DuplicateResourceException when email taken")
    void register_DuplicateEmail() {
        AuthDTO.RegisterRequest request = new AuthDTO.RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@test.com");
        request.setPassword("Password1!");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email");
    }

    // ─── login ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login: should return both tokens on valid credentials")
    void login_Success() {
        AuthDTO.LoginRequest request = new AuthDTO.LoginRequest();
        request.setUsername("newuser");
        request.setPassword("Password1!");

        when(userRepository.findByUsername("newuser")).thenReturn(Optional.of(savedUser));
        when(jwtUtil.generateToken(any(User.class))).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(any())).thenReturn(stubRefreshToken);

        AuthDTO.AuthResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-uuid-stub");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(refreshTokenService).createRefreshToken(any(User.class));
    }

    @Test
    @DisplayName("login: should throw BadCredentialsException on wrong password")
    void login_BadCredentials() {
        AuthDTO.LoginRequest request = new AuthDTO.LoginRequest();
        request.setUsername("newuser");
        request.setPassword("wrongpassword");

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    // ─── refresh ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("refresh: should rotate tokens and return new pair")
    void refresh_Success() {
        AuthDTO.RefreshRequest request = new AuthDTO.RefreshRequest();
        request.setRefreshToken("refresh-uuid-stub");

        when(refreshTokenService.verifyRefreshToken("refresh-uuid-stub")).thenReturn(stubRefreshToken);
        when(jwtUtil.generateToken(any(User.class))).thenReturn("new-access-token");

        RefreshToken newRefresh = RefreshToken.builder()
                .token("new-refresh-uuid")
                .user(savedUser)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();
        when(refreshTokenService.createRefreshToken(any())).thenReturn(newRefresh);

        AuthDTO.RefreshResponse response = authService.refresh(request);

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-uuid");
        // Old token must be revoked (rotation)
        assertThat(stubRefreshToken.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("refresh: should throw when refresh token is invalid")
    void refresh_InvalidToken() {
        AuthDTO.RefreshRequest request = new AuthDTO.RefreshRequest();
        request.setRefreshToken("expired-token");

        when(refreshTokenService.verifyRefreshToken("expired-token"))
                .thenThrow(new CardOperationException("Refresh token is expired or revoked"));

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(CardOperationException.class)
                .hasMessageContaining("expired or revoked");
    }

    // ─── logout ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("logout: should revoke all refresh tokens for the user")
    void logout_RevokesAllTokens() {
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.of(savedUser));

        authService.logout("newuser");

        verify(refreshTokenService).revokeAll(savedUser.getId());
    }

    @Test
    @DisplayName("logout: should do nothing if user not found")
    void logout_UserNotFound_NoException() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        authService.logout("ghost");

        verify(refreshTokenService, never()).revokeAll(any());
    }
}