package com.ft.transaction.domain;

import com.ft.common.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.ft.common.exception.ErrorCode.*;
import static org.assertj.core.api.Assertions.*;

@DisplayName("Transaction 도메인 테스트")
class TransactionTest {
    private static final LocalDate TODAY = LocalDate.now();
    @Nested
    @DisplayName("거래 생성")
    class Create {

        @Test
        @DisplayName("성공 - 수입 거래가 생성된다")
        void success_income_createsTransaction() {
            // when
            Transaction tx = Transaction.create(1L, 10L, null, 5L, TransactionType.INCOME,
                    new BigDecimal("10000"), "월급", TODAY);

            // then
            assertThat(tx.getUserId()).isEqualTo(1L);
            assertThat(tx.getType()).isEqualTo(TransactionType.INCOME);
            assertThat(tx.getAmount()).isEqualByComparingTo(new BigDecimal("10000"));
        }

        @Test
        @DisplayName("성공 - 이체 거래는 카테고리 없이 생성된다")
        void success_transfer_createsWithoutCategory() {
            // when
            Transaction tx = Transaction.create(1L, 10L, 20L, null, TransactionType.TRANSFER,
                    new BigDecimal("5000"), "이체", TODAY);

            // then
            assertThat(tx.getType()).isEqualTo(TransactionType.TRANSFER);
            assertThat(tx.getCategoryId()).isNull();
        }

        @Test
        @DisplayName("실패 - 지출 거래에 카테고리가 없으면 예외 발생")
        void fail_expenseWithoutCategory_throwsException() {
            // when & then
            assertThatThrownBy(() -> Transaction.create(1L, 10L, null, null, TransactionType.EXPENSE,
                    new BigDecimal("5000"), "식비", TODAY))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(TRANSACTION_CATEGORY_REQUIRED));
        }

        @Test
        @DisplayName("실패 - 금액이 0이면 예외 발생")
        void fail_zeroAmount_throwsException() {
            // when & then
            assertThatThrownBy(() -> Transaction.create(1L, 10L, null, 5L, TransactionType.EXPENSE,
                    BigDecimal.ZERO, "식비", TODAY))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(TRANSACTION_INVALID_AMOUNT));
        }

        @Test
        @DisplayName("실패 - 금액이 null이면 예외 발생")
        void fail_nullAmount_throwsException() {
            // when & then
            assertThatThrownBy(() -> Transaction.create(1L, 10L, null, 5L, TransactionType.EXPENSE,
                    null, "식비", TODAY))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(TRANSACTION_INVALID_AMOUNT));
        }
    }

    @Nested
    @DisplayName("소유자 검증")
    class ValidateOwner {
        @Test
        @DisplayName("성공 - 본인이면 예외 없음")
        void success_sameUser_noException() {
            // given
            Transaction tx = Transaction.create(1L, 10L, null, 5L, TransactionType.INCOME,
                    new BigDecimal("10000"), "월급", TODAY);

            // when & then
            assertThatNoException().isThrownBy(() -> tx.validateOwner(1L));
        }

        @Test
        @DisplayName("실패 - 다른 사용자면 예외 발생")
        void fail_differentUser_throwsException() {
            // given
            Transaction tx = Transaction.create(1L, 10L, null, 5L, TransactionType.INCOME,
                    new BigDecimal("10000"), "월급", TODAY);

            // when & then
            assertThatThrownBy(() -> tx.validateOwner(999L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(TRANSACTION_NO_ACCESS));
        }
    }

    @Nested
    @DisplayName("금액 수정")
    class UpdateAmount {
        @Test
        @DisplayName("성공 - 금액이 변경된다")
        void success_validAmount_amountUpdated() {
            // given
            Transaction tx = Transaction.create(1L, 10L, null, 5L, TransactionType.INCOME,
                    new BigDecimal("10000"), "월급", TODAY);

            // when
            tx.updateAmount(new BigDecimal("20000"));

            // then
            assertThat(tx.getAmount()).isEqualByComparingTo(new BigDecimal("20000"));
        }

        @Test
        @DisplayName("실패 - 0원이면 예외 발생")
        void fail_zeroAmount_throwsException() {
            // given
            Transaction tx = Transaction.create(1L, 10L, null, 5L, TransactionType.INCOME,
                    new BigDecimal("10000"), "월급", TODAY);

            // when & then
            assertThatThrownBy(() -> tx.updateAmount(BigDecimal.ZERO))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(TRANSACTION_INVALID_AMOUNT));
        }
    }

    @Nested
    @DisplayName("거래일 수정")
    class UpdateTransactionDate {
        @Test
        @DisplayName("성공 - 거래일이 변경된다")
        void success_validDate_dateUpdated() {
            // given
            Transaction tx = Transaction.create(1L, 10L, null, 5L, TransactionType.INCOME,
                    new BigDecimal("10000"), "월급", TODAY);
            LocalDate newDate = TODAY.minusDays(1);

            // when
            tx.updateTransactionDate(newDate);

            // then
            assertThat(tx.getTransactionDate()).isEqualTo(newDate);
        }

        @Test
        @DisplayName("실패 - null이면 예외 발생")
        void fail_nullDate_throwsException() {
            // given
            Transaction tx = Transaction.create(1L, 10L, null, 5L, TransactionType.INCOME,
                    new BigDecimal("10000"), "월급", TODAY);

            // when & then
            assertThatThrownBy(() -> tx.updateTransactionDate(null))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(TRANSACTION_INVALID_DATE));
        }
    }
}
