# Plan: Flyway 도입 1단계 (baseline + 인덱스 마이그레이션 + 프로파일 배선)

- 작성일: 2026-07-06
- 작업 유형: 인프라(스키마 마이그레이션 체계 도입)
- 대상 저장소: meet-again-backend (현재 브랜치 develop)
- 관련 자산: `infrastructure/storage/ma-db-core/src/main/resources/script/ddl.sql`,
  `.../script/migration/20260706_add_indexes.sql` (PR #43)

---

## 목표

현재 `local`/운영 환경은 **자동 DDL(Data Definition Language, 스키마 정의문)이 없어** `script/ddl.sql`을
사람이 손으로 실행해 스키마를 만든다. 이 수동 절차를 **Flyway 버전 관리 마이그레이션**으로 대체한다.
1단계 범위는 **베이스라인 + 인덱스 마이그레이션 + local·운영 프로파일 배선**까지이며,
**테스트는 기존 H2 + `SchemaUtils` 방식을 그대로 유지**한다.

---

## 현황 분석 (코드 확인 결과)

| 항목 | 현재 상태 | 시사점 |
|------|-----------|--------|
| 스키마 생성(local/운영) | 자동 DDL 없음, `ddl.sql` 수동 실행 | **Flyway가 메꿀 공백** |
| 스키마 생성(test) | H2 인메모리 + 각 테스트 `SchemaUtils.create/drop` (명시 테이블 목록). batch 테스트만 `exposed.generate-ddl:true` + `spring.batch.jdbc.initialize-schema:always` | **테스트는 flyway 비활성 유지** |
| 프로파일 | `local`(기본), `test`만 존재. **운영(prod) 프로파일 없음** | prod 프로파일 신설 필요 |
| 설정 로딩 | 커스텀 `MaEnvironmentPostProcessor`가 활성 프로파일 기준 전 모듈 `config/application-{profile}.yml` 머지. DB 설정은 `ma-db-core` | flyway 설정도 `ma-db-core`의 `config/`에 배치 |
| 앱 배선 | web·batch 둘 다 `ma-db-core`를 `runtimeOnly` 의존 | flyway 추가 시 **두 앱 모두 부팅 시 자동 마이그레이션**(Flyway 락으로 동시 실행 안전) |
| 빌드 | Spring Boot 3.3.4 → Flyway 10.x (BOM 관리) | **MariaDB는 `flyway-mysql` 모듈 필수**(Flyway 10부터 core에서 분리) |
| ddl vs 인덱스 스크립트 | `ddl.sql`은 **이미 PR#43 인덱스 포함(완성형)**. `20260706_add_indexes.sql`은 **인덱스 없는 기존 DB** 대상 추가분 | 신규 빈 DB에 `V1(ddl)→V2(add index)` 순차 적용 시 **중복 인덱스로 V2 실패** → V2 멱등화 필수 |

---

## 확정 결정 (2026-07-06 사용자 확정)

- **baseline: (A) 자동 baseline** — `baseline-on-migrate=true`, `baseline-version=1`. 기존 비어있지 않은 DB는 V1을 자동 스탬프 후 V2부터 실행. **V2는 `IF NOT EXISTS` 멱등**으로 인덱스 유무와 무관하게 안전.
- **실행 모델: 하이브리드**
  - **local**: 앱 부팅 시 자동 마이그레이션(`spring.flyway.enabled=true`). 로컬은 재시작 비용이 없어 편의 우선.
  - **prod**: 앱 부팅 자동 **비활성**(`spring.flyway.enabled=false`), 대신 배포 파이프라인의 **Gradle `flywayMigrate` 스텝**이 소유. 이유: 긴 DDL 이 부팅을 막거나 마이그레이션 실패가 라이브 앱 부팅을 막는 것을 방지. → 운영에선 web·batch 어느 쪽도 부팅 자동 마이그레이션을 하지 않음(버전 스큐/부팅 블로킹 원천 차단).

---

## 작업 범위

> ✅ **구현 완료 (2026-07-06).** 아래 범위대로 구현·검증 완료. 실제 MariaDB 11.4 컨테이너로 신규/기존 두 시나리오 E2E 검증 통과(§4).

### 1) 의존성 — `infrastructure/storage/ma-db-core/build.gradle.kts`

- 런타임(부팅 자동, local): `implementation("org.flywaydb:flyway-core")` + `implementation("org.flywaydb:flyway-mysql")` — Spring Boot BOM이 **10.10.0** 관리.
- 운영 분리 스텝: Gradle 플러그인 `id("org.flywaydb.flyway") version "10.10.0"`. DB 드라이버/모듈은 플러그인 classpath(`buildscript.dependencies.classpath`)에 `mariadb-java-client:3.3.3` + `flyway-mysql:10.10.0` 주입(플러그인 10.10.0엔 `flywayMigration` configuration 없음 → buildscript 방식).
- `flyway { url/user/password = System.getenv(DB_URL/DB_USERNAME/DB_PASSWORD); locations = filesystem:src/main/resources/db/migration; baselineOnMigrate=true; baselineVersion="1" }`

### 2) 마이그레이션 파일 — `ma-db-core/src/main/resources/db/migration/`

| 파일 | 내용 |
|------|------|
| `V1__baseline.sql` | 현재 `script/ddl.sql` 내용 그대로(전체 테이블 + 인덱스) |
| `V2__add_indexes.sql` | `20260706_add_indexes.sql` 기반. **멱등화**: 인덱스 추가를 `ADD INDEX IF NOT EXISTS`(MariaDB 지원)로 변경 → 신규 DB(V1에서 이미 인덱스 생성)에선 no-op, 기존 DB에선 실제 추가. 좋아요 중복 정리 `DELETE`는 빈 테이블에서도 무해하므로 유지. **FK drop 섹션(§3)은 자동 마이그레이션에서 제외** — 제약명이 환경마다 달라 결정적 실행 불가, FK금지 규칙 + ddl.sql 무FK라 신규엔 무관, 레거시 DB 한정 수동 절차로 문서에 남김 |

- 기존 `script/ddl.sql`, `script/migration/20260706_add_indexes.sql`은 **"현재 전체 스키마" 참조 문서로 유지**(사람이 읽는 소스, 삭제하지 않음).
- 마이그레이션 SQL도 **FK 미사용(PK·INDEX만)** 규칙 준수.

### 3) 프로파일 배선

| 파일 | 변경 |
|------|------|
| `ma-db-core/src/main/resources/config/application-local.yml` | `spring.flyway` 블록 추가(enabled=true, locations=`classpath:db/migration`, baseline-on-migrate=true, baseline-version=1) |
| `ma-db-core/src/main/resources/config/application-prod.yml` | **신규**. 운영 datasource(env 주입 `${DB_URL}`/`${DB_USERNAME}`/`${DB_PASSWORD}`) + 동일 flyway 블록 |
| 테스트 yml (db-core `src/test`·`src/testFixtures`, batch `src/test`) | **flyway 비활성** — `spring.flyway.enabled: false`. "기본 off, local/prod만 on" 방향으로 배선해 web API 테스트(test 프로파일) 포함 전 테스트가 H2+SchemaUtils 유지 |

> web API 테스트는 `@BaseApiTest`가 db-core testFixtures의 datasource(H2)를 사용 → testFixtures yml의 flyway off가 web 테스트도 커버하는지 구현 시 확인.

### 4) 검증 결과 (✅ 통과)

- **전체 테스트**: `./gradlew test` 그린 — flyway가 classpath에 있어도 test 프로파일에선 비활성이라 기존 H2+SchemaUtils 방식 그대로. web API 테스트는 testFixtures yml의 `flyway.enabled=false`로 커버됨(확인 완료).
- **E2E (MariaDB 11.4 컨테이너, `flywayMigrate` 실제 실행)**:
  - **신규 빈 DB**: V1→V2 적용, `flyway_schema_history`에 V1(SQL)·V2(SQL) 둘 다 Success, 테이블 20개, 유니크 인덱스 존재. V2는 V1이 만든 인덱스 위에서 `IF NOT EXISTS`로 no-op(“Duplicate key name” = 오류 아닌 **경고**, BUILD SUCCESSFUL). 재실행 시 Pending 0(멱등).
  - **기존 pre-index DB**(V1 스키마 직접 로드 후 인덱스 4개 드롭으로 재현): `flywayMigrate` → history rank1 = `<< Flyway Baseline >>` **type=BASELINE**(V1 실행 스킵, CREATE 충돌 없음), rank2 = V2 Success. 드롭된 인덱스 4개 전부 V2가 재생성. 나머지 인덱스는 경고만 남기고 no-op.

> 참고: 신규 DB에선 V2가 매번 16개 “Duplicate key name” 경고를 남긴다(V1=완성형 ddl 채택의 트레이드오프). 오류 아님, 부팅/마이그레이션 정상 성공.

---

## 범위 밖 (후속 플래그 — Phase 1 아님)

- **Spring Batch 메타데이터 테이블(`BATCH_*`)**: `ddl.sql`/V1에 없음. 운영 배치는 별도 대응 필요(후속 `V3__batch_metadata.sql` 또는 `spring.batch.jdbc.initialize-schema`). 지금은 명시적 플래그만.
- **batch local 부팅 자동 마이그레이션 + lazy-init**: batch `application.yml`의 `spring.main.lazy-initialization=true` 때문에 batch를 **로컬에서 단독 부팅**하면 flyway 초기화가 지연될 수 있음. 운영은 flyway off(Gradle 스텝 소유)라 무관하고, 로컬은 보통 web이 마이그레이션을 담당하므로 실무상 문제 없음. 필요 시 후속 검토.
- 운영 시크릿/S3 등 인프라 주입은 별도 작업.
- `exposed.generate-ddl`, dummy-data 로딩 정책 재정비.

---

## 유지보수 규칙

- **스키마 변경은 새 V파일(V3, V4…)로만.** 이미 적용된 V1/V2는 체크섬 때문에 절대 수정 금지.
- **DB를 손으로 직접 변경 금지.** 수동 변경은 이력과 어긋나 다음 배포 시 부팅 실패를 유발.
- `script/ddl.sql`은 "현재 전체 스키마" 참조 스냅샷으로 유지 — 스키마 변경 시 새 V파일과 함께 현행화.

## 참조

- 모듈·패키지 배치: [[clean-architecture]]
- FK 금지 규칙: PK·INDEX만 사용
