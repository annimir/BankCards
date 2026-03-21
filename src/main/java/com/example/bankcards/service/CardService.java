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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardEncryptionUtil encryptionUtil;

    // ─── Admin operations ────────────────────────────────────────────────────

    @Transactional
    public CardDTO.CardResponse createCard(CardDTO.CreateCardRequest request) {
        log.info("Creating card: ownerId={}, expiryDate={}, initialBalance={}",
                request.getOwnerId(), request.getExpiryDate(), request.getInitialBalance());

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

        Card saved = cardRepository.save(card);
        log.info("Card created successfully: id={}, masked={}, ownerId={}", saved.getId(), masked, owner.getId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<CardDTO.CardResponse> getAllCards(CardStatus status, Long ownerId, Pageable pageable) {
        log.debug("Admin fetching all cards: status={}, ownerId={}, page={}",
                status, ownerId, pageable.getPageNumber());
        return cardRepository.findAllWithFilters(status, ownerId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CardDTO.CardResponse getCardById(Long id) {
        log.debug("Fetching card by id={}", id);
        return toResponse(findById(id));
    }

    @Transactional
    public CardDTO.CardResponse activateCard(Long id) {
        log.info("Activating card id={}", id);
        Card card = findById(id);
        if (card.getExpiryDate().isBefore(LocalDate.now())) {
            log.warn("Activation failed: card id={} is expired (expiryDate={})", id, card.getExpiryDate());
            throw new CardOperationException("Cannot activate an expired card");
        }
        card.setStatus(CardStatus.ACTIVE);
        log.info("Card id={} activated successfully", id);
        return toResponse(cardRepository.save(card));
    }

    @Transactional
    public CardDTO.CardResponse blockCard(Long id) {
        log.info("Admin blocking card id={}", id);
        Card card = findById(id);
        if (card.getStatus() == CardStatus.EXPIRED) {
            log.warn("Block failed: card id={} is already EXPIRED", id);
            throw new CardOperationException("Cannot block an expired card");
        }
        card.setStatus(CardStatus.BLOCKED);
        log.info("Card id={} blocked successfully by admin", id);
        return toResponse(cardRepository.save(card));
    }

    @Transactional
    public void deleteCard(Long id) {
        log.info("Deleting card id={}", id);
        if (!cardRepository.existsById(id)) {
            throw new ResourceNotFoundException("Card", id);
        }
        cardRepository.deleteById(id);
        log.info("Card id={} deleted successfully", id);
    }

    // ─── User operations ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<CardDTO.CardResponse> getMyCards(Long userId, CardStatus status,
                                                 String maskedNumber, Pageable pageable) {
        log.debug("User id={} fetching own cards: status={}, maskedNumber='{}', page={}",
                userId, status, maskedNumber, pageable.getPageNumber());
        return cardRepository.findByOwnerIdWithFilters(userId, status, maskedNumber, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CardDTO.CardResponse getMyCard(Long cardId, Long userId) {
        log.debug("User id={} fetching card id={}", userId, cardId);
        Card card = cardRepository.findByIdAndOwnerId(cardId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found or does not belong to you"));
        return toResponse(card);
    }

    @Transactional
    public CardDTO.CardResponse requestBlock(Long cardId, Long userId) {
        log.info("User id={} requesting block for card id={}", userId, cardId);
        Card card = cardRepository.findByIdAndOwnerId(cardId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found or does not belong to you"));
        if (card.getStatus() != CardStatus.ACTIVE) {
            log.warn("Block request denied: card id={} is not ACTIVE, current status={}", cardId, card.getStatus());
            throw new CardOperationException("Only active cards can be blocked");
        }
        card.setStatus(CardStatus.BLOCKED);
        log.info("Card id={} blocked by owner userId={}", cardId, userId);
        return toResponse(cardRepository.save(card));
    }

    @Transactional
    public void transfer(Long userId, CardDTO.TransferRequest request) {
        log.info("Transfer initiated: userId={}, fromCardId={}, toCardId={}, amount={}",
                userId, request.getFromCardId(), request.getToCardId(), request.getAmount());

        if (request.getFromCardId().equals(request.getToCardId())) {
            log.warn("Transfer rejected: source and destination are the same card id={}", request.getFromCardId());
            throw new CardOperationException("Source and destination cards must be different");
        }

        Card fromCard = cardRepository.findByIdAndOwnerId(request.getFromCardId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Source card not found or does not belong to you"));

        Card toCard = cardRepository.findByIdAndOwnerId(request.getToCardId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Destination card not found or does not belong to you"));

        if (fromCard.getStatus() != CardStatus.ACTIVE) {
            log.warn("Transfer rejected: source card id={} has status={}", fromCard.getId(), fromCard.getStatus());
            throw new CardOperationException("Source card is not active");
        }
        if (toCard.getStatus() != CardStatus.ACTIVE) {
            log.warn("Transfer rejected: destination card id={} has status={}", toCard.getId(), toCard.getStatus());
            throw new CardOperationException("Destination card is not active");
        }
        if (fromCard.getBalance().compareTo(request.getAmount()) < 0) {
            log.warn("Transfer rejected: insufficient funds on card id={}, available={}, requested={}",
                    fromCard.getId(), fromCard.getBalance(), request.getAmount());
            throw new InsufficientFundsException(
                    "Insufficient funds. Available: " + fromCard.getBalance() +
                            ", requested: " + request.getAmount());
        }

        fromCard.setBalance(fromCard.getBalance().subtract(request.getAmount()));
        toCard.setBalance(toCard.getBalance().add(request.getAmount()));

        cardRepository.save(fromCard);
        cardRepository.save(toCard);
        log.info("Transfer completed: {} transferred from card id={} to card id={}",
                request.getAmount(), fromCard.getId(), toCard.getId());
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