# Backend API To-Do List

---

# ✅ 완료된 API

### 인증

| Method | Endpoint | 용도 |
|--------|----------|------|
| POST | /api/auth/sign-up | 회원가입 |
| POST | /api/auth/login | 로그인 |
| POST | /api/auth/refresh-token | 토큰 갱신 |
| POST | /api/sms/verification-code | SMS 인증코드 전송 |
| POST | /api/sms/verification-code/confirm | SMS 인증코드 확인 |

### 회원

| Method | Endpoint | 용도 |
|--------|----------|------|
| POST | /api/members/nickname/exists | 닉네임 중복 확인 |
| POST | /api/members/email/exists | 이메일 중복 확인 |
| POST | /api/members/photos | 프로필 사진 업로드 |
| DELETE | /api/members/photos | 프로필 사진 삭제 |

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

### 커뮤니티

| Method | Endpoint | 용도 |
|--------|----------|------|
| GET | /api/community/posts | 게시글 목록 조회 |
| GET | /api/community/posts/{id} | 게시글 상세 조회 |
| POST | /api/community/posts | 게시글 작성 |
| POST | /api/community/posts/{postId}/likes | 게시글 좋아요 추가 |
| DELETE | /api/community/posts/{postId}/likes | 게시글 좋아요 취소 |
| POST | /api/community/posts/{postId}/comments | 댓글/대댓글 작성 |
| GET | /api/community/comments/{commentId} | 댓글 상세 조회 |
| DELETE | /api/community/posts/{postId}/comments/{commentId} | 댓글 삭제 |
| POST | /api/community/comments/{commentId}/likes | 댓글 좋아요 추가 |
| DELETE | /api/community/comments/{commentId}/likes | 댓글 좋아요 취소 |

### 고객지원

| Method | Endpoint | 용도 |
|--------|----------|------|
| POST | /api/domain/inquiries | 1:1 문의 접수 |

### 인연

| Method | Endpoint | 용도 |
|--------|----------|------|
| GET | /api/points | 인연 상품 목록 조회 |

---

# 📋 TODO

## 매칭

### 상대방에게 당신이 나의 X임을 표시하기
- **인증**: 필요 / 불필요

**상세내용**:
- Matching Result 결과를 보고 이 사람이 나의 X임을 의사 전달하기

**수정사항**:
- findClaimedByMe는 MatchingResult 결과가 아닌 X 요청자 정보가 리턴 되어야함.

---

## 커뮤니티

### 게시글에 댓글 알림 설정

---

## X룸

X룸은 과거 연인과의 추억을 테마 공간에 블록 단위로 배치하여 꾸미는 기능이다.

### X룸 생성
- **인증**: 필요

**상세내용**:
- 지정한 Target-Info 대상으로 X룸을 생성할 수 있다.
- X룸은 일단 생성하고 추후에 블록들을 업데이트 하면서 X룸을 완성해가는 형태로 구성
- 테마(여름, 학교, 모던하우스, 가든, 벚꽃 등 컨셉)는 우선은 가든으로 고정인데 추후에 선택 가능하도록 확장 가능성 열어두고 구현.

### 블록 저장
- **인증**: 필요

**상세내용**:
- 생성한 X룸에 블록 단위로 저장.

### X룸 관리

| Method | Endpoint | 용도 | 인증 |
|--------|----------|------|------|
| GET | /api/xrooms/me | 내 X룸 조회 | 필요 |
| PATCH | /api/xrooms/{id} | X룸 설정 수정 (테마 변경, 배경음악 등) | 필요 |
| DELETE | /api/xrooms/{id} | X룸 삭제 | 필요 |

### 테마

| Method | Endpoint | 용도 | 인증 |
|--------|----------|------|------|
| GET | /api/xrooms/themes | 테마 목록 조회 | 필요 |

**테마 종류**:
- 코르크보드 — 폴라로이드 사진을 핀으로 꽂는 느낌
- 스트링라이트 — 사진을 빨래줄처럼 걸어두는 따뜻한 감성
- 드리미 버블 — 추억이 떠다니는 몽환적 공간

### 콘텐츠 블록

X룸 안에 배치하는 콘텐츠 단위. 각 블록은 위치/크기/회전 속성을 가진다.

| Method | Endpoint | 용도 | 인증 |
|--------|----------|------|------|
| GET | /api/xrooms/{xroomId}/blocks | 블록 목록 조회 | 필요 |
| POST | /api/xrooms/{xroomId}/blocks | 블록 추가 | 필요 |
| PATCH | /api/xrooms/{xroomId}/blocks/{blockId} | 블록 수정 (내용, 위치, 크기 등) | 필요 |
| DELETE | /api/xrooms/{xroomId}/blocks/{blockId} | 블록 삭제 | 필요 |

**블록 타입**:
- **PHOTO** — 사진 + 캡션 + 날짜
- **TEXT** — 편지/메모 스타일 텍스트
- **MUSIC** — 노래 링크 (우리의 노래)
- **DDAY** — 기념일 카운터 ("처음 만난 날" 등)

**블록 공통 속성**:
```json
{
  "type": "PHOTO | TEXT | MUSIC | DDAY",
  "positionX": "number",
  "positionY": "number",
  "rotation": "number (degree)",
  "content": "블록 타입별 데이터"
}
```

### X룸 공유

| Method | Endpoint | 용도 | 인증 |
|--------|----------|------|------|
| POST | /api/xrooms/{xroomId}/share | 상대방에게 X룸 공유 (초대) | 필요 |

---

## 인연 (앱 내 재화)

"인연"은 앱 내 재화로, 매칭 등 유료 기능에 사용된다.

### 인연 API

| Method | Endpoint | 용도 | 인증 |
|--------|----------|------|------|
| POST | /api/points | 인연 충전 (구매) | 필요 |
| GET | /api/points/me | 내 인연 잔액 조회 | 필요 |

**상품 예시**:

| 인연 수량 | 가격 |
|-----------|------|
| 10개 | 1,000원 |
| 30개 | 2,500원 |
| 50개 | 4,000원 |

**참고사항**:
- 인연 차감은 매칭 등 기능 사용 시 서버에서 자동 처리 (별도 API 불필요)
- 잔액 부족 시 에러 응답
- 동시성 제어 필요 (중복 차감 방지)
- PG사 연동 방식 결정 필요 (토스페이먼츠, 카카오페이 등)
- 인연 소비 단위 (매칭 1건당 몇 인연?) 결정 필요

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

## 데이터 정합성
### 파일 삭제 처리 시 db 데이터 파일 존재 여부와 정합성 맞추기 필요.


---

## 템플릿

<!-- 아래를 복사하여 해당 도메인 섹션에 붙여넣기 -->

```
### 제목 : 
- **인증**: 필요 / 불필요

**상세내용**:
- 특이사항 기술
```
