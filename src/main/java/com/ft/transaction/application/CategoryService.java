package com.ft.transaction.application;

import com.ft.common.exception.CustomException;
import com.ft.transaction.application.dto.CategoryResult;
import com.ft.transaction.application.dto.CreateCategoryCommand;
import com.ft.transaction.application.port.CategoryRepository;
import com.ft.transaction.domain.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.ft.common.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryResult create(Long userId, CreateCategoryCommand command) {
        Category category = Category.create(userId, command.name(), command.type());
        return CategoryResult.from(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<CategoryResult> findAll(Long userId) {
        return categoryRepository.findAllAccessibleByUserId(userId)
                .stream()
                .map(CategoryResult::from)
                .toList();
    }

    @Transactional
    public void delete(Long userId, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CustomException(TRANSACTION_NOT_FOUND));
        category.validateAccessible(userId);
        if (category.isDefault()) {
            throw new CustomException(TRANSACTION_NO_ACCESS);
        }
        categoryRepository.delete(category);
    }
}
