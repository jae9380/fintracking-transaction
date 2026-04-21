package com.ft.transaction.application.dto;

import com.ft.transaction.domain.CategoryType;

public record CreateCategoryCommand(String name, CategoryType type) {}
