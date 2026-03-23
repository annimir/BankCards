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
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardEncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CardService unit tests")
class CardServiceTest {

    @Mock private CardRepository cardRepository;
    @Mock private UserRepository userRepository;
    @Mock private CardEncryptionUtil encryptionUtil;
    @Mock private CardMapper cardMapper;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private CardService cardService;

    private User owner;
    private Card activeCard;
    private Card secondCard;
    private CardDTO.CardResponse stubResponse;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@test.com")
                .build();

        activeCard = Card.builder()
                .id(1L)
                .cardNumberEncrypted("encrypted")
                .cardNumberMasked("**** **** **** 1234")
                .owner(owner)
                .expiryDate(LocalDate.now().plusYears(2))
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("1000.00"))
                .build();

        secondCard = Card.builder()
                .id(2L)
                .cardNumberEncrypted("encrypted2")
                .cardNumberMasked("**** **** **** 5678")
                .owner(owner)
                .expiryDate(LocalDate.now().plusYears(2))
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("500.00"))
                .build();

        // Stub-ответ маппера — возвращается при toResponse(any card)
        stubResponse = CardDTO.CardResponse.builder()
                .id(1L)
                .cardNumberMasked("**** **** **** 1234")
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("1000.00"))
                .ownerId(1L)
                .ownerUsername("testuser")
                .build();
    }

    // ─── createCard ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("createCard: should create card successfully")
    void createCard_Success() {
        CardDTO.CreateCardRequest request = new CardDTO.CreateCardRequest();
        request.setOwnerId(1L);
        request.setExpiryDate(LocalDate.now().plusYears(3));
        request.setInitialBalance(BigDecimal.valueOf(500));

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(encryptionUtil.generateCardNumber()).thenReturn("4532015112830366");
        when(encryptionUtil.encrypt(any())).thenReturn("encrypted");
        when(encryptionUtil.mask(any())).thenReturn("**** **** **** 3456");
        when(cardRepository.save(any())).thenAnswer(inv -> {
            Card c = inv.getArgument(0);
            c.setId(10L);
            return c;
        });
        when(cardMapper.toResponse(any(Card.class))).thenReturn(stubResponse);

        CardDTO.CardResponse response = cardService.createCard(request);

        assertThat(response).isNotNull();
        assertThat(response.getCardNumberMasked()).isEqualTo("**** **** **** 1234");
        verify(cardRepository).save(any(Card.class));
        verify(cardMapper).toResponse(any(Card.class));
    }

    @Test
    @DisplayName("createCard: should throw ResourceNotFoundException when owner not found")
    void createCard_OwnerNotFound() {
        CardDTO.CreateCardRequest request = new CardDTO.CreateCardRequest();
        request.setOwnerId(99L);
        request.setExpiryDate(LocalDate.now().plusYears(1));

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.createCard(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ─── blockCard ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("blockCard: should block active card")
    void blockCard_Success() {
        CardDTO.CardResponse blockedResponse = CardDTO.CardResponse.builder()
                .id(1L).status(CardStatus.BLOCKED).build();
        when(cardRepository.findById(1L)).thenReturn(Optional.of(activeCard));
        when(cardRepository.save(any())).thenReturn(activeCard);
        when(cardMapper.toResponse(any(Card.class))).thenReturn(blockedResponse);

        CardDTO.CardResponse response = cardService.blockCard(1L);

        assertThat(response.getStatus()).isEqualTo(CardStatus.BLOCKED);
    }

    @Test
    @DisplayName("blockCard: should throw CardOperationException for expired card")
    void blockCard_ExpiredCard() {
        activeCard.setStatus(CardStatus.EXPIRED);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(activeCard));

        assertThatThrownBy(() -> cardService.blockCard(1L))
                .isInstanceOf(CardOperationException.class);
    }

    // ─── activateCard ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("activateCard: should activate blocked card")
    void activateCard_Success() {
        CardDTO.CardResponse activeResponse = CardDTO.CardResponse.builder()
                .id(1L).status(CardStatus.ACTIVE).build();
        activeCard.setStatus(CardStatus.BLOCKED);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(activeCard));
        when(cardRepository.save(any())).thenReturn(activeCard);
        when(cardMapper.toResponse(any(Card.class))).thenReturn(activeResponse);

        CardDTO.CardResponse response = cardService.activateCard(1L);

        assertThat(response.getStatus()).isEqualTo(CardStatus.ACTIVE);
    }

    @Test
    @DisplayName("activateCard: should throw CardOperationException for expired card")
    void activateCard_ExpiredCard() {
        activeCard.setExpiryDate(LocalDate.now().minusDays(1));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(activeCard));

        assertThatThrownBy(() -> cardService.activateCard(1L))
                .isInstanceOf(CardOperationException.class)
                .hasMessageContaining("expired");
    }

    // ─── transfer ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("transfer: should transfer funds and publish event")
    void transfer_Success() {
        CardDTO.TransferRequest request = new CardDTO.TransferRequest();
        request.setFromCardId(1L);
        request.setToCardId(2L);
        request.setAmount(new BigDecimal("300.00"));

        when(cardRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(activeCard));
        when(cardRepository.findByIdAndOwnerId(2L, 1L)).thenReturn(Optional.of(secondCard));
        when(cardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        cardService.transfer(1L, request);

        assertThat(activeCard.getBalance()).isEqualByComparingTo("700.00");
        assertThat(secondCard.getBalance()).isEqualByComparingTo("800.00");
        verify(cardRepository, times(2)).save(any(Card.class));
        // Проверяем что событие было опубликовано
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    @DisplayName("transfer: should throw InsufficientFundsException when balance too low")
    void transfer_InsufficientFunds() {
        CardDTO.TransferRequest request = new CardDTO.TransferRequest();
        request.setFromCardId(1L);
        request.setToCardId(2L);
        request.setAmount(new BigDecimal("9999.00"));

        when(cardRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(activeCard));
        when(cardRepository.findByIdAndOwnerId(2L, 1L)).thenReturn(Optional.of(secondCard));

        assertThatThrownBy(() -> cardService.transfer(1L, request))
                .isInstanceOf(InsufficientFundsException.class);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("transfer: should throw CardOperationException when same card")
    void transfer_SameCard() {
        CardDTO.TransferRequest request = new CardDTO.TransferRequest();
        request.setFromCardId(1L);
        request.setToCardId(1L);
        request.setAmount(new BigDecimal("100.00"));

        assertThatThrownBy(() -> cardService.transfer(1L, request))
                .isInstanceOf(CardOperationException.class);
    }

    @Test
    @DisplayName("transfer: should throw CardOperationException when source card is blocked")
    void transfer_SourceCardBlocked() {
        activeCard.setStatus(CardStatus.BLOCKED);
        CardDTO.TransferRequest request = new CardDTO.TransferRequest();
        request.setFromCardId(1L);
        request.setToCardId(2L);
        request.setAmount(new BigDecimal("100.00"));

        when(cardRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(activeCard));
        when(cardRepository.findByIdAndOwnerId(2L, 1L)).thenReturn(Optional.of(secondCard));

        assertThatThrownBy(() -> cardService.transfer(1L, request))
                .isInstanceOf(CardOperationException.class)
                .hasMessageContaining("Source card is not active");
    }

    // ─── requestBlock ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("requestBlock: user can request blocking their own active card")
    void requestBlock_Success() {
        CardDTO.CardResponse blockedResponse = CardDTO.CardResponse.builder()
                .id(1L).status(CardStatus.BLOCKED).build();
        when(cardRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(activeCard));
        when(cardRepository.save(any())).thenReturn(activeCard);
        when(cardMapper.toResponse(any(Card.class))).thenReturn(blockedResponse);

        CardDTO.CardResponse response = cardService.requestBlock(1L, 1L);

        assertThat(response.getStatus()).isEqualTo(CardStatus.BLOCKED);
    }

    @Test
    @DisplayName("requestBlock: should throw CardOperationException for non-active card")
    void requestBlock_AlreadyBlocked() {
        activeCard.setStatus(CardStatus.BLOCKED);
        when(cardRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(activeCard));

        assertThatThrownBy(() -> cardService.requestBlock(1L, 1L))
                .isInstanceOf(CardOperationException.class);
    }
}