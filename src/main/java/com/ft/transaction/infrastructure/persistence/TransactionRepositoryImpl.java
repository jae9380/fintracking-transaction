package com.ft.transaction.infrastructure.persistence;

import com.ft.transaction.application.port.TransactionRepository;
import com.ft.transaction.domain.Transaction;
import com.ft.transaction.domain.TransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TransactionRepositoryImpl implements TransactionRepository {

    private final JpaTransactionRepository jpaTransactionRepository;

    @Override
    public Transaction save(Transaction transaction) {
        return jpaTransactionRepository.save(transaction);
    }

    @Override
    public Optional<Transaction> findById(Long id) {
        return jpaTransactionRepository.findById(id);
    }

    @Override
    public List<Transaction> findAllByUserId(Long userId) {
        return jpaTransactionRepository.findAllByUserId(userId);
    }

    @Override
    public List<Transaction> findAllByUserIdAndAccountId(Long userId, Long accountId) {
        return jpaTransactionRepository.findAllByUserIdAndAccountId(userId, accountId);
    }

    @Override
    public void delete(Transaction transaction) {
        jpaTransactionRepository.delete(transaction);
    }

    @Override
    public BigDecimal sumExpenseByUserIdAndCategoryIdAndYearMonth(Long userId, Long categoryId, int year, int month) {
        return jpaTransactionRepository.sumAmountByUserIdAndCategoryIdAndTypeAndYearMonth(
                userId, categoryId, TransactionType.EXPENSE, year, month);
    }
}
