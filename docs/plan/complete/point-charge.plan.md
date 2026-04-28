# Plan: 인연 충전 (구매)

> 작성일: 2026-04-20

## 1. 개요

`POST /api/points`로 회원이 "인연 상품(PointProduct)"을 구매하여 자기 계정의 인연 잔액(MemberPoint)을 적립하는 기능을 구현한다.

현재 인연 도메인에는 상품 조회(`PointQueryService.findProducts`)와 할인 정책만 존재하며, 회원별 잔액/거래 이력이라는 개념이 없다. 이번 계획에서 `MemberPoint`(잔액 애그리거트)와 `PointTransaction`(충전/소모 이력 애그리거트)을 인연(point) 도메인 하위에 추가한다. PG 연동은 포트 + Router 구조로 추상화하여 우선 Mock 구현체로 시작하고, 실 PG 어댑터는 후속 PR에서 추가만 하면 되도록 한다.

### 도메인 분리 판단

별도 도메인을 새로 만들지 않고 **기존 `point` 도메인 하위 패키지**로 추가한다.
- 회원별 잔액/거래 이력은 인연(포인트) 도메인의 핵심 상태로, 이미 있는 PointProduct/DiscountPolicy와 같은 애그리거트 집합 안에 응집시킨다.
- 하위 패키지: `point/domain/balance/`, `point/domain/transaction/`, `point/domain/payment/`로 책임 분리

### 1차 구현 범위와 후속 과제 분리

**1차 구현 범위 (이 계획)**
- `POST /api/points` 충전 API
- `MemberPoint`(잔액), `PointTransaction`(이력) 도메인 + 테이블 신규
- `PaymentApprover` 포트 + **`PaymentApproverRouter`**(라우팅) 구조 도입
- 1차 구현체는 `MockPaymentApprover` 1개 (모든 결제수단 수용)
- `PaymentMethod` enum 도입 (CARD, KAKAO_PAY, NAVER_PAY, BANK, VIRTUAL_ACCOUNT, PHONE)
- 멱등키(idempotencyKey)로 중복 요청 방지 — **409 Conflict** 방식
- DB 비관적 잠금으로 동시 충전 시 잔액 정합성 확보
- 가격 변경 탐지: `expectedPrice` 검증

**후속 과제 (이번 계획에서 제외)**
- `GET /api/points/me` 잔액 조회 API — 별도 PR
- 실제 PG사 어댑터(Portone/Toss/Kakao 등) — 별도 PR. **어댑터만 추가**하면 Router가 자동 편입
- 인연 소모(차감) 로직 — 매칭/X룸 등 기능과 함께 도입
- PG 승인 후 DB 실패 시 **보상 트랜잭션(cancel)** — Mock엔 no-op로 준비만
- 멱등키 "동일 요청 재응답(200)" 정책 — 현재는 409로 단순화

---

## 2. API 설계

api-todo.md 기준 **인연 > 인연 API** 섹션의 `POST /api/points` 항목. 동일 섹션 참고사항("잔액 부족 시 에러 응답", "동시성 제어 필요", "PG사 연동 방식 결정 필요")을 반영한다.

| Method | Endpoint | 용도 | 인증 |
|--------|----------|------|------|
| POST | `/api/points` | 인연 충전 (구매) | 필요 |

**Request Body**:
```json
{
  "pointProductId": "암호화된 ID",
  "paymentMethod": "CARD",
  "paymentToken": "PG사 결제 인증 토큰",
  "expectedPrice": 2000,
  "idempotencyKey": "클라이언트 생성 UUID"
}
```

- `pointProductId`: 구매할 상품 ID (`@DecryptId(POINT_PRODUCT)` 적용)
- `paymentMethod`: 결제수단 (CARD, KAKAO_PAY, NAVER_PAY, BANK, VIRTUAL_ACCOUNT, PHONE). Router가 이 값으로 적절한 어댑터 선택
- `paymentToken`: 프론트의 PG SDK가 승인 전 발급한 토큰. 서버가 이걸로 PG 승인 호출
- `expectedPrice`: 클라이언트가 화면에서 본 최종 가격(원). 서버 재계산값과 **불일치 시 409**로 거부
- `idempotencyKey`: 클라이언트 UUID. 필수. 동일 키 재요청은 409

**Response (201 Created)**:
```json
{
  "pointTransactionId": "암호화된 ID",
  "balance": 30,
  "chargedQuantity": 30,
  "paidAmount": 2000,
  "approvalNumber": "PG사 승인 번호"
}
```

**에러**
- 400: 필수 필드 누락, 잘못된 형식 (`@NotBlank`, `@Valid` 실패)
- 402: PG 승인 실패 (`PaymentApprovalFailedException`)
- 404: PointProduct 미존재
- 409: (1) 멱등키 중복 / (2) `expectedPrice`와 서버 계산 금액 불일치 / (3) 지원하지 않는 `paymentMethod`

**보안 분리**: `GET /api/points`는 기존대로 permitAll(상품 목록은 비회원에게도 노출 가능), `POST /api/points`는 인증 필요. SecurityConfig에서 `requestMatchers(POST, "/api/points")` 명시적 인증.

---

## 3. 아키텍처

```
┌──────────────────────────────────────────────────────────────────────┐
│ boot/ma-boot-web                                                     │
│  PointChargeApi                                                      │
│    └── POST /api/points → pointChargeService.charge(command)         │
│  ChargePointRequest / ChargePointResponse                            │
└───────────────────────────────────┬──────────────────────────────────┘
                                    │ (ChargePointCommand)
┌───────────────────────────────────▼──────────────────────────────────┐
│ domain/ma-domain-core (point 도메인)                                 │
│  PointChargeService (조합만 담당)                                    │
│    1) PointChargeValidator.validate(command, product, expectedPrice) │
│    2) PaymentApproverRouter.resolve(method) → approver              │
│    3) approver.approve(request) → PaymentApproval                    │
│    4) MemberPointRepository.findOneOrInitial(email) (row lock)       │
│    5) memberPoint.charge(quantity)                                   │
│    6) MemberPointRepository.save(memberPoint)                        │
│    7) PointTransactionRepository.save(newTransaction)                │
│                                                                      │
│  PaymentApproverRouter (@Component) ── PaymentApprover 목록 보유     │
│    ├─ resolve(method): PaymentApprover (supports true인 첫 어댑터)    │
│    └─ 없으면 error("지원하지 않는 결제수단: $method")                  │
│                                                                      │
│  PaymentApprover (interface)                                         │
│    ├─ supports(method: PaymentMethod): Boolean                       │
│    ├─ approve(request): PaymentApproval                              │
│    └─ cancel(approvalNumber, reason): Unit   (후속 PR에서 사용)       │
│                                                                      │
│  도메인 모델                                                         │
│    - MemberPoint(ownerEmail, balance: PointQuantity)                 │
│        fun charge(quantity), fun spend(quantity) [후속]              │
│    - PointQuantity VO — 음수 방지                                    │
│    - NewPointTransaction, PointTransaction, PointTransactionType     │
│    - PaymentMethod(enum: CARD, KAKAO_PAY, ...)                       │
│    - PaymentApprovalRequest, PaymentApproval                         │
│                                                                      │
│  Repository 포트                                                     │
│    - MemberPointRepository, PointTransactionRepository               │
│    - PointProductQueryRepository.findOne (확장)                      │
│    - DiscountPolicyQueryRepository.findOneOrNull (확장)              │
└───────────────────────────────────┬──────────────────────────────────┘
                                    │ (implements)
┌───────────────────────────────────▼──────────────────────────────────┐
│ infrastructure/storage/ma-db-core                                    │
│  MemberPointTable/Entity/Dao/CoreRepository                          │
│  PointTransactionTable/Entity/Dao/CoreRepository                     │
│  기존 Point/Discount DAO·CoreRepository에 findOne/findOneOrNull 추가 │
│                                                                      │
│ boot/ma-boot-web (1차는 boot 쪽에 Mock 배치, 후속에 별도 모듈로 분리) │
│  MockPaymentApprover (@Component @Profile("local","test"))           │
│    - supports(method) = true (모든 수단 수용)                         │
│    - approve → "MOCK-{UUID}" 반환                                    │
│    - cancel → no-op                                                  │
└──────────────────────────────────────────────────────────────────────┘

[후속 PR 진화]
 PaymentApproverRouter
   └─ List<PaymentApprover> (Spring DI)
       ├─ MockPaymentApprover       (local/test)
       ├─ PortonePaymentApprover    (prod, 초기)
       ├─ TossPaymentApprover       (수수료 최적화 시)
       └─ ...
```

---

## 4. 변경 전략

| 레이어 | 내용 | 비고 |
|--------|------|------|
| DDL | `MEMBER_POINTS`, `POINT_TRANSACTIONS` 테이블 신규 | FK 미사용, INDEX만 |
| Domain VO | `PointQuantity` 신규 | 음수 방지 |
| Domain Model | `MemberPoint`, `NewPointTransaction`, `PointTransaction`, `PointTransactionType` 신규 | `point.domain.balance`, `point.domain.transaction` |
| Domain Payment | `PaymentMethod` enum, `PaymentApprovalRequest`, `PaymentApproval`, `PaymentApprover` 포트, `PaymentApproverRouter` 신규 | `point.domain.payment` |
| Command | `ChargePointCommand(email, pointProductId, paymentMethod, paymentToken, expectedPrice, idempotencyKey)` | Service 입력 |
| Result | `ChargeResult(pointTransactionId, balance, chargedQuantity, paidAmount, approvalNumber)` | Service 반환 |
| Port | `MemberPointRepository`, `PointTransactionRepository` 신규. 기존 `PointProductQueryRepository`, `DiscountPolicyQueryRepository` 확장 | 단건 조회 메서드 |
| Service | `PointChargeService` 신규 — 조합만 담당 | `PointQueryService`와 분리 |
| Validator | `PointChargeValidator` 신규 — 상품 존재, 멱등키 중복, expectedPrice 일치 검증 | `XroomValidator` 패턴 |
| Infrastructure | Table/Entity/Dao/Repository 신규. 기존 Dao/Repository에 `findOne`/`findOneOrNull` 추가 | |
| Payment Adapter | `MockPaymentApprover` — `ma-boot-web` 내에 배치 (1차) | 후속 PR에서 실 어댑터는 별도 모듈 분리 가능 |
| SecurityConfig | `POST /api/points`만 인증 요구하도록 명시 (GET은 기존 permitAll 유지) | |
| Boot | `PointChargeApi`, Request/Response DTO, `ObfuscationType` 확장 | |
| Exception | `EntityType.POINT_PRODUCT/POINT_TRANSACTION/MEMBER_POINT`, `PaymentApprovalFailedException` 추가 | |

### 타입/네이밍 규칙 요약

- **잔액 단위**: `PointQuantity` VO로 포장. `plus`, `minus`, `isLessThan`, `isZero`, `ZERO`
- **결제 금액**: 기존 `Money` VO 재사용
- **이메일**: Command는 `String`, 도메인은 `Email` (변환은 Service 내부)
- **포트 메서드**: `findOne` non-null / `findOneOrNull` nullable. `By` 접미사는 같은 타입 파라미터에서 오버로드 구분용만 허용

---

## 5. 변경 파일 목록

### Phase 1: DDL

| # | 파일 | 내용 |
|---|------|------|
| 1 | `infrastructure/storage/ma-db-core/src/main/resources/script/ddl.sql` | `MEMBER_POINTS`, `POINT_TRANSACTIONS` 테이블 추가 |

- `MEMBER_POINTS`: `MEMBER_POINT_ID` PK, `OWNER_EMAIL` VARCHAR(255) NOT NULL, `BALANCE` INT NOT NULL DEFAULT 0, BaseTable 공통, `UNIQUE INDEX (OWNER_EMAIL)`
- `POINT_TRANSACTIONS`: `POINT_TRANSACTION_ID` PK, `OWNER_EMAIL`, `POINT_PRODUCT_ID` BIGINT NULL, `TRANSACTION_TYPE` VARCHAR(16), `QUANTITY` INT, `PAID_AMOUNT` INT NOT NULL, `PAYMENT_METHOD` VARCHAR(32), `IDEMPOTENCY_KEY` VARCHAR(64) NOT NULL, `APPROVAL_NUMBER` VARCHAR(64) NULL, BaseTable 공통, `UNIQUE INDEX (IDEMPOTENCY_KEY)`, `INDEX (OWNER_EMAIL)`
- FK 미사용

### Phase 2: Domain - Value Objects / Enums

| # | 파일 | 내용 |
|---|------|------|
| 2 | `domain/.../point/domain/balance/PointQuantity.kt` | VO. `init` 음수 검증. `plus`, `minus(throws InvalidStateException 부족 시)`, `isLessThan`, `isZero`, `toInt()`. `companion { ZERO }` |
| 3 | `domain/.../point/domain/transaction/PointTransactionType.kt` | enum: `CHARGE`, `SPEND` |
| 4 | `domain/.../point/domain/payment/PaymentMethod.kt` | enum: `CARD`, `KAKAO_PAY`, `NAVER_PAY`, `BANK`, `VIRTUAL_ACCOUNT`, `PHONE` |

### Phase 3: Domain - Balance / Transaction

| # | 파일 | 내용 |
|---|------|------|
| 5 | `domain/.../point/domain/balance/MemberPoint.kt` | `MemberPoint(id: Long?, ownerEmail: Email, balance: PointQuantity)`. `fun charge(quantity)`, `fun spend(quantity)`. `companion { fun initial(email: Email) }` |
| 6 | `domain/.../point/domain/transaction/NewPointTransaction.kt` | 생성자 + 팩토리 `forCharge(...)` |
| 7 | `domain/.../point/domain/transaction/PointTransaction.kt` | 조회 결과 모델. 모든 필드 + `createdDate` |

### Phase 4: Domain - Payment 포트/Router/예외

| # | 파일 | 내용 |
|---|------|------|
| 8 | `domain/.../point/domain/payment/PaymentApprovalRequest.kt` | `paymentMethod: PaymentMethod`, `paymentToken: String`, `amount: Money`, `idempotencyKey: String`, `orderName: String` |
| 9 | `domain/.../point/domain/payment/PaymentApproval.kt` | `approvalNumber: String`, `approvedAmount: Money`, `approvedAt: LocalDateTime`, `paymentMethod: PaymentMethod` |
| 10 | `domain/.../point/domain/payment/PaymentApprover.kt` | interface. `supports(method)`, `approve(request)`, `cancel(approvalNumber, reason)` 3개 |
| 11 | `domain/.../point/domain/payment/PaymentApproverRouter.kt` | `@Component`. 생성자 `List<PaymentApprover>` 주입. `fun resolve(method): PaymentApprover` — `supports(method) == true`인 첫 어댑터 반환, 없으면 `error`. **PaymentApprover는 implement하지 않음** (자가 주입 회피) |
| 12 | `domain/.../point/exception/PaymentApprovalFailedException.kt` | `BusinessException` 상속. HTTP 402. PG 응답 메시지 포함 |

### Phase 5: Domain - Repository 포트

| # | 파일 | 내용 |
|---|------|------|
| 13 | `domain/.../point/domain/port/MemberPointRepository.kt` | `findOneOrInitial(email: Email): MemberPoint` (row lock, 없으면 `MemberPoint.initial`). `save(memberPoint)` (insert 또는 update) |
| 14 | `domain/.../point/domain/port/PointTransactionRepository.kt` | `save(newTransaction): Long`. `findOneByIdempotencyKeyOrNull(key: String): PointTransaction?` |
| 15 | `domain/.../point/domain/port/PointProductQueryRepository.kt` | **수정**: `fun findOne(id: Long): PointProduct` 추가 |
| 16 | `domain/.../point/domain/port/DiscountPolicyQueryRepository.kt` | **수정**: `fun findOneOrNull(id: Long): DiscountPolicy?` 추가 |

### Phase 6: Application (Command / Result / Validator / Service)

| # | 파일 | 내용 |
|---|------|------|
| 17 | `domain/.../point/application/command/ChargePointCommand.kt` | `email: String`, `pointProductId: Long`, `paymentMethod: PaymentMethod`, `paymentToken: String`, `expectedPrice: Int`, `idempotencyKey: String` |
| 18 | `domain/.../point/application/result/ChargeResult.kt` | `pointTransactionId: Long`, `balance: PointQuantity`, `chargedQuantity: PointQuantity`, `paidAmount: Money`, `approvalNumber: String` |
| 19 | `domain/.../point/domain/PointChargeValidator.kt` | `@Component`. `validate(command, productWithDiscount, serverPrice)`: (1) 멱등키 중복 → `DuplicateException`, (2) `expectedPrice != serverPrice.toInt()` → `InvalidStateException`(409 매핑) |
| 20 | `domain/.../point/application/PointChargeService.kt` | `@Service @Transactional`. 의존: `pointProductQueryRepository`, `discountPolicyQueryRepository`, `memberPointRepository`, `pointTransactionRepository`, `paymentApproverRouter`, `pointChargeValidator`. 흐름 §3 참조 |

### Phase 7: Infrastructure - DB

| # | 파일 | 내용 |
|---|------|------|
| 21 | `infrastructure/.../point/entity/table/MemberPointTable.kt` | `BaseTable("MEMBER_POINTS", "MEMBER_POINT_ID")`. `ownerEmail`, `balance` |
| 22 | `infrastructure/.../point/entity/table/PointTransactionTable.kt` | `ownerEmail`, `pointProductId(nullable)`, `transactionType`, `quantity`, `paidAmount`, `paymentMethod`, `idempotencyKey`, `approvalNumber(nullable)` |
| 23 | `infrastructure/.../point/entity/MemberPointEntity.kt` | `toDomain()`, `companion { from(row) }` |
| 24 | `infrastructure/.../point/entity/PointTransactionEntity.kt` | 동일 |
| 25 | `infrastructure/.../point/dao/MemberPointDao.kt` | `findOneForUpdateOrNull(email)` — Exposed `.selectAll().where{...}.forUpdate()`. `insert`, `updateBalance(id, newBalance)` |
| 26 | `infrastructure/.../point/dao/PointTransactionDao.kt` | `save`, `findOneByIdempotencyKeyOrNull` |
| 27 | `infrastructure/.../point/dao/PointProductQueryDao.kt` | **수정**: `findOne(id)` 추가 |
| 28 | `infrastructure/.../point/dao/DiscountPolicyQueryDao.kt` | **수정**: `findOneOrNull(id)` 추가 |
| 29 | `infrastructure/.../point/repository/MemberPointCoreRepository.kt` | `findOneOrInitial`: row lock 조회 후 없으면 `MemberPoint.initial`. `save`: id 존재 시 update, 없으면 insert |
| 30 | `infrastructure/.../point/repository/PointTransactionCoreRepository.kt` | 포트 구현 |
| 31 | `infrastructure/.../point/repository/PointProductQueryCoreRepository.kt` | **수정**: `findOne(id)` — 없으면 `EntityNotFoundException(POINT_PRODUCT, "id", id.toString())` |
| 32 | `infrastructure/.../point/repository/DiscountPolicyCoreRepository.kt` | **수정**: `findOneOrNull(id)` |

### Phase 8: Payment Adapter (Mock)

| # | 파일 | 내용 |
|---|------|------|
| 33 | `boot/.../config/MockPaymentApprover.kt` | `@Component @Profile("local","test")`. `supports(method) = true`. `approve`: paymentToken이 "FAIL"로 시작하면 `PaymentApprovalFailedException`, 아니면 `PaymentApproval("MOCK-${UUID}", request.amount, now(), request.paymentMethod)`. `cancel`: no-op + 로그 |

> **모듈 분리는 후속 PR에서** 실 PG 어댑터 추가 시점에. 1차 Mock은 `boot/ma-boot-web/config`에 둔다.

### Phase 9: Common / Exception / Security

| # | 파일 | 내용 |
|---|------|------|
| 34 | `domain/.../common/domain/id/ObfuscationType.kt` | **수정**: `POINT_PRODUCT`, `POINT_TRANSACTION` 추가 |
| 35 | `domain/.../exception/EntityType.kt` | **수정**: `POINT_PRODUCT`, `POINT_TRANSACTION`, `MEMBER_POINT` |
| 36 | `boot/.../config/SecurityConfig.kt` | **수정**: `POST /api/points`만 `authenticated()` 명시. 나머지 POST 규칙 기존 유지 |
| 37 | `boot/.../support/error/GlobalExceptionHandler.kt` | **수정**: `PaymentApprovalFailedException` → 402 매핑 (신규 handler) |

### Phase 10: Boot (Web)

| # | 파일 | 내용 |
|---|------|------|
| 38 | `boot/.../point/api/request/ChargePointRequest.kt` | `@DecryptId(POINT_PRODUCT) pointProductId: Long`, `@NotNull paymentMethod: PaymentMethod`, `@NotBlank paymentToken`, `@Min(0) expectedPrice: Int`, `@NotBlank idempotencyKey`. `toCommand(email)` |
| 39 | `boot/.../point/api/response/ChargePointResponse.kt` | `@EncryptId(POINT_TRANSACTION) pointTransactionId: Long`, `balance: Int`, `chargedQuantity: Int`, `paidAmount: Int`, `approvalNumber: String`. `companion { from(result) }` |
| 40 | `boot/.../point/api/PointChargeApi.kt` | `@RestController @RequestMapping("/api/points")`. `@PostMapping @ResponseStatus(CREATED)` |
| 41 | `boot/.../support/validation/ValidationMessages.kt` | **수정**: 필요 상수 추가 |

---

## 6. 구현 순서 (의존성 기준)

| 단계 | 묶음 | 비고 |
|------|------|------|
| 1 | Phase 9 (ObfuscationType, EntityType, Exception) | 하위 단계 참조 |
| 2 | Phase 1 (DDL) | 테이블 확정 |
| 3 | Phase 2 (VO/enum) | 무의존 |
| 4 | Phase 3 (MemberPoint, PointTransaction 도메인) | |
| 5 | Phase 4 (Payment 포트 + Router + 예외) | |
| 6 | Phase 5 (Repository 포트 + 기존 확장) | |
| 7 | Phase 6 (Command/Result/Validator/Service) | 위 모두 필요 |
| 8 | Phase 7 (DB 인프라) | |
| 9 | Phase 8 (Mock Payment Adapter) | |
| 10 | Phase 9 (SecurityConfig, GlobalExceptionHandler) 완료 확인 | |
| 11 | Phase 10 (API) | |
| 12 | 테스트 + REST Docs | |

---

## 7. 고려사항

### 7.1 동시성 제어

- **1차: DB 비관적 잠금 (`SELECT ... FOR UPDATE`)**. Exposed `.selectAll().forUpdate()` 사용
- 최초 insert 레이스는 `UNIQUE INDEX(OWNER_EMAIL)`이 최종 방어. 중복 insert 시 한쪽은 예외 → 재시도 (단순화를 위해 1차에선 호출자 에러)
- 대안(낙관적 잠금)은 재시도 로직 필요 → 부하 높아지면 검토

### 7.2 멱등성 (409 방식 확정)

- 클라이언트 UUID를 `IDEMPOTENCY_KEY`에 UNIQUE INDEX로 저장
- `PointChargeValidator`에서 사전 확인 → 있으면 `DuplicateException` (409)
- DB UNIQUE도 함께 두어 레이스 최종 방어
- 재응답(200) 방식은 응답 캐시가 필요하므로 **후속 과제**로 분리

### 7.3 PG 연동 추상화 + Router

- `PaymentApprover` 포트 + `PaymentApproverRouter`로 다중 PG/결제수단 구조를 1차부터 갖춤
- Router는 `PaymentApprover`를 implement하지 않음 — 자가 주입 순환 회피
- 1차 Mock 하나만 있어도 Router가 `approvers.find { it.supports(method) }` 로 단일 어댑터 반환
- 새 PG 추가 = 구현체 `@Component` 하나 추가. Service/Router/도메인 무수정 → OCP

### 7.4 보상 트랜잭션 (Cancel)

- `PaymentApprover.cancel(approvalNumber, reason)` 시그니처는 지금 포함 (Mock은 no-op)
- 1차에서 **자동 보상 호출은 구현하지 않음** (승인 후 DB 저장 실패 시 logger.error만). 후속 PR에서 try-catch로 승인 → 실패 시 cancel 호출 추가. 이렇게 해도 **Router/Port 시그니처는 불변**

### 7.5 트랜잭션 경계

- `PointChargeService.charge`에 `@Transactional` 적용
- PG 승인 호출을 트랜잭션 **안**에 둠 (1차 단순성 우선)
- 개선: PG 승인을 분리하고 DB 반영만 트랜잭션 안에 두는 구조 → 후속

### 7.6 가격 변경 처리

- 클라이언트가 `expectedPrice` 전송, 서버 재계산값과 불일치 시 `InvalidStateException` (409)
- `paidAmount`는 서버 재계산값 사용. PG 승인 응답의 `approvedAmount`가 서버 값과 다르면 `InvalidStateException`

### 7.7 결제 금액 산출

- `PointProductWithDiscount.discountedPrice()` 재사용. 할인 정책 포함
- 서버 계산 금액으로 PG 승인 요청 → 클라이언트 조작 불가

### 7.8 이력 저장 필수

- `POINT_TRANSACTIONS`에 모든 충전 건 기록. 이력이 있어야 후속 환불/CS/소모 이력도 가능

### 7.9 캐시 무효화

- 이 API는 PointProduct를 변경하지 않으므로 `PointProductRedisCacheRepository` 무효화 불필요
- `MemberPoint` 캐싱은 1차 범위 아님

### 7.10 FK 미사용

- `POINT_PRODUCT_ID`, `OWNER_EMAIL`은 FK 없음. 애플리케이션 레벨(`PointChargeValidator`)에서 참조 무결성 보장

### 7.11 `/api/points` 인증 분리

- 기존 GET(상품 목록)은 permitAll
- POST는 `authenticated()`로 명시적 지정. `SecurityConfig.filterChain`에서 `requestMatchers(HttpMethod.POST, "/api/points")` 추가

### 7.12 초기 잔액 레코드

- 회원가입 시점에 자동 insert하지 않음. 첫 충전 시 `MemberPoint.initial`로 만들고 save(=insert)
- 장점: 기존 회원 마이그레이션 없이 점진적으로 레코드 생성

---

## 8. 검증 항목

- [ ] `PointQuantity` 단위 테스트 — 음수/덧셈/뺄셈/부족 예외
- [ ] `MemberPoint.charge` 단위 테스트
- [ ] `PaymentApproverRouter.resolve` — 매칭 어댑터 선택, 없을 때 예외
- [ ] `MockPaymentApprover` — FAIL 토큰 / 정상 토큰 분기
- [ ] `PointChargeValidator` — 상품 미존재 / 멱등키 중복 / expectedPrice 불일치
- [ ] `PointChargeService` (Mockk) — 정상 / PG 실패 / 할인 적용 금액 일치
- [ ] `MemberPointDao`, `PointTransactionDao` 통합 테스트 — row lock, UNIQUE 제약 위반
- [ ] `PointChargeApi` REST Docs — 201 / 409 (멱등 중복, 가격 불일치, 미지원 수단) / 402 (PG 실패) / 404 (상품 미존재)
- [ ] `./gradlew build` 전체 그린
- [ ] `SecurityConfig`에서 GET permitAll, POST authenticated 확인

---

## 9. 확인 필요 사항 (확정 상태)

| # | 항목 | 결정 |
|---|------|------|
| 1 | 멱등키 재요청 정책 | ✅ **A. 409 Conflict** (재응답 200은 후속 개선) |
| 2 | PG 모듈 분리 | ✅ **1차는 `boot/ma-boot-web/config`에 Mock 배치**, 실 PG 어댑터 추가 시점에 별도 모듈로 분리 검토 |
| 3 | 보상 트랜잭션(cancel) | ✅ 시그니처만 포함(Mock no-op), **자동 호출은 후속 PR** |
| 4 | 가격 변경 처리 | ✅ **A. `expectedPrice` 검증 → 불일치 시 409** |
| 5 | 멱등키 필수 | ✅ **필수** (`@NotBlank`) |
| 6 | 금액 계산 기준 | ✅ **서버 재계산** (클라이언트 금액 수용 X) |
| 7 | 인증 분리 | ✅ GET permitAll / **POST authenticated** |
| 8 | 응답 필드 | ✅ `transactionId / chargedQuantity / balance / paidAmount / approvalNumber` |
| 9 | `PaymentApproval` 구조 | ✅ `approvalNumber / approvedAmount / approvedAt / paymentMethod` (풍성, 확장 여지) |
| 10 | 초기 잔액 | ✅ **첫 충전 시 upsert** (회원가입 시 자동 insert 안 함) |
| 11 | `paymentToken` 필드 | ✅ **포함** (실 PG 연동 시 의미 생김) |
| 12 | 다중 PG 아키텍처 | ✅ **`PaymentApprover` + `PaymentApproverRouter`** + `PaymentMethod` enum 1차부터 도입 |
| 13 | `supports(method)` 인터페이스 | ✅ `PaymentApprover` 포트에 포함. Mock은 항상 true |
