package com.ft.transaction.application;

import com.ft.common.exception.CustomException;
import com.ft.common.exception.ErrorCode;
import com.ft.transaction.application.dto.CreateTransactionCommand;
import com.ft.transaction.application.dto.TransactionResult;
import com.ft.transaction.application.dto.UpdateTransactionCommand;
import com.ft.transaction.application.port.CategoryRepository;
import com.ft.transaction.application.port.TransactionRepository;
import com.ft.transaction.domain.Category;
import com.ft.transaction.domain.CategoryType;
import com.ft.transaction.domain.Transaction;
import com.ft.transaction.domain.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService 단위 테스트")
class TransactionServiceTest {
    @Mock TransactionRepository transactionRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock TransactionEventPublisher eventPublisher;
    @InjectMocks TransactionService transactionService;

    private Transaction incomeTransaction(Long userId) {
        return Transaction.create(userId, 10L, null, 5L, TransactionType.INCOME,
                new BigDecimal("10000"), "월급", LocalDate.now());
    }

    @Nested
    @DisplayName("거래 생성")
    class Create {
        @Test
        @DisplayName("성공 - 거래를 저장하고 이벤트를 발행한다")
        void create_whenValidCommand_savesAndPublishesEvent() {
            // given
            CreateTransactionCommand command = new CreateTransactionCommand(
                    10L, 5L, null, TransactionType.INCOME, new BigDecimal("10000"), "월급", LocalDate.now());
            Category category = Category.create(1L, "급여", CategoryType.INCOME);
            Transaction saved = incomeTransaction(1L);
            given(categoryRepository.findById(5L)).willReturn(Optional.of(category));
            given(transactionRepository.save(any(Transaction.class))).willReturn(saved);

            // when
            transactionService.create(1L, command);

            // then
            then(transactionRepository).should().save(any(Transaction.class));
            then(eventPublisher).should().publish(any());
        }

        @Test
        @DisplayName("실패 - 카테고리가 존재하지 않으면 예외 발생")
        void create_whenCategoryNotFound_throwsCustomException() {
            // given
            CreateTransactionCommand command = new CreateTransactionCommand(
                    10L, 999L, null, TransactionType.INCOME, new BigDecimal("10000"), "월급", LocalDate.now());
            given(categoryRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> transactionService.create(1L, command))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ErrorCode.TRANSACTION_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("거래 목록 조회")
    class FindAll {
        @Test
        @DisplayName("성공 - 유저의 모든 거래를 반환한다")
        void findAll_whenByUserId_returnsAllTransactions() {
            // given
            given(transactionRepository.findAllByUserId(1L))
                    .willReturn(List.of(incomeTransaction(1L), incomeTransaction(1L)));

            // when
            List<TransactionResult> results = transactionService.findAll(1L, null);

            // then
            assertThat(results).hasSize(2);
        }

        @Test
        @DisplayName("성공 - 계좌ID로 필터링된 거래를 반환한다")
        void findAll_whenByUserIdAndAccountId_returnsFilteredTransactions() {
            // given
            given(transactionRepository.findAllByUserIdAndAccountId(1L, 10L))
                    .willReturn(List.of(incomeTransaction(1L)));

            // when
            List<TransactionResult> results = transactionService.findAll(1L, 10L);

            // then
            assertThat(results).hasSize(1);
        }
    }

    @Nested
    @DisplayName("거래 단건 조회")
    class FindById {
        @Test
        @DisplayName("성공 - 본인의 거래이면 결과를 반환한다")
        void findById_whenValidOwner_returnsTransactionResult() {
            // given
            Transaction tx = incomeTransaction(1L);
            given(transactionRepository.findById(100L)).willReturn(Optional.of(tx));

            // when
            TransactionResult result = transactionService.findById(1L, 100L);

            // then
            assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("10000"));
        }

        @Test
        @DisplayName("실패 - 다른 사용자의 거래이면 예외 발생")
        void findById_whenWrongOwner_throwsCustomException() {
            // given
            Transaction tx = incomeTransaction(1L);
            given(transactionRepository.findById(100L)).willReturn(Optional.of(tx));

            // when & then
            assertThatThrownBy(() -> transactionService.findById(999L, 100L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ErrorCode.TRANSACTION_NO_ACCESS));
        }

        @Test
        @DisplayName("실패 - 거래가 존재하지 않으면 예외 발생")
        void findById_whenNotFound_throwsCustomException() {
            // given
            given(transactionRepository.findById(100L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> transactionService.findById(1L, 100L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ErrorCode.TRANSACTION_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("거래 수정")
    class Update {
        @Test
        @DisplayName("성공 - 유효한 소유자이면 거래가 수정된다")
        void update_whenValidOwner_updatesTransaction() {
            // given
            Transaction tx = incomeTransaction(1L);
            UpdateTransactionCommand command = new UpdateTransactionCommand(
                    new BigDecimal("20000"), "수정 메모", LocalDate.now().minusDays(1));
            given(transactionRepository.findById(100L)).willReturn(Optional.of(tx));

            // when
            TransactionResult result = transactionService.update(1L, 100L, command);

            // then
            assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("20000"));
        }

        @Test
        @DisplayName("실패 - 다른 사용자이면 예외 발생")
        void update_whenWrongOwner_throwsCustomException() {
            // given
            Transaction tx = incomeTransaction(1L);
            UpdateTransactionCommand command = new UpdateTransactionCommand(null, null, null);
            given(transactionRepository.findById(100L)).willReturn(Optional.of(tx));

            // when & then
            assertThatThrownBy(() -> transactionService.update(999L, 100L, command))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ErrorCode.TRANSACTION_NO_ACCESS));
        }
    }

    @Nested
    @DisplayName("거래 삭제")
    class Delete {
        @Test
        @DisplayName("성공 - 유효한 소유자이면 삭제를 호출한다")
        void delete_whenValidOwner_deletesTransaction() {
            // given
            Transaction tx = incomeTransaction(1L);
            given(transactionRepository.findById(100L)).willReturn(Optional.of(tx));

            // when
            transactionService.delete(1L, 100L);

            // then
            then(transactionRepository).should().delete(tx);
        }

        @Test
        @DisplayName("실패 - 다른 사용자이면 예외 발생")
        void delete_whenWrongOwner_throwsCustomException() {
            // given
            Transaction tx = incomeTransaction(1L);
            given(transactionRepository.findById(100L)).willReturn(Optional.of(tx));

            // when & then
            assertThatThrownBy(() -> transactionService.delete(999L, 100L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ErrorCode.TRANSACTION_NO_ACCESS));
        }
    }
}
