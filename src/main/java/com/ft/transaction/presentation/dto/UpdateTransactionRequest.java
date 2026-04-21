package com.ft.transaction.presentation.dto;

import com.ft.transaction.application.dto.UpdateTransactionCommand;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateTransactionRequest(
        @Positive(message = "금액은 0보다 커야 합니다.")
        BigDecimal amount,

        String description,

        LocalDate transactionDate
) {
    public UpdateTransactionCommand toCommand() {
        return new UpdateTransactionCommand(amount, description, transactionDate);
    }
}
