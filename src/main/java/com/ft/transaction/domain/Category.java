package com.ft.transaction.domain;

import com.ft.common.entity.BaseEntity;
import com.ft.common.exception.CustomException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.ft.common.exception.ErrorCode.*;

@Entity
@Table(name = "categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;   // null이면 기본 제공 카테고리

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoryType type;

    @Column(nullable = false)
    private boolean isDefault;

    private Category(Long userId, String name, CategoryType type, boolean isDefault) {
        this.userId = userId;
        this.name = name;
        this.type = type;
        this.isDefault = isDefault;
    }

    // 사용자 정의 카테고리
    public static Category create(Long userId, String name, CategoryType type) {
        validateName(name);
        return new Category(userId, name, type, false);
    }

    // 기본 제공 카테고리 (시스템)
    public static Category createDefault(String name, CategoryType type) {
        validateName(name);
        return new Category(null, name, type, true);
    }

    // 소유자 검증 (기본 카테고리는 모든 사용자 사용 가능)
    public void validateAccessible(Long userId) {
        if (!isDefault && !userId.equals(this.userId)) {
            throw new CustomException(TRANSACTION_NO_ACCESS);
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new CustomException(TRANSACTION_INVALID_CATEGORY_NAME);
        }
    }
}
