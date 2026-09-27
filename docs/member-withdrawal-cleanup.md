# 회원 탈퇴 익명화 배치 — 삭제 vs 익명화 기준

유예 만료 회원 익명화 배치(`memberWithdrawalCompleteJob`)가 도메인별로 **무엇을 삭제하고 무엇을 익명화(보존)하는지**와 그 판단 기준을 정리한다.

> 코드: `boot/ma-boot-batch/.../job/domain/member/` 의 `*CleanupItemWriter`, `MemberAnonymizeItemWriter`

---

## 한 줄 기준

- **삭제(delete)** — *탈퇴자 본인만의 것.* 보존해도 의미 없거나 보존하면 안 되는 데이터 (본인 소유 자원 · 본인 행위 흔적).
- **익명화(update/anonymize)** — *남이 계속 보거나, 기록으로 남겨야 하는 것.* 행(row)은 보존하고 **탈퇴자의 식별정보만** `withdrawn_{id}@deleted.local`로 치환.

> 핵심 질문: **"이 데이터가 탈퇴자만의 것인가, 아니면 다른 사용자에게 보이거나 남겨야 할 기록인가?"**
> 전자면 삭제, 후자면 익명화 보존.

---

## 도메인별 처리

배치 step 실행 순서대로 (마지막 `memberAnonymize`가 회원 본체 익명화).

| # | Step | 대상 데이터 | 처리 | 방식 | 이유 |
|---|------|-----------|------|------|------|
| 1 | auth | refresh token | **삭제** | hard | 본인 전용 휘발성 인증 토큰, 보존 의미 없음 |
| 2 | matching | 내가 등록한 타겟정보(`TargetInfo`, register=본인) | **삭제** | soft | 본인이 등록한 "찾는 상대" 정보 |
| 2 | matching | 내 매칭 결과(`MatchingResult`, **register**=본인) | **삭제** | soft | 내 매칭 목록 = 본인 것 |
| 2 | matching | 상대가 보는 매칭 결과(`MatchingResult`, **target**=본인) | **익명화** | update | ⚠️ 상대방 매칭 목록에 계속 떠야 함 → `targetEmail`만 익명 치환 |
| 3 | point | 포인트 잔액(`MemberPoint`, owner=본인) | **삭제** | soft | 본인 소유 잔액 |
| 3 | point | 포인트 이력(`PointHistory`) | **익명화** | update | 정산/이력 기록 보존 → `ownerEmail` 익명 치환 |
| 4 | community | 게시글(`Post`) | **익명화** | update | 다른 사용자에게 계속 보여야 함 → 작성자 익명 치환 |
| 4 | community | 댓글(`Comment`) | **익명화** | update | 동일 — 작성자 익명 치환 |
| 4 | community | 내가 누른 게시글/댓글 좋아요(`PostLike`/`CommentLike`) | **삭제** | hard | 본인 행위 흔적 |
| 5 | support | 문의(`Inquiry`) | **익명화** | update | CS/문의 이력 보존 → 작성자 익명 치환 |
| 6 | xroom | 내 xroom(owner=본인) | **삭제** | soft | 본인 전용 방 (단방향, 상대 영향 없음) |
| 7 | memberPhoto | 프로필 사진 (스토리지 파일 + 레코드) | **삭제** | 파일 삭제 + soft | 개인정보, 물리·논리 모두 제거 |
| 8 | member | 회원 본체(`Member`) | **익명화 + soft delete** | update | PII 익명화 후 논리 삭제 (아래 참조) |

### 회원 본체 익명화(`Member.anonymize` + `anonymizeAndSoftDelete`)

| 필드 | 익명화 후 값 |
|------|-------------|
| email | `withdrawn_{id}@deleted.local` |
| password / nickname / name / phoneNumber / birthDate / region | `WithdrawnSentinel.*` (고정 sentinel) |
| highSchool / university / profileImageUrl | `null` |
| deleted | `true` (soft delete) |

---

## 익명화 식별자 규칙

- 익명 이메일: `Email.withdrawn(memberId)` → **`withdrawn_{id}@deleted.local`** (`Email.WITHDRAWN_FORMAT`).
- 모든 도메인의 `anonymizeAuthor` / `anonymizeOwner` / `anonymizeTarget` 가 탈퇴자의 원본 이메일을 이 값으로 치환 → 원본 식별정보 미보존, 익명화된 회원 레코드와 식별자 일치.

---

## ⚠️ 매칭 결과 특이사항 (가장 헷갈리는 부분)

`MATCHING_RESULTS` 행은 **register(보는 사람)** 와 **target(상대)** 두 회원을 가진다. 그래서 탈퇴 시 한 행을 두 사람이 공유한다.

- **register=본인** 행 → 내 매칭 목록 → **삭제**(`deleteByRegister`).
- **target=본인** 행 → 상대방의 매칭 목록 → **삭제하면 상대가 보던 결과가 사라짐** → 삭제 X, `targetEmail`만 익명화(`anonymizeTarget`).

> 과거에는 `deleteByMember`가 `register OR target` 양방향을 전부 soft delete해서 **상대방 매칭 결과까지 사라지는 문제**가 있었고, 위와 같이 분리(`deleteByRegister` + `anonymizeTarget`)로 수정했다.

> 참고: xroom도 두 사람 관계처럼 보이지만 `ownerEmail` 단일 소유라 단방향 삭제로 충분(상대 영향 없음). 양방향 공유 행은 **매칭 결과뿐**.

### 프론트 계약 (탈퇴 타겟 표시)

`GET /api/matching-results` 응답에서 타겟이 탈퇴 회원인 행은:

- `isWithdrawn = true` (`= targetMemberId == null`)
- `targetMemberId / targetName / targetNickname / profileImageUrl = null`
- 매칭 항목 자체(`matchingResultId`, `remainingDays`, `matchRate`, `claimed` 등)는 그대로 → 목록에서 사라지지 않음

→ 프론트는 `isWithdrawn === true`면 "탈퇴한 회원" placeholder로 렌더. (서버는 플래그+null만 제공, 표기는 프론트 처리. `docs/frontend-discussion.md` 2번 참조)

---

## 삭제 방식 요약 (soft vs hard)

- **hard delete** (`deleteWhere`, 물리 삭제): refresh token, 좋아요(Post/Comment Like).
- **soft delete** (`deleted=true`): 타겟정보, 매칭 결과(register), 포인트 잔액, xroom, 사진 레코드, 회원 본체.
- **익명화(update)**: 게시글/댓글, 매칭 결과(target), 포인트 이력, 문의, 회원 본체 필드.
- **스토리지 파일 삭제**: 프로필 사진은 레코드 soft delete + 실제 파일(`deleteFiles`) 제거.
