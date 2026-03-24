package com.example.bankcards.controller;

import com.example.bankcards.dto.TransferHistoryDTO;
import com.example.bankcards.entity.User;
import com.example.bankcards.service.TransferHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints истории переводов текущего пользователя.
 *
 * <p>Предоставляет постраничный доступ к истории всех переводов пользователя
 * и истории по конкретной карте. Принадлежность карты пользователю проверяется
 * в {@code TransferHistoryService#getCardHistory}.
 */
@RestController
@RequestMapping("/api/user/transfers")
@RequiredArgsConstructor
@Tag(name = "Transfer History", description = "История переводов пользователя")
@SecurityRequirement(name = "bearerAuth")
public class TransferHistoryController {

    private final TransferHistoryService transferHistoryService;

    @GetMapping
    @Operation(summary = "Вся история переводов текущего пользователя")
    public ResponseEntity<Page<TransferHistoryDTO>> getMyHistory(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20, sort = "occurredAt") Pageable pageable) {
        return ResponseEntity.ok(transferHistoryService.getMyHistory(user.getId(), pageable));
    }

    @GetMapping("/cards/{cardId}")
    @Operation(summary = "История переводов по конкретной карте (входящие + исходящие)")
    public ResponseEntity<Page<TransferHistoryDTO>> getCardHistory(
            @AuthenticationPrincipal User user,
            @PathVariable Long cardId,
            @PageableDefault(size = 20, sort = "occurredAt") Pageable pageable) {
        return ResponseEntity.ok(
                transferHistoryService.getCardHistory(cardId, user.getId(), pageable));
    }
}