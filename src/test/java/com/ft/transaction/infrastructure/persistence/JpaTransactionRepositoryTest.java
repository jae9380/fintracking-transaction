package com.ft.transaction.infrastructure.persistence;

import com.ft.transaction.domain.Transaction;
import com.ft.transaction.domain.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("JpaTransactionRepository 테스트")
class JpaTransactionRepositoryTest {
    @Autowired
    JpaTransactionRepository jpaTransactionRepository;

    private Transaction income(Long userId, Long accountId, Long categoryId, LocalDate date) {
        return Transaction.create(userId, accountId, null, categoryId,
                TransactionType.INCOME, new BigDecimal("10000"), "수입", date);
    }

    private Transaction expense(Long userId, Long accountId, Long categoryId, LocalDate date) {
        return Transaction.create(userId, accountId, null, categoryId,
                TransactionType.EXPENSE, new BigDecimal("5000"), "지출", date);
    }

    @Nested
    @DisplayName("유저 ID로 거래 목록 조회")
    class FindAllByUserId {
        @Test
        @DisplayName("성공 - 해당 유저의 거래만 반환한다")
        void findAllByUserId_whenTransactionsExist_returnsOnlyUserTransactions() {
            // given
            jpaTransactionRepository.saveAll(List.of(
                    income(1L, 10L, 5L, LocalDate.now()),
                    expense(1L, 10L, 6L, LocalDate.now()),
                    income(2L, 20L, 5L, LocalDate.now())
            ));

            // when
            List<Transaction> results = jpaTransactionRepository.findAllByUserId(1L);

            // then
            assertThat(results).hasSize(2);
            assertThat(results).allMatch(t -> t.getUserId().equals(1L));
        }
    }

    @Nested
    @DisplayName("유저 ID와 계좌 ID로 거래 목록 조회")
    class FindAllByUserIdAndAccountId {
        @Test
        @DisplayName("성공 - 해당 계좌의 거래만 반환한다")
        void findAllByUserIdAndAccountId_whenFilteredByAccount_returnsFilteredTransactions() {
            // given
            jpaTransactionRepository.saveAll(List.of(
                    income(1L, 10L, 5L, LocalDate.now()),
                    expense(1L, 20L, 6L, LocalDate.now())
            ));

            // when
            List<Transaction> results = jpaTransactionRepository.findAllByUserIdAndAccountId(1L, 10L);

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getAccountId()).isEqualTo(10L);
        }
    }

    @Nested
    @DisplayName("월별 카테고리 지출 합계 조회")
    class SumAmountByYearMonth {
        @Test
        @DisplayName("성공 - 해당 카테고리의 당월 지출 합계를 반환한다")
        void sumAmount_whenExpenseExists_returnsCorrectSum() {
            // given
            LocalDate thisMonth = LocalDate.now();
            jpaTransactionRepository.saveAll(List.of(
                    expense(1L, 10L, 5L, thisMonth),
                    expense(1L, 10L, 5L, thisMonth),
                    expense(1L, 10L, 6L, thisMonth)  // 다른 카테고리
            ));

            // when
            BigDecimal sum = jpaTransactionRepository.sumAmountByUserIdAndCategoryIdAndTypeAndYearMonth(
                    1L, 5L, TransactionType.EXPENSE, thisMonth.getYear(), thisMonth.getMonthValue());

            // then
            assertThat(sum).isEqualByComparingTo(new BigDecimal("10000"));
        }

        @Test
        @DisplayName("성공 - 해당하는 거래가 없으면 0을 반환한다")
        void sumAmount_whenNoMatch_returnsZero() {
            // when
            BigDecimal sum = jpaTransactionRepository.sumAmountByUserIdAndCategoryIdAndTypeAndYearMonth(
                    1L, 999L, TransactionType.EXPENSE, LocalDate.now().getYear(), LocalDate.now().getMonthValue());

            // then
            assertThat(sum).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}
