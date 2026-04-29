package com.ft.transaction.application;

import com.ft.common.event.TransactionCreatedEvent;
import com.ft.common.exception.CustomException;
import com.ft.common.metric.annotation.Monitored;
import com.ft.transaction.application.dto.CreateTransactionCommand;
import com.ft.transaction.application.dto.TransactionResult;
import com.ft.transaction.application.dto.UpdateTransactionCommand;
import com.ft.transaction.application.port.CategoryRepository;
import com.ft.transaction.application.port.TransactionRepository;
import com.ft.transaction.domain.Category;
import com.ft.transaction.domain.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.ft.common.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionEventPublisher eventPublisher;

    @Monitored(domain = "transaction", layer = "service", api = "create")
    @Transactional
    public TransactionResult create(Long userId, CreateTransactionCommand command) {
        String categoryName = null;
        if (command.categoryId() != null) {
            Category category = categoryRepository.findById(command.categoryId())
                    .orElseThrow(() -> new CustomException(TRANSACTION_NOT_FOUND));
            category.validateAccessible(userId);
            categoryName = category.getName();
        }

        Transaction transaction = Transaction.create(
                userId,
                command.accountId(),
                command.toAccountId(),
                command.categoryId(),
                command.type(),
                command.amount(),
                command.description(),
                command.transactionDate()
        );

        TransactionResult result = TransactionResult.from(transactionRepository.save(transaction));

        eventPublisher.publish(new TransactionCreatedEvent(
                UUID.randomUUID().toString(),
                userId,
                command.accountId(),
                command.toAccountId(),
                result.id(),
                command.amount(),
                command.type().name(),
                command.categoryId(),
                categoryName,
                LocalDateTime.now()
        ));

        return result;
    }

    @Monitored(domain = "transaction", layer = "service", api = "find_all")
    @Transactional(readOnly = true)
    public List<TransactionResult> findAll(Long userId, Long accountId) {
        List<Transaction> transactions = accountId != null
                ? transactionRepository.findAllByUserIdAndAccountId(userId, accountId)
                : transactionRepository.findAllByUserId(userId);
        return transactions.stream().map(TransactionResult::from).toList();
    }

    @Monitored(domain = "transaction", layer = "service", api = "find_by_id")
    @Transactional(readOnly = true)
    public TransactionResult findById(Long userId, Long transactionId) {
        Transaction transaction = getTransaction(transactionId);
        transaction.validateOwner(userId);
        return TransactionResult.from(transaction);
    }

    @Monitored(domain = "transaction", layer = "service", api = "update")
    @Transactional
    public TransactionResult update(Long userId, Long transactionId, UpdateTransactionCommand command) {
        Transaction transaction = getTransaction(transactionId);
        transaction.validateOwner(userId);

        if (command.description() != null) {
            transaction.updateDescription(command.description());
        }
        if (command.transactionDate() != null) {
            transaction.updateTransactionDate(command.transactionDate());
        }
        if (command.amount() != null) {
            transaction.updateAmount(command.amount());
        }

        return TransactionResult.from(transaction);
    }

    @Monitored(domain = "transaction", layer = "service", api = "delete")
    @Transactional
    public void delete(Long userId, Long transactionId) {
        Transaction transaction = getTransaction(transactionId);
        transaction.validateOwner(userId);
        transactionRepository.delete(transaction);
    }

    private Transaction getTransaction(Long transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new CustomException(TRANSACTION_NOT_FOUND));
    }
}
