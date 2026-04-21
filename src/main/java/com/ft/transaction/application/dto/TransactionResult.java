package com.ft.transaction.application.dto;

import com.ft.transaction.domain.Transaction;
import com.ft.transaction.domain.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TransactionResult(
        Long id,
        Long accountId,
        Long toAccountId,
        Long categoryId,
        TransactionType type,
        BigDecimal amount,
        String description,
        LocalDate transactionDate,
        LocalDateTime createdAt
) {
    public static TransactionResult from(Transaction transaction) {
        return new TransactionResult(
                transaction.getId(),
                transaction.getAccountId(),
                transaction.getToAccountId(),
                transaction.getCategoryId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getTransactionDate(),
                transaction.getCreatedAt()
        );
    }
}
