# Plan: 비밀번호 재설정 API (POST /api/auth/find-password)

- 작성일: 2026-07-12
- 작업 유형: 기능 개발
- 브랜치(제안): `feat/auth-find-password`
- 상태: Confirmed (요구사항 사용자 확정 완료, 2026-07-12)

## 요구사항 요약

- 인증 불필요(public) 엔드포인트. SMS 인증(휴대폰 점유 증명) 선행 후, `email`+`name`+`phone` 3필드가 모두 일치하는 회원의 비밀번호를 **새 비밀번호로 직접 설정**한다. (단일 엔드포인트 — 재설정 토큰/링크 발송 없음)
- Request Body: `{ "email": "string", "name": "string", "phone": "string", "newPassword": "string" }`
- 성공 응답: **204 No Content** (응답 본문 없음)
- 검증 규칙:
  1. SMS 인증(confirmed) 선행 필수 — 기존 `SmsVerificationValidator`(auth/domain) 재사용. 미인증 시 `SmsNotVerifiedException` → 400. (find-email·sign-up 선례)
  2. `email`+`name`+`phone` 3필드 모두 일치하는 활성 회원이어야 재설정 허용. 불일치/부재 시 `EntityNotFoundException` → 404 (find-email 선례 그대로).
  3. 탈퇴 유예 중(deleted=false, `withdrawalRequestedAt`≠null) 회원도 허용 — find-email과 동일. 익명화(deleted=true)는 `activeRows` 필터로 자동 제외.
- 새 비밀번호 암호화: `PasswordEncryptor` 포트(`ma-crypto-core`) 재사용 (sign-up 선례).

## 설계 결정 (확정)

| # | 쟁점 | 조사 결과 / 근거 | 결정 |
|---|------|-----------------|------|
| D1 | 3필드(email+name+phone) 매칭 방식 | find-email은 `MemberQueryRepository.findOne(name, PhoneNumber)` 오버로드를 쓰고, 없으면 Repository 구현체가 `EntityNotFoundException` 던짐(→404). Service는 분기 없이 위임만. | **포트에 `findOne(email, name, phoneNumber)` 오버로드 추가.** DAO는 3조건 `activeRows` 쿼리, Repository 구현체가 null이면 `EntityNotFoundException(MEMBER, ...)`. Service 분기 0개 유지. |
| D2 | 새 비밀번호 영속화 방식 | 상태 변경 선례: `member.requestWithdrawal()`(도메인 행위, 불변조건 검증) → `memberCommandRepository.requestWithdrawal(memberId, requestedAt)`(id+컬럼 타겟 update). password 재설정은 **강제할 불변조건이 없다**(유예 회원도 허용). 암호화는 sign-up처럼 Service의 포트 호출 1건. | **`MemberCommandRepository.updatePassword(memberId, encodedPassword)` 포트 + DAO 신설.** `requestWithdrawal(memberId, …)`와 동일한 id+단일컬럼 update 패턴. Member에 신규 도메인 메서드는 두지 않음(검증할 상태 전이 없음 → 도메인 메서드는 공허한 세터가 됨). Service는 `passwordEncryptor.encode(newPassword)` 인라인 포트 호출 후 위임(sign-up 선례). |
| D3 | 검증 순서 계약 | find-email E2E가 확정한 순서를 그대로 계승. | **① Bean Validation(400) → ② SMS 인증(400) → ③ 회원 3필드 조회(404) → 암호화 → update → 204.** SMS 게이트가 회원 조회보다 먼저(②<③): 회원 부재여도 미인증이면 400. |
| D4 | 예외 → 상태코드 매핑 | `GlobalExceptionHandler`에 이미 전부 존재. | **신규 예외 클래스 0개.** `SmsNotVerifiedException`→400, `EntityNotFoundException`→404, `MethodArgumentNotValidException`→400 모두 기존 핸들러 재사용. |
| D5 | 응답 형식 | 성공은 204(본문 없음) → 이메일 노출·마스킹 이슈 없음(find-email의 D2 마스킹 고민 불필요). | **Response DTO 불필요.** 컨트롤러는 `@ResponseStatus(HttpStatus.NO_CONTENT)` + Unit 반환 (logout 선례). |
| D6 | newPassword 형식 검증 | sign-up과 동일 정책(영문/숫자 8~16). `ValidationPatterns.PASSWORD` / `ValidationMessages.PASSWORD_*` 상수 존재. | **Request DTO에서 `@Pattern(PASSWORD)`+`@NotBlank(PASSWORD_REQUIRED)`로 검증.** 도메인은 이미 암호화된 값만 저장(재검증 없음). |
| D7 | 클래스 네이밍 (구현 후 사용자 결정으로 개정) | 초안은 URL 미러링 `FindPassword*`/기존 `FindEmail*`. 사용자 피드백: SignUp·Login 같은 굳어진 용어가 아닌 `Find*` 조어를 클래스명에 쓰지 말 것 — 특히 find-password는 실제 행위가 "재설정"이라 이름과 모순. | **클래스는 행위·업계 용어 기반으로 전면 리네임**: `PasswordReset*`(Service/Api/Request/테스트), 기존 find-email도 `EmailRecovery*`로 동반 리네임. URL(`/api/auth/find-password`·`/find-email`)과 REST Docs 스니펫 식별자는 프론트 계약이라 유지(화면 용어 "찾기"는 HTTP 경계까지만). |

## 영향 범위 분석

### phone / email 저장 형식 (핵심 제약)

- `MemberTable.phoneNumber`는 **하이픈 없는 `fullNumber`로 저장**(`MemberCommandDao.save`). 신규 DAO 쿼리도 `PhoneNumber(phone).fullNumber`로 정규화해 매칭한다(Request 원문 하이픈 유무 무관).
- 3필드 조회는 반드시 `MemberQueryDao`의 `activeRows { }`(deleted=false 자동 필터) 위에서 작성 — 익명화 회원 자동 제외, 유예 회원 포함이 공짜로 성립(find-email와 동일).

### 아키텍처 흐름

```
[POST /api/auth/find-password]  (permitAll)
        │  PasswordResetRequest { email, name, phone, newPassword }  (raw String, VO 생성 없음)
        ▼
PasswordResetApi ─────────────────────────────────  boot/ma-boot-web
        │  resetPassword(email, name, phone, newPassword)
        ▼
PasswordResetService (@Service @Transactional) ────  domain/ma-domain-core (application)
        │  ① PhoneNumber(phone) / Email(email) VO 생성
        │  ② smsVerificationValidator.validate(phoneNumber)      → 미인증 시 400
        │  ③ memberQueryRepository.findOne(email, name, phone)   → 부재 시 404
        │  ④ passwordEncryptor.encode(newPassword)               (포트 호출, 인라인)
        │  ⑤ memberCommandRepository.updatePassword(id, encoded)
        ▼
   ┌───────────────┬──────────────────────┬─────────────────────┐
   ▼               ▼                      ▼                     ▼
SmsVerification  MemberQueryRepository  PasswordEncryptor    MemberCommandRepository
Validator        (port, 오버로드 추가)   (port, ma-crypto)    (port, updatePassword 추가)
   │               │                                            │
   ▼               ▼(impl)                                      ▼(impl)
SmsRepository   MemberQueryCoreRepository                   MemberCommandCoreRepository
(Redis)          → MemberQueryDao.findOne(email,name,phone)  → MemberCommandDao.updatePassword
                    (activeRows 3조건)                          (MemberTable.update by id)
```

### 변경 대상 파일

| # | 파일 경로 | 변경 내용 | 수준 |
|---|-----------|----------|------|
| 1 | `domain/.../member/domain/port/MemberQueryRepository.kt` | `findOne(email: Email, name: String, phoneNumber: PhoneNumber): Member` 오버로드 선언 | 수정 |
| 2 | `infrastructure/.../member/dao/MemberQueryDao.kt` | `findOne(email: String, name: String, phoneNumber: String): MemberEntity?` — `activeRows`에 email/name/phone 3조건 + `limit(1).firstOrNull()` | 수정 |
| 3 | `infrastructure/.../member/repository/MemberQueryCoreRepository.kt` | 오버로드 구현 — null이면 `EntityNotFoundException(EntityType.MEMBER, ...)` | 수정 |
| 4 | `domain/.../member/domain/port/MemberCommandRepository.kt` | `updatePassword(memberId: Long, encodedPassword: String)` 선언 | 수정 |
| 5 | `infrastructure/.../member/dao/MemberCommandDao.kt` | `updatePassword(memberId, encodedPassword)` — `MemberTable.update({ id eq memberId }) { password, lastModifiedBy }` | 수정 |
| 6 | `infrastructure/.../member/repository/MemberCommandCoreRepository.kt` | `updatePassword` 위임 구현 | 수정 |
| 7 | `domain/.../auth/application/PasswordResetService.kt` | 신규 Service (`@Service @Transactional`) — 위 흐름 ①~⑤ 오케스트레이션 | 신규 |
| 8 | `boot/.../auth/api/request/PasswordResetRequest.kt` | 신규 Request DTO — email/name/phone/newPassword + Bean Validation | 신규 |
| 9 | `boot/.../auth/api/PasswordResetApi.kt` | 신규 컨트롤러 — `POST /find-password`, 204, Service 위임 | 신규 |
| 10 | `boot/.../config/SecurityConfig.kt` | `POST /api/auth/find-password` permitAll 등록 | 수정 |
| 11 | `docs/api-todo.md` | 해당 항목을 "완료된 API > 인증"으로 이동, Request Body에 newPassword 반영 | 수정 |

### 시그니처 요약 (구현 계약)

- 포트: `MemberQueryRepository.findOne(email: Email, name: String, phoneNumber: PhoneNumber): Member`
- 포트: `MemberCommandRepository.updatePassword(memberId: Long, encodedPassword: String)`
- Service: `PasswordResetService.resetPassword(email: String, name: String, phone: String, newPassword: String)`
- 컨트롤러: `resetPassword(@Valid @RequestBody request: PasswordResetRequest)` → 204 (Unit 반환 + `@ResponseStatus(NO_CONTENT)`)
- Request DTO 검증: `email` `@NotBlank(EMAIL_REQUIRED)`+`@Email(EMAIL_INVALID)`(WithdrawalCancelRequest 선례) / `name` `@NotBlank(NAME_REQUIRED)`+`@Pattern(NAME)` / `phone` `@NotBlank(PHONE_NUMBER_REQUIRED)`+`@Pattern(PHONE_NUMBER)` / `newPassword` `@NotBlank(PASSWORD_REQUIRED)`+`@Pattern(PASSWORD, PASSWORD_INVALID)`

## 구현 계획 (의존성 순서)

### Step 1: 포트 확장 (domain)
- `MemberQueryRepository`에 3필드 `findOne` 오버로드, `MemberCommandRepository`에 `updatePassword` 선언.
- 변경 파일: 위 표 #1, #4.

### Step 2: 어댑터 구현 (infrastructure)
- `MemberQueryDao.findOne(email, name, phoneNumber)` — `activeRows { (email eq) and (name eq) and (phoneNumber eq) }.limit(1).firstOrNull()?.let { RowEntityMapper.toMemberEntity(it) }`.
- `MemberQueryCoreRepository`에서 `?: throw EntityNotFoundException(...)`.
- `MemberCommandDao.updatePassword` + `MemberCommandCoreRepository.updatePassword` (DAO는 `requestWithdrawal`와 동일한 id 기반 단일컬럼 update).
- 변경 파일: #2, #3, #5, #6.

### Step 3: 애플리케이션 서비스 (domain)
- `PasswordResetService` 신규. flat 위임만(분기·private 헬퍼 금지): VO 생성 → validate → findOne → encode → updatePassword.
- 변경 파일: #7.

### Step 4: 웹 계층 (boot)
- `PasswordResetRequest`(raw String만, VO 생성 금지) → `PasswordResetApi`(Service만 의존, 204) → `SecurityConfig` permitAll.
- 변경 파일: #8, #9, #10.

### Step 5: 문서/큐 정리
- `docs/api-todo.md` 항목 이동 + Request Body newPassword 반영.
- 변경 파일: #11.

## 리스크 및 주의사항

- **권한 게이트는 SMS 인증뿐**: SMS 인증을 통과(휴대폰 점유 증명)하고 email+name을 아는 주체면 비밀번호를 바꿀 수 있다. 이는 의도된 설계(휴대폰 점유=본인확인). 단 SMS confirmed 상태의 TTL(Redis 10분)과 재사용 가능성은 기존 정책을 그대로 따른다 — 이번 범위에서 변경 없음.
- **동일 name+phone 복수 매칭**: `MemberTable.phoneNumber`는 non-unique index. 가입 시 `existsByPhoneNumber`(deleted=false, PR #18)로 활성 회원 phone 중복이 앱 레벨 차단되어 사실상 단건이나, DB 유니크 미보장이므로 `limit(1)`로 단건 취급(find-email D5와 동일 리스크·동일 방어).
- **email 3필드 포함의 효과**: find-email 대비 email까지 일치해야 하므로 매칭 대상이 더 좁아진다. 반대로 email이 틀리면(오타 등) 404 — 사용자에게는 "일치 회원 없음"으로만 노출(어느 필드가 틀렸는지 미노출, enumeration 완화).
- **트랜잭션 경계**: `PasswordResetService`는 쓰기(updatePassword) 포함 → 클래스 단위 `@Transactional`(find-email·sign-up과 동일 선언 방식). 느린 외부호출 없음 → `TransactionTemplate` 불필요.
- **DDL 무변경**: `MemberTable.password` 컬럼 기존재. 스키마 변경·FK 없음.

## 테스트 전략 (/tdd 파이프라인에서 별도 진행 — 시나리오 목록만)

- **API 슬라이스 테스트** (`PasswordResetApiTest`, `@WebMvcTest` + `@MockkBean PasswordResetService`, REST Docs 스냅샷):
  - 정상: 유효 요청 → 204 (`auth-find-password`).
  - SMS 미인증: Service가 `SmsNotVerifiedException` → 400 (`auth-find-password-sms-not-verified`).
  - Bean Validation 실패: 이름 형식/전화번호 하이픈/newPassword 형식(짧음·문자만·숫자만) → 400.
- **E2E 통합 테스트** (`PasswordResetIntegrationTest`, `@SpringBootTest` + `@AutoConfigureMockMvc`, 실제 DB+embedded Redis, `SmsRepository.confirmVerificationCode`로 인증 셋업 — find-email 통합테스트 그대로 미러링):
  - 정상: SMS 인증 + 3필드 일치 → 204, 이후 `MemberTable.password`를 `transaction{}`으로 직접 읽어 `PasswordEncryptor.matches(newPassword, stored)` == true (실제 재설정 검증). 또는 신규 비밀번호로 `/api/auth/login` 200 확인.
  - 유예 회원(withdrawalRequestedAt≠null) → 204 + 비밀번호 갱신.
  - 검증 순서: 미인증이면 회원 존재해도 400 / 회원 부재해도 400(②<③).
  - 3필드 불일치(email만 틀림 / name만 틀림 / phone만 틀림) → 404.
  - 익명화(deleted=true) 회원의 원래 email/name/phone → 404(activeRows 제외).
- **주의(선례 반영)**: Service 빈 직접 주입 통합테스트 금지 — API→DB E2E로만. DB 상태는 `transaction{}` 테이블 직접 읽기. `@WithAuthMember` 중복 금지(BaseApiTest 포함, permitAll이라 무관).

## REST Docs (rest-docs-generator가 수행 — 계획에만 명시)

- 스니펫 식별자 `auth-find-password`(+ 실패 케이스). `requestBody(email(), name(), phoneNumber("phone"), <newPassword 필드>)`. `newPassword`는 `CommonVocabulary`의 `password()` 재사용 또는 `newPassword` 필드명 지정. 성공 응답은 204(본문 없음)이므로 `responseBody` 없음.
- `main.adoc`에 find-email 항목 인접에 링크 추가.

## 구현 시 참조 (규칙 본문은 복제하지 않음)

- 객체/서비스 구현 규칙: [[code-implementation-rules]] (§1 Service 분기 금지, §6 포트 규칙, §11 메서드 네이밍)
- 모듈·패키지 배치: [[clean-architecture]]
- 가독성/네이밍: [[clean-code]]
- 테스트 작성: [[kotest-writing]]
- API 문서화: [[rest-docs-writing]]
