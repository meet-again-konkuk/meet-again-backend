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
| POST | /api/community/posts | 게시글 작성 | Done |
| POST | /api/community/posts/{postId}/comments | 댓글/대댓글 작성 | Done |

---

## 매칭

- ~~MatchingResult를 NewMatchingResult / MatchingResult로 분리 (생성/조회 분리 일관성)~~ ✔ 완료

---

## 커뮤니티

### GET /api/community/posts/{id} — 게시글 상세

- **인증**: 필요
  **참고사항**:
- 댓글 목록을 같이 조회해오는데 대댓글은 최신순으로 세 개까지만 내용을 가져오고 그 외에는 개수만 표시
- 댓글 응답 값은 닉네임, 댓글 내용, 좋아요 개수, 작성 경과 시간 등이 표기

### POST /api/community/posts/{postId}/like — 좋아요 토글

- **인증**: 필요
- **설명**: 게시글에 좋아요를 토글한다

### GET /api/community/posts/{postId}/comments — 댓글 목록

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

---

## 테스트 보완

### API 실패 테스트 케이스 추가
- SignUpApiTest — 유효성 검증 실패 (이메일/비밀번호/닉네임 형식 오류)
- MemberPhotoApiTest — 파일 관련 실패 케이스
- MatchingResultCommandApiTest — 소유권 검증 실패

### 도메인 객체 실패 테스트 케이스 추가
- MemberPhotoServiceTest — 예외 전파 케이스
- MemberPhotoProcessorTest — 파일 처리 실패 케이스
- PostCommandServiceTest — 예외 전파 케이스
- PostQueryServiceTest — 예외 전파 케이스
- StoragePathTest — 잘못된 입력 검증
