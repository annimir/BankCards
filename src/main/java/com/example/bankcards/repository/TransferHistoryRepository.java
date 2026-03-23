package com.example.bankcards.repository;

import com.example.bankcards.entity.TransferHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferHistoryRepository extends JpaRepository<TransferHistory, Long> {

    Page<TransferHistory> findByOwnerIdOrderByOccurredAtDesc(Long ownerId, Pageable pageable);

    Page<TransferHistory> findByFromCardIdOrToCardIdOrderByOccurredAtDesc(
            Long fromCardId, Long toCardId, Pageable pageable);
}