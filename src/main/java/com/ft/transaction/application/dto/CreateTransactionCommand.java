package com.ft.transaction.application.dto;

import com.ft.transaction.domain.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTransactionCommand(
        Long accountId,
        Long categoryId,
        Long toAccountId,       // TRANSFER 전용, 나머지는 null
        TransactionType type,
        BigDecimal amount,
        String description,
        LocalDate transactionDate
) {}
