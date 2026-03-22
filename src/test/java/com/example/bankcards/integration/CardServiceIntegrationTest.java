package com.example.bankcards.integration;

import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardOperationException;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.CardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Интеграционные тесты CardService с реальным PostgreSQL через Testcontainers.
 * Проверяет полный цикл: создание карты → перевод → блокировка → истечение.
 * @Transactional откатывает каждый тест — БД остаётся чистой.
 */
@SpringBootTest
@Transactional
@DisplayName("CardService integration tests (Testcontainers + PostgreSQL)")
class CardServiceIntegrationTest extends PostgreSQLContainerBase {

    @Autowired private CardService cardService;
    @Autowired private CardRepository cardRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(User.builder()
                .username("integrationuser_" + System.nanoTime())
                .email("integration_" + System.nanoTime() + "@test.com")
                .password(passwordEncoder.encode("Password1!"))
                .role(Role.USER)
                .enabled(true)
                .build());
    }

    @Test
    @DisplayName("createCard: persists card with ACTIVE status and correct masked number")
    void createCard_PersistsCorrectly() {
        CardDTO.CreateCardRequest request = new CardDTO.CreateCardRequest();
        request.setOwnerId(owner.getId());
        request.setExpiryDate(LocalDate.now().plusYears(3));
        request.setInitialBalance(new BigDecimal("1000.00"));

        CardDTO.CardResponse response = cardService.createCard(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(CardStatus.ACTIVE);
        assertThat(response.getCardNumberMasked()).matches("\\*{4} \\*{4} \\*{4} \\d{4}");
        assertThat(response.getBalance()).isEqualByComparingTo("1000.00");
        assertThat(response.getOwnerUsername()).isEqualTo(owner.getUsername());

        // Проверяем реальную запись в БД
        Card saved = cardRepository.findById(response.getId()).orElseThrow();
        assertThat(saved.getCardNumberEncrypted()).isNotEqualTo(saved.getCardNumberMasked());
    }

    @Test
    @DisplayName("transfer: correctly updates balances in database")
    void transfer_UpdatesBalancesInDb() {
        CardDTO.CreateCardRequest req1 = new CardDTO.CreateCardRequest();
        req1.setOwnerId(owner.getId());
        req1.setExpiryDate(LocalDate.now().plusYears(3));
        req1.setInitialBalance(new BigDecimal("500.00"));

        CardDTO.CreateCardRequest req2 = new CardDTO.CreateCardRequest();
        req2.setOwnerId(owner.getId());
        req2.setExpiryDate(LocalDate.now().plusYears(3));
        req2.setInitialBalance(new BigDecimal("100.00"));

        CardDTO.CardResponse card1 = cardService.createCard(req1);
        CardDTO.CardResponse card2 = cardService.createCard(req2);

        CardDTO.TransferRequest transfer = new CardDTO.TransferRequest();
        transfer.setFromCardId(card1.getId());
        transfer.setToCardId(card2.getId());
        transfer.setAmount(new BigDecimal("200.00"));

        cardService.transfer(owner.getId(), transfer);

        Card from = cardRepository.findById(card1.getId()).orElseThrow();
        Card to   = cardRepository.findById(card2.getId()).orElseThrow();
        assertThat(from.getBalance()).isEqualByComparingTo("300.00");
        assertThat(to.getBalance()).isEqualByComparingTo("300.00");
    }

    @Test
    @DisplayName("transfer: throws InsufficientFundsException when balance too low")
    void transfer_InsufficientFunds_Throws() {
        CardDTO.CreateCardRequest req1 = new CardDTO.CreateCardRequest();
        req1.setOwnerId(owner.getId());
        req1.setExpiryDate(LocalDate.now().plusYears(3));
        req1.setInitialBalance(new BigDecimal("50.00"));

        CardDTO.CreateCardRequest req2 = new CardDTO.CreateCardRequest();
        req2.setOwnerId(owner.getId());
        req2.setExpiryDate(LocalDate.now().plusYears(3));
        req2.setInitialBalance(BigDecimal.ZERO);

        CardDTO.CardResponse card1 = cardService.createCard(req1);
        CardDTO.CardResponse card2 = cardService.createCard(req2);

        CardDTO.TransferRequest transfer = new CardDTO.TransferRequest();
        transfer.setFromCardId(card1.getId());
        transfer.setToCardId(card2.getId());
        transfer.setAmount(new BigDecimal("999.00"));

        assertThatThrownBy(() -> cardService.transfer(owner.getId(), transfer))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    @DisplayName("blockCard: changes status to BLOCKED in database")
    void blockCard_ChangesStatusInDb() {
        CardDTO.CreateCardRequest req = new CardDTO.CreateCardRequest();
        req.setOwnerId(owner.getId());
        req.setExpiryDate(LocalDate.now().plusYears(3));
        req.setInitialBalance(BigDecimal.ZERO);

        CardDTO.CardResponse card = cardService.createCard(req);
        cardService.blockCard(card.getId());

        Card blocked = cardRepository.findById(card.getId()).orElseThrow();
        assertThat(blocked.getStatus()).isEqualTo(CardStatus.BLOCKED);
    }

    @Test
    @DisplayName("getMyCards: returns only cards belonging to the owner with pagination")
    void getMyCards_ReturnsOnlyOwnerCards() {
        // Создаём второго пользователя с другими картами
        User otherUser = userRepository.save(User.builder()
                .username("other_" + System.nanoTime())
                .email("other_" + System.nanoTime() + "@test.com")
                .password(passwordEncoder.encode("Password1!"))
                .role(Role.USER)
                .enabled(true)
                .build());

        CardDTO.CreateCardRequest myReq = new CardDTO.CreateCardRequest();
        myReq.setOwnerId(owner.getId());
        myReq.setExpiryDate(LocalDate.now().plusYears(3));
        myReq.setInitialBalance(BigDecimal.ZERO);
        cardService.createCard(myReq);

        CardDTO.CreateCardRequest otherReq = new CardDTO.CreateCardRequest();
        otherReq.setOwnerId(otherUser.getId());
        otherReq.setExpiryDate(LocalDate.now().plusYears(3));
        otherReq.setInitialBalance(BigDecimal.ZERO);
        cardService.createCard(otherReq);

        Page<CardDTO.CardResponse> myCards = cardService.getMyCards(
                owner.getId(), null, null, PageRequest.of(0, 10));

        assertThat(myCards.getContent())
                .isNotEmpty()
                .allMatch(c -> c.getOwnerId().equals(owner.getId()));
    }

    @Test
    @DisplayName("activateCard: throws CardOperationException for expired card")
    void activateCard_ExpiredCard_Throws() {
        CardDTO.CreateCardRequest req = new CardDTO.CreateCardRequest();
        req.setOwnerId(owner.getId());
        req.setExpiryDate(LocalDate.now().plusYears(3));
        req.setInitialBalance(BigDecimal.ZERO);

        CardDTO.CardResponse created = cardService.createCard(req);

        // Вручную ставим истёкшую дату через репозиторий
        Card card = cardRepository.findById(created.getId()).orElseThrow();
        card.setExpiryDate(LocalDate.now().minusDays(1));
        cardRepository.save(card);

        assertThatThrownBy(() -> cardService.activateCard(created.getId()))
                .isInstanceOf(CardOperationException.class)
                .hasMessageContaining("expired");
    }
}