# Plan: 이메일 찾기 API (POST /api/auth/find-email)

- 작성일: 2026-07-10
- 작업 유형: 기능 개발
- 브랜치(제안): `feat/auth-find-email`
- 상태: Confirmed (Q1~Q4 사용자 확정 완료, 2026-07-10)

## 요구사항 요약

- 인증 불필요(public) 엔드포인트로, 이름(name)과 전화번호(phone)를 받아 해당 회원의 가입 이메일을 조회한다.
- Request Body (api-todo.md 기준): `{ "name": "string", "phone": "string" }`
- Response: 조회된 회원의 이메일. **보안상 마스킹 반환 권장**(예: `hol***@naver.com`) — 아래 설계 결정 참조.
- 인증 불필요 → `SecurityConfig` permitAll 등록 필요 (선례: `/api/auth/login`, `/api/members/withdrawal/cancellation`).

## 설계 결정 (구현 전 확정 필요)

| # | 쟁점 | 조사 결과 | 권고안 | 확정 필요 |
|---|------|----------|--------|-----------|
| D1 | 조회 실패 응답 정책 (404 vs 200 빈 결과) | 프로젝트 관례는 `EntityNotFoundException` → **404** (GlobalExceptionHandler L28~32). `/api/members/email/exists`가 이미 이메일 존재 여부를 public 노출 중이라 enumeration은 앱 전반에서 부분적으로 이미 가능. | **404 유지**(관례 일관성). 단 아래 트레이드오프 기록. | Q1 |
| D2 | 이메일 노출 범위 (전체 vs 마스킹) | find-email는 name+phone → email을 알려주므로 full email harvesting 리스크가 email-exists보다 큼. | **마스킹 반환**(`Email.masked()` 신설, 도메인 VO 행위로). | Q2 |
| D3 | 탈퇴 유예 중(withdrawalRequestedAt≠null) 회원 처리 | `requestWithdrawal`은 `withdrawalRequestedAt`만 세팅하고 `deleted=false` 유지. 배치가 7일 후 `anonymizeAndSoftDelete`로 `deleted=true` 전환. 유예 회원은 로그인은 막히나 탈퇴 복구(email+password)에는 이메일이 필요. | **포함**(활성 회원 취급) — 복구 흐름을 위해 이메일 회수 허용. | Q3 |
| D4 | 익명화(soft delete)된 탈퇴 회원 | `activeRows { }` 헬퍼가 `deleted eq false` 자동 필터. 익명화 시 phone은 sentinel(`WithdrawnSentinel.PHONE_NUMBER`)로 치환되어 실제 phone과 매칭 불가. | **자동 제외**(추가 작업 불필요, 신규 DAO 쿼리를 `activeRows`로 작성). | 확정 |
| D5 | 동일 name+phone 복수 매칭 가능성 | `MEMBERS.PHONE_NUMBER`는 `.index()`(non-unique). 그러나 가입 시 `existsByPhoneNumber`(deleted=false 한정, PR #18)로 앱 레벨 중복 차단 → **활성 회원 중 phone은 사실상 유일**. name은 유일성 없음. | `.limit(1)` + `firstOrNull`로 단건 취급(안전). DB 유니크 미보장은 리스크 섹션에 기록. | 확정 |

## 영향 범위 분석

### phone 저장 형식 (핵심 제약)

- `MemberTable.phoneNumber = varchar("PHONE_NUMBER", 255).index()` — **하이픈 없는 `fullNumber`로 저장** (`MemberCommandDao.save`: `it[phoneNumber] = newMember.phoneNumber.fullNumber`).
- 따라서 조회 시 요청 raw phone을 `PhoneNumber(phone).fullNumber`로 정규화한 값으로 매칭해야 함. Request 원문(하이픈 유무)에 관계없이 정규화됨.

### 변경 대상 파일

| # | 파일 경로 | 변경 내용 | 변경 수준 |
|---|-----------|----------|----------|
| 1 | `domain/…/common/domain/Email.kt` | `fun masked(): String` 추가 (VO 행위) | 수정 |
| 2 | `domain/…/member/domain/port/MemberQueryRepository.kt` | `findOne(name: String, phoneNumber: PhoneNumber): Member` 오버로드 추가 | 수정 |
| 3 | `domain/…/auth/application/FindEmailService.kt` | `findEmail(name, phone)` 서비스 신설 | 신규 |
| 4 | `infrastructure/…/member/dao/MemberQueryDao.kt` | `findByNameAndPhoneNumber(name, phoneNumber): MemberEntity?` 추가 (`activeRows` 사용) | 수정 |
| 5 | `infrastructure/…/member/repository/MemberQueryCoreRepository.kt` | 신규 포트 메서드 구현 (미존재 시 `EntityNotFoundException`) | 수정 |
| 6 | `boot/…/auth/api/request/FindEmailRequest.kt` | 요청 DTO (name, phone + bean validation) | 신규 |
| 7 | `boot/…/auth/api/response/FindEmailResponse.kt` | 응답 DTO (마스킹된 email) | 신규 |
| 8 | `boot/…/auth/api/FindEmailApi.kt` | 컨트롤러 `POST /api/auth/find-email` | 신규 |
| 9 | `boot/…/config/SecurityConfig.kt` | `POST /api/auth/find-email` permitAll 등록 | 수정 |
| 10 | `boot/…/auth/api/FindEmailApiTest.kt` | API→DB E2E 테스트 | 신규 |
| 11 | `boot/…/docs/asciidoc/auth/find-email.adoc` + `main.adoc` + vocabulary | REST Docs (rest-docs-generator 산출) | 신규/수정 |
| 12 | `docs/api-todo.md` | find-email 항목을 "완료된 API > 인증"으로 이동 | 수정 |

### 의존성 관계 (레이어 호출 흐름)

```
[web] FindEmailApi  ──(name, phone: raw String)──►  [domain] FindEmailService
   │  @Valid FindEmailRequest                              │
   │  permitAll (SecurityConfig)                         │  PhoneNumber(phone)  ← VO 정규화/검증
   ▼                                                     ▼
FindEmailResponse(email.masked())            MemberQueryRepository.findOne(name, PhoneNumber)  [port]
   ▲  Email.masked()  ← 신규 VO 행위                     │
   │                                                     ▼
   └─────────────── Email(VO) ◄──── Member ◄──── [infra] MemberQueryCoreRepository (adapter)
                                                          │  없으면 EntityNotFoundException → 404
                                                          ▼
                                          MemberQueryDao.findByNameAndPhoneNumber(name, fullNumber)
                                                          │  activeRows { } → deleted=false 자동 필터
                                                          ▼
                                                    MEMBERS 테이블 (name eq ? AND phone eq ? , limit 1)
```

- 도메인 레이어(1~3)는 Spring/infra 의존 없음. 3은 `@Service`(스프링) 이나 기존 auth application 서비스와 동일 관례(`LoginService`, `WithdrawalCancelService`)를 따름.
- 신규 도메인 도입 없음 — 기존 auth/member 도메인의 하위로 처리.

## 구현 계획 (메서드 시그니처)

### Step 1: 도메인 VO 행위 추가 — `Email.masked()`

- 목표: 이메일 마스킹 로직을 Email VO 내부 행위로 캡슐화(원시값 포장 원칙, 서비스/응답에 분산 금지).
- 시그니처: `fun masked(): String`
- 규칙(권고): local part(‘@’ 앞)의 앞 3글자 유지 + 나머지 `***` + `@도메인` 그대로. local part가 3글자 이하이면 앞 1글자만 유지하고 `***`.
  - 예: `holeman@naver.com` → `hol***@naver.com`, `ab@x.com` → `a***@x.com`
- 변경 파일: `Email.kt`. (마스킹 임계값·형식의 최종 확정은 Q2에 종속)

### Step 2: 포트 메서드 추가 — `MemberQueryRepository`

- 목표: name + phone으로 활성 회원 단건 조회(미존재 시 예외) 포트 정의.
- 시그니처: `fun findOne(name: String, phoneNumber: PhoneNumber): Member`
- 근거: 기존 `findOne(email)`, `findOne(id)`와 동일한 "단건 조회·미존재 시 throw" 관례에 정렬(오버로드). 파라미터로 `PhoneNumber` VO를 받아 저장 형식 변환(fullNumber)은 어댑터가 책임지게 함.
- 변경 파일: `MemberQueryRepository.kt`

### Step 3: 어댑터 구현 — DAO + CoreRepository

- DAO 시그니처: `fun findByNameAndPhoneNumber(name: String, phoneNumber: String): MemberEntity?`
  - 쿼리: `MemberTable.activeRows { (MemberTable.name eq name) and (MemberTable.phoneNumber eq phoneNumber) }.limit(1).firstOrNull()?.let { RowEntityMapper.toMemberEntity(it) }`
  - `activeRows`로 `deleted=false` 자동 필터(D4). `.limit(1)`로 복수 매칭 방어(D5).
- CoreRepository 구현: `override fun findOne(name: String, phoneNumber: PhoneNumber): Member`
  - `memberQueryDao.findByNameAndPhoneNumber(name, phoneNumber.fullNumber)?.toDomain() ?: throw EntityNotFoundException(EntityType.MEMBER, "$name/${phoneNumber.masked()}")`
  - 로그(dataMessage)에 **원본 phone 미기록** — `phoneNumber.masked()` 사용(PII 보호).
- 변경 파일: `MemberQueryDao.kt`, `MemberQueryCoreRepository.kt`

### Step 4: 도메인 서비스 — `FindEmailService`

- 목표: 유스케이스 오케스트레이션(입력 raw → VO 조립 → 포트 조회 → Email 반환).
- 시그니처: `fun findEmail(name: String, phone: String): Email`
  - 내부: `val member = memberQueryRepository.findOne(name, PhoneNumber(phone)); return member.email`
- 관례: `LoginService`/`WithdrawalCancelService`와 동일하게 raw String을 받아 서비스 내부에서 VO(`PhoneNumber`) 조립. `@Service @Transactional`(readOnly는 기존 관례상 미사용 — 기존 서비스도 클래스 레벨 `@Transactional`).
- 별도 Validator 불필요: 다단계 업무 검증(권한/상태 교차검증)이 없고, 형식 검증은 bean validation + `PhoneNumber` VO init, 존재 검증은 포트가 담당. (XroomValidator 같은 Validator는 repo조회+분기+위임이 있을 때만 도입)
- 변경 파일: `FindEmailService.kt` (신규)

### Step 5: 웹 레이어 — Request / Response / Controller

- `FindEmailRequest`(요청 DTO, raw 값만 보유 — 도메인 VO 직접 생성 금지):
  - `name: String` — `@field:NotBlank(NAME_REQUIRED)` + `@field:Pattern(ValidationPatterns.NAME, NAME_INVALID)`
  - `phone: String` — `@field:NotBlank(PHONE_NUMBER_REQUIRED)` + `@field:Pattern(ValidationPatterns.PHONE_NUMBER, PHONE_NUMBER_INVALID)`
  - 필드명은 스펙(api-todo)에 맞춰 `phone` 사용(회원가입 DTO의 `phoneNumber`와 명칭 다름 — 프론트 계약 우선).
- `FindEmailResponse`: `class FindEmailResponse(val email: String)` + `constructor(email: Email) : this(email = email.masked())`
  - `LoginResponse(loginInfo)` 패턴과 동일하게 도메인 타입(Email)을 받아 응답 필드로 매핑. 마스킹은 VO 행위 위임.
- `FindEmailApi`: `@RestController @RequestMapping("/api/auth")`, `@PostMapping("/find-email")`
  - `fun findEmail(@Valid @RequestBody request: FindEmailRequest): FindEmailResponse` → `FindEmailResponse(findEmailService.findEmail(request.name, request.phone))`
  - 컨트롤러 엔드포인트당 분리 관례 준수(LoginApi/LogoutApi처럼 단일 책임).
- 변경 파일: `FindEmailRequest.kt`, `FindEmailResponse.kt`, `FindEmailApi.kt` (모두 신규)

### Step 6: 보안 설정 — permitAll

- `SecurityConfig.filterChain`에 `.requestMatchers(HttpMethod.POST, "/api/auth/find-email").permitAll()` 추가(login/refresh-token/withdrawal-cancellation 인접에 배치).
- 변경 파일: `SecurityConfig.kt`

## 구현 순서 (TDD 파이프라인)

의존성상 하위(도메인 VO/포트) → 어댑터 → 서비스 → 웹 순. 테스트는 스켈레톤 후 RED → 구현 GREEN.

| 순서 | 단계 | 대상 | 담당(에이전트) |
|------|------|------|----------------|
| 1 | 스켈레톤 | Email.masked, 포트 시그니처, FindEmailService, Request/Response/Api, SecurityConfig permitAll(컴파일 통과용 최소 골격) | code-implementer |
| 2 | E2E 테스트(RED) | `FindEmailApiTest` — API→DB E2E, DB에 회원 insert 후 POST | kotest-writer |
| 3 | 구현(GREEN) | DAO 쿼리, CoreRepository, FindEmailService, Email.masked, Request/Response/Api 본구현 | code-implementer |
| 4 | 리뷰 | code-implementation-rules / clean-code / clean-architecture 준수 검증 | code-reviewer |
| 5 | REST Docs | `auth/find-email.adoc` + `main.adoc` 링크 + vocabulary(name/phone 필드) 생성 | rest-docs-generator |
| 6 | 문서 현행화 | `docs/api-todo.md`의 find-email를 완료 테이블로 이동 | (수동) |

## 테스트 전략

- **금지**: Service 빈 직접 호출 통합 테스트(`*ServiceIntegrationTest`), 서비스 모킹 기반 검증. → **API→DB E2E(MockMvc)로만** 검증.
- 테스트 클래스명: `FindEmailApiTest` (대상 컨트롤러명 + Test). 케이스는 하나의 스펙에 `context`/`test`로 묶음.
- 요청 본문은 inline `mapOf(...)`로 구성(요청 fixture 금지), 반복 회원 삽입은 `insertMember(...)` 류 DB 헬퍼로.
- DB 상태 검증이 필요하면 `transaction { }`로 테이블 직접 read.
- 검증 케이스:
  1. 정상: name+phone 일치 활성 회원 존재 → 200, 응답 email이 **마스킹 형식**과 일치.
  2. phone 하이픈 포함 입력(예 `010-1234-5678`)이 저장형식(fullNumber)과 매칭되는가 — @Pattern 통과 여부에 따라(Q4) 케이스 조정.
  3. 미존재(name/phone 불일치) → 404 (`ENTITY_NOT_FOUND`).
  4. 익명화(soft delete)된 회원의 원래 name/phone으로 조회 → 404(활성 필터로 제외됨) — 사전 조건: deleted=true + phone sentinel.
  5. (D3 확정 시) 탈퇴 유예 회원 → 200으로 이메일 반환되는지.
  6. 형식 오류: name 비어있음/비한글, phone 형식 위반 → 400 (`INVALID_INPUT_VALUE`).
- REST Docs 테스트는 rest-docs-generator가 정상/400 케이스 스니펫으로 생성(vocabulary에 `name`, `phone` 필드 정의 추가).

## 리스크 및 주의사항

- **Enumeration / PII 노출 (D1·D2)**: public 엔드포인트가 name+phone → email 매핑을 노출. 완화책 = 이메일 마스킹(D2), 로그에 원본 phone 미기록(Step 3), (선택) rate limiting은 이번 범위 밖(별도 과제로 기록). 최종 노출 정책은 Q1/Q2 확정에 종속.
- **phone 저장 형식 불일치 위험**: DB는 fullNumber 저장. 어댑터에서 반드시 `phoneNumber.fullNumber`로 조회해야 하며, 하이픈 포함 입력은 `PhoneNumber` VO가 정규화. 단 Request @Pattern이 `PHONE_NUMBER`(`^010\d{7,8}$`, 하이픈 불허)라 하이픈 입력은 400으로 사전 차단됨 → 프론트가 하이픈 없이 보내는지 확인 필요(Q4).
- **phone DB 유니크 미보장 (D5)**: 활성 회원 유일성은 앱 레벨 보장(가입 시 중복검사)일 뿐 DB 제약 아님. `.limit(1)`로 방어하되, 이론상 레이스로 활성 중복이 생기면 임의 1건 반환. FK/유니크 제약은 이 프로젝트 관례상 추가하지 않음(PK/INDEX만).
- **탈퇴 유예 회원 (D3)**: "포함"으로 확정 — activeRows 기본 동작 그대로, 추가 조건 불필요.

## 구현 시 참조 (규칙 본문 비복제)

- 객체/서비스 구현 규칙(VO 행위·서비스 위임·포트): [[code-implementation-rules]]
- 모듈·패키지 배치(도메인/인프라/웹): [[clean-architecture]]
- 가독성/네이밍/함수: [[clean-code]]
- 테스트 작성(E2E, Fixture, DB 헬퍼): [[kotest-writing]]
- API 문서화(vocabulary, snippet, main.adoc): [[rest-docs-writing]]

## 확정 사항 (2026-07-10 사용자 확정)

- **Q1 → 404**: 조회 실패 시 `EntityNotFoundException` → 404 (프로젝트 관례 일관성).
- **Q2 → 마스킹**: `Email.masked()` 신설, `hol***@naver.com` 형식 (local part 앞 3글자 유지, 3글자 이하면 1글자).
- **Q3 → 포함**: 탈퇴 유예 중(deleted=false) 회원도 이메일 반환 — 탈퇴 복구(email+password) 흐름 지원.
- **Q4 → 하이픈 없이만 허용**: 기존 `ValidationPatterns.PHONE_NUMBER`(`^010\d{7,8}$`) 재사용, 하이픈 입력은 400. 테스트 케이스 2(하이픈 입력)는 400 검증으로 확정.
- **Q5 → find-email 전면 리네임 (2026-07-12 추가 확정)**: 원 스펙의 `find-id`(아이디/비밀번호 찾기 관용 표현)를 URL·클래스 전부 `find-email`/`FindEmail*`로 변경. 근거 — 코드베이스 유비쿼터스 언어는 `email`이고 `id`는 member PK(Long)를 가리키는 데 이미 사용되어 혼동 유발. PR 미머지·프론트 미사용 시점이라 계약 파기 없음.
