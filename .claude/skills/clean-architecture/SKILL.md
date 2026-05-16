---
name: clean-architecture
description: "Kotlin + Spring Boot 백엔드를 멀티 모듈 헥사고날(포트-어댑터) 구조로 설계하기 위한 가이드. 모듈 분리 원칙(boot/domain/infrastructure/config), 모듈 간 의존 방향, 도메인별 패키지 컨벤션(application/domain/exception 및 port 하위 패키지), 어댑터 모듈의 패키지 미러링, 신규 프로젝트의 Gradle 멀티 모듈 셋업 단계, 새 기능을 추가할 때 클래스를 어느 모듈·어느 패키지에 둘지 결정하는 기준을 다룬다. 신규 백엔드 프로젝트를 시작하거나, 기존 프로젝트에서 새 도메인/Service/Port/Adapter를 추가하거나, '어디에 둘지' 판단해야 할 때 반드시 이 스킬을 참조한다. code-implementation-rules가 '어떻게 짤지'를 다룬다면, 이 스킬은 '어떻게 모듈·패키지로 쪼개고 어디에 둘지'를 다룬다."
---

# Clean Architecture (Kotlin + Spring Boot 멀티 모듈 헥사고날)

이 스킬은 Kotlin + Spring Boot 백엔드를 **멀티 모듈 헥사고날(포트-어댑터) 구조**로 설계하기 위한 가이드다. 신규 프로젝트를 시작할 때든, 기존 프로젝트에 새 기능을 추가할 때든, "**어느 모듈·어느 패키지에 둘지**"의 답을 찾는 데 사용한다.

> 객체 설계 패턴(VO, 일급 컬렉션, 도메인 행위 부여 등)은 [[code-implementation-rules]]에서 다룬다. 이 스킬은 그 위 단계의 **모듈/레이어 경계**에 집중한다.

표기 규약:
- `{prefix}` — 프로젝트 약어 (예: 회사/서비스 약자, 보통 2~4자)
- `{group}` — 패키지 네임스페이스 (예: `com.acme.myservice`)
- `{domain}` — 비즈니스 도메인 이름 (예: `member`, `order`, `payment`)

---

## 1. 모듈 구조 (큰 그림)

```
{project-root}/
├── boot/                                # 실행 가능 Spring Boot 애플리케이션
│   ├── {prefix}-boot-web                # REST API 서버
│   └── {prefix}-boot-batch              # 배치 (필요 시)
│
├── domain/
│   └── {prefix}-domain-core             # 비즈니스 로직 (프레임워크 독립)
│
├── infrastructure/
│   ├── storage/                         # 영속화 어댑터
│   │   ├── {prefix}-db-core             # RDB (Exposed/JPA 등)
│   │   └── {prefix}-redis-core          # Redis (필요 시)
│   └── support/                         # 외부 시스템 어댑터 (한 모듈 = 한 시스템)
│       ├── {prefix}-jwt-core
│       ├── {prefix}-crypto-core
│       ├── {prefix}-sms-sender
│       ├── {prefix}-file-storage
│       └── {prefix}-payment-core
│
└── config/                              # 공통 설정 (필요 시)
    ├── {prefix}-config-yaml-importer
    └── {prefix}-config-logging
```

### 왜 이렇게 쪼개는가

| 분리 | 얻는 것 |
|------|---------|
| `boot` ↔ `domain` 분리 | 도메인이 Spring Web/Security/Boot 같은 무거운 의존성 없이도 컴파일/테스트 가능 |
| `boot` ↔ `infrastructure` 분리 (runtime-only) | Controller가 실수로 DAO/Entity를 import할 수 없음. 모듈 경계가 컴파일러로 강제됨 |
| 어댑터를 외부 시스템별로 잘게 분리 | 외부 시스템 교체(SMS 제공자 변경, 결제 PG 추가)가 한 모듈 안에서 닫힘 |
| `config` 분리 | 모든 모듈이 공유하는 yaml import/logging 설정을 중복 없이 재사용 |

### 모듈별 한 줄 정의

| 모듈 | 책임 | 허용되는 외부 의존 |
|------|------|---------------------|
| `boot/*-boot-web` | HTTP 입출력, 인증/인가, 예외 변환, 직렬화 | Spring Web/Security/Validation |
| `boot/*-boot-batch` | 배치 잡 정의(Reader/Processor/Writer) | Spring Batch |
| `domain/*-domain-core` | 도메인 모델, 비즈니스 규칙, 포트(인터페이스), Service 오케스트레이션 | ❌ Spring Boot Starter 금지. `spring-context`, `spring-tx`, `kotlin-logging` 정도까지만 |
| `infrastructure/storage/*` | 포트의 영속화 어댑터 (Entity/Table, DAO, Repository 구현) | Exposed/JPA, Spring Data Redis 등 |
| `infrastructure/support/*` | 포트의 외부 시스템 어댑터 (JWT/SMS/Crypto/PG ...) | 해당 외부 라이브러리만 |
| `config/*` | YAML 로더, 로깅 등 횡단 설정 | 최소 |

**핵심 규칙**: `domain` 모듈은 `spring-boot-starter-*`를 의존하지 않는다. 그래야 도메인이 "프레임워크 없이도 살아남는" 상태가 유지된다.

---

## 2. 의존 방향

```
boot ──compile──▶ domain
boot ──runtime──▶ infrastructure          (어댑터 구현은 런타임에만 보임)
infrastructure ──compile──▶ domain         (포트 구현을 위해 인터페이스를 봄)
domain ──▶ (없음)                          (도메인은 어디에도 의존하지 않음)
```

### Gradle 의존 선언 패턴 (Kotlin DSL)

**boot 모듈** — 도메인은 `implementation`, 어댑터는 `runtimeOnly`. 어댑터를 컴파일 타임에 가리는 것이 핵심이다.
```
implementation(project(":domain:{prefix}-domain-core"))
runtimeOnly(project(":infrastructure:storage:{prefix}-db-core"))
runtimeOnly(project(":infrastructure:storage:{prefix}-redis-core"))
runtimeOnly(project(":infrastructure:support:{prefix}-jwt-core"))
// ... 그 외 어댑터들도 모두 runtimeOnly
```

**infrastructure 모듈** — 도메인은 `implementation`.
```
implementation(project(":domain:{prefix}-domain-core"))
```

**domain 모듈** — 다른 프로젝트 모듈을 의존하지 않는다. Spring 의존도 최소화.
```
implementation("org.springframework:spring-context")
implementation("org.springframework:spring-tx")
implementation("io.github.oshai:kotlin-logging-jvm:...")
```

**추가 규칙**:
- infrastructure 모듈끼리는 서로 의존하지 않는다. 두 어댑터가 같은 데이터를 다뤄야 하면, 도메인에 포트를 두고 양쪽이 그 포트를 의존한다
- `domain`은 외부 라이브러리 타입(Exposed `ResultRow`, Jackson `@JsonProperty`, JJWT `Claims` 등)을 절대 import하지 않는다

---

## 3. 도메인 모듈 내부 구조 (`*-domain-core`)

도메인별로 패키지를 먼저 나누고, 각 도메인 안에서 다시 layer로 나눈다.

```
{group}.domain.{domain}/
├── application/                     # Service (오케스트레이션 only)
│   ├── {Domain}Service.kt
│   ├── {Domain}QueryService.kt
│   ├── command/                     # Service 입력 DTO
│   │   └── {Action}Command.kt
│   └── result/                      # Service 출력 DTO
│       └── {Action}Result.kt
├── domain/                          # 모델, 정책, 검증, Value Object, 도메인 컴포넌트
│   ├── {Domain}.kt                  # 애그리거트 루트
│   ├── New{Domain}.kt               # 생성용 도메인 객체
│   ├── {Domain}s.kt                 # 일급 컬렉션 (행위가 있을 때만)
│   ├── {Domain}Validator.kt         # @Component, 사전 검증
│   ├── port/                        # ⚠️ 포트(인터페이스). 어댑터가 구현
│   │   ├── {Domain}CommandRepository.kt
│   │   ├── {Domain}QueryRepository.kt
│   │   └── {ExternalSystem}.kt      # 영속화 외 외부 시스템 포트도 여기
│   └── {subcontext}/                # 도메인이 커지면 하위 컨텍스트로 분리
└── exception/                       # 도메인 예외
    └── {Domain}NotFoundException.kt
```

### 어떤 클래스를 어디에 둘 것인가

| 클래스 종류 | 위치 |
|-------------|------|
| Service (조합/오케스트레이션) | `application/` |
| Service 입력 명령 | `application/command/` |
| Service 결과 | `application/result/` |
| 애그리거트/엔티티 도메인 | `domain/` |
| Value Object | `domain/` (또는 하위 컨텍스트) |
| 생성용 객체 (`New{Domain}`) | `domain/` |
| 일급 컬렉션 (행위 있음) | `domain/` |
| Validator (`@Component`) | `domain/` |
| 도메인 정책/계산기 (`@Component`) | `domain/` |
| Port 인터페이스 | `domain/port/` |
| 도메인 예외 | `exception/` |

**도메인 패키지가 비대해질 때**: 새 도메인을 만들기 전에, 의미 있는 **하위 컨텍스트**로 분리할 수 있는지 먼저 검토한다. 예를 들어 결제 도메인이 잔액·지불·할인·이력 영역으로 나뉜다면 `payment/domain/{balance,approval,discount,history}/` 같이 한 도메인 안에서 응집을 유지한다.

---

## 4. 어댑터 모듈 구조

### 4-1. 영속화 어댑터 (`*-db-core`, `*-redis-core`)

어댑터 모듈의 패키지는 **도메인 모듈의 패키지를 그대로 미러링**한다. 그래야 "{domain} 도메인의 영속화 코드는 어디 있나?"를 즉시 답할 수 있다.

```
{group}.domain.{domain}/
├── repository/                      # 도메인의 포트를 구현
│   ├── {Domain}CommandCoreRepository.kt   # implements {Domain}CommandRepository
│   └── {Domain}QueryCoreRepository.kt     # implements {Domain}QueryRepository
├── dao/                             # 순수 DB 접근 (DSL/JPA Repository)
│   ├── {Domain}CommandDao.kt
│   └── {Domain}QueryDao.kt
└── entity/
    ├── {Domain}Entity.kt            # toDomain(), companion.from(row)
    └── table/
        └── {Domain}Table.kt         # Exposed Table 정의 (또는 JPA Entity)
```

| 클래스 | 책임 |
|--------|------|
| Repository 구현체 | **포트 구현만** — DAO 호출 + `entity.toDomain()` 변환. 비즈니스 로직, 조건 분기 금지 |
| DAO | 순수 DB 접근. Entity 또는 `List<Entity>` 반환 |
| Entity | DB row를 표현하는 데이터 클래스. `toDomain()`과 `companion object.from(row)` 제공 |
| Table | 테이블 정의. **FK 제약조건은 사용하지 않는다** — PK와 필요한 INDEX만 |

**클래스 명명**: 포트가 `XxxRepository`이면 구현체는 `XxxCoreRepository`(또는 `XxxRdbRepository`, `XxxRedisRepository`)처럼 **포트명 + 구현 기술명** 패턴으로. 포트와 구현체가 같은 이름이면 import 충돌이 난다.

### 4-2. 외부 시스템 어댑터 (`*-jwt-core`, `*-sms-sender`, ...)

각 모듈은 **하나의 외부 시스템에 대한 어댑터 + 그 모듈 전용 설정**만 갖는다.

```
{prefix}-sms-sender/
└── src/main/
    ├── kotlin/{group}/...
    │   ├── {Vendor}SmsSender.kt        # implements SmsSender (도메인의 포트)
    │   └── mock/LocalSmsSender.kt      # local profile용 mock 구현
    └── resources/config/application.yml
```

- 어댑터 클래스명: **구현 기술/벤더 + 포트명** (`CoolSmsSender`, `BCryptPasswordEncryptor`, `JwtTokenManager`, `HashidsIdObfuscator`)
- 한 어댑터 모듈에 여러 도메인의 어댑터가 들어가도 된다 (예: Redis 모듈에 auth, cache 도메인 어댑터 공존)
- 모듈 자체 설정은 **그 모듈의 `src/main/resources/config/`** 에 둔다. boot 모듈에 몰아넣지 않는다

---

## 5. boot 모듈 구조 (`*-boot-web`)

```
{group}/
├── config/                          # Spring Config (Security, Web, ObjectMapper, ...)
│   ├── SecurityConfig.kt
│   └── WebConfig.kt
├── support/                         # Web 횡단 관심사
│   ├── security/                    # 인증 필터, 인가 인터셉터
│   ├── error/                       # GlobalExceptionHandler
│   ├── payload/response/            # 공통 응답 포맷 (ApiError, CursorResponse 등)
│   └── validation/                  # ValidationPatterns 등 검증 상수
└── domain.{domain}/
    └── api/
        ├── {Domain}Api.kt           # Controller (@RestController)
        ├── request/
        │   └── {Action}Request.kt
        └── response/
            └── {Action}Response.kt
```

### Controller(Api) 규칙
- **Service만 의존**. Repository, Entity, DAO를 import하면 안 된다 (애초에 컴파일 타임에 보이지도 않아야 함 — §2의 runtimeOnly 규칙 덕분)
- 책임: 요청 파싱 → Service 호출 → 응답 변환. 로직 금지
- Request DTO에서 도메인 VO(`Email(...)` 등)를 직접 생성하지 않는다. Request/Command는 `String`을 받고, **도메인 모듈 안에서 VO로 변환**한다. VO 검증 실패가 web 예외 핸들러에 잘못 매핑되는 것을 막기 위함

---

## 6. 새 기능 추가 시 — 위치 결정 흐름

### Step 1. 어느 **도메인**인가?
- 기존 도메인 패키지에 들어맞으면 그곳에 둔다
- 새 도메인을 만드는 것은 마지막 선택. 기존 도메인의 **하위 컨텍스트**로 표현 가능한지 먼저 검토

### Step 2. 어느 **레이어**인가?

| 요구사항 | 위치 |
|---------|------|
| HTTP 엔드포인트가 필요하다 | `boot/*-boot-web` Api 추가 |
| 비즈니스 흐름을 묶는 Service 메서드 | `domain/.../application/` |
| 검증·계산·정책 같은 도메인 로직 | `domain/.../domain/` 의 도메인 객체 또는 `@Component` |
| 외부 시스템 호출(DB/Redis/SMS/...) 필요 | **domain 쪽 포트** 추가 → infrastructure 쪽 어댑터로 구현 |
| 배치 작업 | `boot/*-boot-batch` |

### Step 3. 외부 시스템이 필요하면 — Port-Adapter 패턴 적용

1. **포트 인터페이스를 도메인에 정의** — `domain.{domain}.domain.port.XxxRepository` (영속화가 아닌 외부 시스템도 동일)
   - 시그니처는 **도메인 타입**으로. 원시 타입/`ResultRow`/`Map` 금지
   - 단건은 non-null 반환, nullable이 필요하면 `OrNull` 접미사
2. **어댑터를 적절한 infrastructure 모듈에 구현**
   - RDB 영속화 → `*-db-core`
   - 캐시 → `*-redis-core`
   - 새 외부 시스템(예: 이메일) → 새 `infrastructure/support/*-email-sender` 모듈을 만들고 boot의 `build.gradle.kts`에 `runtimeOnly`로 추가
3. Service는 포트만 의존한다. 어댑터 구현체를 직접 import하지 않는다 (DIP)

### Step 4. 검증 로직이 여러 저장소를 조회한다면 → Validator로 분리

Service의 Command 메서드에서 `if (...) throw ...` 또는 조회-후-분기가 2개 이상 있으면 `{Domain}Validator`(`@Component`)로 분리한다. ([[code-implementation-rules]] §1-1 참조)

---

## 7. 신규 프로젝트 셋업 — 단계별

### Step 1. 루트 프로젝트 + Gradle 멀티 모듈 골격

`settings.gradle.kts`:
```
rootProject.name = "{project-name}"

include(
    "boot:{prefix}-boot-web",
    "domain:{prefix}-domain-core",
    "infrastructure:storage:{prefix}-db-core",
    "config:{prefix}-config-yaml-importer",
)
```

루트 `build.gradle.kts`에 `subprojects { ... }` 블록을 두고 Kotlin/Spring Boot 플러그인, Java 21, Kotlin 컴파일 옵션, 공통 의존(`spring-boot-starter`, `kotlin-stdlib-jdk8`)을 일괄 적용한다.

### Step 2. 빈 모듈 생성

각 모듈 디렉토리에 `build.gradle.kts`와 `src/main/kotlin`, `src/main/resources`를 만든다. 의존성은 §2의 패턴을 따른다.

### Step 3. 첫 도메인 추가 (수직 슬라이스 한 개)

처음부터 모든 레이어/모듈을 만들지 말고, **하나의 도메인을 끝까지 관통**해보고 시작한다.

예: `health` 또는 가장 단순한 도메인 하나를 골라
1. `domain/{prefix}-domain-core` 에 `{group}.domain.{domain}.domain.{Domain}` + `port.{Domain}Repository`
2. `domain/{prefix}-domain-core` 에 `application.{Domain}Service`
3. `infrastructure/storage/{prefix}-db-core` 에 `{Domain}Entity`/`Table`/`Dao`/`{Domain}CoreRepository`
4. `boot/{prefix}-boot-web` 에 `domain.{domain}.api.{Domain}Api` + Request/Response DTO

이 사이클을 한 번 돌리고 나면 이후 도메인은 같은 패턴을 복제하면 된다.

### Step 4. 어댑터 모듈은 **필요할 때** 추가

처음부터 SMS/JWT/Redis/PG를 다 세팅하지 않는다. 인증이 필요해진 시점에 `*-jwt-core`, SMS가 필요한 시점에 `*-sms-sender`를 새 모듈로 추가한다.

---

## 8. 안티 패턴

| 안티 패턴 | 왜 문제인가 | 올바른 방향 |
|-----------|-------------|-------------|
| Controller가 DAO/Entity import | boot이 infrastructure를 컴파일 타임에 봄. 모듈 경계 무너짐 | Service → Port → Repository |
| Service가 다른 Service 호출 | 책임 경계 모호, 트랜잭션/순환 의존 위험 | Port와 도메인 컴포넌트만 의존 |
| Repository 구현체에 if/분기 로직 | 비즈니스 로직이 어댑터 층에 새어 나옴 | DAO 호출 + `toDomain()`만 |
| `domain` 패키지에 `org.springframework.web.*` import | 도메인이 프레임워크에 종속됨 | DTO 변환은 boot/Controller 레벨에서 |
| Request DTO에서 도메인 VO 직접 생성 | VO 검증 실패가 boot 모듈에서 던져져 web 예외 핸들러가 도메인 의미를 모름 | Request/Command는 String, 도메인 모듈 안에서 VO 생성 |
| DDL에 FK 사용 | 운영 중 마이그레이션/샤딩/소프트 삭제 제약 | PK + 필요한 컬럼 INDEX만 |
| 새 외부 시스템을 boot 모듈 안에서 직접 호출 | 어댑터 격리 깨짐 | `infrastructure/support/*` 에 새 모듈 추가 |
| 새 도메인 폴더를 무분별하게 만듦 | 도메인 파편화, 응집도 하락 | 기존 도메인의 하위 컨텍스트로 표현 |
| 어댑터 모듈끼리 의존 | 어댑터 간 결합. 한쪽 교체 시 양쪽 영향 | 양쪽 모두 도메인 포트만 의존 |

---

## 9. 빠른 위치 결정 표 (Cheat Sheet)

| 클래스 한 줄 설명 | 모듈 | 패키지 |
|-------------------|------|--------|
| `@RestController` | `*-boot-web` | `domain.{domain}.api` |
| Request DTO | `*-boot-web` | `domain.{domain}.api.request` |
| Response DTO | `*-boot-web` | `domain.{domain}.api.response` |
| `GlobalExceptionHandler` 항목 | `*-boot-web` | `support.error` |
| Spring Batch JobConfig | `*-boot-batch` | `job.domain.{domain}` |
| `@Service` | `*-domain-core` | `domain.{domain}.application` |
| Command/Result | `*-domain-core` | `domain.{domain}.application.{command\|result}` |
| 애그리거트/엔티티 도메인 | `*-domain-core` | `domain.{domain}.domain` |
| Value Object | `*-domain-core` | `domain.{domain}.domain` (또는 하위 컨텍스트) |
| Validator (`@Component`) | `*-domain-core` | `domain.{domain}.domain` |
| Port 인터페이스 | `*-domain-core` | `domain.{domain}.domain.port` |
| 도메인 예외 | `*-domain-core` | `domain.{domain}.exception` |
| Repository 구현체 (RDB) | `*-db-core` | `domain.{domain}.repository` |
| DAO | `*-db-core` | `domain.{domain}.dao` |
| Entity / Table | `*-db-core` | `domain.{domain}.entity` / `domain.{domain}.entity.table` |
| Redis Repository/Dao | `*-redis-core` | `domain.{domain}.{repository\|dao\|entity}` |
| 외부 시스템 어댑터 (JWT/SMS/Crypto 등) | `infrastructure/support/*` | 자유 (관례적으로 `{group}.domain` 또는 `{group}.{tech}` 하위) |

---

## 부록 — 적용 예시 (이 레포)

이 레포(`meet-again-backend`)가 위 가이드의 실제 적용 예다. `{prefix}=ma`, `{group}=com.konkuk.ma` 로 치환한 결과:

- `boot/ma-boot-web`, `boot/ma-boot-batch`
- `domain/ma-domain-core`
- `infrastructure/storage/{ma-db-core, ma-redis-core}`
- `infrastructure/support/{ma-jwt-core, ma-crypto-core, ma-sms-sender, ma-file-storage, ma-id-obfuscator, ma-payment-core}`
- `config/{ma-config-yaml-importer, ma-config-logging}`

도메인 예: `member`, `auth`, `matching`, `point`, `community`, `xroom`, `support`. 도메인이 커진 사례로 `point/domain/{balance, payment, discount, history}` 하위 컨텍스트 분리, `member/domain/photo/` 하위 컨텍스트 분리가 있다.

새 프로젝트를 시작할 때는 이 디렉토리 구조를 그대로 베껴 `ma-` 부분만 새 `{prefix}`로 치환하고 시작하면 된다.
