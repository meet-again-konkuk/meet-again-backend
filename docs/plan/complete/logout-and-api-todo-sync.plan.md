# Plan: 로그아웃 API 구현 + api-todo 현행화

- 작성일: 2026-07-09
- 상태: Draft
- 작업 유형: 기능 개발 (+ 문서 현행화)
- 제안 브랜치: `feat/auth-logout` (base: `develop`)
- 워크플로: code-implementer → kotest-writer → rest-docs-generator → code-reviewer → (마지막) api-todo 현행화 → 전체 GREEN 후 push + PR

---

## 요구사항 요약

한 브랜치/PR로 다음 두 작업을 처리한다.

1. **로그아웃 API 구현** — `POST /api/auth/logout` (인증 필요). 해당 회원의 refresh token을 삭제해 세션 연장을 봉쇄한다. 응답은 **204 No Content**(본문 없음).
2. **api-todo 현행화 (문서만)** — 이미 develop에 머지됐지만 `docs/api-todo.md`에 누락된 커뮤니티 API 10건을 완료 테이블에 추가하고, 로그아웃 완료 시 "기타" 섹션의 로그아웃 항목을 완료(인증) 테이블로 이동한다.

### 스펙 (확정)

| 항목 | 결정 |
|------|------|
| 엔드포인트 | `POST /api/auth/logout` |
| 인증 | 필요 (기본 `authenticated`로 커버 — SecurityConfig 수정 없음) |
| 응답 | **204 No Content**, 본문 없음 (탈퇴/복구 204와 정렬) |
| 요청/응답 DTO | 없음 |
| 동작 | 회원의 refresh token 삭제 → 세션 연장 봉쇄 |
| access token 처리 | stateless JWT라 TTL(1h)까지 잔존 수용 (탈퇴 로그인 차단과 동일 정책) |
| 멱등성 | 토큰 없는 상태로 재호출해도 204 (삭제가 이미 no-op) |

---

## 영향 범위 분석

### 변경 대상 파일

| # | 파일 경로 | 변경 내용 | 변경 수준 |
|---|-----------|----------|----------|
| 1 | `domain/ma-domain-core/.../domain/auth/application/LogoutService.kt` | 로그아웃 유스케이스 오케스트레이션 Service | 신규 |
| 2 | `boot/ma-boot-web/.../domain/auth/api/LogoutApi.kt` | 로그아웃 컨트롤러 | 신규 |
| 3 | `boot/ma-boot-web/.../integration/LogoutIntegrationTest.kt` | API→DB E2E 통합 테스트 | 신규 |
| 4 | `boot/ma-boot-web/.../domain/auth/api/LogoutApiTest.kt` | REST Docs 문서화 테스트 | 신규 |
| 5 | `boot/ma-boot-web/src/docs/asciidoc/auth/logout.adoc` | 로그아웃 스니펫 include 문서 | 신규 |
| 6 | `boot/ma-boot-web/src/docs/asciidoc/main.adoc` | 인증 섹션에 로그아웃 링크 추가 | 수정 |
| 7 | `docs/api-todo.md` | 커뮤니티 10건 추가 + 로그아웃 완료 이동 | 수정 |

> 패키지 루트는 `com.konkuk.ma.domain` (모듈명은 `ma-domain-core`지만 패키지엔 `.domain.auth...`로 들어감). 신규 클래스 패키지: 도메인 = `com.konkuk.ma.domain.auth.application`, 웹 = `com.konkuk.ma.domain.auth.api`.

### 재사용 (신규 포트/DAO/DDL/SecurityConfig 변경 없음)

- **포트 `RefreshTokenRepository.delete(memberId: Long)` 이미 존재** — `domain/.../auth/domain/port/RefreshTokenRepository.kt`. 탈퇴 정리에서 이미 사용 중. 신규 포트 불필요.
- **삭제는 이미 멱등** — `RefreshTokenCoreRepository.delete` → `RefreshTokenDao.delete`가 Exposed `deleteWhere`로 0행이어도 예외 없이 삭제. 재로그아웃도 204. DAO/Repository **무변경**.
- **저장소 = MariaDB `RefreshTokenTable`** (Exposed) → E2E에서 `transaction{}`으로 행 삭제 직접 검증 가능.
- **인증 주입 선례** — `@LoginMember memberInfo: MemberInfo` → `memberInfo.id` (`community/api/BlockApi.kt`).
- **컨트롤러 분리 선례** — auth Api는 관심사별 클래스(LoginApi / RefreshTokenApi / SignUpApi 각각 `@RestController @RequestMapping("/api/auth")`). 로그아웃도 별도 `LogoutApi`.
- **SecurityConfig 무변경** — permitAll 목록 뒤 `.anyRequest().authenticated()`로 끝나므로 `/api/auth/logout`은 기본 authenticated로 커버. **permitAll에 절대 추가하지 말 것.**

### 의존성 관계

```
LogoutApi (web, /api/auth)
   └─ 의존: LogoutService
            └─ 의존: RefreshTokenRepository (기존 포트, delete)
                     └─ 구현: RefreshTokenCoreRepository → RefreshTokenDao (기존, 멱등 delete)
```

`@LoginMember` ArgumentResolver가 인증 필터(JWT) 통과 후 `MemberInfo`를 주입 → `memberInfo.id`를 Service로 전달. 아래 방향 의존만 존재하며 헥사고날 의존 방향(web → domain application → port) 준수.

---

## 구현 계획

### Step 1: LogoutService (domain application) — code-implementer

- 목표: refresh token 삭제 위임만 수행하는 오케스트레이션 Service.
- 위치: `domain/ma-domain-core/.../domain/auth/application/LogoutService.kt`, 패키지 `com.konkuk.ma.domain.auth.application`.
- 시그니처:
  - 클래스: `@Service @Transactional class LogoutService(private val refreshTokenRepository: RefreshTokenRepository)`
  - 메서드: `fun logout(memberId: Long)` → 본문은 `refreshTokenRepository.delete(memberId)` 위임 한 줄.
- 규칙 준수: 분기/조회-후-판단 없음(순수 위임) → code-implementation-rules §1 통과. `@Transactional`은 Service 경계에만(§1-2). RefreshTokenService와 동일한 `@Service @Transactional` 배치.

### Step 2: LogoutApi (web) — code-implementer

- 목표: 인증된 회원의 로그아웃 요청을 받아 Service 호출, 204 반환.
- 위치: `boot/ma-boot-web/.../domain/auth/api/LogoutApi.kt`, 패키지 `com.konkuk.ma.domain.auth.api`.
- 시그니처:
  - 클래스: `@RestController @RequestMapping("/api/auth") class LogoutApi(private val logoutService: LogoutService)`
  - 메서드: `@PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT) fun logout(@LoginMember memberInfo: MemberInfo)` → 본문은 `logoutService.logout(memberInfo.id)` 한 줄. 반환 타입 없음(Unit), 응답 DTO 없음.
- 규칙 준수: Api는 Service만 의존(§10). `BlockApi.unblock`의 `@ResponseStatus(HttpStatus.NO_CONTENT)` + `@LoginMember` 패턴과 동일.

### Step 3: E2E 통합 테스트 — kotest-writer

- 목표: API→DB 경로로 로그아웃 동작·멱등성·인증을 검증.
- 위치: `boot/ma-boot-web/.../integration/LogoutIntegrationTest.kt` (`@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")`, FunSpec — `LoginIntegrationTest` 셋업 답습).
- 셋업: `LoginIntegrationTest`와 동일하게 `beforeSpec`에서 `SchemaUtils.create(MemberTable, RefreshTokenTable)`, `afterEach` deleteAll, `afterSpec` drop. `insertMember` DB 헬퍼 재사용, 로그인 요청 본문은 inline `mapOf`.
- 테스트 케이스:
  1. 로그인으로 accessToken 발급 + `RefreshTokenTable` 행 존재 확인 → `POST /api/auth/logout`(Bearer accessToken) → **204** & `transaction{}`으로 해당 회원 refresh token 행 **삭제 확인**.
  2. 로그아웃 후 그 refresh token으로 `POST /api/auth/refresh-token` 재발급 시도 → **404 Not Found** (findOne이 삭제된 행에 `EntityNotFoundException` → GlobalExceptionHandler가 404 매핑. 확인된 현행 정책 상태코드).
  3. 토큰 없이 `POST /api/auth/logout` → **401 Unauthorized**.
  4. 로그아웃 후 같은 accessToken으로 재로그아웃 → **204** (멱등 — 삭제 no-op).
- 주의: **Service 빈 직접주입 중간레벨 통합테스트 금지** — 반드시 MockMvc로 API→DB E2E.

### Step 4: REST Docs 문서화 — rest-docs-generator

- 목표: 204 응답 스니펫 생성 + main.adoc 연결.
- 위치: `boot/ma-boot-web/.../domain/auth/api/LogoutApiTest.kt` (`BaseApiTest` 확장 — 기존 `LoginApiTest`/`RefreshTokenApiTest` 패턴, `@WithAuthMember` 중복 선언 금지: BaseApiTest 기본값 `holeman@naver.com` 사용).
- 문서화 범위: 요청은 `Authorization` 헤더만(requestHeaders), 요청 본문·응답 본문 없음(204). auth Vocabulary 신규 필드 **불필요**.
- 스니펫 문서: `boot/ma-boot-web/src/docs/asciidoc/auth/logout.adoc` 신규 — `login.adoc`/`refresh-token.adoc`와 동일한 include 구조.
- `main.adoc` 인증 섹션(라인 61~64 `[[auth-refresh]]` 뒤)에 `[[auth-logout]]` 앵커 + `* link:auth/logout.html[로그아웃 API,window=_blank]` 추가.

### Step 5: api-todo 현행화 (문서만) — 마지막 단계

`docs/api-todo.md` 수정 (동일 PR):

1. **"완료된 API > 커뮤니티" 테이블(라인 47~58)에 누락 10행 추가** — 이미 develop 머지분:

   | Method | Endpoint | 용도 |
   |--------|----------|------|
   | PATCH | /api/community/posts/{postId} | 게시글 수정 (REQ-012) |
   | DELETE | /api/community/posts/{postId} | 게시글 삭제 (REQ-012) |
   | POST | /api/community/posts/{postId}/image | 게시글 이미지 업로드/교체 (REQ-013, multipart) |
   | DELETE | /api/community/posts/{postId}/image | 게시글 이미지 삭제 (REQ-013) |
   | POST | /api/community/posts/{postId}/reports | 게시글 신고 (REQ-014) |
   | POST | /api/community/comments/{commentId}/reports | 댓글 신고 (REQ-014) |
   | POST | /api/community/posts/{postId}/author/block | 게시글 작성자 차단 (REQ-014) |
   | POST | /api/community/comments/{commentId}/author/block | 댓글 작성자 차단 (REQ-014) |
   | GET | /api/community/blocks | 차단 목록 조회 (REQ-014) |
   | DELETE | /api/community/blocks/{blockId} | 차단 해제 (REQ-014) |

   > 경로 최종 확인처: `boot/.../community/api/{PostCommandApi, PostImageApi, ReportApi, BlockApi}.kt`.

2. **"완료된 API > 인증" 테이블(라인 9~15)에 로그아웃 행 추가**: `| POST | /api/auth/logout | 로그아웃 (refresh token 삭제, 204) |`.

3. **"## 기타" 섹션(라인 165~175)의 로그아웃 항목 제거** — 프로젝트 컨벤션대로 도메인 헤더(`## 기타`)는 남기고 하위 내용을 `작업할 내용 없음`으로 표기. (기타 섹션에 로그아웃 외 다른 항목이 없으면 섹션 전체를 "작업할 내용 없음"으로 정리)

---

## 구현 순서 (의존성 순)

| # | 단계 | 파일 | 담당 | 변경 유형 |
|---|------|------|------|----------|
| 1 | LogoutService | `domain/.../auth/application/LogoutService.kt` | code-implementer | 신규 |
| 2 | LogoutApi | `boot/.../auth/api/LogoutApi.kt` | code-implementer | 신규 |
| 3 | E2E 통합 테스트 | `boot/.../integration/LogoutIntegrationTest.kt` | kotest-writer | 신규 |
| 4 | REST Docs 테스트 | `boot/.../auth/api/LogoutApiTest.kt` | rest-docs-generator | 신규 |
| 5 | 스니펫 + main.adoc | `.../asciidoc/auth/logout.adoc`, `main.adoc` | rest-docs-generator | 신규/수정 |
| 6 | 컨벤션 검증 | (변경분 전체) | code-reviewer | 검토 |
| 7 | api-todo 현행화 | `docs/api-todo.md` | (직접) | 수정 |

---

## 리스크 및 주의사항

- **SecurityConfig permitAll 오추가 주의**: `/api/auth/logout`은 인증 필요 엔드포인트다. login/refresh/sign-up 같은 public 엔드포인트와 달리 permitAll 목록에 **넣으면 안 된다**(넣으면 `@LoginMember` 주입 대상 인증이 사라져 401 케이스가 깨진다). 기본 `.anyRequest().authenticated()`로 이미 커버되므로 SecurityConfig는 손대지 않는다.
- **멱등성 근거는 DAO 동작에 의존**: 재로그아웃 204는 `deleteWhere`가 0행에도 예외를 안 던지는 현행 동작 덕분. DAO/Repository는 이번 PR에서 변경하지 않아 이 전제가 유지된다.
- **"로그아웃 후 refresh" 상태코드는 404로 고정 검증**: refresh token JWT 자체는 서명상 유효하므로 `getMemberIdFromToken`은 통과하고, 그 다음 `findOne`이 삭제된 행에서 `EntityNotFoundException`을 던져 404가 된다. 401이 아니라 404임에 주의(현행 GlobalExceptionHandler 매핑 확인 완료).
- **access token 잔존은 의도된 정책**: 로그아웃 후에도 발급된 access token은 TTL(1h)까지 유효. 즉시 무효화(블랙리스트)는 이번 스코프 밖 — 탈퇴 로그인 차단과 동일하게 수용.
- **Service에 분기 유입 금지**: LogoutService는 위임 한 줄만. "행이 있으면/없으면" 류 분기를 넣지 않는다(멱등은 DAO가 이미 처리).

---

## 테스트 전략

| 검증 항목 | 방법 |
|-----------|------|
| 로그아웃 성공 + 행 삭제 | E2E: 204 & `transaction{}`으로 `RefreshTokenTable` 행 부재 확인 |
| 세션 연장 봉쇄 | E2E: 로그아웃 후 `/refresh-token` → 404 |
| 인증 강제 | E2E: 토큰 없이 호출 → 401 |
| 멱등성 | E2E: 재로그아웃 → 204 |
| API 문서 | REST Docs: 204 스니펫 + Authorization 헤더 문서화, main.adoc 링크 |

- 검증 명령: `./gradlew :boot:ma-boot-web:test :domain:ma-domain-core:test`
- 전체 GREEN 확인 후 push + `gh pr create` (base: develop).

---

## 구현 시 참조 (규칙 본문은 복제하지 않음)

- 객체/서비스 구현 규칙: [[code-implementation-rules]] (§1 Service 조합만, §1-2 트랜잭션 경계, §10 Api 규칙)
- 모듈·패키지 배치: [[clean-architecture]]
- 가독성/네이밍/함수: [[clean-code]]
- 테스트 작성: [[kotest-writing]] (API→DB E2E, DB 헬퍼, inline 요청 본문)
- API 문서화: [[rest-docs-writing]] (Vocabulary 재활용, main.adoc 연결)
