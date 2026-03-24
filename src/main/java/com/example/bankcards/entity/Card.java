package com.example.bankcards.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Сущность банковской карты.
 *
 * <p>Номер карты хранится в двух форматах: {@code cardNumberEncrypted} (AES, Base64)
 * для возможного отображения полного номера и {@code cardNumberMasked} для API-ответов.
 *
 * <p>{@code @Version} обеспечивает optimistic locking — защищает от race condition
 * при параллельных переводах: второй concurrent UPDATE не найдёт строку и
 * выбросит {@code ObjectOptimisticLockingFailureException}.
 *
 * <p>Аудит-поля ({@code createdBy}, {@code lastModifiedBy}, {@code lastModifiedAt})
 * заполняются автоматически через {@code AuditingEntityListener}.
 */
@Entity
@Table(name = "cards")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_number_encrypted", nullable = false, unique = true)
    private String cardNumberEncrypted;

    @Column(name = "card_number_masked", nullable = false, length = 19)
    private String cardNumberMasked;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CardStatus status;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 50)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "last_modified_by", length = 50)
    private String lastModifiedBy;

    @LastModifiedDate
    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (balance == null) balance = BigDecimal.ZERO;
        if (status == null) status = CardStatus.ACTIVE;
    }
}