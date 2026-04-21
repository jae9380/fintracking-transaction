package com.ft.transaction.domain;

import com.ft.common.entity.BaseEntity;
import com.ft.common.exception.CustomException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.ft.common.exception.ErrorCode.*;

@Entity
@Table(name = "transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long accountId;

    private Long toAccountId;  // TRANSFER 전용

    private Long categoryId;  // TRANSFER는 null 허용

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false)
    private BigDecimal amount;

    private String description;

    @Column(nullable = false)
    private LocalDate transactionDate;

    private Transaction(Long userId, Long accountId, Long toAccountId, Long categoryId,
                        TransactionType type, BigDecimal amount,
                        String description, LocalDate transactionDate) {
        this.userId = userId;
        this.accountId = accountId;
        this.toAccountId = toAccountId;
        this.categoryId = categoryId;
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.transactionDate = transactionDate;
    }

    public static Transaction create(Long userId, Long accountId, Long toAccountId, Long categoryId,
                                     TransactionType type, BigDecimal amount,
                                     String description, LocalDate transactionDate) {
        validateAmount(amount);
        validateCategory(type, categoryId);
        return new Transaction(userId, accountId, toAccountId, categoryId, type, amount, description, transactionDate);
    }

    // 소유자 검증
    public void validateOwner(Long userId) {
        if (!this.userId.equals(userId)) {
            throw new CustomException(TRANSACTION_NO_ACCESS);
        }
    }

    // 설명 수정
    public void updateDescription(String description) {
        this.description = description;
    }

    // 금액 수정
    public void updateAmount(BigDecimal amount) {
        validateAmount(amount);
        this.amount = amount;
    }

    // 거래일 수정
    public void updateTransactionDate(LocalDate transactionDate) {
        if (transactionDate == null) {
            throw new CustomException(TRANSACTION_INVALID_DATE);
        }
        this.transactionDate = transactionDate;
    }

    // TRANSFER가 아닌 거래는 카테고리 필수
    private static void validateCategory(TransactionType type, Long categoryId) {
        if (type != TransactionType.TRANSFER && categoryId == null) {
            throw new CustomException(TRANSACTION_CATEGORY_REQUIRED);
        }
    }

    private static void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException(TRANSACTION_INVALID_AMOUNT);
        }
    }
}
