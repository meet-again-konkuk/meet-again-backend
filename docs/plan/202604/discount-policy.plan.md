# Plan: 포인트 상품 할인 정책 (DiscountPolicy) 구현

> 작성일: 2026-04-16

## 1. 개요

PointProduct에 할인 정책(DiscountPolicy)을 적용하여, 고정 금액 할인과 비율 할인을 지원한다.
기존 GET /api/points 응답에 할인 정보(할인 유형, 할인 전/후 가격, 할인 활성 여부)를 포함한다.
DB는 CTI(Class Table Inheritance) 전략으로 부모/자식 테이블을 분리한다.

## 2. 변경 전략

### 2.1 도메인 모델 설계

| 클래스 | 유형 | 핵심 필드/행위 |
|--------|------|---------------|
| `DiscountPolicy` | sealed class | discountPolicyId, startDate, endDate, `isActive(now)`, `calculateDiscountedPrice(price)` |
| `AmountDiscountPolicy` | DiscountPolicy 자식 | discountAmount: Int |
| `PercentDiscountPolicy` | DiscountPolicy 자식 | discountPercent: Int (10 = 10%) |
| `PointProduct` | 기존 수정 | `discountPolicy: DiscountPolicy?` 추가, `discountedPrice(now): Int` 행위 추가 |

- `DiscountPolicy`는 sealed class로 구현 (Kotlin sealed class로 타입 안전한 분기 가능)
- `isActive(now: LocalDate)`: startDate <= now <= endDate
- `calculateDiscountedPrice(price: Int)`: 할인 적용된 가격 반환 (0 미만이면 0)
- `PointProduct.discountedPrice(now)`: discountPolicy가 null이거나 비활성이면 원래 price, 아니면 policy에 위임

### 2.2 DB 설계 (CTI 전략)

| 테이블 | 역할 | 주요 컬럼 |
|--------|------|----------|
| `DISCOUNT_POLICIES` | 부모 테이블 | DISCOUNT_POLICY_ID (PK, AUTO_INCREMENT), POLICY_TYPE (VARCHAR(20), NOT NULL), START_DATE (DATE, NOT NULL), END_DATE (DATE, NOT NULL) + BaseTable 공통 |
| `AMOUNT_DISCOUNT_POLICIES` | 자식 (고정 금액) | DISCOUNT_POLICY_ID (PK), DISCOUNT_AMOUNT (INT, NOT NULL) |
| `PERCENT_DISCOUNT_POLICIES` | 자식 (비율) | DISCOUNT_POLICY_ID (PK), DISCOUNT_PERCENT (INT, NOT NULL) |
| `POINT_PRODUCTS` | 기존 수정 | DISCOUNT_POLICY_ID (BIGINT, NULL) 컬럼 추가 |

- FK 사용 금지 (프로젝트 규칙). 자식 테이블 PK는 부모 PK와 동일 값이지만 FK 제약 없음
- POINT_PRODUCTS.DISCOUNT_POLICY_ID도 INDEX만 설정, FK 없음
- POLICY_TYPE: 'AMOUNT' | 'PERCENT'

### 2.3 인프라 레이어 전략

| 레이어 | 현재 | 변경 후 | 변환 위치 |
|--------|------|---------|-----------|
| Table | PointProductTable만 존재 | DiscountPolicyTable, AmountDiscountPolicyTable, PercentDiscountPolicyTable 신규, PointProductTable에 discountPolicyId 추가 | 인프라 테이블 정의 |
| Entity | PointProductEntity만 존재 | DiscountPolicyEntity 신규 (policyType 포함, amount/percent nullable), PointProductEntity에 discountPolicyId 추가 | Entity 클래스 |
| Entity.toDomain() | PointProduct 변환 | DiscountPolicyEntity.toDomain()에서 policyType으로 분기하여 AmountDiscountPolicy/PercentDiscountPolicy 생성, PointProductEntity.toDomain(discountPolicy) 오버로드 | Entity 변환 메서드 |
| DAO | PointProductQueryDao.find() | DiscountPolicyQueryDao 신규 — LEFT JOIN으로 부모+자식 한 번에 조회. PointProductQueryDao는 변경 없음 | DAO 클래스 |
| Repository | PointProductQueryCoreRepository | DiscountPolicyQueryDao 조합하여 PointProduct에 DiscountPolicy 연결. find()에서 상품 목록 조회 후 discountPolicyId가 있는 것들만 벌크로 DiscountPolicy 조회, 메모리에서 매핑 | Repository 구현체 |
| Cache | CachedPointProduct | 할인 관련 필드 추가: discountType, discountAmount, discountPercent, startDate, endDate (모두 nullable) | Redis DTO |
| Response | PointProductResponse | discountedPrice, discountType, isDiscountActive 필드 추가 | Response DTO |

### 2.4 DAO 조회 전략 (N+1 방지)

DiscountPolicy 조회는 LEFT JOIN 방식으로 한 번의 쿼리로 부모+자식을 모두 가져온다:
- `DiscountPolicyTable LEFT JOIN AmountDiscountPolicyTable LEFT JOIN PercentDiscountPolicyTable`
- POLICY_TYPE으로 분기하여 Entity 생성
- PointProductQueryCoreRepository.find()에서:
  1. PointProductQueryDao.find()로 상품 목록 조회
  2. discountPolicyId가 null이 아닌 것들의 ID 수집
  3. DiscountPolicyQueryDao.find(ids)로 벌크 조회
  4. Map<Long, DiscountPolicy>로 변환 후 상품에 매핑

## 3. 변경 파일 목록

### Phase 1: DDL

| # | 파일 | 내용 |
|---|------|------|
| 1 | `infrastructure/storage/ma-db-core/src/main/resources/script/ddl.sql` | DISCOUNT_POLICIES, AMOUNT_DISCOUNT_POLICIES, PERCENT_DISCOUNT_POLICIES 테이블 추가. POINT_PRODUCTS에 DISCOUNT_POLICY_ID 컬럼 + INDEX 추가 |

### Phase 2: Domain Model

| # | 파일 | 내용 |
|---|------|------|
| 2 | `domain/.../point/domain/discount/DiscountPolicy.kt` | sealed class DiscountPolicy — `isActive(now: LocalDate): Boolean`, `calculateDiscountedPrice(price: Int): Int` 추상 메서드 |
| 3 | `domain/.../point/domain/discount/AmountDiscountPolicy.kt` | DiscountPolicy 자식 — discountAmount 필드, calculateDiscountedPrice에서 price - discountAmount (최소 0) |
| 4 | `domain/.../point/domain/discount/PercentDiscountPolicy.kt` | DiscountPolicy 자식 — discountPercent 필드, calculateDiscountedPrice에서 price * (100 - percent) / 100 |
| 5 | `domain/.../point/domain/PointProduct.kt` | `discountPolicy: DiscountPolicy?` 필드 추가, `discountedPrice(now: LocalDate): Int` 행위 추가 |

### Phase 3: Infrastructure - Table & Entity

| # | 파일 | 내용 |
|---|------|------|
| 6 | `infrastructure/.../point/entity/table/DiscountPolicyTable.kt` | BaseTable 상속, POLICY_TYPE, START_DATE, END_DATE 컬럼 |
| 7 | `infrastructure/.../point/entity/table/AmountDiscountPolicyTable.kt` | LongIdTable("AMOUNT_DISCOUNT_POLICIES", "DISCOUNT_POLICY_ID"), DISCOUNT_AMOUNT 컬럼. BaseTable 미상속 (공통 감사 컬럼 불필요) |
| 8 | `infrastructure/.../point/entity/table/PercentDiscountPolicyTable.kt` | LongIdTable("PERCENT_DISCOUNT_POLICIES", "DISCOUNT_POLICY_ID"), DISCOUNT_PERCENT 컬럼. BaseTable 미상속 |
| 9 | `infrastructure/.../point/entity/table/PointProductTable.kt` | discountPolicyId 컬럼 추가 (long, nullable) |
| 10 | `infrastructure/.../point/entity/DiscountPolicyEntity.kt` | policyType, startDate, endDate, discountAmount?, discountPercent? 필드. `toDomain(): DiscountPolicy` — policyType으로 분기 |
| 11 | `infrastructure/.../point/entity/PointProductEntity.kt` | discountPolicyId: Long? 필드 추가. `toDomain(discountPolicy: DiscountPolicy?): PointProduct` 오버로드 추가 |

### Phase 4: Infrastructure - DAO & Repository

| # | 파일 | 내용 |
|---|------|------|
| 12 | `infrastructure/.../point/dao/DiscountPolicyQueryDao.kt` | `fun find(ids: List<Long>): List<DiscountPolicyEntity>` — LEFT JOIN 3테이블, IN 절로 벌크 조회 |
| 13 | `infrastructure/.../common/RowEntityMapper.kt` | `toDiscountPolicyEntity(row: ResultRow): DiscountPolicyEntity` 추가, `toPointProductEntity`에 discountPolicyId 매핑 추가 |
| 14 | `infrastructure/.../point/repository/PointProductQueryCoreRepository.kt` | DiscountPolicyQueryDao 주입, find()에서 상품 조회 -> discountPolicyId 수집 -> 벌크 조회 -> 매핑 |

### Phase 5: Redis Cache

| # | 파일 | 내용 |
|---|------|------|
| 15 | `infrastructure/.../point/dao/PointProductCacheDao.kt` | CachedPointProduct에 discountType, discountAmount, discountPercent, startDate, endDate 필드 추가 (모두 nullable, 기본값 null) |
| 16 | `infrastructure/.../point/repository/PointProductRedisCacheRepository.kt` | toDomain/toCached 변환에 할인 정보 매핑 추가 |

### Phase 6: API Response

| # | 파일 | 내용 |
|---|------|------|
| 17 | `boot/.../point/api/response/PointProductResponse.kt` | discountedPrice: Int?, discountType: String?, isDiscountActive: Boolean 필드 추가. from()에서 LocalDate.now() 기준 계산 |
| 18 | `boot/.../point/api/PointQueryApi.kt` | 변경 없음 (Service 반환 타입 동일, Response 변환에서 처리) |

### Phase 7: Fixture & Test

| # | 파일 | 내용 |
|---|------|------|
| 19 | `domain/.../point/fixture/DiscountPolicyFixture.kt` | AmountDiscountPolicy, PercentDiscountPolicy 팩토리 메서드 |
| 20 | `domain/.../point/fixture/PointProductFixture.kt` | discountPolicy 파라미터 추가 (기본값 null) |
| 21 | `domain/...test.../point/domain/discount/DiscountPolicyTest.kt` | isActive, calculateDiscountedPrice 단위 테스트 |
| 22 | `domain/...test.../point/domain/PointProductTest.kt` | discountedPrice 행위 테스트 (할인 있을 때/없을 때/비활성일 때) |
| 23 | `boot/...test.../point/api/PointQueryApiTest.kt` | 할인 정보 포함된 응답 문서화 테스트 추가 |

## 4. 고려사항

- **자식 테이블에 BaseTable 미상속**: AMOUNT_DISCOUNT_POLICIES, PERCENT_DISCOUNT_POLICIES는 부모 테이블(DISCOUNT_POLICIES)이 감사 컬럼을 관리하므로 자식에는 불필요. LongIdTable 직접 상속
- **FK 미사용**: 프로젝트 규칙에 따라 POINT_PRODUCTS.DISCOUNT_POLICY_ID와 자식 테이블 PK 모두 FK 없이 INDEX만 설정
- **N+1 방지**: 상품 조회 후 discountPolicyId를 수집하여 한 번의 IN 쿼리로 벌크 조회. 상품 수가 적으므로(3~5개) 성능 이슈 없음
- **Redis 캐시 호환**: CachedPointProduct에 nullable 필드 추가. 기존 캐시 데이터에 새 필드가 없어도 Jackson 역직렬화 시 null 기본값으로 처리됨. 다만 배포 시 기존 캐시 무효화 또는 TTL 자연 만료(24시간) 대기 필요
- **sealed class vs abstract class**: sealed class를 사용하면 when 분기 시 else 불필요, 새 타입 추가 시 컴파일 타임 체크 가능. 도메인 모듈에 Spring 의존성 없이 순수 Kotlin으로 구현
- **discountedPrice 계산 시점**: Response 변환 시 LocalDate.now() 사용. 서버 시간 기준
- **discountPercent는 정수**: 10 = 10%. 소수점 할인이 필요하면 향후 확장. 현재는 정수로 충분
- **할인가가 0 이하**: AmountDiscountPolicy에서 price - discountAmount가 음수가 되면 0으로 처리 (maxOf(0, price - discountAmount))
- **PointProductEntity.toDomain() 호환**: 기존 `toDomain()`은 유지하되 discountPolicy = null로 생성. 새로운 `toDomain(discountPolicy)` 오버로드 추가. Repository에서 매핑 시 오버로드 사용

## 5. 검증 항목

- [ ] DiscountPolicy.isActive 단위 테스트: 기간 내/기간 전/기간 후/경계값
- [ ] AmountDiscountPolicy.calculateDiscountedPrice: 정상 할인, 0 이하 방지
- [ ] PercentDiscountPolicy.calculateDiscountedPrice: 10% 할인, 100% 할인, 0% 할인
- [ ] PointProduct.discountedPrice: 할인 있을 때, null일 때, 비활성일 때
- [ ] PointQueryApiTest: 할인 정보 포함 응답 REST Docs 문서화
- [ ] 빌드 성공: `./gradlew build`
- [ ] 기존 테스트 통과: 기존 PointQueryServiceTest, PointProductProviderTest, PointQueryApiTest
