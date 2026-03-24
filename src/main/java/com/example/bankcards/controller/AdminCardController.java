package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints управления картами для администраторов.
 *
 * <p>Все методы требуют роль {@code ADMIN} (проверяется через {@code @PreAuthorize}).
 * Поддерживает создание, чтение, активацию, блокировку и удаление карт,
 * а также фильтрацию по статусу и владельцу с пагинацией.
 */
@RestController
@RequestMapping("/api/admin/cards")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Cards", description = "Admin card management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminCardController {

    private final CardService cardService;

    @PostMapping
    @Operation(summary = "Create a new bank card for a user")
    public ResponseEntity<CardDTO.CardResponse> createCard(
            @Valid @RequestBody CardDTO.CreateCardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cardService.createCard(request));
    }

    @GetMapping
    @Operation(summary = "Get all cards with optional filters and pagination")
    public ResponseEntity<Page<CardDTO.CardResponse>> getAllCards(
            @RequestParam(required = false) CardStatus status,
            @RequestParam(required = false) Long ownerId,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(cardService.getAllCards(status, ownerId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a card by ID")
    public ResponseEntity<CardDTO.CardResponse> getCardById(@PathVariable Long id) {
        return ResponseEntity.ok(cardService.getCardById(id));
    }

    @PutMapping("/{id}/activate")
    @Operation(summary = "Activate a card")
    public ResponseEntity<CardDTO.CardResponse> activateCard(@PathVariable Long id) {
        return ResponseEntity.ok(cardService.activateCard(id));
    }

    @PutMapping("/{id}/block")
    @Operation(summary = "Block a card")
    public ResponseEntity<CardDTO.CardResponse> blockCard(@PathVariable Long id) {
        return ResponseEntity.ok(cardService.blockCard(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a card")
    public ResponseEntity<Void> deleteCard(@PathVariable Long id) {
        cardService.deleteCard(id);
        return ResponseEntity.noContent().build();
    }
}