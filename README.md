# fintracking-transaction

> 거래 내역 CRUD · 카테고리 관리 · Kafka 이벤트 발행

---

## 사용 기술

| 분류        | 기술                        |
| ----------- | --------------------------- |
| 메시지      | Apache Kafka (Producer)     |
| 설정 관리   | Spring Cloud Config         |
| 서비스 등록 | Spring Cloud Eureka Client  |
| 문서화      | SpringDoc OpenAPI (Swagger) |

---

## 거래 생성 후 이벤트 흐름

거래가 생성되면 이 서비스는 Kafka로 이벤트를 발행하고,
관련 서비스들이 각자 독립적으로 반응합니다.

```
클라이언트
    │
    │  POST /transaction-service/api/v1/transactions
    ▼
TransactionController
    │
    ▼
TransactionService
    │  1. Transaction 엔티티 생성 + 검증
    │  2. DB 저장
    │  3. Kafka 이벤트 발행 ───────────────────────────────┐
    │                                                   │
    ▼                                               topic: transaction.created
[응답 반환]                                                │
                                     ┌───────────────────┴──────────────────┐
                                     │                                      │
                                     ▼                                      ▼
                               [account-service]                      [budget-service]
                               잔액 업데이트                           월간 지출 누적
                               (입금/출금/이체)                        → 예산 초과 시 알림 발행
```

**핵심 원칙**: 거래 서비스는 계좌 잔액이나 예산을 직접 건드리지 않습니다.
이벤트를 발행하고, 각 서비스가 알아서 처리 → **느슨한 결합**

---

## 도메인 모델

### Transaction (거래)

```java
Transaction
├── id              (Long)           ← PK
├── userId          (Long)           ← 소유자 ID
├── accountId       (Long)           ← 출금/입금 계좌 ID
├── toAccountId     (Long, nullable) ← 이체 대상 계좌 (TRANSFER 타입에만 사용)
├── categoryId      (Long, nullable) ← 카테고리 (INCOME/EXPENSE만 사용)
├── type            (TransactionType)← INCOME / EXPENSE / TRANSFER
├── amount          (BigDecimal)     ← 거래 금액 (0 초과)
├── description     (String)         ← 메모
└── transactionDate (LocalDate)      ← 거래 발생일
```

### 거래 타입별 검증 규칙

```
INCOME (수입)
  ├── categoryId 필수
  └── toAccountId 불필요

EXPENSE (지출)
  ├── categoryId 필수
  └── toAccountId 불필요

TRANSFER (이체)
  ├── toAccountId 필수 → 없으면 TRANSACTION_INVALID_TRANSFER 에러
  └── categoryId 불필요
```

### Category (카테고리)

```java
Category
├── id           (Long)
├── userId       (Long)         ← null이면 시스템 기본 카테고리
├── name         (String)       ← "식비", "교통", "월급" 등
└── type         (CategoryType) ← INCOME / EXPENSE / DEFAULT
```

---

## Kafka 이벤트 발행

### TransactionCreatedEvent

```java
// fintracking-common에 정의된 레코드
record TransactionCreatedEvent(
    String eventId,          // UUID (멱등성 보장)
    Long userId,
    Long accountId,
    Long toAccountId,        // nullable (TRANSFER 시만 존재)
    Long transactionId,
    BigDecimal amount,
    String type,             // "INCOME" / "EXPENSE" / "TRANSFER"
    Long categoryId,         // nullable
    LocalDateTime occurredAt
)
```

**발행 방법**: `AbstractEventPublisher<TransactionCreatedEvent>` 를 상속한
`TransactionEventPublisher` 가 `transaction.created` 토픽으로 발행합니다.

```
TransactionEventPublisher
  extends AbstractEventPublisher<TransactionCreatedEvent>
    │
    └── topic() = "transaction.created"
    └── publish(event) → KafkaTemplate.send()
```

---

## 패키지 구조

```
com.ft.transaction
├── domain/
│   ├── Transaction.java       ← 거래 엔티티 + 검증 로직
│   ├── TransactionType.java   ← INCOME / EXPENSE / TRANSFER
│   ├── Category.java          ← 카테고리 엔티티
│   └── CategoryType.java      ← INCOME / EXPENSE / DEFAULT
│
├── application/
│   ├── TransactionService.java         ← 거래 CRUD + 이벤트 발행 유스케이스
│   ├── CategoryService.java            ← 카테고리 CRUD 유스케이스
│   ├── TransactionEventPublisher.java  ← Kafka Producer
│   ├── port/
│   │   ├── TransactionRepository.java  ← DB 접근 추상화
│   │   └── CategoryRepository.java
│   └── dto/
│       ├── CreateTransactionCommand.java
│       ├── UpdateTransactionCommand.java
│       ├── TransactionResult.java
│       ├── CreateCategoryCommand.java
│       └── CategoryResult.java
│
├── infrastructure/
│   └── persistence/
│       ├── JpaTransactionRepository.java
│       ├── TransactionRepositoryImpl.java
│       ├── JpaCategoryRepository.java
│       └── CategoryRepositoryImpl.java
│
└── presentation/
    ├── TransactionController.java   ← 거래 API
    ├── CategoryController.java      ← 카테고리 API
    └── dto/                         ← Request/Response DTO
```

---

## API 엔드포인트

### 거래 API

| 메서드 | 경로                                            | 설명                            |
| ------ | ----------------------------------------------- | ------------------------------- |
| GET    | `/transaction-service/api/v1/transactions`      | 거래 목록 조회 (계좌 필터 가능) |
| POST   | `/transaction-service/api/v1/transactions`      | 거래 생성                       |
| GET    | `/transaction-service/api/v1/transactions/{id}` | 거래 단건 조회                  |
| PUT    | `/transaction-service/api/v1/transactions/{id}` | 거래 수정                       |
| DELETE | `/transaction-service/api/v1/transactions/{id}` | 거래 삭제                       |

### 카테고리 API

| 메서드 | 경로                                     | 설명               |
| ------ | ---------------------------------------- | ------------------ |
| GET    | `/transaction-service/api/v1/categories` | 카테고리 목록 조회 |
| POST   | `/transaction-service/api/v1/categories` | 카테고리 생성      |

---

## 에러 코드

| 코드                             | HTTP | 설명                      |
| -------------------------------- | ---- | ------------------------- |
| `TRANSACTION_NOT_FOUND`          | 404  | 거래를 찾을 수 없음       |
| `TRANSACTION_NO_ACCESS`          | 403  | 접근 권한 없음            |
| `TRANSACTION_INVALID_TRANSFER`   | 400  | 이체 대상 계좌 누락       |
| `TRANSACTION_CATEGORY_REQUIRED`  | 400  | 수입/지출에 카테고리 누락 |
| `TRANSACTION_INVALID_AMOUNT`     | 400  | 금액이 0 이하             |
| `TRANSACTION_CATEGORY_NOT_FOUND` | 404  | 카테고리를 찾을 수 없음   |

---

## 테스트

```
test/
├── domain/
│   ├── TransactionTest.java        ← 거래 검증 규칙 단위 테스트
│   └── CategoryTest.java           ← 카테고리 검증 단위 테스트
├── application/
│   ├── TransactionServiceTest.java ← 유스케이스 (Mockito)
│   └── CategoryServiceTest.java
├── infrastructure/
│   └── JpaTransactionRepositoryTest.java ← @DataJpaTest + H2
└── kafka/
    └── KafkaTopicLogTest.java      ← 토픽 상수 검증
```
