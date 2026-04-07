# Brief: 사진 업로드 API 분리

## 현재 상태
- `SignUpApi`가 multipart/form-data로 회원가입 + 사진을 함께 처리
- `SignUpService.signUp(command, photoFile?)`에서 `MemberPhotoUploader` 호출

## 변경 요약
회원가입 API에서 사진 업로드를 분리하여 별도 API로 제공

## 변경 파일

### 수정 (4개)

| 파일 | 변경 |
|------|------|
| `SignUpApi` | multipart → `@RequestBody` JSON 복원, photo 파라미터 제거 |
| `SignUpService` | `photoFile` 파라미터 제거, `MemberPhotoUploader` 의존 제거 |
| `SignUpApiTest` | multipart 테스트 → JSON 테스트로 복원 |
| `auth/sign-up.adoc` | multipart 관련 snippet 제거, JSON request 문서로 복원 |

### 신규 (4개)

| 파일 | 역할 |
|------|------|
| `MemberPhotoApi` | `POST /api/members/photos` (multipart, 인증 필요), `DELETE /api/members/photos` (인증 필요) |
| `MemberPhotoResponse` | 업로드 응답 DTO |
| `MemberPhotoApiTest` | API 테스트 + REST Docs |
| `member/member-photo.adoc` | AsciiDoc 문서 + main.adoc 연결 |

### 삭제 없음
- `MemberPhotoUploader`, `PhotoFile`, `FileStorage` 등 도메인/인프라 코드는 그대로 유지
- `MemberPhotoApi`에서 `MemberPhotoUploader`를 직접 사용

## API 설계

### 사진 업로드
- `POST /api/members/photos`
- 인증 필요 (`@AuthenticationPrincipal email`)
- multipart/form-data (`@RequestPart("photo") photo: MultipartFile`)
- 기존 사진 있으면 교체 (1장 제한)

### 사진 삭제
- `DELETE /api/members/photos`
- 인증 필요

## 보안
- 기존 `anyRequest().authenticated()`에 의해 자동 보호
- SecurityConfig 변경 불필요
