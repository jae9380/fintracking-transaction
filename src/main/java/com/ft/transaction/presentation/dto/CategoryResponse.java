package com.ft.transaction.presentation.dto;

import com.ft.transaction.application.dto.CategoryResult;
import com.ft.transaction.domain.CategoryType;

public record CategoryResponse(Long id, String name, CategoryType type, boolean isDefault) {

    public static CategoryResponse from(CategoryResult result) {
        return new CategoryResponse(result.id(), result.name(), result.type(), result.isDefault());
    }
}
