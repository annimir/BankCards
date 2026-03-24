package com.example.bankcards.repository;

import com.example.bankcards.entity.TransferHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Репозиторий истории переводов.
 *
 * <p>Поддерживает два сценария выборки:
 * <ul>
 *   <li>по владельцу — все переводы пользователя, отсортированные по дате</li>
 *   <li>по карте — все переводы где карта была отправителем или получателем</li>
 * </ul>
 * Оба метода возвращают {@code Page} для постраничной навигации.
 */
public interface TransferHistoryRepository extends JpaRepository<TransferHistory, Long> {

    Page<TransferHistory> findByOwnerIdOrderByOccurredAtDesc(Long ownerId, Pageable pageable);

    Page<TransferHistory> findByFromCardIdOrToCardIdOrderByOccurredAtDesc(
            Long fromCardId, Long toCardId, Pageable pageable);
}