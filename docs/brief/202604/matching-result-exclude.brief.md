# Brief: 매칭 상대 제외/해제

> 작성일: 2026-04-06

## 목표
매칭 결과에서 특정 상대를 제외(차단)하거나 해제하는 기능을 추가하여, 사용자가 원치 않는 매칭 결과를 목록에서 숨길 수 있도록 한다.

## 변경 파일
| 파일 | 변경 | 설명 |
|------|------|------|
| `domain/ma-domain-core/.../matching/domain/MatchingResult.kt` | 수정 | `excluded` 필드 추가, `exclude()`, `include()` 행위 메서드 추가 |
| `domain/ma-domain-core/.../matching/domain/port/MatchingResultRepository.kt` | 수정 | `updateExcluded()` 포트 메서드 추가 |
| `domain/ma-domain-core/.../matching/application/MatchingResultCommandService.kt` | 신규 | 제외/해제 유스케이스 조합 (findById + validateOwnership + exclude/include + updateExcluded) |
| `infrastructure/.../matching/entity/table/MatchingResultTable.kt` | 수정 | `excluded` 컬럼 추가 (BOOLEAN DEFAULT FALSE) |
| `infrastructure/.../matching/entity/MatchingResultEntity.kt` | 수정 | `excluded` 필드 추가, `from()` 및 `toDomain()` 매핑 반영 |
| `infrastructure/.../matching/dao/MatchingResultCommandDao.kt` | 수정 | `updateExcluded()` 메서드 추가 |
| `infrastructure/.../matching/dao/MatchingResultQueryDao.kt` | 수정 | `findByRegisterEmail()`에 `excluded = false` 조건 추가 |
| `infrastructure/.../matching/repository/MatchingResultCoreRepository.kt` | 수정 | `updateExcluded()` 포트 구현 |
| `boot/ma-boot-web/.../matching/api/MatchingResultCommandApi.kt` | 신규 | PATCH `/{id}/exclude`, PATCH `/{id}/include` 엔드포인트 |
| DDL (마이그레이션 SQL) | 신규 | `ALTER TABLE MATCHING_RESULTS ADD COLUMN EXCLUDED BOOLEAN DEFAULT FALSE` |

## 구현 순서
1. DDL 변경 - MATCHING_RESULTS 테이블에 EXCLUDED 컬럼 추가
2. MatchingResultTable에 `excluded` 컬럼 정의 추가
3. MatchingResult 도메인 객체에 `excluded` 필드와 `exclude()`, `include()` 메서드 추가
4. MatchingResultEntity에 `excluded` 필드 추가 및 `from()`, `toDomain()` 반영
5. MatchingResultRepository 포트에 `updateExcluded()` 메서드 추가
6. MatchingResultCommandDao에 `updateExcluded()` 구현 (Exposed update DSL)
7. MatchingResultQueryDao의 `findByRegisterEmail()`에 `excluded eq false` 조건 추가
8. MatchingResultCoreRepository에서 `updateExcluded()` 포트 구현
9. MatchingResultCommandService 신규 생성 - `exclude()`, `include()` 유스케이스
10. MatchingResultCommandApi 신규 생성 - PATCH 엔드포인트 2개

## 주의사항
- `findByRegisterEmail()`에만 excluded 필터를 적용하고, `findById()`에는 적용하지 않아야 제외된 항목의 상세 조회 및 해제가 가능하다
- `exclude()`, `include()` 메서드는 MatchingResult 도메인 객체 내부에서 상태를 변경하는 행위 메서드로, 현재 상태와 동일한 경우 무시하거나 예외를 던질지 정책 결정이 필요하다
- MatchingResultCommandDao의 `saveAll()`은 batchInsert용이므로 `excluded` 컬럼도 함께 반영해야 한다
- `updateExcluded()`는 단건 update이므로 id와 excluded 값만 갱신하면 된다
