# Backend API To-Do List

## 완료된 API

| Method | Endpoint | 용도 | 상태 |
|--------|----------|------|------|
| POST | /api/auth/sign-up | 회원가입 | Done |
| POST | /api/auth/login | 로그인 | Done |
| POST | /api/auth/refresh-token | 토큰 갱신 | Done |
| POST | /api/sms/verification-code | SMS 인증코드 전송 | Done |
| POST | /api/sms/verification-code/confirm | SMS 인증코드 확인 | Done |
| POST | /api/members/duplicated-nickname | 닉네임 중복 확인 | Done |
| POST | /api/members/duplicated-email | 이메일 중복 확인 | Done |
| POST | /api/target-infos | 찾는 사람 정보 등록 | Done |
| POST | /api/members/photos | 프로필 사진 업로드 | Done |
| DELETE | /api/members/photos | 프로필 사진 삭제 | Done |
| GET | /api/matching-results | 매칭 결과 목록 조회 | Done |
| GET | /api/matching-results/{id} | 매칭 결과 상세 조회 | Done |
| PATCH | /api/matching-results/{id}/exclude | 매칭 상대 제외(차단) | Done |
| PATCH | /api/matching-results/{id}/include | 매칭 상대 제외 해제 | Done |
| GET | /api/matching-results?excluded=true | 제외된 매칭 결과 조회 | Done |
| GET | /api/community/posts | 게시글 목록 조회 | Done |

---

## 매칭

> 작업할 내용 없음

---

## 커뮤니티

### POST /api/community/posts — 게시글 작성

- **인증**: 필요
- **설명**: 커뮤니티에 새 게시글을 작성한다

**Request Body**:
```json
{
  "category": "SUCCESS_STORY | CHEER | COUNSELING",
  "title": "string (max 40자)",
  "content": "string"
}
```

**참고사항**:
- category는 enum으로 관리

### GET /api/community/posts/{id} — 게시글 상세

- **인증**: 필요

### POST /api/community/posts/{postId}/like — 좋아요 토글

- **인증**: 필요
- **설명**: 게시글에 좋아요를 토글한다

### GET /api/community/posts/{postId}/comments — 댓글 목록

- **인증**: 필요

### POST /api/community/posts/{postId}/comments — 댓글 작성

- **인증**: 필요

**Request Body**:
```json
{ "content": "string" }
```

### DELETE /api/community/posts/{postId}/comments/{commentId} — 댓글 삭제

- **인증**: 필요

### 댓글 좋아요 토글

### 게시글에 댓글 알림 설정

---

## X룸/추억

### GET /api/xroom/status — X룸 존재 여부 확인

- **인증**: 필요

### POST /api/xroom — X룸 생성 (템플릿 선택)

- **인증**: 필요

### GET /api/xroom/templates — 템플릿 목록

- **인증**: 필요

### 추억 CRUD

- **인증**: 필요

**데이터 구조**:
```json
{
  "title": "string",
  "date": "YYYY.MM.DD",
  "content": "string",
  "photo": "string (파일명)",
  "mood": "string (예: 설렘, 그리움)"
}
```

| Method | Endpoint | 용도 |
|--------|----------|------|
| GET | /api/memories | 추억 목록 조회 |
| POST | /api/memories | 추억 등록 |
| PUT | /api/memories/{id} | 추억 수정 |
| DELETE | /api/memories/{id} | 추억 삭제 |

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

### POST /api/support/inquiries — 1:1 문의 접수

- **인증**: 필요

**Request Body**:
```json
{ "title": "string (max 50자)", "content": "string" }
```

### 회원 탈퇴

- **인증**: 필요

**참고사항**:
- 비밀번호 확인 추가 필요할 수 있음

### 로그아웃

- **인증**: 필요

**참고사항**:
- 토큰 무효화 처리
