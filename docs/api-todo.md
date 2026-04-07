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

---

## 매칭

- ~~/api/matching-results 매칭 결과 목록에 exclude 된 대상은 필터 되고 조회하도록 수정~~ ✔ 구현 완료
- 필터 된 매칭 결과 리스트만 조회 하는 기능 필요.

---

## 커뮤니티

### POST /api/community/posts — 게시글 작성
- 상태: TODO
- Request Body:
```json
{
  "category": "성공 스토리 | 응원 | 고민상담",
  "title": "string (max 40자)",
  "content": "string"
}
```

### GET /api/community/posts — 게시글 목록 조회
- 상태: TODO
- Query Params: `category={category}&page={page}`
- Response:
```json
{
  "posts": [{
    "id": "int",
    "nickname": "string",
    "category": "성공 스토리 | 응원 | 고민상담",
    "title": "string",
    "content": "string",
    "likes": "int",
    "comments": "int",
    "timeAgo": "string"
  }]
}
```

### GET /api/community/posts/{id} — 게시글 상세
- 상태: TODO

### POST /api/community/posts/{postId}/like — 좋아요 토글
- 상태: TODO
- PathVariable: postId

### GET /api/community/posts/{postId}/comments — 댓글 목록
- 상태: TODO

### POST /api/community/posts/{postId}/comments — 댓글 작성
- 상태: TODO
- Request Body:
```json
{ "content": "string" }
```

### DELETE /api/community/posts/{postId}/comments/{commentId} — 댓글 삭제
- 상태: TODO

---

## X룸/추억

### GET /api/xroom/status — X룸 존재 여부 확인
- 상태: TODO

### POST /api/xroom — X룸 생성 (템플릿 선택)
- 상태: TODO

### GET /api/xroom/templates — 템플릿 목록
- 상태: TODO

### 추억 CRUD
기반 데이터 구조 (dummy_memories.dart):
```json
{
  "title": "string",
  "date": "YYYY.MM.DD",
  "content": "string",
  "photo": "string (파일명)",
  "mood": "string (예: 설렘, 그리움)"
}
```

| Method | Endpoint | 용도 | 상태 |
|--------|----------|------|------|
| GET | /api/memories | 추억 목록 조회 | TODO |
| POST | /api/memories | 추억 등록 | TODO |
| PUT | /api/memories/{id} | 추억 수정 | TODO |
| DELETE | /api/memories/{id} | 추억 삭제 | TODO |

---

## 아이디/비밀번호 찾기

### POST /api/auth/find-id — 이메일 찾기
- 상태: TODO
- Request Body:
```json
{ "name": "string", "phone": "string" }
```

### POST /api/auth/find-password — 비밀번호 재설정 요청
- 상태: TODO
- Request Body:
```json
{ "email": "string", "name": "string", "phone": "string" }
```

---

## 기타

### POST /api/support/inquiries — 1:1 문의 접수
- 상태: TODO
- Request Body:
```json
{ "title": "string (max 50자)", "content": "string" }
```

### POST /api/auth/withdraw — 회원 탈퇴
- 상태: TODO
- 비밀번호 확인 추가 필요할 수 있음

### POST /api/auth/logout — 로그아웃 (토큰 무효화)
- 상태: TODO
