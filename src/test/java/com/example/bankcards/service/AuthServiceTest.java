package com.example.bankcards.service;

import com.example.bankcards.dto.AuthDTO;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
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

    @InjectMocks private AuthService authService;

    private AuthDTO.RegisterRequest registerRequest;
    private User savedUser;

    @BeforeEach
    void setUp() {
        registerRequest = new AuthDTO.RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setEmail("new@test.com");
        registerRequest.setPassword("Password1!");

        savedUser = User.builder()
                .id(1L)
                .username("newuser")
                .email("new@test.com")
                .password("encoded")
                .role(Role.USER)
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("register: should register successfully and return token")
    void register_Success() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any())).thenReturn(savedUser);
        when(jwtUtil.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthDTO.AuthResponse response = authService.register(registerRequest);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUsername()).isEqualTo("newuser");
        assertThat(response.getRole()).isEqualTo("USER");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register: should throw DuplicateResourceException when username taken")
    void register_DuplicateUsername() {
        when(userRepository.existsByUsername("newuser")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username");
    }

    @Test
    @DisplayName("register: should throw DuplicateResourceException when email taken")
    void register_DuplicateEmail() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email");
    }

    @Test
    @DisplayName("login: should return token on valid credentials")
    void login_Success() {
        AuthDTO.LoginRequest loginRequest = new AuthDTO.LoginRequest();
        loginRequest.setUsername("newuser");
        loginRequest.setPassword("Password1!");

        when(userRepository.findByUsername("newuser")).thenReturn(Optional.of(savedUser));
        when(jwtUtil.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthDTO.AuthResponse response = authService.login(loginRequest);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("login: should throw BadCredentialsException on wrong password")
    void login_BadCredentials() {
        AuthDTO.LoginRequest loginRequest = new AuthDTO.LoginRequest();
        loginRequest.setUsername("newuser");
        loginRequest.setPassword("wrongpassword");

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);
    }
}