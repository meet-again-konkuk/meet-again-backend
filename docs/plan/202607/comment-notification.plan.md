# Plan: 게시글 댓글 인앱 알림 (설정 + 알림 인박스)

- 작성일: 2026-07-17
- 작업 유형: 신규 기능 개발 (신규 도메인 1개 + 커뮤니티 도메인 확장 + 테이블 2개)
- 브랜치: `feat/community-comment-notification`
- 상태: ✅ 구현 완료 (2026-07-18)
- 범위: ① 게시글별 댓글 알림 on/off 토글(기본 on) ② 댓글/대댓글 작성 시 알림 레코드 생성 ③ 알림 목록 조회 + 읽음 처리 API. **푸시(FCM) 미포함 — 인앱 폴링 전제.**

## ✅ 구현 완료 — plan 대비 달라진 점

- **`CommentNotificationRegistrar` 배치**: plan §9의 `community/application/` → **`community/domain/`으로 이동** (리뷰 반영 — BlockRegistrar 선례, "Service에서 추출한 분기 컴포넌트는 domain/" 규칙).
- **설정 토글 분기 컴포넌트 추출**: plan §9 N12의 Service 내 분기(존재검증 + enabled 라우팅)가 §1(Service 분기 금지) 위반으로 지적됨 → **`CommentNotificationSettingSwitcher`(community/domain/, PostLiker 선례) 신설**, Service는 위임 1줄. opt-out 멱등 판단(기존 DAO에 있던 것)도 Switcher로 이동, DAO는 순수 insert.
- **`Notifications` 일급 컬렉션 추가** (plan 목록 외): QueryService의 닉네임 조합을 도메인 행위(`withActor`/`combineWithActors`)로 응집.
- **`insertIgnore` 미사용**: H2(MySQL 모드)+Exposed에서 UnsupportedByDialect → check-then-insert(Switcher 판단)로 대체.
- **`Notification.isUnread()` 제거**: 프로덕션 미사용 죽은 코드 (리뷰 반영).
- **(cross-cutting) 역직렬화 오류 처리 정비**: `GlobalExceptionHandler`에 `HttpMessageNotReadableException`→400 추가 + `fail-on-null-for-primitives` 활성화 — 원시 타입 null이 false/0으로 조용히 강제 변환되던 것을 400으로 차단. 본문 내 잘못된 난독화 id도 500→400 (EncryptIdRequestBodyIntegrationTest 갱신). 영향권의 기존 원시 필드는 point 도메인(제거 예정)뿐이라 별도 회귀 테스트 생략.
- **리뷰 미반영 2건(사유)**: `@NotNull(message=...)` 재부착 — 원시 타입에선 검증 전에 역직렬화가 끝나 죽은 코드가 되므로 미반영(전역 설정이 단일 방어선). 부모 댓글 중복 조회 — §16 후속 유지.

---

## 1. 요구사항 요약

### 목표
회원이 자기 게시글에 달린 새 댓글, 또는 자기 댓글에 달린 대댓글을 **인앱 알림 인박스**로 확인하고, 게시글 단위로 알림을 끌 수 있게 한다.

### 기능 요구사항
1. **알림 생성** — 댓글/대댓글 작성 트랜잭션 안에서 수신자 1명에게 알림 레코드 생성.
   - 루트 댓글(`parentCommentId == null`) → **게시글 작성자**에게 (`type = COMMENT_ON_POST`)
   - 대댓글(`parentCommentId != null`) → **부모 댓글 작성자**에게 (`type = REPLY_ON_COMMENT`)
2. **알림 목록 조회** — 로그인 회원이 자신이 받은 알림을 최신순(커서 페이징)으로 조회, 읽음 여부 포함.
3. **읽음 처리** — 단건 읽음 / 전체 읽음.
4. **알림 설정 토글** — 게시글 단위로 댓글 알림 on/off (기본 on). off인 게시글에 대해서는 해당 회원에게 알림을 만들지 않는다.
5. (부가) **안읽음 개수** 조회 — 뱃지용. 저비용이라 포함(§7).

### 알림 대상 규칙 (변경 불가 스펙)
- 내 게시글에 새 댓글 → 글 작성자에게 / 내 댓글에 대댓글 → 그 댓글 작성자에게.
- **본인이 단 댓글엔 알림 없음** (자기 글 자기 댓글 / 자기 댓글 자기 대댓글).
- 설정 토글은 **게시글 단위**(기본 on).

### 비기능 요구사항
- 알림 생성은 댓글 작성 응답을 막지 않을 만큼 가벼워야 함(인덱스 조회 2~3건 + insert 1건). §12 리스크 참조.
- FK 미사용 원칙 유지, soft-delete/audit 컬럼은 `BaseTable` 상속으로 공통화.
- id 표현은 커뮤니티 API(raw `Long`)와 **반드시 일치**해야 함(내비게이션 정합). §3 D5.

### 용어
- **수신자(recipient)** = 알림을 받는 회원(글 작성자 또는 부모 댓글 작성자).
- **행위자(actor)** = 댓글/대댓글을 단 회원. 항상 수신자에서 제외.
- **알림 인박스(notification)** = 수신자별 알림 레코드 저장소(신규 `notification` 도메인).
- **댓글 알림 설정(comment-notification setting)** = 게시글별 opt-out 레코드(커뮤니티 도메인).

---

## 2. 실측 요약 (계획의 전제 — grep/파일 실측 결과)

| # | 실측 항목 | 결과 | 설계 영향 |
|---|-----------|------|-----------|
| F1 | 댓글 생성 흐름 | `CommentCommandService.create(newComment): Long` → `commentValidator.validate()` → `commentCommandRepository.save(newComment): Long`(DB생성 id 반환). | 알림 생성 훅은 `save` **직후**, 반환된 `commentId`로. |
| F2 | `CommentValidator` | `postQueryRepository.exists(postId)`(로드 안 함) + 대댓글이면 `commentQueryRepository.findOne(parentCommentId)`로 부모 로드 후 `validateIsRootComment()` — **부모 Comment(authorId 보유)를 로드하지만 버림**. | 수신자 해석 시 부모 재조회 필요(마이크로 중복, §12). 글 작성자는 아직 미로드 → `postQueryRepository.findOne` 필요. |
| F3 | 수신자 해석 포트 | `PostQueryRepository.findOne(id): Post`(`authorId` 보유), `CommentQueryRepository.findOne(id): Comment`(`authorId` 보유). | 루트→`postQueryRepository.findOne(postId).authorId`, 대댓글→`commentQueryRepository.findOne(parentCommentId).authorId`. |
| F4 | **차단(block)** | `Block(blockerId→blockedId)` 단방향. `BlockQueryRepository.findBlockedMemberIds(blockerId): Set<Long>` = "내가 차단한 사람". **write 경로는 block 미조회**, 조회 시에만: 게시글 목록=**완전 제외**, 댓글 목록=**마스킹**(`"차단한 사용자의 댓글입니다."`, row 유지). | §11 결정: **수신자가 actor를 차단했으면 알림 생략**(`findBlockedMemberIds(recipientId).contains(actorId)`). |
| F5 | **이벤트 퍼블리셔 선례** | `ApplicationEventPublisher`/`@EventListener`/`@TransactionalEventListener`/`publishEvent` **전무(0건)**. 크로스 애그리거트는 서비스가 여러 포트를 직접 조합(`PostCommandService.delete`, `ReportCommandService`). `@Component` 협력자 조합 선례 = `CommentValidator`(여러 QueryRepository 주입). | §3 D3: **직접 조합**. `CommentValidator` 패턴의 `@Component` Registrar로 캡슐화. 이벤트는 후속(§13). |
| F6 | **id 난독화** | `ObfuscationType` = MEMBER/TARGET_INFO/MEMBER_PHOTO/MATCHING_RESULT/XROOM/MEMORY/MEDIA/POINT_HISTORY. **POST/COMMENT 없음** — 커뮤니티 API는 `@DecryptId`/`@EncryptId` 미사용, **raw `Long`**. matching/xroom만 난독화. | §3 D5: 알림도 **raw `Long`**(postId/commentId가 커뮤니티와 같은 id여야 내비게이션 정합). `ObfuscationType.NOTIFICATION` 추가 안 함. |
| F7 | **DDL/테이블 관례** | `ddl.sql`은 Exposed Table이 소스오브트루스인 미러. PK `<SINGULAR>_ID BIGINT AUTO_INCREMENT`. soft-delete=`DELETED BOOLEAN`(+`DELETED_DATE/BY`), `CREATED_DATE DATETIME`, 감사컬럼 전부 `BaseTable`. **FK 전무**(BIGINT + `idx_...` 보조 인덱스). enum 컬럼: 커뮤니티는 `varchar`+`.name`/`valueOf`(matching만 `enumerationByName`). | §6 테이블: `BaseTable` 상속, FK 없음, `TYPE`은 커뮤니티 관례대로 `varchar(32)`. |
| F8 | **페이징 관례** | 게시글 목록=**커서(seek)**: `id less cursorId` + `id DESC`, `CursorResult`/`CursorResponse`(data/hasNext/nextCursorId), `CursorIdCondition(cursorId, size=20)`. **댓글 목록은 독립 페이징 없음**(상세 안에서 전량 ASC 로드). `SliceResponse` 없음. | §7 알림 목록: **게시글 목록 커서 패턴 그대로 미러**(id DESC, `CursorResponse`). |
| F9 | **탈퇴 연쇄** | `MemberDataCleaner`(도메인 `withdrawal`, @Component) — 도메인별 `private fun clean*(member)` 한 줄씩. 커뮤니티는 `cleanCommunity` = `postLikeRepository.deleteByMember` + `commentLikeRepository.deleteByMember`(글/댓글 본문은 anonymize로 유지). `MemberWithdrawalBackupCollector`는 백업 스냅샷 read. **포트 인터페이스 아님 — 중앙 클래스 직접 편집**. | §10: `MemberDataCleaner`에 `cleanNotification` 추가(알림·설정 `deleteByMember`). 백업은 **생략**(알림=파생/비분쟁증거). |
| F10 | **REST Docs/api-todo** | `main.adoc`에 도메인별 `[[..]] ==` 섹션 + API별 `link:`. 스니펫 `include::`는 `asciidoc/<domain>/<api>.adoc` per-API 파일. `api-todo.md` 커뮤니티 TODO에 **"게시글에 댓글 알림 설정"** 딱 1건(=본 기능). | §13/§14: `[[notification]]` 섹션 신설 + community 토글 링크 추가, TODO→완료 이관. |
| F11 | 신규성 | `notification`/`알림` 코드·테이블 전무 — **그린필드**. | 마이그레이션 없음, 신규 생성만. |

---

## 3. 확정 결정 사항

| # | 쟁점 | 조사 결과 / 근거 | 결정 |
|---|------|-----------------|------|
| **D1** | **도메인 배치** — 신규 `notification` 도메인 vs `community` 하위 | `feedback_no_new_domain`은 **절대 규칙 아님**(§5 실측). "기존 애그리거트에 속하면 새 도메인 금지", 단 `support-inquiry-create.plan.md:75`에서 "어느 도메인에도 안 속하면 새 패키지 적절"로 **명시적 override**, 실제 `withdrawal`은 크로스컷팅이라 top-level로 shipped. 알림은 Post/Comment의 하위 엔티티가 아니라 **수신자별 독립 라이프사이클(read/unread)**을 갖는 별개 개념이고, 스펙상 향후 claim 수락/거절 등 **커뮤니티 밖으로 확장** 예정. | **신규 top-level `notification` 도메인(제네릭 인박스)** 채택. 단, **게시글별 댓글 알림 설정은 `community`에 둔다**(§5.3) — 커뮤니티 전용 선호도라서. 방향은 **community → notification 단방향**(cycle 없음). |
| **D2** | 알림 생성 트리거 위치 | `CommentCommandService.create`가 유일한 댓글/대댓글 진입점(F1). 이벤트 선례 없음(F5). | **`create` 트랜잭션 내 직접 조합**. `save` 직후 `commentNotificationRegistrar.register(newComment, commentId)` 호출. |
| **D3** | 생성 로직 캡슐화 | 서비스는 조합만(clean-code). 수신자 해석+제외규칙은 크로스-포트 협력. 선례 = `CommentValidator`(@Component, QueryRepository 다중 주입). | **`CommentNotificationRegistrar`(@Component, community.application)** 신설. 수신자 해석/자기제외/설정off/차단 판단 + 인박스 저장을 캡슐화. |
| **D4** | 설정 저장 방식 | "기본 on" → opt-out만 저장하면 저장량 최소·기본값 무레코드. | **opt-out 레코드 모델**: `(MEMBER_ID, POST_ID)` 유니크. 행 존재=OFF, 없음=ON. 토글 ON=행 삭제, OFF=행 삽입(idempotent). |
| **D5** | id 인코딩 | 커뮤니티는 raw `Long`(F6). 알림 응답의 postId/commentId가 커뮤니티의 그것과 **다르게 인코딩되면 같은 리소스가 두 표현**이 되어 내비게이션 깨짐. | **전부 raw `Long`**(notificationId/postId/commentId/actorId). `ObfuscationType.NOTIFICATION` 미추가. 커뮤니티가 추후 난독화로 전환하면 함께 전환(§13). |
| **D6** | 알림 목록 페이징 | 게시글 목록=커서(F8). 댓글 목록은 페이징 없음(부적합). | **게시글 목록 커서 패턴 미러**: `id less cursorId` + `id DESC`, `CursorResponse<List<NotificationResponse>>`. |
| **D7** | 표시 데이터 비정규화 | 닉네임 비정규화는 stale. 선례 = `CommentQueryService`가 `MemberQueryRepository.findByIds`로 read 시 닉네임 조합. | **최소 id만 저장**, read 시 **actor 닉네임만** `MemberQueryRepository`로 enrich(notification→member, 선례 有). post title/댓글 preview 비정규화는 **미포함**(§13). |
| **D8** | 차단 관계 | block은 read-side, 단방향, 댓글은 마스킹(F4). 알림은 수신자가 소비하는 push 콘텐츠. | **수신자가 actor를 차단했으면 생략**(§11). 역방향(actor가 수신자 차단)은 미적용. |
| **D9** | 중복 알림(글작성자==부모댓글작성자) | 스펙 규칙상 **대댓글은 부모 댓글 작성자에게만** 알림(글 작성자에겐 루트 댓글만). 한 댓글 = 수신자 1명. | 구조적으로 **중복 불가**(수신자 1명). Registrar를 **수신자 집합 기반**으로 설계해(§9 N7) 향후 "대댓글도 글작성자에게" 확장 시 `Set`이 자동 dedup. 현재는 크기 ≤1. |
| **D10** | 트랜잭션 경계 | `create`는 `@Transactional`. 이벤트 없음(F5). | **동일 트랜잭션**(댓글+알림 원자성). 알림 실패가 댓글을 롤백. best-effort 분리(async/event)는 후속(§13). |
| **D11** | 탈퇴 백업 여부 | 알림은 이미 백업되는 글/댓글의 파생이고 분쟁 증거 아님(F9). | 백업 **생략**, **정리(cleanup)만**: `MemberDataCleaner.cleanNotification`에서 알림·설정 `deleteByMember`. |

---

## 4. 아키텍처

```
┌──────────────────────────────────────────────────────────────────────────────┐
│ boot/ma-boot-web                                                               │
│  NotificationQueryApi    GET  /api/notifications           (커서 목록)         │  ← 신규
│                          GET  /api/notifications/unread-count                  │  ← 신규
│  NotificationCommandApi  PATCH /api/notifications/{id}/read                    │  ← 신규
│                          PATCH /api/notifications/read-all                     │  ← 신규
│  CommentNotificationSettingApi                                                 │  ← 신규
│                          PATCH /api/community/posts/{postId}/comment-notification (on/off)│
│  (CommentCommandApi: 변경 없음 — create 흐름은 그대로, 훅은 도메인 내부)        │
└───────────────┬─────────────────────────────────────────────┬─────────────────┘
                │ (port)                                        │ (port)
┌───────────────▼──────────────────────┐   ┌───────────────────▼─────────────────┐
│ domain/ma-domain-core : community     │   │ domain/ma-domain-core : notification │ ← 신규 top-level
│                                       │   │  (제네릭 알림 인박스 — community 무의존)│
│  CommentCommandService.create()       │   │  Notification (recipientId/type/     │
│    → save → registrar.register(...)   │   │     actorId/postId/commentId/read)   │
│                                       │   │    + validateRecipient(memberId)     │
│  CommentNotificationRegistrar @Comp   │   │  NotificationType(enum)              │
│    수신자 해석(post/parent author)    │   │    COMMENT_ON_POST / REPLY_ON_COMMENT │
│    - 자기제외 / 설정off / 차단 제외    ├──▶│  NewNotification (생성 입력)          │
│    - NotificationCommandRepository.save│   │  NotificationQueryService.find()     │
│      (community → notification 단방향) │   │    (커서 + MemberQueryRepo로 닉네임)  │
│                                       │   │  NotificationCommandService          │
│  CommentNotificationSetting (opt-out) │   │    markAsRead / markAllAsRead        │
│  CommentNotificationSettingRepository │   │  port: NotificationCommandRepository  │
│  CommentNotificationSettingCmdService │   │        NotificationQueryRepository    │
│    .set(memberId, postId, enabled)    │   └───────────────────┬──────────────────┘
│  (기존 PostQueryRepository로 post검증) │                       │ (implements)
│                                       │                       │
│  withdrawal.MemberDataCleaner         │        notification →  │ member.MemberQueryRepository
│    + cleanNotification(member)        │        (read 닉네임, 선례 有)
└───────────────┬───────────────────────┘   ┌───────────────────▼──────────────────┐
                │ (implements)               │ infrastructure/storage/ma-db-core     │
┌───────────────▼───────────────────────┐   │  NotificationTable ("NOTIFICATIONS")  │
│ infrastructure/storage/ma-db-core      │   │  NotificationEntity                   │
│  CommentNotificationSettingTable       │   │  NotificationCommandDao / QueryDao    │
│   ("COMMUNITY_COMMENT_NOTIFICATION_    │   │  NotificationCommand/QueryCoreRepo     │
│     SETTINGS", UNIQUE(MEMBER_ID,POST_ID))│  │  ddl.sql: NOTIFICATIONS               │
│  ...SettingDao / CoreRepository        │   └───────────────────────────────────────┘
└────────────────────────────────────────┘

방향 정리:  community ──▶ notification (인박스 write / 설정은 community 자체 보유)
           notification ──▶ member (닉네임 read, 선례)      ※ notification ──▶ community 없음 → cycle 없음
```

---

## 5. 도메인 배치 판단 (D1 상세 — 가장 중요한 결정)

### 5.1 `feedback_no_new_domain`의 실제 취지 (실측)
- 원본 메모리 파일은 **부재**(code-implementer 메모리 비어 있음). 규칙은 plan 인용으로만 존재.
- 모든 인용이 **이유와 함께** 적용됨: "애그리거트의 상태 전이 그 자체"(claim-accept-reject), "같은 애그리거트 컨텍스트"(xroom-memory), "X룸 없이 단독 존재 불가"(xroom-block).
- **명시적 override 선례**: `support-inquiry-create.plan.md:75` — *"memory의 새 도메인 분리 지양 피드백이 있으나, 문의(inquiry)는 기존 도메인 어디에도 속하지 않는 독립 도메인이므로 신규 패키지가 적절하다."*
- **크로스컷팅 top-level 선례**: `withdrawal` 도메인은 plan이 "member 하위"라 적었으나 실제로는 여러 도메인을 가로지르는 집계 로직이라 **top-level로 shipped**.
- 결론: 규칙은 **"기존 애그리거트에 속하는 것을 굳이 분리하지 말라"**는 반증가능한 기본값. **어디에도 안 속하거나 크로스컷팅이면 신규 top-level이 정답.**

### 5.2 알림 = 제네릭 서브도메인 → top-level `notification`
- **DDD 관점**: `Notification`은 Post/Comment 애그리거트의 하위 엔티티가 아니다. postId/commentId를 **id로 참조**할 뿐, 수신자별 read/unread 라이프사이클은 댓글과 독립적이다. 전형적인 **generic subdomain(알림 인박스)** = 별개 bounded context.
- **확장성**: 스펙이 명시한 향후 "claim 수락/거절 알림"은 `matching` 발원. 알림을 `community` 하위에 두면 나중에 `matching`이 `community`에 의존(부적절)하거나 모델을 중복해야 한다. `notification`을 제네릭으로 두면 각 발원 도메인이 자기 Registrar로 **같은 인박스에 적재**만 하면 된다.
- **선례 정렬**: `support-inquiry` override("단독 도메인") + `withdrawal`(크로스컷팅 top-level)이 정확히 이 경우를 지지.

### 5.3 단, 설정(opt-out)은 `community`에 남긴다
- "게시글별 댓글 알림 on/off"는 **커뮤니티 댓글 알림에만 의미 있는** 선호도(제네릭 알림 설정이 아님). 게시글 존재 검증(`PostQueryRepository.exists`)도 커뮤니티 관심사.
- 설정 모델/포트/토글 서비스를 `community`에 두면 **notification 도메인은 순수 제네릭 인박스**로 유지되고, 의존 방향이 **community → notification 단방향**으로 깔끔해진다(notification은 community를 절대 import 안 함 → cycle 없음).
- 발원 도메인(`community`)이 **자기 Registrar에서 자기 설정을 확인**하고 인박스에 적재 → 이후 `matching`도 동일 패턴(자기 설정 + `ClaimNotificationRegistrar`).

> **판정**: 인박스(레코드/조회/읽음) = `notification` top-level. 발원 규칙+설정 = 발원 도메인(`community`). 이 분리가 clean-architecture(단방향) · DDD(generic subdomain) · 확장성 · 선례를 모두 만족한다.

---

## 6. 테이블 설계 (DDL)

> Exposed Table이 소스오브트루스, `ddl.sql`은 미러(F7). `BaseTable`이 `CREATED_DATE/BY`, `LAST_MODIFIED_DATE/BY`, `DELETED/DELETED_DATE/BY`를 `clientDefault`로 공급. **FK 없음.**

### 6.1 `NOTIFICATIONS` (신규 — notification 도메인)
```sql
CREATE TABLE NOTIFICATIONS
(
    NOTIFICATION_ID    BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- NotificationTable 특화 컬럼
    RECIPIENT_ID       BIGINT       NOT NULL,   -- 수신자 memberId
    TYPE               VARCHAR(32)  NOT NULL,   -- COMMENT_ON_POST | REPLY_ON_COMMENT
    ACTOR_ID           BIGINT       NOT NULL,   -- 행위자(댓글 작성자) memberId
    POST_ID            BIGINT       NOT NULL,   -- 관련 게시글(내비게이션, raw)
    COMMENT_ID         BIGINT       NOT NULL,   -- 관련 (대)댓글(raw)
    IS_READ            BOOLEAN      DEFAULT FALSE,

    -- BaseTable 공통 컬럼
    CREATED_DATE       DATETIME     DEFAULT CURRENT_TIMESTAMP,
    CREATED_BY         VARCHAR(255) DEFAULT 'MEET_AGAIN',
    LAST_MODIFIED_DATE DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    LAST_MODIFIED_BY   VARCHAR(255) DEFAULT 'MEET_AGAIN',
    DELETED            BOOLEAN      DEFAULT FALSE,
    DELETED_DATE       DATETIME     NULL,
    DELETED_BY         VARCHAR(255) NULL,

    -- 인덱스
    INDEX idx_notification_recipient_id (RECIPIENT_ID)          -- 수신자별 목록(id DESC seek)
    -- (선택) INDEX idx_notification_recipient_read (RECIPIENT_ID, IS_READ) -- 안읽음 카운트 최적화
);
```
- 목록 쿼리 `WHERE RECIPIENT_ID=? AND DELETED=false [AND id<cursor] ORDER BY id DESC LIMIT size`는 `idx_notification_recipient_id`가 커버(InnoDB 보조인덱스에 PK 포함 → id 정렬 효율적).
- 안읽음 카운트가 병목이면 `(RECIPIENT_ID, IS_READ)` 복합 인덱스 추가(초기엔 보류).

### 6.2 `COMMUNITY_COMMENT_NOTIFICATION_SETTINGS` (신규 — community 도메인, opt-out)
```sql
CREATE TABLE COMMUNITY_COMMENT_NOTIFICATION_SETTINGS
(
    COMMENT_NOTIFICATION_SETTING_ID BIGINT AUTO_INCREMENT PRIMARY KEY,

    MEMBER_ID          BIGINT       NOT NULL,   -- 알림을 끈 회원
    POST_ID            BIGINT       NOT NULL,   -- 대상 게시글

    -- BaseTable 공통 컬럼 (동일 세트)
    CREATED_DATE       DATETIME     DEFAULT CURRENT_TIMESTAMP,
    CREATED_BY         VARCHAR(255) DEFAULT 'MEET_AGAIN',
    LAST_MODIFIED_DATE DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    LAST_MODIFIED_BY   VARCHAR(255) DEFAULT 'MEET_AGAIN',
    DELETED            BOOLEAN      DEFAULT FALSE,
    DELETED_DATE       DATETIME     NULL,
    DELETED_BY         VARCHAR(255) NULL,

    UNIQUE INDEX idx_comment_noti_setting_member_post (MEMBER_ID, POST_ID)
);
```
- **행 존재 = 그 회원이 그 게시글의 댓글 알림 OFF** (기본 ON은 무레코드). `COMMUNITY_POST_LIKES`의 `UNIQUE INDEX (POST_ID, MEMBER_ID)` 관례 미러.
- OFF 토글은 `INSERT ... (idempotent, 중복 시 무시)`, ON 토글은 `DELETE`(하드) — like 토글과 동일 성격. (soft-delete 컬럼은 BaseTable 상속으로 존재하나 설정은 하드 삭제가 자연스러움. 재-OFF 시 유니크 충돌 방지 위해 하드 삭제 채택.)

---

## 7. API 설계

| # | Method | URL | 용도 | 인증 | 응답 |
|---|--------|-----|------|------|------|
| A1 | GET | `/api/notifications?cursorId=&size=` | 내 알림 목록(최신순, 읽음여부 포함) | 필요 | 200 `CursorResponse<List<NotificationResponse>>` |
| A2 | GET | `/api/notifications/unread-count` | 안읽음 개수(뱃지) | 필요 | 200 `{ "count": <Long> }` |
| A3 | PATCH | `/api/notifications/{notificationId}/read` | 단건 읽음 | 필요 | 200 (본문 없음) |
| A4 | PATCH | `/api/notifications/read-all` | 전체 읽음 | 필요 | 200 (본문 없음) |
| A5 | PATCH | `/api/community/posts/{postId}/comment-notification` | 게시글 댓글 알림 on/off | 필요 | 200 (본문 없음) |

### Request / Response 스키마
- **A5 Request** `CommentNotificationSettingRequest`: `{ "enabled": Boolean }` (`@field:NotNull`).
- **A1 NotificationResponse**:
  ```json
  {
    "id": 12,
    "type": "COMMENT_ON_POST",
    "actorId": 5,
    "actorNickname": "홍길동",
    "postId": 30,
    "commentId": 77,
    "read": false,
    "createdDate": "2026-07-17T10:20:30"
  }
  ```
  (모든 id raw `Long` — D5. `type`으로 클라이언트가 문구 렌더: COMMENT_ON_POST="회원님의 게시글에 댓글이 달렸어요", REPLY_ON_COMMENT="회원님의 댓글에 답글이 달렸어요".)
- **A2 UnreadCountResponse**: `{ "count": Long }`.

### 에러 케이스 (기존 `GlobalExceptionHandler` 매핑 재사용)
| HTTP | 상황 | 예외 | 발생 위치 |
|------|------|------|-----------|
| 403 | A3에서 알림 수신자가 호출자가 아님 | `AccessDeniedException(EntityType.NOTIFICATION,…)` | `Notification.validateRecipient` |
| 404 | A3 알림 id 부재 / A5 게시글 부재 | `EntityNotFoundException(NOTIFICATION / COMMUNITY_POST)` | `NotificationQueryRepository.findOne` / `CommentNotificationSettingCommandService.set` |
| 400 | A5 `enabled` 누락 | Bean Validation | `@Valid` |

---

## 8. 영향 파일 (신규 N / 수정 M) — 전체 목록

### 8.1 도메인 모듈 `domain/ma-domain-core` — notification (신규 도메인)
| # | 파일 | 유형 | 내용 |
|---|------|------|------|
| N1 | `…/domain/notification/domain/NotificationType.kt` | 신규 | enum `COMMENT_ON_POST / REPLY_ON_COMMENT` (확장점) |
| N2 | `…/domain/notification/domain/Notification.kt` | 신규 | 모델 + `validateRecipient` + `isUnread` |
| N3 | `…/domain/notification/domain/NewNotification.kt` | 신규 | 생성 입력 VO |
| N4 | `…/domain/notification/domain/NotificationWithActor.kt` | 신규 | read 조합용(알림 + actor 닉네임) |
| N5 | `…/domain/notification/domain/port/NotificationCommandRepository.kt` | 신규 | `save`/`markAsRead`/`markAllAsRead`/`deleteByMember` |
| N6 | `…/domain/notification/domain/port/NotificationQueryRepository.kt` | 신규 | `findOne`/`findByRecipient(cursor)`/`countUnread` |
| N7 | `…/domain/notification/application/NotificationQueryService.kt` | 신규 | 목록(커서+닉네임 enrich) / 안읽음 카운트 |
| N8 | `…/domain/notification/application/NotificationCommandService.kt` | 신규 | `markAsRead`(소유권) / `markAllAsRead` |

### 8.2 도메인 모듈 `domain/ma-domain-core` — community (확장)
| # | 파일 | 유형 | 내용 |
|---|------|------|------|
| N9 | `…/community/domain/CommentNotificationSetting.kt` | 신규 | opt-out 모델(memberId, postId) |
| N10 | `…/community/domain/port/CommentNotificationSettingRepository.kt` | 신규 | `isOptedOut`/`optOut`/`optIn`/`deleteByMember` |
| N11 | `…/community/application/CommentNotificationRegistrar.kt` | 신규 | @Component 수신자해석+제외규칙+인박스 저장(D3) |
| N12 | `…/community/application/CommentNotificationSettingCommandService.kt` | 신규 | `set(memberId, postId, enabled)` |
| M1 | `…/community/application/CommentCommandService.kt` | 수정 | `create`에 `registrar.register(newComment, commentId)` 1줄 + 생성자에 Registrar 주입 |

### 8.3 도메인 모듈 — withdrawal / exception
| # | 파일 | 유형 | 내용 |
|---|------|------|------|
| M2 | `…/withdrawal/domain/MemberDataCleaner.kt` | 수정 | `cleanNotification(member)` 추가 + 포트 2개 주입 |
| M3 | `…/exception/EntityType.kt` | 수정 | `NOTIFICATION("Notification","id")` 값 추가 |

### 8.4 인프라 모듈 `infrastructure/storage/ma-db-core`
| # | 파일 | 유형 | 내용 |
|---|------|------|------|
| N13 | `…/notification/entity/table/NotificationTable.kt` | 신규 | Exposed 테이블 |
| N14 | `…/notification/entity/NotificationEntity.kt` | 신규 | row ↔ 도메인 매핑 |
| N15 | `…/notification/dao/NotificationCommandDao.kt` | 신규 | insert/markRead/markAllRead/deleteByMember |
| N16 | `…/notification/dao/NotificationQueryDao.kt` | 신규 | findOne/findByRecipient(seek)/countUnread |
| N17 | `…/notification/repository/NotificationCommandCoreRepository.kt` | 신규 | 포트 구현(위임) |
| N18 | `…/notification/repository/NotificationQueryCoreRepository.kt` | 신규 | 포트 구현(위임) |
| N19 | `…/community/entity/table/CommentNotificationSettingTable.kt` | 신규 | Exposed 테이블(유니크) |
| N20 | `…/community/dao/CommentNotificationSettingDao.kt` | 신규 | isOptedOut/optOut(insertIgnore)/optIn(delete)/deleteByMember |
| N21 | `…/community/repository/CommentNotificationSettingCoreRepository.kt` | 신규 | 포트 구현 |
| M4 | `…/resources/script/ddl.sql` | 수정 | §6 테이블 2개 추가 |

### 8.5 부트 모듈 `boot/ma-boot-web`
| # | 파일 | 유형 | 내용 |
|---|------|------|------|
| N22 | `…/notification/api/NotificationQueryApi.kt` | 신규 | A1/A2 |
| N23 | `…/notification/api/NotificationCommandApi.kt` | 신규 | A3/A4 |
| N24 | `…/notification/api/response/NotificationResponse.kt` | 신규 | raw Long 필드 |
| N25 | `…/notification/api/response/UnreadCountResponse.kt` | 신규 | `count` |
| N26 | `…/community/api/CommentNotificationSettingApi.kt` | 신규 | A5 |
| N27 | `…/community/api/request/CommentNotificationSettingRequest.kt` | 신규 | `enabled` |

> `CommentCommandApi`(N/A)·`CursorResponse`·`CursorIdCondition`·`CursorResult`·`MemberInfo`/`@LoginMember`는 **재사용, 무변경**.

---

## 9. 파일별 상세 설계 (컴파일 수준 스니펫 — 핵심 클래스)

### N1. `NotificationType.kt`
```kotlin
package com.konkuk.ma.domain.notification.domain

enum class NotificationType {
    COMMENT_ON_POST,   // 내 게시글에 새 (루트)댓글
    REPLY_ON_COMMENT,  // 내 댓글에 대댓글
    // 확장점: CLAIM_ACCEPTED, CLAIM_REJECTED … (matching 발원, 값만 추가)
}
```

### N2. `Notification.kt`
```kotlin
package com.konkuk.ma.domain.notification.domain

import com.konkuk.ma.exception.AccessDeniedException
import com.konkuk.ma.exception.EntityType
import java.time.LocalDateTime

class Notification(
    val id: Long = 0L,
    val recipientId: Long,
    val type: NotificationType,
    val actorId: Long,
    val postId: Long,
    val commentId: Long,
    val read: Boolean = false,
    val createdDate: LocalDateTime = LocalDateTime.now(),
) {
    fun validateRecipient(memberId: Long) {
        if (recipientId != memberId) {
            throw AccessDeniedException(EntityType.NOTIFICATION, recipientId.toString(), memberId.toString())
        }
    }
}
```

### N3. `NewNotification.kt`
```kotlin
package com.konkuk.ma.domain.notification.domain

class NewNotification(
    val recipientId: Long,
    val type: NotificationType,
    val actorId: Long,
    val postId: Long,
    val commentId: Long,
)
```
> 자기 제외/설정off/차단은 **필터링 결정**이라 발원 Registrar(N11)에서 처리(생성 전 skip). `NewNotification`은 순수 입력 VO — 여기 도달하면 이미 "생성 대상 확정".

### N4. `NotificationWithActor.kt`
```kotlin
package com.konkuk.ma.domain.notification.domain

class NotificationWithActor(
    val notification: Notification,
    val actorNickname: String,
)
```

### N5. `NotificationCommandRepository.kt` (port)
```kotlin
package com.konkuk.ma.domain.notification.domain.port

import com.konkuk.ma.domain.notification.domain.NewNotification

interface NotificationCommandRepository {
    fun save(newNotification: NewNotification): Long
    fun markAsRead(notificationId: Long)
    fun markAllAsRead(recipientId: Long)
    fun deleteByMember(recipientId: Long)   // 탈퇴 정리
}
```

### N6. `NotificationQueryRepository.kt` (port)
```kotlin
package com.konkuk.ma.domain.notification.domain.port

import com.konkuk.ma.domain.common.domain.page.CursorIdCondition
import com.konkuk.ma.domain.common.domain.page.CursorResult
import com.konkuk.ma.domain.notification.domain.Notification

interface NotificationQueryRepository {
    fun findOne(id: Long): Notification
    fun findByRecipient(recipientId: Long, cursor: CursorIdCondition): CursorResult<List<Notification>>
    fun countUnread(recipientId: Long): Long
}
```

### N7. `NotificationQueryService.kt`
```kotlin
package com.konkuk.ma.domain.notification.application

import com.konkuk.ma.domain.common.domain.page.CursorIdCondition
import com.konkuk.ma.domain.common.domain.page.CursorResult
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import com.konkuk.ma.domain.notification.domain.NotificationWithActor
import com.konkuk.ma.domain.notification.domain.port.NotificationQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class NotificationQueryService(
    private val notificationQueryRepository: NotificationQueryRepository,
    private val memberQueryRepository: MemberQueryRepository,   // 선례: CommentQueryService
) {
    fun find(recipientId: Long, cursor: CursorIdCondition): CursorResult<List<NotificationWithActor>> {
        val result = notificationQueryRepository.findByRecipient(recipientId, cursor)
        val nicknameById = memberQueryRepository
            .findByIds(result.data.map { it.actorId }.toSet())
            .associate { it.id to it.nickname }
        val enriched = result.data.map { NotificationWithActor(it, nicknameById[it.actorId] ?: UNKNOWN) }
        return CursorResult(enriched, result.hasNext, result.nextCursorId)  // 페이징 플래그 보존
    }

    fun countUnread(recipientId: Long): Long = notificationQueryRepository.countUnread(recipientId)

    companion object { private const val UNKNOWN = "알 수 없음" }  // 탈퇴 회원 대비
}
```

### N8. `NotificationCommandService.kt`
```kotlin
package com.konkuk.ma.domain.notification.application

import com.konkuk.ma.domain.notification.domain.port.NotificationCommandRepository
import com.konkuk.ma.domain.notification.domain.port.NotificationQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class NotificationCommandService(
    private val notificationQueryRepository: NotificationQueryRepository,
    private val notificationCommandRepository: NotificationCommandRepository,
) {
    fun markAsRead(notificationId: Long, memberId: Long) {
        val notification = notificationQueryRepository.findOne(notificationId)  // 없으면 404
        notification.validateRecipient(memberId)                               // 남의 알림이면 403
        notificationCommandRepository.markAsRead(notificationId)
    }

    fun markAllAsRead(memberId: Long) {
        notificationCommandRepository.markAllAsRead(memberId)
    }
}
```

### N9. `CommentNotificationSetting.kt` (community)
```kotlin
package com.konkuk.ma.domain.community.domain

class CommentNotificationSetting(
    val memberId: Long,
    val postId: Long,   // 존재 = 이 회원이 이 게시글 댓글 알림 OFF (기본 ON은 무레코드)
)
```

### N10. `CommentNotificationSettingRepository.kt` (port, community)
```kotlin
package com.konkuk.ma.domain.community.domain.port

interface CommentNotificationSettingRepository {
    fun isOptedOut(memberId: Long, postId: Long): Boolean
    fun optOut(memberId: Long, postId: Long)   // OFF: insert (idempotent)
    fun optIn(memberId: Long, postId: Long)     // ON: delete
    fun deleteByMember(memberId: Long)          // 탈퇴 정리
}
```

### N11. `CommentNotificationRegistrar.kt` (community, @Component — 핵심)
```kotlin
package com.konkuk.ma.domain.community.application

import com.konkuk.ma.domain.community.domain.NewComment
import com.konkuk.ma.domain.community.domain.block.BlockedMemberIds
import com.konkuk.ma.domain.community.domain.port.BlockQueryRepository
import com.konkuk.ma.domain.community.domain.port.CommentNotificationSettingRepository
import com.konkuk.ma.domain.community.domain.port.CommentQueryRepository
import com.konkuk.ma.domain.community.domain.port.PostQueryRepository
import com.konkuk.ma.domain.notification.domain.NewNotification
import com.konkuk.ma.domain.notification.domain.NotificationType
import com.konkuk.ma.domain.notification.domain.port.NotificationCommandRepository
import org.springframework.stereotype.Component

@Component
class CommentNotificationRegistrar(
    private val postQueryRepository: PostQueryRepository,
    private val commentQueryRepository: CommentQueryRepository,
    private val blockQueryRepository: BlockQueryRepository,
    private val commentNotificationSettingRepository: CommentNotificationSettingRepository,
    private val notificationCommandRepository: NotificationCommandRepository,  // community → notification
) {
    fun register(newComment: NewComment, commentId: Long) {
        val recipientId = resolveRecipient(newComment)
        val actorId = newComment.authorId

        if (recipientId == actorId) return                                    // 자기 자신 제외
        if (commentNotificationSettingRepository.isOptedOut(recipientId, newComment.postId)) return  // 설정 OFF
        if (hasBlocked(recipientId, actorId)) return                          // 수신자가 actor 차단(§11)

        notificationCommandRepository.save(
            NewNotification(
                recipientId = recipientId,
                type = if (newComment.hasParent()) NotificationType.REPLY_ON_COMMENT
                       else NotificationType.COMMENT_ON_POST,
                actorId = actorId,
                postId = newComment.postId,
                commentId = commentId,
            )
        )
    }

    private fun resolveRecipient(newComment: NewComment): Long =
        if (newComment.hasParent()) {
            commentQueryRepository.findOne(newComment.parentCommentId!!).authorId  // 부모 댓글 작성자
        } else {
            postQueryRepository.findOne(newComment.postId).authorId                // 게시글 작성자
        }

    private fun hasBlocked(recipientId: Long, actorId: Long): Boolean =
        BlockedMemberIds(blockQueryRepository.findBlockedMemberIds(recipientId)).contains(actorId)
}
```
> **D9(중복) 대비 확장 노트**: 현재 수신자는 1명이라 dedup 불필요. 향후 "대댓글도 글작성자에게" 요구가 오면 `resolveRecipient`를 `resolveRecipients(): Set<Long>`로 바꾸고 `set.filterNot{ == actor || optedOut || blocked }.forEach{ save }` — `Set`이 (글작성자==부모작성자) 케이스를 1건으로 자동 병합.

### M1. `CommentCommandService.kt` (수정)
```kotlin
@Service
@Transactional
class CommentCommandService(
    private val commentCommandRepository: CommentCommandRepository,
    private val commentQueryRepository: CommentQueryRepository,
    private val commentValidator: CommentValidator,
    private val commentNotificationRegistrar: CommentNotificationRegistrar,   // ← 추가
) {
    fun create(newComment: NewComment): Long {
        commentValidator.validate(newComment)
        val commentId = commentCommandRepository.save(newComment)
        commentNotificationRegistrar.register(newComment, commentId)          // ← 추가 (동일 tx, D10)
        return commentId
    }
    // delete(...) 변경 없음
}
```

### N12. `CommentNotificationSettingCommandService.kt` (community)
```kotlin
package com.konkuk.ma.domain.community.application

import com.konkuk.ma.domain.community.domain.port.CommentNotificationSettingRepository
import com.konkuk.ma.domain.community.domain.port.PostQueryRepository
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.exception.EntityType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CommentNotificationSettingCommandService(
    private val postQueryRepository: PostQueryRepository,
    private val commentNotificationSettingRepository: CommentNotificationSettingRepository,
) {
    fun set(memberId: Long, postId: Long, enabled: Boolean) {
        if (!postQueryRepository.exists(postId)) {
            throw EntityNotFoundException(EntityType.COMMUNITY_POST, postId.toString())
        }
        if (enabled) commentNotificationSettingRepository.optIn(memberId, postId)
        else commentNotificationSettingRepository.optOut(memberId, postId)
    }
}
```

### N13. `NotificationTable.kt` (infra)
```kotlin
package com.konkuk.ma.domain.notification.entity.table

import com.konkuk.ma.domain.common.entity.table.BaseTable

object NotificationTable : BaseTable("NOTIFICATIONS", "NOTIFICATION_ID") {
    val recipientId = long("RECIPIENT_ID").index()
    val type = varchar("TYPE", 32)                 // 커뮤니티 관례: varchar + name/valueOf
    val actorId = long("ACTOR_ID")
    val postId = long("POST_ID")
    val commentId = long("COMMENT_ID")
    val read = bool("IS_READ").clientDefault { false }
}
```

### N19. `CommentNotificationSettingTable.kt` (infra)
```kotlin
package com.konkuk.ma.domain.community.entity.table

import com.konkuk.ma.domain.common.entity.table.BaseTable

object CommentNotificationSettingTable :
    BaseTable("COMMUNITY_COMMENT_NOTIFICATION_SETTINGS", "COMMENT_NOTIFICATION_SETTING_ID") {
    val memberId = long("MEMBER_ID")
    val postId = long("POST_ID")

    init { uniqueIndex(memberId, postId) }
}
```

### N16. `NotificationQueryDao.kt` (핵심 seek 쿼리)
```kotlin
// findByRecipient: 게시글 목록(PostQueryDao) 커서 패턴 미러
fun findByRecipient(recipientId: Long, cursor: CursorIdCondition): CursorResult<List<Notification>> {
    var condition: Op<Boolean> = (NotificationTable.recipientId eq recipientId) and
        (NotificationTable.deleted eq false)
    cursor.cursorId?.let { condition = condition and (NotificationTable.id less it) }

    val rows = NotificationTable.selectAll().where { condition }
        .orderBy(NotificationTable.id to SortOrder.DESC)
        .limit(cursor.size)
        .map { NotificationEntity.from(it).toDomain() }

    return CursorResult.of(rows, cursor.size) { it.id }
}

fun countUnread(recipientId: Long): Long =
    NotificationTable.selectAll().where {
        (NotificationTable.recipientId eq recipientId) and
        (NotificationTable.read eq false) and (NotificationTable.deleted eq false)
    }.count()
```
> `activeRows{}` 헬퍼가 있으면 그것으로 대체(BaseTable 관례). `markAllAsRead`는 `update({recipientId eq ? and read eq false}){ it[read]=true }`.

### N22 / N23. Boot API
```kotlin
@RestController
@RequestMapping("/api/notifications")
class NotificationQueryApi(
    private val notificationQueryService: NotificationQueryService,
) {
    @GetMapping
    fun find(
        @LoginMember memberInfo: MemberInfo,
        @RequestParam(required = false) cursorId: Long?,
        @RequestParam(required = false) size: Int?,
    ): CursorResponse<List<NotificationResponse>> {
        val result = notificationQueryService.find(memberInfo.id, CursorIdCondition.of(cursorId, size))
        return CursorResponse(
            data = result.data.map { NotificationResponse.from(it) },
            hasNext = result.hasNext,
            nextCursorId = result.nextCursorId,
        )
    }

    @GetMapping("/unread-count")
    fun countUnread(@LoginMember memberInfo: MemberInfo): UnreadCountResponse =
        UnreadCountResponse(notificationQueryService.countUnread(memberInfo.id))
}

@RestController
@RequestMapping("/api/notifications")
class NotificationCommandApi(
    private val notificationCommandService: NotificationCommandService,
) {
    @PatchMapping("/{notificationId}/read")
    fun markAsRead(@LoginMember memberInfo: MemberInfo, @PathVariable notificationId: Long) {
        notificationCommandService.markAsRead(notificationId, memberInfo.id)
    }

    @PatchMapping("/read-all")
    fun markAllAsRead(@LoginMember memberInfo: MemberInfo) {
        notificationCommandService.markAllAsRead(memberInfo.id)
    }
}
```

### N26 / N27. 커뮤니티 설정 API
```kotlin
@RestController
@RequestMapping("/api/community/posts/{postId}/comment-notification")
class CommentNotificationSettingApi(
    private val commentNotificationSettingCommandService: CommentNotificationSettingCommandService,
) {
    @PatchMapping
    fun set(
        @LoginMember memberInfo: MemberInfo,
        @PathVariable postId: Long,
        @Valid @RequestBody request: CommentNotificationSettingRequest,
    ) {
        commentNotificationSettingCommandService.set(memberInfo.id, postId, request.enabled)
    }
}

class CommentNotificationSettingRequest(
    @field:NotNull val enabled: Boolean,
)
```

### N24. `NotificationResponse.kt`
```kotlin
class NotificationResponse(
    val id: Long,                    // raw (D5)
    val type: NotificationType,
    val actorId: Long,
    val actorNickname: String,
    val postId: Long,
    val commentId: Long,
    val read: Boolean,
    val createdDate: LocalDateTime,
) {
    companion object {
        fun from(v: NotificationWithActor): NotificationResponse = with(v.notification) {
            NotificationResponse(id, type, actorId, v.actorNickname, postId, commentId, read, createdDate)
        }
    }
}
```

### M3. `EntityType.kt` (수정)
```kotlin
NOTIFICATION("Notification", "id"),   // 추가
```

---

## 10. 탈퇴 연쇄 영향 (M2)

`MemberDataCleaner`(도메인 `withdrawal`)에 커뮤니티/매칭 등과 동일한 방식으로 알림 정리 추가. **포트 인터페이스가 아니라 중앙 클래스 직접 편집**(F9).

```kotlin
@Component
class MemberDataCleaner(
    /* ...기존 주입... */
    private val notificationCommandRepository: NotificationCommandRepository,               // 추가
    private val commentNotificationSettingRepository: CommentNotificationSettingRepository,  // 추가
) {
    fun clean(member: Member) {
        cleanAuth(member); cleanMatching(member); cleanPoint(member)
        cleanCommunity(member); cleanNotification(member)   // ← 추가 (anonymize 전)
        cleanXroom(member); cleanPhoto(member)
        anonymizeMember(member)
    }

    private fun cleanNotification(member: Member) {
        notificationCommandRepository.deleteByMember(member.id)               // 내가 받은 알림 제거
        commentNotificationSettingRepository.deleteByMember(member.id)        // 내 알림 설정 제거
    }
}
```
- **정리 범위**: `RECIPIENT_ID = 탈퇴회원`인 알림 + `MEMBER_ID = 탈퇴회원`인 설정만 삭제. 탈퇴회원이 **actor**로 남아있는 타인의 알림은 보존(글/댓글이 anonymize로 남는 것과 동일 정책 — read 시 닉네임은 `"알 수 없음"`으로 표시, N7).
- **백업 생략(D11)**: 알림은 이미 백업되는 글/댓글의 파생·비분쟁증거라 `MemberWithdrawalBackupCollector`/`MemberWithdrawalBackup` **미변경**. (분쟁증거로 판단이 바뀌면 그때 collector에 `find(memberId)` + carrier 필드 추가.)
- `deleteByMember`는 like 정리(`postLikeRepository.deleteByMember`)와 동일 성격의 **하드 삭제**.

---

## 11. 차단(block) 처리 결정 (D8 상세)

**결정: 수신자가 행위자(actor)를 차단한 경우 알림을 생성하지 않는다.** (`findBlockedMemberIds(recipientId).contains(actorId)`)

| 관점 | 실측 | 판단 |
|------|------|------|
| block 방향 | `Block(blockerId → blockedId)`, `findBlockedMemberIds(blockerId)` = "내가 차단한 사람"(F4) | 수신자 관점으로 조회: `findBlockedMemberIds(recipientId)` |
| 기존 렌더링 | 댓글 목록은 차단자의 댓글을 **마스킹**("차단한 사용자의 댓글입니다.", row 유지). 게시글 목록은 **완전 제외**. block은 **read-side 전용**, write는 미조회. | 알림 = 수신자가 소비하는 **push 콘텐츠** → read-side 선호도(마스킹/제외)와 정렬 |
| 채택 | — | **수신자가 차단한 actor의 활동 알림은 생략.** 차단은 "이 사람과의 상호작용을 원치 않음" 표현이고, 마스킹으로 콘텐츠까지 가리는데 알림으로 그 사람 활동을 능동적으로 밀어주는 건 모순. |
| 미채택(역방향) | actor가 수신자를 차단한 경우 | 수신자는 보호 대상이 아니므로 **알림 정상 발송**. 역방향 조회 불필요. |
| 뉘앙스 | 차단자의 댓글은 스레드엔 (마스킹된 채) **여전히 보임** | 알림(능동 push)은 생략하되 스레드 표시(수동)는 유지 — 일관. 리뷰 시 재확인. |

> 대안: "댓글은 마스킹만 하니 알림도 그대로 보내자"도 가능하나, 차단 UX상 push 억제가 안전. 위 표로 결정을 고정.

---

## 12. 구현 순서 (TDD: RED → GREEN, 의존성 순서)

각 단계 **테스트 선작성 → RED 확인 → 구현 → GREEN 확인**. 테스트는 KoTest+Mockk(도메인/서비스), `@BaseApiTest`(웹). 순서는 컴파일 의존성 순.

| Step | 대상 | 내용 | RED 기준 |
|------|------|------|----------|
| **1** | notification 도메인 모델 | (T) `NotificationTest`: `validateRecipient` 성공/403 → `NotificationType`(N1)·`Notification`(N2)·`NewNotification`(N3)·`NotificationWithActor`(N4) 구현 → GREEN. `EntityType.NOTIFICATION`(M3) 추가. | 클래스/enum 미존재 컴파일 실패 |
| **2** | notification 포트 | `NotificationCommandRepository`(N5)·`NotificationQueryRepository`(N6) 인터페이스 정의 | — (컴파일 정합) |
| **3** | notification read 서비스 | (T) `NotificationQueryServiceTest`: 커서 결과 닉네임 enrich·페이징 플래그 보존·안읽음 카운트(mock) → RED → `NotificationQueryService`(N7) → GREEN | `find`/`countUnread` 미존재 |
| **4** | notification cmd 서비스 | (T) `NotificationCommandServiceTest`: `markAsRead` 정상/타인알림 403/부재 404, `markAllAsRead`(mock) → RED → `NotificationCommandService`(N8) → GREEN | `markAsRead` 미존재 |
| **5** | community 설정 모델/포트 | `CommentNotificationSetting`(N9)·`CommentNotificationSettingRepository`(N10) | — |
| **6** | Registrar | (T) `CommentNotificationRegistrarTest`(mock): 루트→글작성자/대댓글→부모작성자/자기제외/설정off/차단제외/정상저장 6케이스 → RED → `CommentNotificationRegistrar`(N11) → GREEN | `register` 미존재 |
| **7** | 댓글 서비스 훅 | (T) `CommentCommandServiceTest`에 "create가 registrar.register 호출" 검증 추가(mock) → RED → `CommentCommandService`(M1) 주입+호출 → GREEN | registrar 미주입 |
| **8** | 설정 토글 서비스 | (T) `CommentNotificationSettingCommandServiceTest`: enabled=false→optOut / true→optIn / 게시글 부재 404(mock) → RED → `CommentNotificationSettingCommandService`(N12) → GREEN | `set` 미존재 |
| **9** | 인프라 (notification) | N13~N18 구현. (T) `NotificationQueryDaoTest`: `findByRecipient` id DESC·커서·deleted 제외·hasNext / `countUnread` / `NotificationCommandDaoTest`: save·markAsRead·markAllAsRead·deleteByMember(recipient만) → RED → 구현 → GREEN | 테이블/메서드 미존재 |
| **10** | 인프라 (설정) | N19~N21. (T) `CommentNotificationSettingDaoTest`: optOut(idempotent)·isOptedOut·optIn·deleteByMember·유니크 → RED → 구현 → GREEN | 테이블 미존재 |
| **11** | DDL | `ddl.sql`(M4, §6) 테이블 2개 추가 (Step9/10 통합테스트 스키마 사용) | 스키마 불일치 |
| **12** | 웹 계층 | N22~N27. (T) `NotificationQueryApiTest`(목록/카운트)·`NotificationCommandApiTest`(read 200/403/404, read-all 200)·`CommentNotificationSettingApiTest`(200/404/400) → RED → 구현 → GREEN | 엔드포인트 404 |
| **13** | 탈퇴 연쇄 | (T) `MemberDataCleanerTest`에 알림·설정 `deleteByMember` 호출 검증 추가 → `MemberDataCleaner`(M2) → GREEN | 미호출 |
| **14** | REST Docs | §13 — 스니펫 5종 + `notification.adoc` + main.adoc 링크 | — |
| **15** | 문서/정리 | `api-todo.md`(§14) 이관, `./gradlew build` 전체 GREEN, code-reviewer 반영 | — |

---

## 13. REST Docs (rest-docs-generator 수행 — 계획에만 명시)

- 신규 섹션 `boot/ma-boot-web/src/docs/asciidoc/notification/`:
  - `find-notifications.adoc`, `unread-count.adoc`, `read-notification.adoc`, `read-all-notifications.adoc` — 각 `include::{snippets}/notification/<api>/...`.
- 커뮤니티 섹션: `asciidoc/community/set-comment-notification.adoc` 추가.
- 각 API 테스트에 문서화 스니펫:
  - A1: `queryParameters`(cursorId,size) + `responseFields`(data[].*, hasNext, nextCursorId) + `andDocument("notification/find-notifications")`.
  - A3/A4/A5: `pathParameters`(+ A5 `requestFields enabled`) + 성공 200/실패(403/404).
- `boot/ma-boot-web/src/docs/asciidoc/main.adoc`: `[[notification]] == 알림` 섹션 신설(목록/안읽음/읽음/전체읽음 `link:`), `[[community]]`에 "댓글 알림 설정" `link:` 1줄 추가.
- Vocabulary: id는 raw Long이라 기존 encode Vocabulary 불필요. `notification` 응답 필드 정의 함수 신설(rest-docs-writing 관례).

---

## 14. api-todo.md 갱신 (M5, `docs/api-todo.md`)

- `# 📋 TODO > ## 커뮤니티 > ### 게시글에 댓글 알림 설정` 항목 **제거**(완료 이관, 이관 주석).
- `# ✅ 완료된 API`에 **`### 알림` 섹션 신설** + 표:

  | Method | Endpoint | 용도 |
  |--------|----------|------|
  | GET | /api/notifications | 알림 목록 조회 |
  | GET | /api/notifications/unread-count | 안읽음 개수 |
  | PATCH | /api/notifications/{id}/read | 알림 읽음 |
  | PATCH | /api/notifications/read-all | 전체 읽음 |

- `### 커뮤니티` 완료 표에 1행 추가:

  | PATCH | /api/community/posts/{postId}/comment-notification | 게시글 댓글 알림 on/off |

---

## 15. 리스크 / 주의

- **[핵심] 트랜잭션 결합(D10)**: 알림 생성이 댓글 작성과 동일 tx. 알림 저장 실패 시 댓글도 롤백된다. MVP는 일관성 우선으로 수용하되, "알림 실패가 댓글을 막으면 안 됨" 요구가 생기면 `@TransactionalEventListener(AFTER_COMMIT)` 또는 async로 분리(이벤트 선례 전무하므로 도입 시 선례를 만드는 셈 — §16).
- **[성능] 댓글 hot path에 조회 추가**: `register`가 수신자 해석(post/parent 1건) + 설정 조회 1건 + 차단 조회 1건 + insert 1건. 전부 인덱스 조회라 경미. 대댓글은 부모 재조회(`CommentValidator`가 이미 로드 후 버림 — F2)로 **중복 1회**. 필요 시 validator가 부모 Comment를 반환하도록 리팩터(§16).
- **id raw 노출(D5)**: notificationId/postId/commentId를 raw Long로 노출. 커뮤니티와 정합. 커뮤니티가 추후 난독화로 전환하면 알림도 함께 전환(그 전엔 raw 유지가 정답 — 불일치가 더 위험).
- **차단 뉘앙스(§11)**: 마스킹된 댓글은 스레드에 남지만 알림은 생략 — 리뷰에서 UX 재확인.
- **표시 데이터 최소화(D7)**: post title/댓글 preview 미저장. 클라이언트는 `type` + `actorNickname` + `postId`로 렌더/내비게이션. 리치 프리뷰 필요 시 후속.
- **삭제된 리소스 알림**: 댓글/게시글이 알림 후 삭제되면 목록엔 남고 내비게이션 시 404 가능. MVP 방치, 후속에서 read 시 필터 or 생성 시 스냅샷.
- **FK 없음(F7)**: 알림·설정은 BIGINT 참조 + 보조 인덱스만. `feedback_no_fk` 준수.
- **테스트 인증**: 웹 테스트는 `@BaseApiTest`/`@WithAuthMember` 기본값 사용, 403 케이스는 알림 `recipientId`를 인증 memberId와 다르게 픽스처 구성.

---

## 16. 범위 밖 (후속으로만 기록)

- **푸시(FCM)**: 알림 레코드 생성 직후 push 발송. 인박스 모델은 그대로, 발송 어댑터만 추가.
- **claim 수락/거절 알림**: `matching` 발원. `NotificationType`에 값 추가 + `ClaimNotificationRegistrar`(matching) 신설 → **같은 `notification` 인박스 재사용**(본 도메인 배치의 이유, §5.2).
- **이벤트 기반 분리**: `ApplicationEventPublisher` 도입으로 댓글 tx와 알림 발행 디커플. 선례를 새로 만드는 작업이라 별도 논의.
- **리치 알림**: post title / 댓글 preview 비정규화, 삭제 리소스 필터, 그룹핑("외 3명").
- **설정 확장**: 게시글 단위 외 "전체 댓글 알림 off"(회원 단위), 대댓글/좋아요 등 타입별 on/off.
- **부모 재조회 제거**: `CommentValidator.validate`가 부모 Comment를 반환하도록 바꿔 Registrar 중복 조회 제거.
- **읽음 응답 바디**: A3/A4를 200 무바디 대신 갱신된 unread-count 반환(프론트 왕복 절감).

---

## 17. 구현 시 참조 (규칙 본문은 복제하지 않음)

- 모듈·패키지 배치: [[clean-architecture]] (신규 `notification` top-level 도메인, 어댑터 패키지 미러링, 설정은 community 잔류)
- 도메인 모델링/경계: [[domain-driven-design]] (notification = generic subdomain, 인박스 애그리거트, 발원 도메인이 적재)
- 객체/서비스 구현: [[code-implementation-rules]] (Service 조합만, 도메인 행위 부여 `validateRecipient`, 포트 규칙, @Component 협력자 `Registrar`)
- 가독성/네이밍: [[clean-code]]
- 테스트: [[kotest-writing]] (KoTest + Mockk, `@BaseApiTest`, `@WithAuthMember`)
- API 문서화: [[rest-docs-writing]] (Vocabulary 재사용, main.adoc `[[notification]]` 섹션 연결)
- 도메인 배치 근거 상세: `docs/plan/complete/support-inquiry-create.plan.md`(§5 override 선례), `domain/.../withdrawal/`(크로스컷팅 top-level 선례)
