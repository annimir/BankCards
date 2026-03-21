package com.example.bankcards.service;

import com.example.bankcards.entity.RefreshToken;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardOperationException;
import com.example.bankcards.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService unit tests")
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @InjectMocks private RefreshTokenService refreshTokenService;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpirationMs", 604800000L);
        user = User.builder()
                .id(1L).username("testuser").role(Role.USER).enabled(true).build();
    }

    @Test
    @DisplayName("createRefreshToken: should persist and return a token")
    void createRefreshToken_Success() {
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken token = refreshTokenService.createRefreshToken(user);

        assertThat(token.getToken()).isNotBlank();
        assertThat(token.getUser()).isEqualTo(user);
        assertThat(token.isRevoked()).isFalse();
        assertThat(token.getExpiresAt()).isAfter(LocalDateTime.now());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("verifyRefreshToken: should return valid token")
    void verifyRefreshToken_Valid() {
        RefreshToken token = RefreshToken.builder()
                .token("valid-uuid")
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken("valid-uuid")).thenReturn(Optional.of(token));

        RefreshToken result = refreshTokenService.verifyRefreshToken("valid-uuid");

        assertThat(result).isEqualTo(token);
    }

    @Test
    @DisplayName("verifyRefreshToken: should throw when token not found")
    void verifyRefreshToken_NotFound() {
        when(refreshTokenRepository.findByToken("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.verifyRefreshToken("ghost"))
                .isInstanceOf(CardOperationException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    @DisplayName("verifyRefreshToken: should throw when token is expired")
    void verifyRefreshToken_Expired() {
        RefreshToken token = RefreshToken.builder()
                .token("old-uuid")
                .user(user)
                .expiresAt(LocalDateTime.now().minusDays(1)) // expired
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken("old-uuid")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> refreshTokenService.verifyRefreshToken("old-uuid"))
                .isInstanceOf(CardOperationException.class)
                .hasMessageContaining("expired or revoked");
    }

    @Test
    @DisplayName("verifyRefreshToken: should throw when token is revoked")
    void verifyRefreshToken_Revoked() {
        RefreshToken token = RefreshToken.builder()
                .token("revoked-uuid")
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(true) // revoked
                .build();

        when(refreshTokenRepository.findByToken("revoked-uuid")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> refreshTokenService.verifyRefreshToken("revoked-uuid"))
                .isInstanceOf(CardOperationException.class);
    }

    @Test
    @DisplayName("revokeAll: should call repository to revoke user tokens")
    void revokeAll_CallsRepository() {
        when(refreshTokenRepository.revokeAllByUserId(1L)).thenReturn(2);

        refreshTokenService.revokeAll(1L);

        verify(refreshTokenRepository).revokeAllByUserId(1L);
    }

    @Test
    @DisplayName("cleanupExpiredTokens: should delete stale tokens")
    void cleanupExpiredTokens_DeletesStale() {
        when(refreshTokenRepository.deleteExpiredTokens(any())).thenReturn(5);

        refreshTokenService.cleanupExpiredTokens();

        verify(refreshTokenRepository).deleteExpiredTokens(any(LocalDateTime.class));
    }
}