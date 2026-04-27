package com.ft.transaction.application;

import com.ft.common.exception.CustomException;
import com.ft.transaction.application.dto.CategoryResult;
import com.ft.transaction.application.dto.CreateCategoryCommand;
import com.ft.transaction.application.port.CategoryRepository;
import com.ft.transaction.domain.Category;
import com.ft.transaction.domain.CategoryType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.ft.common.exception.ErrorCode.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService 단위 테스트")
class CategoryServiceTest {

    @Mock CategoryRepository categoryRepository;
    @InjectMocks CategoryService categoryService;

    private Category userCategory(String name) {
        return Category.create(1L, name, CategoryType.EXPENSE);
    }

    private Category defaultCategory(String name) {
        return Category.createDefault(name, CategoryType.INCOME);
    }

    @Nested
    @DisplayName("카테고리 생성")
    class Create {

        @Test
        @DisplayName("성공 - 카테고리가 저장되고 결과를 반환한다")
        void create_whenValidCommand_returnsCategoryResult() {
            // given
            CreateCategoryCommand command = new CreateCategoryCommand("식비", CategoryType.EXPENSE);
            Category saved = userCategory("식비");
            given(categoryRepository.save(any(Category.class))).willReturn(saved);

            // when
            CategoryResult result = categoryService.create(1L, command);

            // then
            assertThat(result.name()).isEqualTo("식비");
            assertThat(result.isDefault()).isFalse();
        }
    }

    @Nested
    @DisplayName("카테고리 목록 조회")
    class FindAll {

        @Test
        @DisplayName("성공 - 본인 카테고리와 기본 카테고리를 함께 반환한다")
        void findAll_whenCategoriesExist_returnsAccessibleCategories() {
            // given
            given(categoryRepository.findAllAccessibleByUserId(1L))
                    .willReturn(List.of(userCategory("식비"), defaultCategory("급여")));

            // when
            List<CategoryResult> results = categoryService.findAll(1L);

            // then
            assertThat(results).hasSize(2);
        }
    }

    @Nested
    @DisplayName("카테고리 삭제")
    class Delete {

        @Test
        @DisplayName("성공 - 본인의 커스텀 카테고리이면 삭제를 호출한다")
        void delete_whenValidOwnerAndCustomCategory_deletesCategory() {
            // given
            Category category = userCategory("식비");
            given(categoryRepository.findById(10L)).willReturn(Optional.of(category));

            // when
            categoryService.delete(1L, 10L);

            // then
            then(categoryRepository).should().delete(category);
        }

        @Test
        @DisplayName("실패 - 카테고리가 존재하지 않으면 예외 발생")
        void delete_whenNotFound_throwsCustomException() {
            // given
            given(categoryRepository.findById(10L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> categoryService.delete(1L, 10L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(TRANSACTION_NOT_FOUND));
        }

        @Test
        @DisplayName("실패 - 다른 사용자의 카테고리이면 예외 발생")
        void delete_whenWrongOwner_throwsCustomException() {
            // given
            Category category = userCategory("식비");
            given(categoryRepository.findById(10L)).willReturn(Optional.of(category));

            // when & then
            assertThatThrownBy(() -> categoryService.delete(999L, 10L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(TRANSACTION_NO_ACCESS));
        }

        @Test
        @DisplayName("실패 - 기본 카테고리는 삭제할 수 없다")
        void delete_whenDefaultCategory_throwsCustomException() {
            // given
            Category category = defaultCategory("급여");
            given(categoryRepository.findById(10L)).willReturn(Optional.of(category));

            // when & then
            assertThatThrownBy(() -> categoryService.delete(1L, 10L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(TRANSACTION_NO_ACCESS));
        }
    }
}
