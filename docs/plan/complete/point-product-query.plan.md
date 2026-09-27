# Plan: 포인트 상품 조회 API

> 작성일: 2026-04-15

## 1. 개요

앱 내 재화(인연) 상품 목록을 조회하는 API를 구현한다. api-todo.md에 정의된 `GET /api/points` 엔드포인트를 사용하며, DB에서 활성 상품 목록을 조회하여 반환한다. 인증이 필요한 API이다.

---

## 2. 아키텍처

```
┌─────────────────────────────────────────────────┐
│ boot/ma-boot-web                                │
│  PointQueryApi                                  │
│    GET /api/points → findProducts()             │
│    └── PointQueryService.findProducts()         │
│    └── 반환: List<PointProductResponse>         │
└──────────────────────┬──────────────────────────┘
                       │ (port)
┌──────────────────────▼──────────────────────────┐
│ domain/ma-domain-core                           │
│  PointQueryService                              │
│    + findProducts(): List<PointProduct>          │
│  PointProductQueryRepository (port)             │
│    + find(): List<PointProduct>                  │
│  PointProduct (domain model)                    │
│    - pointProductId: Long                       │
│    - name: String                               │
│    - quantity: Int                               │
│    - price: Int                                  │
│    - displayOrder: Int                           │
└──────────────────────┬──────────────────────────┘
                       │ (implements)
┌──────────────────────▼──────────────────────────┐
│ infrastructure/storage/ma-db-core               │
│  PointProductQueryCoreRepository                │
│    → PointProductQueryDao                       │
│    → PointProductEntity.toDomain()              │
│  PointProductTable (BaseTable 상속)              │
│  RowEntityMapper.toPointProductEntity()          │
└─────────────────────────────────────────────────┘
```

---

## 3. 변경 전략

| 레이어 | 구성요소 | 역할 |
|--------|----------|------|
| DDL | `POINT_PRODUCTS` 테이블 | 상품 정보 저장 (name, quantity, price, display_order) |
| Table | `PointProductTable` | Exposed Table 정의, BaseTable 상속 |
| Entity | `PointProductEntity` | ResultRow → Entity 매핑, `toDomain()` 제공 |
| RowEntityMapper | `toPointProductEntity()` | ResultRow → PointProductEntity 변환 함수 추가 |
| Domain Model | `PointProduct` | 도메인 모델 (비즈니스 행위 없음, 단순 조회용) |
| Port | `PointProductQueryRepository` | `fun find(): List<PointProduct>` |
| DAO | `PointProductQueryDao` | activeRows 조회, displayOrder ASC 정렬 |
| Repository | `PointProductQueryCoreRepository` | 포트 구현, DAO 위임 + toDomain() 변환 |
| Service | `PointQueryService` | Repository 호출만 (조합 역할) |
| Response DTO | `PointProductResponse` | `companion object { fun from(PointProduct) }` |
| Controller | `PointQueryApi` | GET /api/points, 인증 필요 |

---

## 4. 상세 설계

### 4.1 DDL - POINT_PRODUCTS 테이블

**파일**: `infrastructure/storage/ma-db-core/src/main/resources/script/ddl.sql`
**변경 유형**: 수정 (DDL 추가)

- `POINT_PRODUCTS` 테이블 추가
- 컬럼: `POINT_PRODUCT_ID` (PK, BIGINT AUTO_INCREMENT), `NAME` (VARCHAR 100), `QUANTITY` (INT), `PRICE` (INT), `DISPLAY_ORDER` (INT)
- BaseTable 공통 컬럼 포함 (CREATED_DATE, CREATED_BY, LAST_MODIFIED_DATE, LAST_MODIFIED_BY, DELETED, DELETED_DATE, DELETED_BY)
- FK 사용 금지, 인덱스 불필요 (소량 데이터)

### 4.2 Infrastructure - PointProductTable

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/point/entity/table/PointProductTable.kt`
**변경 유형**: 신규

- `BaseTable("POINT_PRODUCTS", "POINT_PRODUCT_ID")` 상속
- 컬럼: `name` (varchar 100), `quantity` (integer), `price` (integer), `displayOrder` (integer)

### 4.3 Infrastructure - PointProductEntity

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/point/entity/PointProductEntity.kt`
**변경 유형**: 신규

- 프로퍼티: `id: Long`, `name: String`, `quantity: Int`, `price: Int`, `displayOrder: Int`
- `fun toDomain(): PointProduct` — Entity를 도메인 모델로 변환

### 4.4 Infrastructure - RowEntityMapper

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/common/RowEntityMapper.kt`
**변경 유형**: 수정

- `fun toPointProductEntity(row: ResultRow): PointProductEntity` 추가
- PointProductTable에서 각 컬럼 매핑

### 4.5 Infrastructure - PointProductQueryDao

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/point/dao/PointProductQueryDao.kt`
**변경 유형**: 신규

- `fun find(): List<PointProductEntity>` — `PointProductTable.activeRows { Op.TRUE }` + `orderBy(displayOrder to SortOrder.ASC)`

### 4.6 Domain - PointProduct

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/point/domain/PointProduct.kt`
**변경 유형**: 신규

- 프로퍼티: `pointProductId: Long`, `name: String`, `quantity: Int`, `price: Int`, `displayOrder: Int`
- 단순 조회용 모델, 별도 행위 없음

### 4.7 Domain - PointProductQueryRepository (Port)

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/point/domain/port/PointProductQueryRepository.kt`
**변경 유형**: 신규

- `fun find(): List<PointProduct>` — 활성 상품 전체 조회

### 4.8 Infrastructure - PointProductQueryCoreRepository

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/point/repository/PointProductQueryCoreRepository.kt`
**변경 유형**: 신규

- `PointProductQueryRepository` 구현
- DAO 호출 → `entity.toDomain()` 변환

### 4.9 Domain - PointQueryService

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/point/application/PointQueryService.kt`
**변경 유형**: 신규

- `@Service`, `@Transactional(readOnly = true)`
- `fun findProducts(): List<PointProduct>` — Repository.find() 호출

### 4.10 Boot - PointProductResponse

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/point/api/response/PointProductResponse.kt`
**변경 유형**: 신규

- 프로퍼티: `pointProductId: Long` (EncryptId), `name: String`, `quantity: Int`, `price: Int`
- `companion object { fun from(pointProduct: PointProduct): PointProductResponse }`
- displayOrder는 정렬용이므로 응답에 포함하지 않음

### 4.11 Domain - ObfuscationType

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/common/domain/id/ObfuscationType.kt`
**변경 유형**: 수정

- `POINT_PRODUCT("point-product")` 추가

### 4.12 Boot - PointQueryApi

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/point/api/PointQueryApi.kt`
**변경 유형**: 신규

- `@RestController`, `@RequestMapping("/api/points")`
- `@GetMapping fun findProducts(): List<PointProductResponse>` — Service 호출 → Response 변환

---

## 5. 구현 순서

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 1 | `infrastructure/.../script/ddl.sql` | 수정 | POINT_PRODUCTS 테이블 DDL 추가 |
| 2 | `infrastructure/.../point/entity/table/PointProductTable.kt` | 신규 | Exposed Table 정의 |
| 3 | `infrastructure/.../point/entity/PointProductEntity.kt` | 신규 | Entity + toDomain() |
| 4 | `infrastructure/.../common/RowEntityMapper.kt` | 수정 | toPointProductEntity() 추가 |
| 5 | `infrastructure/.../point/dao/PointProductQueryDao.kt` | 신규 | 전체 조회 DAO |
| 6 | `domain/.../point/domain/PointProduct.kt` | 신규 | 도메인 모델 |
| 7 | `domain/.../point/domain/port/PointProductQueryRepository.kt` | 신규 | 조회 포트 |
| 8 | `domain/.../common/domain/id/ObfuscationType.kt` | 수정 | POINT_PRODUCT 추가 |
| 9 | `infrastructure/.../point/repository/PointProductQueryCoreRepository.kt` | 신규 | 포트 구현체 |
| 10 | `domain/.../point/application/PointQueryService.kt` | 신규 | 서비스 |
| 11 | `boot/.../point/api/response/PointProductResponse.kt` | 신규 | 응답 DTO |
| 12 | `boot/.../point/api/PointQueryApi.kt` | 신규 | 컨트롤러 |

---

## 6. 고려사항

- **도메인 패키지**: `point` 패키지를 신규 생성한다. api-todo.md에 "인연" 도메인이 별도로 정의되어 있으며, 추후 충전(POST /api/points), 잔액 조회(GET /api/points/me) 등 확장이 예정되어 있으므로 독립 도메인으로 분리하는 것이 적절하다.
- **상품 데이터 규모**: 상품은 소량(3~10건 수준)이므로 페이징 불필요. 전체 조회 후 displayOrder 정렬로 충분하다.
- **displayOrder 컬럼**: 상품 노출 순서를 관리자가 제어할 수 있도록 정렬 순서 컬럼을 둔다. 응답에는 포함하지 않는다.
- **ID 난독화**: 기존 프로젝트 패턴에 맞게 ObfuscationType.POINT_PRODUCT을 추가하고 EncryptId 적용한다.
- **EntityType 추가 여부**: 상품 조회는 단건 조회가 없으므로 EntityNotFoundException을 던질 경우가 없다. EntityType 추가는 불필요하다.
- **캐싱**: 상품 정보는 자주 변경되지 않으므로 추후 Redis 캐싱 적용을 고려할 수 있으나, 1차 구현에서는 DB 직접 조회로 진행한다.
- **FK 미사용**: DDL 규칙에 따라 FK 제약조건 없이 PK + INDEX만 사용한다.

---

## 7. 검증 항목

- [ ] `./gradlew build` 빌드 성공
- [ ] PointProductQueryDao 단위 테스트 — 상품 조회 정렬 확인
- [ ] PointQueryService 단위 테스트 — Repository mock, 조회 결과 반환 확인
- [ ] PointQueryApi REST Docs 테스트 — GET /api/points 응답 스니펫 생성
- [ ] 상품 0건일 때 빈 리스트 반환 확인
