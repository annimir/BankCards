package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {

    Page<Card> findByOwnerId(Long ownerId, Pageable pageable);

    Page<Card> findByOwnerIdAndStatus(Long ownerId, CardStatus status, Pageable pageable);

    @Query("SELECT c FROM Card c WHERE c.owner.id = :ownerId " +
            "AND (:status IS NULL OR c.status = :status) " +
            "AND (:maskedNumber IS NULL OR c.cardNumberMasked LIKE %:maskedNumber%)")
    Page<Card> findByOwnerIdWithFilters(
            @Param("ownerId") Long ownerId,
            @Param("status") CardStatus status,
            @Param("maskedNumber") String maskedNumber,
            Pageable pageable);

    @Query("SELECT c FROM Card c WHERE " +
            "(:status IS NULL OR c.status = :status) " +
            "AND (:ownerId IS NULL OR c.owner.id = :ownerId)")
    Page<Card> findAllWithFilters(
            @Param("status") CardStatus status,
            @Param("ownerId") Long ownerId,
            Pageable pageable);

    List<Card> findByOwnerId(Long ownerId);

    Optional<Card> findByIdAndOwnerId(Long id, Long ownerId);
}