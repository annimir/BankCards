package com.example.bankcards.service;

import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardOperationException;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.mapper.CardMapper;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.CardSpecification;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardEncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
    private final CardMapper cardMapper;

    // ─── Admin operations ────────────────────────────────────────────────────

    @Transactional
    public CardDTO.CardResponse createCard(CardDTO.CreateCardRequest request) {
        log.info("Creating card: ownerId={}, expiryDate={}", request.getOwnerId(), request.getExpiryDate());
        User owner = userRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getOwnerId()));

        String rawNumber = encryptionUtil.generateCardNumber();
        Card card = Card.builder()
                .cardNumberEncrypted(encryptionUtil.encrypt(rawNumber))
                .cardNumberMasked(encryptionUtil.mask(rawNumber))
                .owner(owner)
                .expiryDate(request.getExpiryDate())
                .status(CardStatus.ACTIVE)
                .balance(request.getInitialBalance())
                .build();

        Card saved = cardRepository.save(card);
        log.info("Card created: id={}, masked={}, ownerId={}", saved.getId(), saved.getCardNumberMasked(), owner.getId());
        return cardMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<CardDTO.CardResponse> getAllCards(CardStatus status, Long ownerId, Pageable pageable) {
        log.debug("Admin fetching cards: status={}, ownerId={}", status, ownerId);
        return cardRepository.findAll(CardSpecification.forAdmin(status, ownerId), pageable)
                .map(cardMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CardDTO.CardResponse getCardById(Long id) {
        return cardMapper.toResponse(findById(id));
    }

    @Transactional
    public CardDTO.CardResponse activateCard(Long id) {
        log.info("Activating card id={}", id);
        Card card = findById(id);
        if (card.getExpiryDate().isBefore(LocalDate.now())) {
            throw new CardOperationException("Cannot activate an expired card");
        }
        card.setStatus(CardStatus.ACTIVE);
        return cardMapper.toResponse(cardRepository.save(card));
    }

    @Transactional
    public CardDTO.CardResponse blockCard(Long id) {
        log.info("Admin blocking card id={}", id);
        Card card = findById(id);
        if (card.getStatus() == CardStatus.EXPIRED) {
            throw new CardOperationException("Cannot block an expired card");
        }
        card.setStatus(CardStatus.BLOCKED);
        return cardMapper.toResponse(cardRepository.save(card));
    }

    @Transactional
    public void deleteCard(Long id) {
        log.info("Deleting card id={}", id);
        if (!cardRepository.existsById(id)) throw new ResourceNotFoundException("Card", id);
        cardRepository.deleteById(id);
    }

    // ─── User operations ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<CardDTO.CardResponse> getMyCards(Long userId, CardStatus status,
                                                 String maskedNumber, Pageable pageable) {
        Specification<Card> spec = CardSpecification.forUser(userId, status, maskedNumber);
        return cardRepository.findAll(spec, pageable).map(cardMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CardDTO.CardResponse getMyCard(Long cardId, Long userId) {
        Card card = cardRepository.findByIdAndOwnerId(cardId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found or does not belong to you"));
        return cardMapper.toResponse(card);
    }

    @Transactional
    public CardDTO.CardResponse requestBlock(Long cardId, Long userId) {
        log.info("User id={} requesting block for card id={}", userId, cardId);
        Card card = cardRepository.findByIdAndOwnerId(cardId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found or does not belong to you"));
        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new CardOperationException("Only active cards can be blocked");
        }
        card.setStatus(CardStatus.BLOCKED);
        return cardMapper.toResponse(cardRepository.save(card));
    }

    @Transactional
    public void transfer(Long userId, CardDTO.TransferRequest request) {
        log.info("Transfer: userId={}, from={}, to={}, amount={}",
                userId, request.getFromCardId(), request.getToCardId(), request.getAmount());

        if (request.getFromCardId().equals(request.getToCardId())) {
            throw new CardOperationException("Source and destination cards must be different");
        }
        Card fromCard = cardRepository.findByIdAndOwnerId(request.getFromCardId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Source card not found or does not belong to you"));
        Card toCard = cardRepository.findByIdAndOwnerId(request.getToCardId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Destination card not found or does not belong to you"));

        if (fromCard.getStatus() != CardStatus.ACTIVE)
            throw new CardOperationException("Source card is not active");
        if (toCard.getStatus() != CardStatus.ACTIVE)
            throw new CardOperationException("Destination card is not active");
        if (fromCard.getBalance().compareTo(request.getAmount()) < 0)
            throw new InsufficientFundsException(
                    "Insufficient funds. Available: " + fromCard.getBalance() + ", requested: " + request.getAmount());

        fromCard.setBalance(fromCard.getBalance().subtract(request.getAmount()));
        toCard.setBalance(toCard.getBalance().add(request.getAmount()));
        cardRepository.save(fromCard);
        cardRepository.save(toCard);
        log.info("Transfer completed: {} from card={} to card={}", request.getAmount(), fromCard.getId(), toCard.getId());
    }

    private Card findById(Long id) {
        return cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Card", id));
    }
}