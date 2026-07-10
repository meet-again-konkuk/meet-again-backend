# Backend API To-Do List

---

# ✅ 완료된 API

### 인증

| Method | Endpoint | 용도 |
|--------|----------|------|
| POST | /api/auth/sign-up | 회원가입 |
| POST | /api/auth/login | 로그인 |
| POST | /api/auth/refresh-token | 토큰 갱신 |
| POST | /api/auth/logout | 로그아웃 (refresh token 삭제, 204) |
| POST | /api/sms/verification-code | SMS 인증코드 전송 |
| POST | /api/sms/verification-code/confirm | SMS 인증코드 확인 |

### 회원

| Method | Endpoint | 용도 |
|--------|----------|------|
| POST | /api/members/nickname/exists | 닉네임 중복 확인 |
| POST | /api/members/email/exists | 이메일 중복 확인 |
| POST | /api/members/photos | 프로필 사진 업로드 |
| DELETE | /api/members/photos | 프로필 사진 삭제 |
| POST | /api/members/withdrawal | 회원 탈퇴 신청 (비밀번호 검증, 7일 유예) |
| POST | /api/members/withdrawal/cancellation | 탈퇴 복구 (public, email/password, 204) |

### 매칭

| Method | Endpoint | 용도 |
|--------|----------|------|
| POST | /api/target-infos | 찾는 사람 정보 등록 |
| GET | /api/target-infos | 찾는 사람 목록 조회 |
| GET | /api/target-infos/{targetInfoId} | 찾는 사람 상세 조회 |
| PUT | /api/target-infos/{targetInfoId} | 찾는 사람 정보 수정 |
| DELETE | /api/target-infos/{targetInfoId} | 찾는 사람 정보 삭제 |
| GET | /api/matching-results | 매칭 결과 목록 조회 |
| GET | /api/matching-results/{id} | 매칭 결과 상세 조회 |
| GET | /api/matching-results?excluded=true | 제외된 매칭 결과 조회 |
| PATCH | /api/matching-results/{id}/exclude | 매칭 상대 제외(차단) |
| PATCH | /api/matching-results/{id}/include | 매칭 상대 제외 해제 |
| PATCH | /api/matching-results/{id}/claim | 매칭 결과 Claim |
| GET | /api/claimers/me | 나를 Claim한 요청자 목록 조회 |

### 커뮤니티

| Method | Endpoint | 용도 |
|--------|----------|------|
| GET | /api/community/posts | 게시글 목록 조회 |
| GET | /api/community/posts/{id} | 게시글 상세 조회 |
| POST | /api/community/posts | 게시글 작성 |
| PATCH | /api/community/posts/{postId} | 게시글 수정 (REQ-012) |
| DELETE | /api/community/posts/{postId} | 게시글 삭제 (REQ-012) |
| POST | /api/community/posts/{postId}/image | 게시글 이미지 업로드/교체 (REQ-013, multipart) |
| DELETE | /api/community/posts/{postId}/image | 게시글 이미지 삭제 (REQ-013) |
| POST | /api/community/posts/{postId}/likes | 게시글 좋아요 추가 |
| DELETE | /api/community/posts/{postId}/likes | 게시글 좋아요 취소 |
| POST | /api/community/posts/{postId}/comments | 댓글/대댓글 작성 |
| GET | /api/community/comments/{commentId} | 댓글 상세 조회 |
| DELETE | /api/community/posts/{postId}/comments/{commentId} | 댓글 삭제 |
| POST | /api/community/comments/{commentId}/likes | 댓글 좋아요 추가 |
| DELETE | /api/community/comments/{commentId}/likes | 댓글 좋아요 취소 |
| POST | /api/community/posts/{postId}/reports | 게시글 신고 (REQ-014) |
| POST | /api/community/comments/{commentId}/reports | 댓글 신고 (REQ-014) |
| POST | /api/community/posts/{postId}/author/block | 게시글 작성자 차단 (REQ-014) |
| POST | /api/community/comments/{commentId}/author/block | 댓글 작성자 차단 (REQ-014) |
| GET | /api/community/blocks | 차단 목록 조회 (REQ-014) |
| DELETE | /api/community/blocks/{blockId} | 차단 해제 (REQ-014) |

### 고객지원

| Method | Endpoint | 용도 |
|--------|----------|------|
| POST | /api/domain/inquiries | 1:1 문의 접수 |

### 인연

| Method | Endpoint | 용도 |
|--------|----------|------|
| GET | /api/points | 인연 상품 목록 조회 |
| POST | /api/points | 인연 충전 (구매) |

### X룸 (기억의 방)

| Method | Endpoint | 용도 |
|--------|----------|------|
| POST | /api/xrooms | 방 생성 |
| GET | /api/xrooms/me | 내가 만든 방 목록 조회 |
| GET | /api/xrooms/received | 내가 수신한 방 목록 조회 |
| GET | /api/xrooms/{xroomId} | 방 상세 조회 (기억·사진 포함) |
| PATCH | /api/xrooms/{xroomId} | 끝맺음 메시지 수정 |
| POST | /api/xrooms/{xroomId}/memories | 기억 추가 |
| PATCH | /api/xrooms/{xroomId}/memories/{memoryId} | 기억 수정 |
| DELETE | /api/xrooms/{xroomId}/memories/{memoryId} | 기억 삭제 |
| POST | /api/xrooms/{xroomId}/memories/{memoryId}/photo | 기억 사진 업로드/교체 |
| DELETE | /api/xrooms/{xroomId}/memories/{memoryId}/photo | 기억 사진 삭제 |

---

# 📋 TODO

## 매칭

### X 거절하기

- **인증**: 필요

**참고사항**:
- 받아주기(claim)는 구현 완료 (`PATCH /api/matching-results/{id}/claim`) — 거절만 남음
- 착수 전 스펙 결정 필요: 수신자가 claim을 명시적으로 거부하는 별도 상태인지, 등록자 쪽 exclude(제외)로 충분한지 프론트와 확인

---

## 커뮤니티

### 게시글에 댓글 알림 설정

---

## X룸 (기억의 방)

작업할 내용 없음 (기억의 방 재설계 Phase 0~3 완료 — 위 "완료된 API" 참조)

---

## 인연 (앱 내 재화)

### point 도메인 제거 (2026-07-10 결정)

앱에 결제 요소가 적어 별도 재화(인연)를 유지하지 않기로 결정. point 도메인 전체를 제거한다.

**결정 근거**:
- 인연을 실제로 차감(소비)하는 기능이 0곳 (매칭 claim 포함 참조 없음)
- 프론트엔드에서 /api/points 참조 0건 — API 계약 파기 부담 없음
- PG사·소비 단위·동시성 제어 전부 미결정 — 유료화 시점에 요구사항이 달라질 가능성 높음
- 탈퇴 배치 백업/정리, Redis 캐시 설정, ma-payment-core 모듈 등 유지보수 비용 발생 중

**제거 범위** (실측):
- point 도메인 프로덕션 코드 59파일 + 테스트 20파일
- `ma-payment-core` 모듈 통째
- 접점: 탈퇴 백업/정리 3곳(MemberWithdrawalBackup·Collector·MemberDataCleaner), GlobalExceptionHandler, CachedDiscountJacksonConfig, DDL 테이블 ~6개, REST Docs(points), 완료된 API 테이블의 인연 2건

**진행 순서**:
1. `archive/point-domain` 보존 브랜치 생성 (제거 전 develop 시점) — 복원 시 레퍼런스
2. `/plan`으로 제거 계획 수립 (핵심 리스크: 탈퇴 배치 연쇄, DDL DROP 처리)
3. 단일 PR로 제거 (`refactor/remove-point-domain`)
4. DROP TABLE은 마이그레이션 스크립트로만 준비, 실행은 배포 시점 판단

<!-- 기존 TODO(GET /api/points/me 잔액 조회, PG 연동)는 제거 결정으로 폐기 -->
<!-- 완료된 API의 GET/POST /api/points 2건은 제거 PR에서 테이블에서 삭제 예정 -->

---

## 아이디/비밀번호 찾기

### POST /api/auth/find-id — 이메일 찾기

- **인증**: 불필요

**Request Body**:
```json
{ "name": "string", "phone": "string" }
```

### POST /api/auth/find-password — 비밀번호 재설정 요청

- **인증**: 불필요

**Request Body**:
```json
{ "email": "string", "name": "string", "phone": "string" }
```

---

## 기타

작업할 내용 없음 (로그아웃 구현 완료 — 위 "완료된 API > 인증" 참조)

<!-- 회원 탈퇴는 구현 완료되어 완료된 API 테이블로 이동 (신청 시 비밀번호 검증 포함, PR #17) -->

---

## 테스트 보완

### API 실패 테스트 케이스 추가

## 데이터 정합성

> 작업할 내용 없음 (X룸 media 파일 생명주기 정리 완료 — 탈퇴 연쇄 soft delete + soft-deleted media 물리파일 purge 배치, PR #39)


---

## 템플릿

<!-- 아래를 복사하여 해당 도메인 섹션에 붙여넣기 -->

```
### 제목 : 
- **인증**: 필요 / 불필요

**상세내용**:
- 특이사항 기술
```
