package com.ft.transaction.application.port;

import com.ft.transaction.domain.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {
    Category save(Category category);
    Optional<Category> findById(Long id);
    List<Category> findAllAccessibleByUserId(Long userId); // user's own + default
    void delete(Category category);
}
