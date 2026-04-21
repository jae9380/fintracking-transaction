package com.ft.transaction.infrastructure.persistence;

import com.ft.transaction.domain.Transaction;
import com.ft.transaction.domain.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface JpaTransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findAllByUserId(Long userId);
    List<Transaction> findAllByUserIdAndAccountId(Long userId, Long accountId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.userId = :userId AND t.categoryId = :categoryId AND t.type = :type " +
           "AND YEAR(t.transactionDate) = :year AND MONTH(t.transactionDate) = :month")
    BigDecimal sumAmountByUserIdAndCategoryIdAndTypeAndYearMonth(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("type") TransactionType type,
            @Param("year") int year,
            @Param("month") int month);

    // 월별 전체 수입/지출 합계 (배치 집계용)
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.userId = :userId AND t.type = :type " +
           "AND YEAR(t.transactionDate) = :year AND MONTH(t.transactionDate) = :month")
    BigDecimal sumByUserIdAndTypeAndYearMonth(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            @Param("year") int year,
            @Param("month") int month);

    // 카테고리별 지출 합계 (배치 집계용) — [categoryId, sum] 형태로 반환
    @Query("SELECT t.categoryId, COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.userId = :userId AND t.type = 'EXPENSE' " +
           "AND YEAR(t.transactionDate) = :year AND MONTH(t.transactionDate) = :month " +
           "GROUP BY t.categoryId")
    List<Object[]> sumExpenseGroupedByCategoryAndYearMonth(
            @Param("userId") Long userId,
            @Param("year") int year,
            @Param("month") int month);
}
