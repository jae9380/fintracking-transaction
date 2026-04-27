package com.ft.transaction.domain;

import com.ft.common.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.ft.common.exception.ErrorCode.*;
import static org.assertj.core.api.Assertions.*;

@DisplayName("Category 도메인 테스트")
class CategoryTest {
    @Nested
    @DisplayName("카테고리 생성")
    class Create {
        @Test
        @DisplayName("성공 - 유저 카테고리가 생성된다")
        void success_validUserCategory_createsCategory() {
            // given
            Long userId = 1L;
            String name = "식비";
            CategoryType type = CategoryType.EXPENSE;

            // when
            Category category = Category.create(userId, name, type);

            // then
            assertThat(category.getUserId()).isEqualTo(1L);
            assertThat(category.getName()).isEqualTo("식비");
            assertThat(category.isDefault()).isFalse();
        }

        @Test
        @DisplayName("성공 - 기본 카테고리가 생성된다")
        void success_defaultCategory_hasNullUserId() {
            // when
            Category category = Category.createDefault("급여", CategoryType.INCOME);

            // then
            assertThat(category.getUserId()).isNull();
            assertThat(category.isDefault()).isTrue();
        }

        @Test
        @DisplayName("실패 - 이름이 공백이면 예외 발생")
        void fail_blankName_throwsException() {
            // given
            String blankName = "  ";

            // when & then
            assertThatThrownBy(() -> Category.create(1L, blankName, CategoryType.EXPENSE))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(TRANSACTION_INVALID_CATEGORY_NAME));
        }

        @Test
        @DisplayName("실패 - 이름이 null이면 예외 발생")
        void fail_nullName_throwsException() {
            // when & then
            assertThatThrownBy(() -> Category.create(1L, null, CategoryType.EXPENSE))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(TRANSACTION_INVALID_CATEGORY_NAME));
        }
    }

    @Nested
    @DisplayName("카테고리 접근 검증")
    class ValidateAccessible {
        @Test
        @DisplayName("성공 - 기본 카테고리는 모든 유저가 접근 가능하다")
        void success_defaultCategory_allowsAnyUser() {
            // given
            Category category = Category.createDefault("급여", CategoryType.INCOME);

            // when & then
            assertThatNoException().isThrownBy(() -> category.validateAccessible(1L));
            assertThatNoException().isThrownBy(() -> category.validateAccessible(999L));
        }

        @Test
        @DisplayName("성공 - 본인 카테고리이면 예외 없음")
        void success_ownCategory_noException() {
            // given
            Category category = Category.create(1L, "식비", CategoryType.EXPENSE);

            // when & then
            assertThatNoException().isThrownBy(() -> category.validateAccessible(1L));
        }

        @Test
        @DisplayName("실패 - 다른 유저의 카테고리이면 예외 발생")
        void fail_otherUsersCategory_throwsException() {
            // given
            Category category = Category.create(1L, "식비", CategoryType.EXPENSE);

            // when & then
            assertThatThrownBy(() -> category.validateAccessible(999L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(TRANSACTION_NO_ACCESS));
        }
    }
}
