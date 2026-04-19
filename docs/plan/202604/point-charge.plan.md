# Plan: 인연 충전 (구매)

> 작성일: 2026-04-17

## 1. 개요

`POST /api/points`로 회원이 "인연 상품(PointProduct)"을 구매하여 자기 계정의 인연 잔액(MemberPoint)을 적립하는 기능을 구현한다.

현재 인연 도메인에는 상품 조회(`PointQueryService.findProducts`)와 할인 정책만 존재하며, 회원별 잔액/거래 이력이라는 개념이 없다. 이번 계획에서 `MemberPoint`(잔액 애그리거트)와 `PointTransaction`(충전/소모 이력 애그리거트)을 인연(point) 도메인 하위에 추가한다. PG 연동은 별도 포트로 추상화하여 우선 Mock 구현체로 둔다.

### 도메인 분리 판단

별도 도메인을 새로 만들지 않고 **기존 `point` 도메인 하위 패키지**로 추가한다.
- 회원별 잔액/거래 이력은 인연(포인트) 도메인의 핵심 상태로, 이미 있는 PointProduct/DiscountPolicy와 같은 애그리거트 집합 안에 응집시킨다.
- 잔액·거래 이력은 독립된 상위 도메인이 아니라 "인연 도메인의 상태 변화"를 다루는 개념이다.
- 하위 패키지: `point/domain/balance/`, `point/domain/transaction/`로 책임 분리.

### 1차 구현 범위와 후속 과제 분리

**1차 구현 범위 (이 계획)**
- `POST /api/points` 충전 API
- `MemberPoint`(잔액), `PointTransaction`(이력) 도메인 + 테이블 신규
- PG 승인은 `PaymentApprover` 포트로 추상화하고 Mock 구현체 제공 (실제 연동은 후속)
- 멱등키(idempotencyKey)를 이용한 중복 요청 방지
- DB 비관적 잠금으로 동시 충전 시 잔액 정합성 확보
- 충전 이력만 저장 (소모는 추후)

**후속 과제 (이번 계획에서 제외)**
- `GET /api/points/me` 잔액 조회 API — 별도 PR
- 실제 PG사(토스페이먼츠/카카오페이) 연동 어댑터 — 별도 PR
- 인연 소모(차감) 로직 — 매칭/X룸 등 기능과 함께 도입
- 환불/취소 시나리오

---

## 2. API 설계

api-todo.md 기준 **인연 > 인연 API** 섹션의 `POST /api/points` 항목에 해당한다. 동일 섹션 참고사항("잔액 부족 시 에러 응답", "동시성 제어 필요", "PG사 연동 방식 결정 필요")을 반영한다.

| Method | Endpoint | 용도 | 인증 |
|--------|----------|------|------|
| POST | `/api/points` | 인연 충전 (구매) | 필요 |

**Request Body**:
```json
{
  "pointProductId": "암호화된 ID",
  "paymentToken": "PG사 결제 인증 토큰",
  "idempotencyKey": "클라이언트 생성 UUID"
}
```

- `pointProductId`: 구매할 상품 ID (`@DecryptId(POINT_PRODUCT)` 적용)
- `paymentToken`: PG사에서 결제 인증 후 받은 토큰. 서버는 이 토큰으로 PG 승인을 요청
- `idempotencyKey`: 클라이언트가 생성하는 UUID. 같은 키로 재요청해도 중복 적립되지 않음

**Response (201 Created)**:
```json
{
  "pointTransactionId": "암호화된 ID",
  "balance": 30,
  "chargedQuantity": 30,
  "paidAmount": 2500,
  "approvalNumber": "PG사 승인 번호"
}
```

- `balance`: 충전 후 잔액
- `chargedQuantity`: 이번 충전으로 적립된 수량
- `paidAmount`: 실제 결제 금액(할인 적용 후)
- `approvalNumber`: PG 승인 번호 (조회/환불 추적용)

**에러 케이스**
- 400: 유효하지 않은 상품 ID, 잘못된 요청 형식
- 402: PG 승인 실패 (카드 한도 초과, 잔액 부족 등)
- 404: PointProduct 미존재
- 409: 동일 idempotencyKey로 이미 충전된 요청 (기존 결과를 200으로 재응답할지 409로 거부할지 "4. 고려사항"에서 결정)

---

## 3. 아키텍처

```
┌──────────────────────────────────────────────────────────────────────┐
│ boot/ma-boot-web                                                     │
│  PointChargeApi                                                      │
│    └── POST /api/points → pointChargeService.charge(command)         │
│  ChargePointRequest / ChargePointResponse                            │
└───────────────────────────────────┬──────────────────────────────────┘
                                    │ (ChargePointCommand: String email)
┌───────────────────────────────────▼──────────────────────────────────┐
│ domain/ma-domain-core (point 도메인)                                 │
│  PointChargeService                                                  │
│    └── charge(command): ChargeResult                                 │
│        1) PointChargeValidator.validate(command)                     │
│        2) PointProduct 조회 + 할인 적용 → 결제 금액 산출              │
│        3) PaymentApprover.approve(...) → PaymentApproval            │
│        4) MemberPointRepository.lockAndLoad(email) (비관적 잠금)     │
│        5) memberPoint.charge(quantity) → newBalance                  │
│        6) MemberPointRepository.save(memberPoint)                    │
│        7) PointTransactionRepository.save(newTransaction)            │
│                                                                      │
│  PointChargeValidator (@Component)                                   │
│    - 상품 존재 + 멱등키 중복 여부 확인                                │
│                                                                      │
│  도메인 모델                                                         │
│    - MemberPoint(ownerEmail, balance: PointQuantity)                 │
│        fun charge(quantity: PointQuantity): MemberPoint              │
│        fun spend(quantity: PointQuantity): MemberPoint (후속)         │
│    - PointQuantity(val value: Int)  — 음수/초과 방지 VO               │
│    - NewPointTransaction(ownerEmail, productId, quantity,            │
│                          paidAmount: Money, type, idempotencyKey,     │
│                          approvalNumber)                             │
│    - PointTransactionType(CHARGE, SPEND)                             │
│                                                                      │
│  포트 (interface)                                                    │
│    - MemberPointRepository                                           │
│        findOneOrInitial(email): MemberPoint (row lock + 없으면 0)    │
│        save(memberPoint: MemberPoint)                                │
│    - PointTransactionRepository                                      │
│        save(newTransaction): Long                                    │
│        findOneByIdempotencyKeyOrNull(key): PointTransaction?         │
│    - PointProductQueryRepository (기존) — findOne(id) 메서드 추가    │
│    - DiscountPolicyQueryRepository (기존) — findOneOrNull(id) 추가   │
│    - PaymentApprover (신규, 외부 연동 포트)                           │
│        approve(request: PaymentApprovalRequest): PaymentApproval     │
└───────────────────────────────────┬──────────────────────────────────┘
                                    │ (implements)
┌───────────────────────────────────▼──────────────────────────────────┐
│ infrastructure/storage/ma-db-core                                    │
│  MemberPointTable / MemberPointEntity / MemberPointDao               │
│  PointTransactionTable / PointTransactionEntity / PointTransactionDao│
│  MemberPointCoreRepository      (포트 구현)                           │
│  PointTransactionCoreRepository (포트 구현)                           │
│  PointProductQueryCoreRepository에 findOne 추가                       │
│  DiscountPolicyCoreRepository에 findOneOrNull 추가                    │
│                                                                      │
│ infrastructure/support/ma-payment-core (신규 모듈 or 기존 support)   │
│  MockPaymentApprover (PaymentApprover 구현, 항상 승인하는 Mock)       │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 4. 변경 전략

| 레이어 | 내용 | 비고 |
|--------|------|------|
| DDL | `MEMBER_POINTS`, `POINT_TRANSACTIONS` 테이블 신규 | FK 미사용, INDEX만 |
| Domain Model | `MemberPoint`, `PointQuantity`, `NewPointTransaction`, `PointTransaction`, `PointTransactionType`, `PaymentApproval`, `PaymentApprovalRequest`, `ChargeResult` 신규 | 모두 point 도메인 하위 |
| Command | `ChargePointCommand(email: String, pointProductId: Long, paymentToken: String, idempotencyKey: String)` | Service 입력 |
| Port | `MemberPointRepository`, `PointTransactionRepository`, `PaymentApprover` 신규. `PointProductQueryRepository.findOne`, `DiscountPolicyQueryRepository.findOneOrNull` 추가 | |
| Service | `PointChargeService` 신규 — 조합만 담당 | `PointQueryService`와 분리 |
| Validator | `PointChargeValidator` 신규 — 상품 존재 + 멱등키 중복 확인 | `XroomValidator` 패턴 준수 |
| Infrastructure | Table/Entity/Dao/Repository 신규. `PointProductQueryDao.findOne` 추가 | 기존 `ma-db-core`에 추가 |
| Payment Adapter | `MockPaymentApprover` 구현 (프로파일 local/test 전용) | 실제 PG 어댑터는 후속 PR |
| Boot | `PointChargeApi`, `ChargePointRequest`, `ChargePointResponse` 신규. `ObfuscationType`에 `POINT_PRODUCT`, `POINT_TRANSACTION` 추가 | |
| Exception | `EntityType.POINT_PRODUCT`, `POINT_TRANSACTION`, `MEMBER_POINT` 추가 | |

### 타입/네이밍 규칙 요약

- **잔액 단위**: `Int`가 아닌 `PointQuantity` VO로 포장. `plus`, `minus`, `isLessThan` 등 행위를 도메인에 부여한다.
- **결제 금액**: 기존 `Money` VO 재사용.
- **이메일**: 도메인 내부는 `Email`, Service 입력(Command)은 `String`으로 받아 도메인 진입 시 `Email(...)`로 변환 (feedback: 도메인 VO 변환은 도메인 모듈 내부에서).
- **포트 메서드**: `findOne(...)` non-null, `findOneOrNull` 접미사로 구분. `By` 접미사는 같은 타입 파라미터의 다른 조건일 때만 사용 → `findOneByIdempotencyKeyOrNull`은 허용(이메일 기반 조회와 충돌 회피).

---

## 5. 변경 파일 목록

### Phase 1: DDL

| # | 파일 | 내용 |
|---|------|------|
| 1 | `infrastructure/storage/ma-db-core/src/main/resources/script/ddl.sql` | `MEMBER_POINTS`, `POINT_TRANSACTIONS` 테이블 추가 |

- `MEMBER_POINTS`: `MEMBER_POINT_ID` PK, `OWNER_EMAIL` VARCHAR(255) NOT NULL, `BALANCE` INT NOT NULL DEFAULT 0, BaseTable 공통 컬럼, `UNIQUE INDEX idx_member_point_owner_email (OWNER_EMAIL)`
- `POINT_TRANSACTIONS`: `POINT_TRANSACTION_ID` PK, `OWNER_EMAIL`, `POINT_PRODUCT_ID` BIGINT NULL(소모 시 null 가능), `TRANSACTION_TYPE` VARCHAR(16), `QUANTITY` INT, `PAID_AMOUNT` INT NOT NULL DEFAULT 0, `IDEMPOTENCY_KEY` VARCHAR(64) NOT NULL, `APPROVAL_NUMBER` VARCHAR(64) NULL, BaseTable 공통, `UNIQUE INDEX idx_point_tx_idempotency_key (IDEMPOTENCY_KEY)`, `INDEX idx_point_tx_owner_email (OWNER_EMAIL)`
- FK 미사용 (프로젝트 규칙)

### Phase 2: Domain Value Objects

| # | 파일 | 내용 |
|---|------|------|
| 2 | `domain/.../point/domain/balance/PointQuantity.kt` | 신규 VO. `init`에서 `value >= 0` 검증. `plus`, `minus(throws InvalidStateException)`, `isLessThan`, `isZero`. `companion object { ZERO }` |
| 3 | `domain/.../point/domain/transaction/PointTransactionType.kt` | enum: `CHARGE`, `SPEND` |

### Phase 3: Domain Models (balance)

| # | 파일 | 내용 |
|---|------|------|
| 4 | `domain/.../point/domain/balance/MemberPoint.kt` | `MemberPoint(ownerEmail: Email, balance: PointQuantity)`. `fun charge(quantity): MemberPoint`(잔액에 quantity 더한 새 인스턴스 반환), `fun spend(quantity): MemberPoint`(내부에서 PointQuantity.minus로 부족 시 예외). `companion object { fun initial(email: Email): MemberPoint }` |

### Phase 4: Domain Models (transaction)

| # | 파일 | 내용 |
|---|------|------|
| 5 | `domain/.../point/domain/transaction/NewPointTransaction.kt` | `NewPointTransaction(ownerEmail: Email, pointProductId: Long?, type: PointTransactionType, quantity: PointQuantity, paidAmount: Money, idempotencyKey: String, approvalNumber: String?)`. 팩토리 `forCharge(...)`, `forSpend(...)` 제공 |
| 6 | `domain/.../point/domain/transaction/PointTransaction.kt` | 조회/응답용 도메인 모델. `id`, `ownerEmail`, `pointProductId`, `type`, `quantity`, `paidAmount`, `idempotencyKey`, `approvalNumber`, `createdDate` |

### Phase 5: Domain - Payment 포트

| # | 파일 | 내용 |
|---|------|------|
| 7 | `domain/.../point/domain/port/payment/PaymentApprovalRequest.kt` | `PaymentApprovalRequest(paymentToken: String, amount: Money, idempotencyKey: String, orderName: String)` |
| 8 | `domain/.../point/domain/port/payment/PaymentApproval.kt` | `PaymentApproval(approvalNumber: String, approvedAmount: Money, approvedAt: LocalDateTime)` |
| 9 | `domain/.../point/domain/port/payment/PaymentApprover.kt` | `interface PaymentApprover { fun approve(request: PaymentApprovalRequest): PaymentApproval }`. 실패 시 `PaymentApprovalFailedException` 던짐 |
| 10 | `domain/.../point/exception/PaymentApprovalFailedException.kt` | `BusinessException` 상속. HTTP 402 매핑 |

### Phase 6: Domain - Repository 포트

| # | 파일 | 내용 |
|---|------|------|
| 11 | `domain/.../point/domain/port/MemberPointRepository.kt` | `findOneOrInitial(email: Email): MemberPoint` (row lock, 없으면 `MemberPoint.initial(email)`), `save(memberPoint: MemberPoint)` (insert or update) |
| 12 | `domain/.../point/domain/port/PointTransactionRepository.kt` | `save(newTransaction: NewPointTransaction): Long`, `findOneByIdempotencyKeyOrNull(key: String): PointTransaction?` |
| 13 | `domain/.../point/domain/port/PointProductQueryRepository.kt` | **수정**: `fun findOne(id: Long): PointProduct` 추가 (non-null, 없으면 `EntityNotFoundException`) |
| 14 | `domain/.../point/domain/port/DiscountPolicyQueryRepository.kt` | **수정**: `fun findOneOrNull(id: Long): DiscountPolicy?` 추가 |

### Phase 7: Application (Service + Validator + Command)

| # | 파일 | 내용 |
|---|------|------|
| 15 | `domain/.../point/application/command/ChargePointCommand.kt` | `ChargePointCommand(email: String, pointProductId: Long, paymentToken: String, idempotencyKey: String)`. Service 입력 DTO |
| 16 | `domain/.../point/application/result/ChargeResult.kt` | `ChargeResult(pointTransactionId: Long, balance: PointQuantity, chargedQuantity: PointQuantity, paidAmount: Money, approvalNumber: String)` |
| 17 | `domain/.../point/domain/PointChargeValidator.kt` | `@Component`. `validate(command, product)`: (1) 멱등키 중복 시 `DuplicateException(POINT_TRANSACTION, "idempotencyKey", key)` — 후속에서 "재응답" 전략으로 바꿀 여지 남김 |
| 18 | `domain/.../point/application/PointChargeService.kt` | `@Service @Transactional`. 의존성: `pointProductQueryRepository`, `discountPolicyQueryRepository`, `memberPointRepository`, `pointTransactionRepository`, `paymentApprover`, `pointChargeValidator`. `fun charge(command: ChargePointCommand): ChargeResult` — 흐름은 § 3 참조. Service는 조합만 담당 |

### Phase 8: Infrastructure - DB

| # | 파일 | 내용 |
|---|------|------|
| 19 | `infrastructure/.../point/entity/table/MemberPointTable.kt` | `object MemberPointTable : BaseTable("MEMBER_POINTS", "MEMBER_POINT_ID")`. 컬럼: `ownerEmail`, `balance` |
| 20 | `infrastructure/.../point/entity/table/PointTransactionTable.kt` | 컬럼: `ownerEmail`, `pointProductId(nullable)`, `transactionType`, `quantity`, `paidAmount`, `idempotencyKey`, `approvalNumber(nullable)` |
| 21 | `infrastructure/.../point/entity/MemberPointEntity.kt` | `id`, `ownerEmail`, `balance`. `toDomain()`, `companion object { from(row) }` |
| 22 | `infrastructure/.../point/entity/PointTransactionEntity.kt` | 모든 컬럼 + `createdDate`. `toDomain()`, `from(row)` |
| 23 | `infrastructure/.../point/dao/MemberPointDao.kt` | `findOneOrNullForUpdate(email)` — `XroomTable.select(...).forUpdate()` 패턴 확인 필요, Exposed의 `selectAll().forUpdate()` 사용. `insert(...)`, `updateBalance(id, newBalance)` |
| 24 | `infrastructure/.../point/dao/PointTransactionDao.kt` | `save(newTransaction)` → insertAndGetId, `findOneByIdempotencyKeyOrNull(key)` |
| 25 | `infrastructure/.../point/dao/PointProductQueryDao.kt` | **수정**: `findOne(id)` 메서드 추가 (`select.where { PointProductTable.id eq id }.singleOrNull()` → Entity) |
| 26 | `infrastructure/.../point/dao/DiscountPolicyQueryDao.kt` | **수정**: `findOneOrNull(id)` 메서드 추가 |
| 27 | `infrastructure/.../point/repository/MemberPointCoreRepository.kt` | `@Repository` + `MemberPointRepository` 구현. `findOneOrInitial`: DAO의 row lock 호출, 없으면 insert 후 row lock 재조회 또는 `MemberPoint.initial(email)` 반환(save 단계에서 upsert). `save`: id 존재 여부로 insert/update 분기 |
| 28 | `infrastructure/.../point/repository/PointTransactionCoreRepository.kt` | `PointTransactionRepository` 구현 |
| 29 | `infrastructure/.../point/repository/PointProductQueryCoreRepository.kt` | **수정**: `findOne(id)` 구현. 없으면 `EntityNotFoundException(EntityType.POINT_PRODUCT, "id", id.toString())` |
| 30 | `infrastructure/.../point/repository/DiscountPolicyCoreRepository.kt` | **수정**: `findOneOrNull(id)` 구현 |

### Phase 9: Payment Adapter (Mock)

| # | 파일 | 내용 |
|---|------|------|
| 31 | `infrastructure/support/ma-payment-core/build.gradle.kts` | 신규 모듈. `ma-domain-core` 의존 |
| 32 | `settings.gradle.kts` | `ma-payment-core` include 추가 |
| 33 | `infrastructure/support/ma-payment-core/.../MockPaymentApprover.kt` | `@Component @Profile("local", "test")` — 전달된 token이 "FAIL"로 시작하면 `PaymentApprovalFailedException`, 그 외에는 `PaymentApproval("MOCK-${UUID}", request.amount, LocalDateTime.now())` 반환 |
| 34 | `boot/ma-boot-web/build.gradle.kts` | `ma-payment-core` 의존성 추가 |

> **대안**: 별도 모듈 신설이 부담이면 `ma-domain-core` 내 `testFixtures`가 아닌 `boot/ma-boot-web`의 `config/` 또는 기존 `infrastructure/support/` 중 가장 가까운 모듈에 `MockPaymentApprover`를 둔다. 신설 여부는 구현 단계에서 최종 결정.

### Phase 10: Common / Exception

| # | 파일 | 내용 |
|---|------|------|
| 35 | `domain/.../common/domain/id/ObfuscationType.kt` | **수정**: `POINT_PRODUCT("point-product")`, `POINT_TRANSACTION("point-transaction")` 추가 |
| 36 | `domain/.../exception/EntityType.kt` | **수정**: `POINT_PRODUCT`, `POINT_TRANSACTION`, `MEMBER_POINT` 추가 |

### Phase 11: Boot (Web)

| # | 파일 | 내용 |
|---|------|------|
| 37 | `boot/.../point/api/request/ChargePointRequest.kt` | `@NotBlank paymentToken`, `@NotBlank idempotencyKey`, `@DecryptId(POINT_PRODUCT) pointProductId: Long`. `toCommand(email: String): ChargePointCommand` |
| 38 | `boot/.../point/api/response/ChargePointResponse.kt` | `@EncryptId(POINT_TRANSACTION) pointTransactionId: Long`, `balance`, `chargedQuantity`, `paidAmount`, `approvalNumber`. `companion object { from(result: ChargeResult) }` |
| 39 | `boot/.../point/api/PointChargeApi.kt` | `@RestController @RequestMapping("/api/points")`. `@PostMapping @ResponseStatus(CREATED) fun charge(@AuthenticationPrincipal email: String, @RequestBody @Valid request: ChargePointRequest): ChargePointResponse` |
| 40 | `boot/.../support/validation/ValidationMessages.kt` | **수정**: `PAYMENT_TOKEN_REQUIRED`, `IDEMPOTENCY_KEY_REQUIRED` 메시지 상수 추가 |

> **기존 `PointQueryApi`는 GET `/api/points` 유지.** 충전은 별도 컨트롤러 파일(`PointChargeApi`)로 분리하여 Query/Command 책임 분리 패턴 유지.

---

## 6. 구현 순서 (의존성 기준)

아래 순서대로 진행하면 하위 의존이 빌드되지 않는 상황을 피할 수 있다.

| 단계 | 묶음 | 비고 |
|------|------|------|
| 1 | Phase 10 (ObfuscationType, EntityType) | 뒤 단계에서 참조 |
| 2 | Phase 1 (DDL) | 테이블 먼저 확정 |
| 3 | Phase 2–4 (VO → MemberPoint/PointTransaction 도메인) | 순수 도메인, 무의존 |
| 4 | Phase 5 (Payment 포트 + 예외) | 도메인이 참조 |
| 5 | Phase 6 (Repository 포트 + 기존 포트 확장) | Service가 참조 |
| 6 | Phase 7 (Command, Result, Validator, Service) | 위 모두 필요 |
| 7 | Phase 8 (DB 인프라) | 포트 구현 |
| 8 | Phase 9 (Payment Mock) | 포트 구현 |
| 9 | Phase 11 (API Controller/DTO) | Service 호출 |
| 10 | 테스트 + REST Docs | 모든 구현 완료 후 |

---

## 7. 고려사항

### 7.1 동시성 제어

"중복 차감 방지" 요구사항은 충전에도 동일하게 필요하다. 충전 동시 요청 시 "잔액을 읽은 시점" 사이에 다른 트랜잭션이 먼저 save하면 한쪽이 덮어써진다.

- **1차 전략: DB 비관적 잠금 (`SELECT ... FOR UPDATE`)**
  - `MemberPointDao.findOneOrNullForUpdate(email)`로 row lock 획득
  - MariaDB는 Exposed의 `.forUpdate()` 지원
  - 충전 빈도가 낮고 로직이 짧으므로 성능 영향은 미미
- **대안: 낙관적 잠금 (`version` 컬럼)**
  - 충전 실패 시 재시도 로직 필요 → 복잡도 상승
  - 이번엔 보류, 향후 부하가 높아지면 검토
- **최초 insert 레이스**: 같은 이메일에 row가 없는 상태에서 두 요청이 동시에 insert 시도 → UNIQUE INDEX(`OWNER_EMAIL`)로 DB가 하나만 통과. 실패한 쪽은 트랜잭션 재시도 또는 다음 조회에서 row lock 성공

### 7.2 멱등성 (idempotencyKey)

- 클라이언트가 생성한 UUID를 `POINT_TRANSACTIONS.IDEMPOTENCY_KEY`에 UNIQUE INDEX로 저장
- `PointChargeValidator`에서 기존 트랜잭션 존재 여부를 사전 확인
- **정책 선택**: 이번 계획은 "기존 트랜잭션이 있으면 `DuplicateException` (409)"으로 단순화. "동일 요청이면 기존 결과를 200으로 재응답"하는 방식은 후속 개선으로 분리 (retry 안전성은 높아지지만 응답 캐시가 필요)
- DB UNIQUE INDEX도 함께 두어 레이스 상황에서도 최종 방어선 확보

### 7.3 PG 연동 추상화

- `PaymentApprover` 포트로 PG사 의존을 캡슐화. 도메인은 PG를 모른다.
- 1차 구현: `MockPaymentApprover`(승인/실패 분기만 제공). 실제 어댑터(`TossPaymentsApprover` 등)는 후속 PR에서 포트만 구현하면 됨 → OCP 충족
- PG 승인은 **반드시 MemberPoint 업데이트 전에** 수행. 승인 실패 시 잔액 변경 없음
- PG 승인 후 DB 트랜잭션이 실패하면 인연이 적립되지 않는데 결제만 된 상태 가능 → **보상 트랜잭션(결제 취소)** 또는 최소한 경고 알람 필요. 1차에서는 이슈 감지를 위해 logger.error로 남기고, 자동 보상은 후속 과제로 명시

### 7.4 트랜잭션 경계

- `PointChargeService.charge`에 `@Transactional` 적용
- **PG 승인 호출을 트랜잭션 안에 둘지 vs 밖에 둘지**:
  - **안에 두는 경우(선택)**: DB 트랜잭션이 열려있는 동안 외부 I/O가 진행되어 DB 커넥션이 오래 점유됨. 트래픽이 커지면 문제 → 1차 구현에서는 트래픽이 낮으므로 단순성 우선, 안에 둔다
  - **개선안**: PG 승인은 트랜잭션 밖에서 먼저 수행하고, 그 결과를 DB에 반영하는 트랜잭션만 따로 여는 구조(두 단계). 후속 개선 항목으로 기록

### 7.5 결제 금액 산출

- 클라이언트가 `paidAmount`를 보내지 않음. 서버가 `PointProduct.price` + `DiscountPolicy` 조합으로 계산 (기존 `PointProductWithDiscount.discountedPrice()` 재사용)
- PG 승인 요청에 서버 산출 금액만 사용 → 클라이언트가 조작 불가
- PG 승인 응답의 `approvedAmount`가 서버 산출 금액과 다르면 `InvalidStateException`으로 실패 처리

### 7.6 이력 저장 필수 여부

- api-todo.md는 이력을 명시하지 않았지만, "중복 차감 방지"·환불 대응·CS 응대를 위해 **이번 계획에 반드시 포함**한다
- 향후 `GET /api/points/me`가 거래 이력 조회까지 확장될 수 있음

### 7.7 Service ↔ Repository 규칙 준수

- `PointChargeService`는 다른 Service에 의존하지 않음 (기존 `PointQueryService`도 참조 X). 필요한 포트만 직접 주입
- 단건 조회 포트는 non-null, 없으면 Repository 구현체에서 예외
- `findOne`/`findOneOrNull`로 반환 타입 구분

### 7.8 FK 미사용

- `POINT_PRODUCT_ID`, `OWNER_EMAIL`은 다른 테이블 참조지만 FK 걸지 않음 (프로젝트 규칙). 참조 무결성은 애플리케이션 레벨에서 `PointChargeValidator`로 보장

### 7.9 캐시 무효화

- `POST /api/points` 자체는 PointProduct를 변경하지 않으므로 `PointProductRedisCacheRepository` 무효화 불필요
- `MemberPoint` 캐싱은 1차 구현 범위 아님 (후속 과제)

---

## 8. 검증 항목

- [ ] `PointQuantity` VO 단위 테스트 — 음수 방지, 덧셈/뺄셈, 부족 시 `InvalidStateException`
- [ ] `MemberPoint.charge`, `.spend` 단위 테스트 (잔액 반영, 부족 시 예외)
- [ ] `PointChargeValidator` 테스트 — 상품 미존재, 멱등키 중복 시나리오
- [ ] `PointChargeService` 테스트 (Mockk) — 정상 흐름, PG 실패, 멱등키 중복, 할인 적용 금액 일치
- [ ] `MemberPointDao`/`PointTransactionDao` 통합 테스트 — row lock 동작, UNIQUE 제약 위반
- [ ] `PointChargeApi` REST Docs — 정상 201, 멱등 충돌 409, PG 실패 402, 상품 미존재 404
- [ ] DDL 적용 후 기존 테스트 전부 통과 확인 (`./gradlew build`)
- [ ] `spring.profiles.active=local/test`에서 `MockPaymentApprover` 주입되는지 확인 (ContextLoads)

---

## 9. 확인 필요 사항

1. **멱등키 정책**: 동일 idempotencyKey 재요청 시 "기존 결과 재응답(200)" vs "중복 거부(409)" 중 어느 것으로 할지. 모바일 재시도 UX상 재응답이 바람직하나, 1차에서는 복잡도 낮은 409 방식으로 제안
2. **PG 어댑터 모듈 위치**: `infrastructure/support/ma-payment-core`를 신설할지, 기존 support 모듈 중 하나에 합칠지. 실제 PG 연동 어댑터가 여러 개 붙을 가능성이 크므로 신설을 권장하나 1차에서는 Mock만 필요하므로 논의 필요
3. **보상 트랜잭션**: PG 승인 성공 후 DB 저장 실패 시 자동 결제 취소를 지금 포함할지, 후속 과제로 분리할지. 1차 분리 권장
4. **요금제(가격) 동적 변경**: PointProduct의 가격이 요청 처리 중 바뀌면 이슈. 결제 승인 시점의 `paidAmount`를 거래에 기록하므로 조회는 일관성 있으나, 사전 표시 금액과 승인 금액 차이가 있을 수 있음 — 클라이언트 UX 정책 확인 필요
