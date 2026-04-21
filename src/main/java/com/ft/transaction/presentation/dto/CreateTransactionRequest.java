package com.ft.transaction.presentation.dto;

import com.ft.transaction.application.dto.CreateTransactionCommand;
import com.ft.transaction.domain.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTransactionRequest(
        @NotNull(message = "계좌 ID는 필수입니다.")
        Long accountId,

        Long categoryId,    // TRANSFER는 null 허용

        Long toAccountId,   // TRANSFER 전용

        @NotNull(message = "거래 유형은 필수입니다.")
        TransactionType type,

        @NotNull(message = "금액은 필수입니다.")
        @Positive(message = "금액은 0보다 커야 합니다.")
        BigDecimal amount,

        String description,

        @NotNull(message = "거래일은 필수입니다.")
        LocalDate transactionDate
) {
    public CreateTransactionCommand toCommand() {
        return new CreateTransactionCommand(
                accountId, categoryId, toAccountId, type, amount, description, transactionDate
        );
    }
}
