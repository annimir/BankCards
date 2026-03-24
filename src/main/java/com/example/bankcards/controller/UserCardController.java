package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.dto.UserDTO;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints для аутентифицированных пользователей.
 *
 * <p>Пользователь видит и управляет только своими картами — владелец
 * определяется из JWT-токена через {@code @AuthenticationPrincipal}.
 * Поддерживает просмотр карт, запрос блокировки и переводы между своими картами.
 * История переводов вынесена в {@link TransferHistoryController}.
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "User", description = "Endpoints for authenticated users")
@SecurityRequirement(name = "bearerAuth")
public class UserCardController {

    private final CardService cardService;
    private final UserService userService;

    // ─── Profile ─────────────────────────────────────────────────────────────

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<UserDTO.UserResponse> getProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.getMyProfile(user.getUsername()));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile")
    public ResponseEntity<UserDTO.UserResponse> updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UserDTO.UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateMyProfile(user.getUsername(), request));
    }

    // ─── Cards ────────────────────────────────────────────────────────────────

    @GetMapping("/cards")
    @Operation(summary = "Get my cards with optional filters and pagination")
    public ResponseEntity<Page<CardDTO.CardResponse>> getMyCards(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) CardStatus status,
            @RequestParam(required = false) String maskedNumber,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(
                cardService.getMyCards(user.getId(), status, maskedNumber, pageable));
    }

    @GetMapping("/cards/{id}")
    @Operation(summary = "Get a specific card by ID (must belong to current user)")
    public ResponseEntity<CardDTO.CardResponse> getMyCard(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(cardService.getMyCard(id, user.getId()));
    }

    @PostMapping("/cards/{id}/block")
    @Operation(summary = "Request blocking of my card")
    public ResponseEntity<CardDTO.CardResponse> requestBlock(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(cardService.requestBlock(id, user.getId()));
    }

    @PostMapping("/cards/transfer")
    @Operation(summary = "Transfer funds between my own cards")
    public ResponseEntity<Void> transfer(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CardDTO.TransferRequest request) {
        cardService.transfer(user.getId(), request);
        return ResponseEntity.ok().build();
    }
}