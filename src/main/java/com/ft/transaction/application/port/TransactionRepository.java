package com.ft.transaction.application.port;

import com.ft.transaction.domain.Transaction;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository {
    Transaction save(Transaction transaction);
    Optional<Transaction> findById(Long id);
    List<Transaction> findAllByUserId(Long userId);
    List<Transaction> findAllByUserIdAndAccountId(Long userId, Long accountId);
    void delete(Transaction transaction);
    BigDecimal sumExpenseByUserIdAndCategoryIdAndYearMonth(Long userId, Long categoryId, int year, int month);
}
