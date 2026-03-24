package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Репозиторий для работы с сущностью {@link com.example.bankcards.entity.Card}.
 *
 * <p>Расширяет {@code JpaSpecificationExecutor} для поддержки динамической фильтрации
 * через {@link com.example.bankcards.repository.CardSpecification}.
 *
 * <p>{@code expireCards} и {@code countExpirable} используют bulk UPDATE/COUNT —
 * загрузка сущностей в память не происходит, что критично при большом числе карт.
 */
public class CardSpecification {

    private CardSpecification() {}

    public static Specification<Card> hasStatus(CardStatus status) {
        return (root, query, cb) ->
                status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<Card> hasOwner(Long ownerId) {
        return (root, query, cb) ->
                ownerId == null ? cb.conjunction() : cb.equal(root.get("owner").get("id"), ownerId);
    }

    public static Specification<Card> maskedNumberContains(String maskedNumber) {
        return (root, query, cb) ->
                (maskedNumber == null || maskedNumber.isBlank())
                        ? cb.conjunction()
                        : cb.like(root.get("cardNumberMasked"), "%" + maskedNumber + "%");
    }

    /**
     * Combines all filters — any parameter can be null (ignored).
     * Usage:
     *   Specification<Card> spec = CardSpecification.forAdmin(status, ownerId);
     *   cardRepository.findAll(spec, pageable);
     */
    public static Specification<Card> forAdmin(CardStatus status, Long ownerId) {
        return Specification
                .where(hasStatus(status))
                .and(hasOwner(ownerId));
    }

    public static Specification<Card> forUser(Long ownerId, CardStatus status, String maskedNumber) {
        return Specification
                .where(hasOwner(ownerId))
                .and(hasStatus(status))
                .and(maskedNumberContains(maskedNumber));
    }
}