package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с сущностью {@link com.example.bankcards.entity.Card}.
 *
 * <p>Расширяет {@code JpaSpecificationExecutor} для поддержки динамической фильтрации
 * через {@link com.example.bankcards.repository.CardSpecification}.
 *
 * <p>{@code expireCards} и {@code countExpirable} используют bulk UPDATE/COUNT —
 * загрузка сущностей в память не происходит, что критично при большом числе карт.
 */
public interface CardRepository extends JpaRepository<Card, Long>, JpaSpecificationExecutor<Card> {

    Page<Card> findByOwnerId(Long ownerId, Pageable pageable);

    List<Card> findByOwnerId(Long ownerId);

    Optional<Card> findByIdAndOwnerId(Long id, Long ownerId);

    boolean existsByIdAndOwnerId(Long id, Long ownerId);

    // For @Scheduled expiry job
    @Modifying
    @Query("UPDATE Card c SET c.status = :newStatus " +
            "WHERE c.expiryDate < :today AND c.status = :currentStatus")
    int expireCards(@Param("today") LocalDate today,
                    @Param("currentStatus") CardStatus currentStatus,
                    @Param("newStatus") CardStatus newStatus);

    @Query("SELECT COUNT(c) FROM Card c WHERE c.expiryDate < :today AND c.status = :status")
    long countExpirable(@Param("today") LocalDate today, @Param("status") CardStatus status);

    // For JPA Specification (replaces raw JPQL filters)
    @Query("SELECT c FROM Card c WHERE c.owner.id = :ownerId")
    Page<Card> findAllByOwnerId(@Param("ownerId") Long ownerId, Pageable pageable);
}