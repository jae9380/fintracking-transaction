package com.ft.transaction.presentation.dto;

import com.ft.transaction.application.dto.CreateCategoryCommand;
import com.ft.transaction.domain.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCategoryRequest(
        @NotBlank(message = "카테고리명은 필수입니다.")
        String name,

        @NotNull(message = "카테고리 유형은 필수입니다.")
        CategoryType type
) {
    public CreateCategoryCommand toCommand() {
        return new CreateCategoryCommand(name, type);
    }
}
