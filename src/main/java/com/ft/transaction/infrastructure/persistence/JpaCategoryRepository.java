package com.ft.transaction.infrastructure.persistence;

import com.ft.transaction.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JpaCategoryRepository extends JpaRepository<Category, Long> {

    @Query("SELECT c FROM Category c WHERE c.userId = :userId OR c.isDefault = true")
    List<Category> findAllByUserIdOrIsDefaultTrue(@Param("userId") Long userId);
}
