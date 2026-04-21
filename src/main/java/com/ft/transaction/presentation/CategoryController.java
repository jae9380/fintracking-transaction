package com.ft.transaction.presentation;

import com.ft.common.response.ApiResponse;
import com.ft.transaction.application.CategoryService;
import com.ft.transaction.presentation.dto.CategoryResponse;
import com.ft.transaction.presentation.dto.CreateCategoryRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ApiResponse<CategoryResponse> create(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreateCategoryRequest request) {
        return ApiResponse.created(CategoryResponse.from(categoryService.create(userId, request.toCommand())));
    }

    @GetMapping
    public ApiResponse<List<CategoryResponse>> findAll(@RequestHeader("X-User-Id") Long userId) {
        List<CategoryResponse> responses = categoryService.findAll(userId)
                .stream()
                .map(CategoryResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @DeleteMapping("/{categoryId}")
    public ApiResponse<Void> delete(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long categoryId) {
        categoryService.delete(userId, categoryId);
        return ApiResponse.noContent();
    }
}
