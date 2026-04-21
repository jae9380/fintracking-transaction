package com.ft.transaction.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateTransactionCommand(
        BigDecimal amount,
        String description,
        LocalDate transactionDate
) {}
