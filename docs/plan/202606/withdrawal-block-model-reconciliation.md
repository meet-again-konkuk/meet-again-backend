# Plan: 회원 탈퇴 "차단(block) 모델" 정합화 — (b)안

- 작성일: 2026-06-06
- 작업 유형: 버그 수정 + 리팩토링 (도메인 정합성 확보)
- 브랜치: `feat/member-withdrawal-1-domain` (PR #17 amend)

## ✅ 구현 완료 (2026-06-06) — 최종안과 plan의 차이

논의 결과 **인터셉터 모델(매 요청 DB 조회)은 과하다**고 판단해 폐기하고, **토큰 발급 지점(login·refresh)에서만 차단**하는 최소안으로 확정·구현함. 구현된 내용:
- LoginService·RefreshTokenService: `member.isWithdrawalRequested()`면 `WithdrawalPendingLoginException`(409) — **추가 DB 쿼리 0** (member 이미 로드됨). `isWithdrawalPending(now)`가 아니라 `isWithdrawalRequested()`로 pending+만료 모두 차단.
- cancel: `POST /api/members/withdrawal/cancellation`(public) + email/password + 비번검증 + 재활성화 + **204**.
- 삭제: `WithdrawalCancelResult`, `WithdrawalCancelResponse`, cancel의 `AuthTokenIssuer`/LoginInfo.
- **D-1 변경**: grace-window 응답 바디 포함은 **드롭**(복잡도↓). 409 + `WITHDRAWAL_PENDING` 코드만. 예외도 email만 보유(window 미포함). 필요 시 후속 확장.
- **access token 윈도우(최대 1시간) 수용** — 인터셉터 없이. refresh도 차단해 세션 연장 봉쇄.
- 검증: domain+web 전체 테스트 통과, asciidoctor 통과.

> 아래 본문 Step은 최초 plan(인터셉터 없는 (b)안 + grace-window 포함) 기준이라 D-1/인터셉터 부분은 위 "구현 완료"가 최종이다.

## 요구사항 요약

탈퇴 신청(grace 기간) 회원이 로그인/일반 API에 접근하지 못하도록 "차단(block) 모델"을 정합화한다.

핵심 결정은 **(b)안**으로 확정됨: `cancelWithdrawal`은 **재활성화만** 수행하고 토큰을 발급하지 않는다. 사용자는 cancel 성공(204) 후 **별도로 login을 호출**해 토큰을 얻는다.

현재 문제(코드 확인 완료):
1. **차단 미구현** — `LoginService.login()`이 `member.isWithdrawalPending(now)`를 확인하지 않아 탈퇴 신청 회원이 그대로 로그인되고 모든 API 사용 가능.
2. **cancel이 인증 엔드포인트** — `MemberWithdrawalApi.cancelWithdrawal`이 `@AuthenticationPrincipal`로 묶여 있음. 차단을 구현하면(로그인 자체가 막히므로 토큰 발급 불가) 도달 불가능. public + email/password 입력 방식이어야 함.
3. **cancel이 비밀번호 미검증** — 재진입(재활성화) 동작인데 자격 검증 없이 토큰(LoginInfo)을 발급 중.

> `api-todo.md` 확인 결과: "기타 > 회원 탈퇴" 항목에 "비밀번호 확인 추가 필요할 수 있음" 메모만 존재. cancel 엔드포인트의 Request/Response Body는 정의돼 있지 않음 → 본 계획에서 RESTful 규칙(code-implementation-rules §14)으로 메서드/경로/바디를 결정함(결정사항 D-3 참고).

## 현행 동작 vs (b)안 목표 동작

| 구분 | 현행 | (b)안 목표 |
|------|------|-----------|
| 탈퇴 신청 회원 로그인 | 성공(차단 없음) | 거부 + grace-window 정보 제공 |
| cancel 엔드포인트 | `DELETE /api/members/me/withdrawal` (JWT 필요) | `POST /api/members/withdrawal/cancellation` (public) |
| cancel 인증 | `@AuthenticationPrincipal`(JWT 필요) | public(permitAll) + email/password 바디 |
| cancel 비번 검증 | 없음 | `passwordVerifier.verify` 수행 |
| cancel 응답 | 200 + email/nickname/cancelledAt/access/refresh | **204 No Content**, 바디 없음 |
| cancel 후 토큰 획득 | cancel 응답에 포함 | cancel(204) 후 사용자가 `/api/auth/login` 별도 호출 |

## 영향 범위 분석

### 변경 대상 파일

| 파일 경로 | 변경 내용 | 변경 수준 |
|-----------|----------|----------|
| `domain/.../member/exception/WithdrawalPendingLoginException.kt` | 로그인 차단 예외 신설(grace-window 보유) | 신규 |
| `domain/.../auth/application/LoginService.kt` | 로그인 시 grace-pending 차단 분기 추가 | 수정 |
| `domain/.../auth/application/command/WithdrawalCancelCommand.kt` | cancel용 Command(email+password) 신설 | 신규 |
| `domain/.../auth/application/WithdrawalCancelService.kt` | `AuthTokenIssuer`/LoginInfo 제거, 비번 검증 추가, 반환 `Unit` | 수정 |
| `domain/.../auth/application/result/WithdrawalCancelResult.kt` | 더 이상 불필요 | 삭제 |
| `boot/.../member/api/MemberWithdrawalApi.kt` | cancel을 `POST .../cancellation` public화·바디 입력·204 반환, 경로 `/me` 제거 | 수정 |
| `boot/.../member/api/request/WithdrawalCancelRequest.kt` | cancel 요청 DTO(email+password) 신설 | 신규 |
| `boot/.../member/api/response/WithdrawalCancelResponse.kt` | 204라 응답 클래스 불필요 | 삭제 |
| `boot/.../config/SecurityConfig.kt` | `POST /api/members/withdrawal/cancellation` permitAll 등록 | 수정 |
| `boot/.../support/error/GlobalExceptionHandler.kt` | 신설 차단 예외 핸들러 매핑 | 수정 |
| `boot/.../support/payload/response/ErrorCode.kt` | 차단용 ErrorCode 추가(D-1 채택 시) | 수정(조건부) |
| `boot/.../docs/asciidoc/member/member-withdrawal.adoc` | cancel 요청 바디/204 응답으로 문서 갱신 | 수정 |

### 의존성 관계 / 호출 흐름

```
[로그인 차단 경로]
LoginApi.login
  └─ LoginService.login(LoginCommand)
       ├─ memberQueryRepository.findOne(email)
       ├─ passwordVerifier.verify(password, member)
       ├─ member.isWithdrawalPending(now) ──► true ──► throw WithdrawalPendingLoginException(graceWindow)
       │                                                    └─ GlobalExceptionHandler → HTTP(409 or 403, D-1)
       └─ (active) tokenManager.generateAccessToken / refreshTokenGenerator.generate → LoginInfo

[탈퇴 복구 경로 — (b)안]
POST /api/members/withdrawal/cancellation                       ← permitAll, 인증 불필요
MemberWithdrawalApi.cancelWithdrawal(WithdrawalCancelRequest)
  └─ WithdrawalCancelService.cancel(WithdrawalCancelCommand): Unit
       ├─ memberQueryRepository.findOne(email)
       ├─ passwordVerifier.verify(password, member)
       ├─ member.cancelWithdrawal(now)         (만료 시 WithdrawalExpiredException → 410)
       └─ memberCommandRepository.cancelWithdrawal(member.email)
  → 204 No Content (바디 없음)

[이후 사용자 흐름]
cancel(204) → 클라이언트가 /api/auth/login 별도 호출 → 정상 LoginInfo 발급
```

도메인 모델(`Member`)에 필요한 행위는 **이미 존재**: `isWithdrawalRequested()`, `isWithdrawalPending(now)`, `isWithdrawalExpired(now)`, `withdrawalGraceWindowOrNull(now)`, `cancelWithdrawal(now)`. → 도메인 모델 변경 없음. 스키마 변경 없음(FK 미사용 제약과 무관).

## 결정사항 (확정)

### D-1. 로그인 차단 응답 → **409 + grace-window 포함** ✅
- 신설 예외 `WithdrawalPendingLoginException`에 `WithdrawalGraceWindow`(requestedAt/expiresAt)를 담는다.
- `ErrorCode.WITHDRAWAL_PENDING`(HTTP **409 Conflict**) 신설. 만료는 이미 `410 GONE`(`WITHDRAWAL_EXPIRED`)을 쓰므로 pending은 409로 구분.
- `GlobalExceptionHandler`가 window(requestedAt/expiresAt)를 **응답 바디에 포함**해 내려준다 → 프론트가 "만료까지 N일, 복구하시겠어요?" UX 구성. `ApiError`에 부가 데이터를 싣는 방식(아래 Step 10에서 구체화).

### D-2. `WithdrawalCancelResult` / `WithdrawalCancelResponse` → **둘 다 삭제** ✅
- cancel이 204·무반환(Unit)이라 두 클래스 모두 존재 이유 소멸. 삭제 후 잔존 import 정리.

### D-3. cancel 엔드포인트 → **`POST /api/members/withdrawal/cancellation` (public)** ✅
- 상태 전이 + 요청 바디(email/password) 동반이라 DELETE+바디(비권장)를 피하고 **명사형 하위 리소스 + POST**로 표현(§14 부합).
- 신청은 `POST /api/members/withdrawal`(인증 필요) 유지. 경로에서 `me` 제거(인증 컨텍스트 없는 public 취소와 경로 통일).

> 아래 Step은 위 확정안(D-1=409+window, D-2=둘 다 삭제, D-3=POST `/api/members/withdrawal/cancellation`) 기준.

## 구현 계획

### Step 1: (도메인) 로그인 차단 예외 신설

- 목표: grace-pending 회원 로그인 거부를 표현하는 도메인 예외 추가.
- 작업 내용:
  - `WithdrawalPendingLoginException(email: Email, graceWindow: WithdrawalGraceWindow)` 신설. `BusinessException` 상속, `logLevel = WARN`, 메시지 "탈퇴 신청 상태의 회원입니다. 복구 후 로그인할 수 있습니다." 류.
  - `graceWindow`를 프로퍼티로 보유(핸들러에서 requestedAt/expiresAt 노출용).
  - 위치: `member/exception/`(기존 탈퇴 예외들과 동일 패키지 — `NotWithdrawalRequestedException`·`WithdrawalExpiredException` 옆).
- 변경 파일: `domain/.../member/exception/WithdrawalPendingLoginException.kt` (신규)

### Step 2: (도메인/application) LoginService에 차단 분기 추가

- 목표: 비번 검증 통과 후, grace 기간 내 탈퇴 신청 회원이면 토큰 발급 전에 차단.
- 작업 내용:
  - `login()`에서 `passwordVerifier.verify(...)` 직후 `val now = LocalDateTime.now()` 기준으로 `member.isWithdrawalPending(now)`가 true면 `member.withdrawalGraceWindowOrNull(now)`를 받아 `WithdrawalPendingLoginException`을 throw.
  - 차단 판단은 도메인 객체(`Member`)에 이미 위임돼 있으므로 Service는 분기·throw만 수행(규칙 §1/§2 준수).
  - 만료(`isWithdrawalExpired`) 케이스는 로그인 차단 대상이 아님(만료 = 탈퇴 확정 영역). (b)안 범위에서는 만료 회원의 로그인 처리(예: 별도 안내/완전탈퇴)는 변경하지 않음 — 현행 동작 유지하되, 필요 시 후속 과제로 분리(리스크 항목 참고).
  - 시그니처 변화 없음: `fun login(loginCommand: LoginCommand): LoginInfo`.
- 변경 파일: `domain/.../auth/application/LoginService.kt`

### Step 3: (도메인/application) cancel Command 신설

- 목표: cancel이 email+password를 받도록 Command 도입(기존 `WithdrawalRequestCommand` 패턴 동일).
- 작업 내용:
  - `WithdrawalCancelCommand(email: String, val password: String)` 신설, 내부에서 `val email: Email = Email(email)`로 변환(Request DTO에서 직접 `Email()` 금지 — 규칙/메모리 준수, `LoginCommand`·`WithdrawalRequestCommand`와 동일 형태).
- 변경 파일: `domain/.../auth/application/command/WithdrawalCancelCommand.kt` (신규)

### Step 4: (도메인/application) WithdrawalCancelService 축소

- 목표: 토큰 발급 제거, 비번 검증 추가, 반환 `Unit`.
- 작업 내용:
  - 생성자에서 `authTokenIssuer: AuthTokenIssuer` 제거. `passwordVerifier: PasswordVerifier` 주입 추가.
  - 시그니처 변경: `fun cancel(command: WithdrawalCancelCommand)` → 반환 `Unit`.
  - 본문 흐름: `findOne(command.email)` → `passwordVerifier.verify(command.password, member)` → `member.cancelWithdrawal(now)` → `memberCommandRepository.cancelWithdrawal(member.email)`. LoginInfo/AuthTokens 생성 로직 전부 제거.
  - `cancelWithdrawal`의 `WithdrawalExpiredException`(410)·`NotWithdrawalRequestedException` 흐름은 그대로 유지(만료 시 복구 불가).
  - import 정리: `AuthTokenIssuer`, `LoginInfo`, `WithdrawalCancelResult` import 제거.
- 변경 파일: `domain/.../auth/application/WithdrawalCancelService.kt`

### Step 5: (도메인/application) WithdrawalCancelResult 삭제

- 목표: 더 이상 참조되지 않는 Result 제거(D-2).
- 작업 내용:
  - `WithdrawalCancelResult.kt` 삭제. (참조처: Step 4에서 제거 완료, Step 7에서 Response 삭제, 테스트는 Step 9에서 정리.)
- 변경 파일: `domain/.../auth/application/result/WithdrawalCancelResult.kt` (삭제)

### Step 6: (web) cancel 요청 DTO 신설

- 목표: public cancel이 email+password 바디를 받도록 DTO 추가.
- 작업 내용:
  - `WithdrawalCancelRequest(email: String, password: String)` 신설.
    - `email`: `@field:NotBlank`(EMAIL_REQUIRED) + `@field:Email`(EMAIL_INVALID) — `LoginRequest`와 동일 검증.
    - `password`: `@field:NotBlank`(PASSWORD_REQUIRED).
    - `fun toCommand() = WithdrawalCancelCommand(email, password)` — DTO는 String만 넘기고 VO 변환은 Command 내부에서.
  - `ValidationMessages` 상수 사용(하드코딩 금지, 규칙 §11).
- 변경 파일: `boot/.../member/api/request/WithdrawalCancelRequest.kt` (신규)

### Step 7: (web) WithdrawalCancelResponse 삭제

- 목표: 204 무반환이므로 응답 클래스 제거(D-2).
- 작업 내용:
  - `WithdrawalCancelResponse.kt` 삭제. 참조처는 `MemberWithdrawalApi`(Step 8)와 API 테스트(Step 9)뿐.
- 변경 파일: `boot/.../member/api/response/WithdrawalCancelResponse.kt` (삭제)

### Step 8: (web) MemberWithdrawalApi — cancel을 POST cancellation·public·204로

- 목표: cancel을 `POST .../cancellation`·인증 해제·바디 입력·204로 전환.
- 작업 내용:
  - `@RequestMapping("/api/members/me/withdrawal")` → `@RequestMapping("/api/members/withdrawal")`로 변경(D-3). 경로에서 `me` 제거에 따라 신청 경로도 `POST /api/members/withdrawal`로 통일됨. 신청은 인증 컨텍스트가 필요하므로 `@AuthenticationPrincipal email` 유지(SecurityConfig의 `anyRequest().authenticated()`로 계속 보호).
  - `cancelWithdrawal` 시그니처: `@PostMapping("/cancellation")` + `@ResponseStatus(HttpStatus.NO_CONTENT)`, 파라미터를 `@Valid @RequestBody request: WithdrawalCancelRequest`로 교체, `@AuthenticationPrincipal` 제거, 반환 `Unit`.
  - 본문: `withdrawalCancelService.cancel(request.toCommand())` 한 줄(규칙 §12: Api는 파싱→Service 호출→응답만).
  - import 정리: `WithdrawalCancelResponse`, `AuthenticationPrincipal`, `DeleteMapping` 제거 / `PostMapping` 추가.
- 변경 파일: `boot/.../member/api/MemberWithdrawalApi.kt`

### Step 9: (web/security) SecurityConfig permitAll 등록

- 목표: cancel 엔드포인트를 public으로 개방. 신청(POST)은 인증 유지.
- 작업 내용:
  - `authorizeHttpRequests`에 `.requestMatchers(HttpMethod.POST, "/api/members/withdrawal/cancellation").permitAll()` 추가.
  - 신청 `POST /api/members/withdrawal`은 permitAll에 **넣지 않음** → `anyRequest().authenticated()`로 인증 강제 유지. (신청과 취소가 둘 다 POST이므로 permitAll 매처는 `/cancellation` 하위 경로로 정확히 한정할 것 — 신청 경로가 실수로 열리지 않도록 주의.)
- 변경 파일: `boot/.../config/SecurityConfig.kt`

### Step 10: (web/error) 차단 예외 핸들링 + ErrorCode

- 목표: `WithdrawalPendingLoginException`을 409 + grace-window 응답으로 매핑(D-1 확정).
- 작업 내용:
  - `ErrorCode`에 `WITHDRAWAL_PENDING(409, "Withdrawal request is pending")` 추가(409 영역).
  - `GlobalExceptionHandler`에 `@ExceptionHandler(WithdrawalPendingLoginException::class)` 추가 → 409 응답. 예외의 `graceWindow`(requestedAt/expiresAt)를 응답 바디에 포함.
    - 노출 방법: 기존 `ApiError` 구조를 먼저 확인 후, 부가 데이터를 nullable 필드로 추가하거나(기존 응답 무영향) 차단 전용 응답 클래스를 핸들러에서 반환. 둘 중 `ApiError` 영향 범위가 작은 쪽 선택.
  - 만료 핸들러(`WithdrawalExpiredException`→410)는 기존 유지.
- 변경 파일: `boot/.../support/payload/response/ErrorCode.kt`, `boot/.../support/error/GlobalExceptionHandler.kt`

### Step 11: 테스트 동기화 (구현 안정 후 작성 — 범위만 명시)

> 컨벤션상 구현이 안정된 뒤 작성. 아래는 깨지는 테스트와 추가 대상 목록.

**깨지는(수정 필요) 테스트**
- `WithdrawalCancelServiceTest` — 생성자 시그니처 변경(authTokenIssuer 제거, passwordVerifier 추가), `cancel(String)`→`cancel(WithdrawalCancelCommand)`, 반환 Unit, LoginInfo/AuthTokens·`WithdrawalCancelResult` 참조 제거. "토큰 발급" 검증 → "비번 검증 + 재활성화(`memberCommandRepository.cancelWithdrawal`) 호출" 검증으로 교체.
- `MemberWithdrawalApiTest` — "회원 탈퇴 복구 API 문서화" 테스트가 `deleteJson`(바디 없음)·200·responseBody(토큰들) 기반 → `postJson`(요청 바디 email+password)·204·responseBody 제거로 재작성. cancel 경로를 `POST /api/members/withdrawal/cancellation`로 갱신. `LoginInfo`/`RefreshToken`/`WithdrawalCancelResult` import 제거. 신청 테스트 경로도 `POST /api/members/withdrawal`로 갱신.

**추가 대상 테스트**
- `LoginServiceTest` — "grace 기간 내 탈퇴 신청 회원 로그인 시 `WithdrawalPendingLoginException` 발생" 케이스 추가(member.requestWithdrawal 후 isWithdrawalPending=true 상황). 만료 회원은 차단 예외가 발생하지 않음을 확인하는 케이스도 선택적 추가.
- `WithdrawalCancelServiceTest` — "비밀번호 불일치 시 PasswordMismatchException", "만료 후 cancel 시 WithdrawalExpiredException", "정상 cancel 시 재활성화" 케이스.
- (선택) `MemberWithdrawalApiTest` — cancel 요청 email/password 누락 시 400 검증.
- (선택) Security 통합 테스트 — cancel `POST /api/members/withdrawal/cancellation`이 토큰 없이 통과(permitAll), 신청 `POST /api/members/withdrawal`은 401. 단 `BaseApiTest`는 `addFilters=false`라 `@WebMvcTest`에서는 permitAll이 직접 검증되지 않음 → 통합 테스트(`integration/`)나 `JwtAuthenticationFilterSecurityTest` 계열에서 확인 필요. (둘 다 POST이므로 매처 경로 한정이 정확한지 검증.)

### Step 12: REST Docs 문서 갱신

- 목표: cancel 요청 바디/204 응답 반영.
- 작업 내용:
  - `member-withdrawal.adoc`의 "회원 탈퇴 복구 API" 섹션:
    - 설명에서 "복구 시 새 access/refresh 토큰을 발급합니다." → "복구(재활성화)만 수행하며 토큰을 발급하지 않습니다. 복구 후 로그인 API로 토큰을 발급받습니다." 로 교체.
    - HTTP 메서드/경로를 `POST /api/members/withdrawal/cancellation`으로 갱신.
    - 요청에 **요청 필드 섹션 추가**(email/password) — `member-withdrawal-cancel/request-fields.adoc` include.
    - 응답을 204로 변경, **응답 필드 섹션 제거**(`response-fields.adoc` include 삭제).
  - 스니펫 키 `member-withdrawal-cancel`는 Step 11 API 테스트의 `andDocument` 재작성으로 새로 생성됨(requestBody(email, password), 응답 바디 없음).
  - `main.adoc`은 이미 `member-withdrawal` 링크 보유 → 추가 wiring 불필요.
- 변경 파일: `boot/.../docs/asciidoc/member/member-withdrawal.adoc` (+ 테스트가 생성하는 스니펫)

## 삭제 대상 정리

| 대상 | 사유 | 잔재 정리 |
|------|------|----------|
| `WithdrawalCancelResult.kt` | (b)안에서 cancel 무반환 | Service/Response/테스트의 참조 제거 |
| `WithdrawalCancelResponse.kt` | 204 무반환 | Api/테스트의 참조 제거 |
| `WithdrawalCancelService`의 `AuthTokenIssuer` 의존 | 토큰 발급 폐지 | 생성자·import·테스트 mock 제거 |
| cancel의 `LoginInfo` 생성 코드 | 토큰 발급 폐지 | Service 본문에서 제거 |

## 구현 순서 요약 (의존성 순)

1. (도메인) WithdrawalPendingLoginException 신설
2. (application) LoginService 차단 분기
3. (application) WithdrawalCancelCommand 신설
4. (application) WithdrawalCancelService 축소(토큰 제거·비번검증·Unit)
5. (application) WithdrawalCancelResult 삭제
6. (web) WithdrawalCancelRequest 신설
7. (web) WithdrawalCancelResponse 삭제
8. (web) MemberWithdrawalApi cancel public화·204·경로 변경
9. (security) SecurityConfig permitAll 등록
10. (error) ErrorCode + GlobalExceptionHandler 매핑
11. 테스트 동기화/추가
12. REST Docs 갱신

## 리스크 및 주의사항

- **만료 회원 로그인 처리 미정의**: (b)안은 grace-pending 차단만 다룬다. 만료(`isWithdrawalExpired`) 회원이 로그인 시도 시 현행은 그대로 로그인 성공한다. "만료 = 탈퇴 확정"이라는 모델이라면 로그인도 막아야 하나, 본 작업 범위 밖. → **후속 과제로 명시**(만료 회원 로그인/완전탈퇴 처리). 본 변경에서는 동작 변경 없음.
- **신청/취소가 둘 다 POST**: `POST /api/members/withdrawal`(신청, 인증)과 `POST /api/members/withdrawal/cancellation`(취소, public)가 같은 prefix·같은 메서드. SecurityConfig permitAll 매처를 `/cancellation` 하위로 **정확히 한정**해야 신청 경로가 실수로 열리지 않음. 통합 테스트로 검증.
- **`@WebMvcTest`에서 permitAll 미검증**: `BaseApiTest`가 `addFilters=false`라 SecurityConfig 매처가 API 테스트에서 적용되지 않음. permitAll 정합성은 통합 테스트로 확인해야 함(테스트 Step 참고).
- **grace-window 노출 vs ApiError 구조(D-1 확정)**: 409 응답 바디에 window(requestedAt/expiresAt)를 포함하려면 `ApiError`에 부가 데이터를 싣거나 전용 응답이 필요. 기존 `ApiError` 스키마 영향 범위(다른 핸들러/문서)를 확인하고, 부가필드는 nullable로 추가해 기존 응답에 영향 없도록 할 것.
- **경로 변경(`/me` 제거)의 클라이언트 영향**: 신청/취소 경로가 `/api/members/me/withdrawal` → `/api/members/withdrawal`(신청)·`/api/members/withdrawal/cancellation`(취소)로 바뀜. 프론트엔드 호출부 동기화 필요(현재 미배포 단계라 영향 적을 것으로 판단하나 확인 권장).
- **트랜잭션 경계**: `WithdrawalCancelService`/`LoginService` 모두 클래스 단위 `@Transactional` 유지. 비번 검증·도메인 호출·repository 쓰기가 한 트랜잭션 내(규칙 §1-2 준수). Repository에 `@Transactional` 추가 금지.

## 테스트 전략

- **도메인 차단 로직**: `LoginServiceTest`에서 grace-pending → `WithdrawalPendingLoginException`, active → 정상 LoginInfo, 만료 → (현행 유지) 차단 미발생을 단위 검증(Mockk).
- **cancel 자격 검증**: `WithdrawalCancelServiceTest`에서 비번 불일치(PasswordMismatch), 만료(WithdrawalExpired), 미신청(NotWithdrawalRequested), 정상 재활성화 검증. `authTokenIssuer` mock 제거, `passwordVerifier` mock 추가.
- **API 계약**: `MemberWithdrawalApiTest`에서 cancel 204·요청 바디 검증·문서 스니펫 재생성, 신청 204 유지.
- **권한 개방**: 통합/시큐리티 테스트에서 cancel DELETE permitAll, 신청 POST 인증 강제 확인.

## 구현 시 참조 (규칙 본문은 복제하지 않음)

- 객체/서비스 구현 규칙(도메인 행위 위임, Command VO 변환, 포트 규칙): [[code-implementation-rules]]
- 모듈·패키지 배치(도메인 예외/Command/Service 위치): [[clean-architecture]]
- 가독성/네이밍: [[clean-code]]
- 테스트 작성(KoTest/Mockk/Fixture): [[kotest-writing]]
- API 문서화(Vocabulary 재활용, snippet, main.adoc): [[rest-docs-writing]]
