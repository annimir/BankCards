package com.example.bankcards.service;

import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardOperationException;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardEncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardEncryptionUtil encryptionUtil;

    // ─── Admin operations ────────────────────────────────────────────────────

    @Transactional
    public CardDTO.CardResponse createCard(CardDTO.CreateCardRequest request) {
        User owner = userRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getOwnerId()));

        String rawNumber = encryptionUtil.generateCardNumber();
        String encrypted = encryptionUtil.encrypt(rawNumber);
        String masked = encryptionUtil.mask(rawNumber);

        Card card = Card.builder()
                .cardNumberEncrypted(encrypted)
                .cardNumberMasked(masked)
                .owner(owner)
                .expiryDate(request.getExpiryDate())
                .status(CardStatus.ACTIVE)
                .balance(request.getInitialBalance())
                .build();

        return toResponse(cardRepository.save(card));
    }

    @Transactional(readOnly = true)
    public Page<CardDTO.CardResponse> getAllCards(CardStatus status, Long ownerId, Pageable pageable) {
        return cardRepository.findAllWithFilters(status, ownerId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CardDTO.CardResponse getCardById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public CardDTO.CardResponse activateCard(Long id) {
        Card card = findById(id);
        if (card.getExpiryDate().isBefore(LocalDate.now())) {
            throw new CardOperationException("Cannot activate an expired card");
        }
        card.setStatus(CardStatus.ACTIVE);
        return toResponse(cardRepository.save(card));
    }

    @Transactional
    public CardDTO.CardResponse blockCard(Long id) {
        Card card = findById(id);
        if (card.getStatus() == CardStatus.EXPIRED) {
            throw new CardOperationException("Cannot block an expired card");
        }
        card.setStatus(CardStatus.BLOCKED);
        return toResponse(cardRepository.save(card));
    }

    @Transactional
    public void deleteCard(Long id) {
        if (!cardRepository.existsById(id)) {
            throw new ResourceNotFoundException("Card", id);
        }
        cardRepository.deleteById(id);
    }

    // ─── User operations ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<CardDTO.CardResponse> getMyCards(Long userId, CardStatus status,
                                                 String maskedNumber, Pageable pageable) {
        return cardRepository.findByOwnerIdWithFilters(userId, status, maskedNumber, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CardDTO.CardResponse getMyCard(Long cardId, Long userId) {
        Card card = cardRepository.findByIdAndOwnerId(cardId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found or does not belong to you"));
        return toResponse(card);
    }

    @Transactional
    public CardDTO.CardResponse requestBlock(Long cardId, Long userId) {
        Card card = cardRepository.findByIdAndOwnerId(cardId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found or does not belong to you"));
        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new CardOperationException("Only active cards can be blocked");
        }
        card.setStatus(CardStatus.BLOCKED);
        return toResponse(cardRepository.save(card));
    }

    @Transactional
    public void transfer(Long userId, CardDTO.TransferRequest request) {
        if (request.getFromCardId().equals(request.getToCardId())) {
            throw new CardOperationException("Source and destination cards must be different");
        }

        Card fromCard = cardRepository.findByIdAndOwnerId(request.getFromCardId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Source card not found or does not belong to you"));

        Card toCard = cardRepository.findByIdAndOwnerId(request.getToCardId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Destination card not found or does not belong to you"));

        if (fromCard.getStatus() != CardStatus.ACTIVE) {
            throw new CardOperationException("Source card is not active");
        }
        if (toCard.getStatus() != CardStatus.ACTIVE) {
            throw new CardOperationException("Destination card is not active");
        }
        if (fromCard.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds. Available: " + fromCard.getBalance() +
                            ", requested: " + request.getAmount());
        }

        fromCard.setBalance(fromCard.getBalance().subtract(request.getAmount()));
        toCard.setBalance(toCard.getBalance().add(request.getAmount()));

        cardRepository.save(fromCard);
        cardRepository.save(toCard);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Card findById(Long id) {
        return cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Card", id));
    }

    public CardDTO.CardResponse toResponse(Card card) {
        return CardDTO.CardResponse.builder()
                .id(card.getId())
                .cardNumberMasked(card.getCardNumberMasked())
                .ownerId(card.getOwner().getId())
                .ownerUsername(card.getOwner().getUsername())
                .expiryDate(card.getExpiryDate())
                .status(card.getStatus())
                .balance(card.getBalance())
                .createdAt(card.getCreatedAt())
                .build();
    }
}