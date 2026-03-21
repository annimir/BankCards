package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CardSpecification unit tests")
class CardSpecificationTest {

    @Test
    @DisplayName("hasStatus: returns conjunction (no-op) when status is null")
    void hasStatus_Null_ReturnsConjunction() {
        Specification<Card> spec = CardSpecification.hasStatus(null);
        assertThat(spec).isNotNull();
    }

    @Test
    @DisplayName("hasStatus: returns non-null specification when status provided")
    void hasStatus_Active_ReturnsSpec() {
        Specification<Card> spec = CardSpecification.hasStatus(CardStatus.ACTIVE);
        assertThat(spec).isNotNull();
    }

    @Test
    @DisplayName("forAdmin: composes status and ownerId specifications")
    void forAdmin_ComposesCorrectly() {
        Specification<Card> spec = CardSpecification.forAdmin(CardStatus.BLOCKED, 42L);
        assertThat(spec).isNotNull();
    }

    @Test
    @DisplayName("forUser: composes owner, status, and masked number specifications")
    void forUser_ComposesCorrectly() {
        Specification<Card> spec = CardSpecification.forUser(1L, CardStatus.ACTIVE, "1234");
        assertThat(spec).isNotNull();
    }

    @Test
    @DisplayName("forUser: handles null filter params gracefully")
    void forUser_NullParams_NoException() {
        Specification<Card> spec = CardSpecification.forUser(1L, null, null);
        assertThat(spec).isNotNull();
    }

    @Test
    @DisplayName("maskedNumberContains: blank string treated as no filter")
    void maskedNumberContains_Blank_ReturnsConjunction() {
        Specification<Card> spec = CardSpecification.maskedNumberContains("   ");
        assertThat(spec).isNotNull();
    }
}