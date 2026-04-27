# fintracking-transaction

거래 내역 CRUD, Kafka 이벤트 발행

---

## 패턴: DDD + Event Driven

- 거래 생성 후 `TransactionCreatedEvent` 발행 (Kafka)
- 잔액 업데이트, 예산 집계 등 부가 로직은 **이 서비스에서 직접 처리하지 않음**
- account-service, budget-service가 이벤트를 구독하여 처리

---

## Kafka Producer

`TransactionEventPublisher` — `AbstractEventPublisher<TransactionCreatedEvent>` 상속

```java
// TransactionCreatedEvent (fintracking-common)
record TransactionCreatedEvent(
    String eventId,      // UUID
    Long userId,
    Long accountId,
    Long toAccountId,    // TRANSFER 시 대상 계좌
    Long transactionId,
    BigDecimal amount,
    String type,         // INCOME / EXPENSE / TRANSFER
    Long categoryId,
    LocalDateTime occurredAt
)
```

- 토픽: `KafkaTopic.TRANSACTION_CREATED` (`transaction.created`)
- `toAccountId`: TRANSFER 타입이 아닌 경우 `null`

---

## 거래 타입

```
INCOME   — 수입
EXPENSE  — 지출
TRANSFER — 이체 (accountId → toAccountId)
```

---

## 이 서비스의 책임 범위

- 거래 내역 생성/조회/수정/삭제
- 이벤트 발행 (잔액 업데이트는 account-service 담당)
- 계좌 소유권 검증 (요청 userId == account.userId)

**금지**:
- AccountRepository 직접 주입 금지 (타 서비스 DB 접근)
- 잔액 업데이트 로직 이 서비스에서 처리 금지

---

## 패키지 구조

```
com.ft.transaction
  ├── domain/          — Transaction, Category 엔티티
  ├── application/     — TransactionService, TransactionEventPublisher
  ├── infrastructure/  — JPA
  └── presentation/    — TransactionController, DTO
```

---

## 주요 ErrorCode

```java
TRANSACTION_NOT_FOUND(404, "TRANSACTION_001", "Transaction not found")
INVALID_TRANSFER(400, "TRANSACTION_002", "Invalid transfer request")
CATEGORY_NOT_FOUND(404, "TRANSACTION_003", "Category not found")
```
