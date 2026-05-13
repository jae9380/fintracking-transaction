package com.ft.transaction.presentation.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record DeleteTransactionsRequest(
        @NotEmpty List<Long> ids
) {}
