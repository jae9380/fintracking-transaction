package com.ft.transaction.application;

// TODO: [Kafka 도입 시] AccountRepository, Account import 제거
// MSA 구조에서 transaction-service는 account-service의 DB에 직접 접근하면 안 됨
// account 관련 의존성은 Kafka 이벤트로 대체되어야 함
import com.ft.account.application.port.AccountRepository;
import com.ft.account.domain.Account;
import com.ft.common.exception.CustomException;
import com.ft.transaction.application.dto.CreateTransactionCommand;
import com.ft.transaction.application.dto.TransactionResult;
import com.ft.transaction.application.dto.UpdateTransactionCommand;
import com.ft.transaction.application.event.TransactionCreatedEvent;
import com.ft.transaction.application.port.CategoryRepository;
import com.ft.transaction.application.port.TransactionRepository;
import com.ft.transaction.domain.Category;
import com.ft.transaction.domain.Transaction;
import com.ft.transaction.domain.TransactionType;
import lombok.RequiredArgsConstructor;
// TODO: [Kafka 도입 시] ApplicationEventPublisher → KafkaTemplate<String, Object>으로 교체
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static com.ft.common.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    // TODO: [Kafka 도입 시] AccountRepository 제거
    // account-service와의 직접 의존성 — MSA 원칙 위반
    // account 소유권 검증은 account-service 책임으로 이전
    // 잔액 변경은 BalanceUpdateEvent Kafka 발행으로 대체
//    private final AccountRepository accountRepository;
    // TODO: [Kafka 도입 시] KafkaTemplate<String, Object> kafkaTemplate 으로 교체
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public TransactionResult create(Long userId, CreateTransactionCommand command) {
        // TODO: [Kafka 도입 시] 아래 두 줄 제거
        // account 소유권 검증 책임은 account-service로 이전
        // transaction-service는 Gateway의 X-User-Id 헤더로 userId 신뢰
        Account account = getAccount(command.accountId());
        account.validateOwner(userId);

        if (command.categoryId() != null) {
            Category category = categoryRepository.findById(command.categoryId())
                    .orElseThrow(() -> new CustomException(TRANSACTION_NOT_FOUND));
            category.validateAccessible(userId);
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

        // TODO: [Kafka 도입 시] updateAccountBalance() 제거 후 아래 Kafka 발행으로 대체
        // kafkaTemplate.send("account.balance.update", new BalanceUpdateEvent(
        //         userId, command.accountId(), command.toAccountId(),
        //         command.type(), command.amount()
        // ));
        updateAccountBalance(userId, account, command);

        TransactionResult result = TransactionResult.from(transactionRepository.save(transaction));

        // TODO: [Kafka 도입 시] eventPublisher → kafkaTemplate.send()로 교체
        // kafkaTemplate.send("transaction.created", new TransactionCreatedEvent(...));
        // budget-service, notification-service가 해당 토픽을 구독
        eventPublisher.publishEvent(new TransactionCreatedEvent(
                userId,
                command.categoryId(),
                command.type(),
                command.amount(),
                command.transactionDate()
        ));

        return result;
    }

    @Transactional(readOnly = true)
    public List<TransactionResult> findAll(Long userId, Long accountId) {
        List<Transaction> transactions = accountId != null
                ? transactionRepository.findAllByUserIdAndAccountId(userId, accountId)
                : transactionRepository.findAllByUserId(userId);
        return transactions.stream().map(TransactionResult::from).toList();
    }

    @Transactional(readOnly = true)
    public TransactionResult findById(Long userId, Long transactionId) {
        Transaction transaction = getTransaction(transactionId);
        transaction.validateOwner(userId);
        return TransactionResult.from(transaction);
    }

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
            BigDecimal oldAmount = transaction.getAmount();
            transaction.updateAmount(command.amount());
            // TODO: [Kafka 도입 시] reconcileBalance() 제거 후 아래 Kafka 발행으로 대체
            // kafkaTemplate.send("account.balance.reconcile", new BalanceReconcileEvent(
            //         userId, transaction.getAccountId(), transaction.getType(), oldAmount, command.amount()
            // ));
            reconcileBalance(userId, transaction, oldAmount, command.amount());
        }

        return TransactionResult.from(transaction);
    }

    @Transactional
    public void delete(Long userId, Long transactionId) {
        Transaction transaction = getTransaction(transactionId);
        transaction.validateOwner(userId);

        // TODO: [Kafka 도입 시] restoreAccountBalance() 제거 후 아래 Kafka 발행으로 대체
        // kafkaTemplate.send("account.balance.restore", new BalanceRestoreEvent(
        //         userId, transaction.getAccountId(), transaction.getToAccountId(),
        //         transaction.getType(), transaction.getAmount()
        // ));
        restoreAccountBalance(userId, transaction);

        transactionRepository.delete(transaction);
    }

    // TODO: [Kafka 도입 시] 메서드 전체 제거 — account-service의 Kafka Consumer로 이전
    // account-service: BalanceUpdateEvent 수신 후 deposit/withdraw 처리
    private void updateAccountBalance(Long userId, Account account, CreateTransactionCommand command) {
        if (command.type() == TransactionType.INCOME) {
            account.deposit(command.amount());
        } else if (command.type() == TransactionType.EXPENSE) {
            account.withdraw(command.amount());
        } else if (command.type() == TransactionType.TRANSFER && command.toAccountId() != null) {
            account.withdraw(command.amount());
            Account toAccount = getAccount(command.toAccountId());
            toAccount.validateOwner(userId);
            toAccount.deposit(command.amount());
        }
    }

    // TODO: [Kafka 도입 시] 메서드 전체 제거 — account-service의 Kafka Consumer로 이전
    // account-service: BalanceRestoreEvent 수신 후 반대 방향으로 deposit/withdraw 처리
    private void restoreAccountBalance(Long userId, Transaction transaction) {
        Account account = getAccount(transaction.getAccountId());
        if (transaction.getType() == TransactionType.INCOME) {
            account.withdraw(transaction.getAmount());
        } else if (transaction.getType() == TransactionType.EXPENSE) {
            account.deposit(transaction.getAmount());
        } else if (transaction.getType() == TransactionType.TRANSFER) {
            account.deposit(transaction.getAmount());
            if (transaction.getToAccountId() != null) {
                Account toAccount = getAccount(transaction.getToAccountId());
                toAccount.validateOwner(userId);
                toAccount.withdraw(transaction.getAmount());
            }
        }
    }

    // TODO: [Kafka 도입 시] 메서드 전체 제거 — account-service의 Kafka Consumer로 이전
    // account-service: BalanceReconcileEvent 수신 후 oldAmount/newAmount 차이만큼 잔액 재조정
    private void reconcileBalance(Long userId, Transaction transaction, BigDecimal oldAmount, BigDecimal newAmount) {
        BigDecimal diff = newAmount.subtract(oldAmount);
        if (diff.compareTo(BigDecimal.ZERO) == 0) return;

        Account account = getAccount(transaction.getAccountId());
        account.validateOwner(userId);

        if (transaction.getType() == TransactionType.INCOME) {
            if (diff.compareTo(BigDecimal.ZERO) > 0) account.deposit(diff);
            else account.withdraw(diff.abs());
        } else if (transaction.getType() == TransactionType.EXPENSE) {
            if (diff.compareTo(BigDecimal.ZERO) > 0) account.withdraw(diff);
            else account.deposit(diff.abs());
        }
    }

    private Transaction getTransaction(Long transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new CustomException(TRANSACTION_NOT_FOUND));
    }

    // TODO: [Kafka 도입 시] 메서드 전체 제거
    // accountId만 저장하고 실제 Account 객체 조회는 account-service 책임
    private Account getAccount(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new CustomException(ACCOUNT_NOT_FOUND));
    }
}
