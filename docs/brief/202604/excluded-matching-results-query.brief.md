# Brief: 매칭 결과 목록 조회에 excluded 필터 추가

> 작성일: 2026-04-07

## 목표
기존 `GET /api/matching-results`에 `excluded` request parameter를 추가하여, excluded=true/false 데이터를 하나의 엔드포인트로 조회한다. 기본값은 false(기존 동작 유지).

## 변경 파일
| 파일 | 변경 | 설명 |
|------|------|------|
| `infrastructure/.../matching/dao/MatchingResultQueryDao.kt` | 수정 | `find(email)` 메서드에 `excluded: Boolean` 파라미터 추가 |
| `domain/.../matching/domain/port/MatchingResultRepository.kt` | 수정 | `find(email)` 메서드에 `excluded: Boolean` 파라미터 추가 |
| `infrastructure/.../matching/repository/MatchingResultCoreRepository.kt` | 수정 | 포트 구현 반영 |
| `domain/.../matching/application/MatchingResultQueryService.kt` | 수정 | `find(email)` 메서드에 `excluded: Boolean` 파라미터 추가 |
| `boot/.../matching/api/MatchingResultQueryApi.kt` | 수정 | `@RequestParam excluded: Boolean = false` 추가 |

## 구현 순서
1. `MatchingResultQueryDao.find(email)` → `find(email, excluded: Boolean = false)` 변경, WHERE 조건에 `excluded eq excluded` 적용
2. `MatchingResultRepository.find(email)` → `find(email, excluded: Boolean = false)` 변경
3. `MatchingResultCoreRepository` 포트 구현 반영
4. `MatchingResultQueryService.find(email)` → `find(email, excluded: Boolean = false)` 변경
5. `MatchingResultQueryApi.findMyMatchingResults`에 `@RequestParam excluded: Boolean = false` 추가, Service 호출 시 전달

## 주의사항
- 기본값 `false`로 기존 동작과 완전 호환
- 별도 엔드포인트/메서드 추가 없이 파라미터만 추가하므로 변경 범위 최소화
- 테스트 코드에서 기존 호출부(`find(email)`)는 기본값으로 동작하므로 수정 불필요
