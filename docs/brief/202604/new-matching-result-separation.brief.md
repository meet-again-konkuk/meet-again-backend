# Brief: NewMatchingResult / MatchingResult 생성-조회 분리

> 작성일: 2026-04-06

## 목표
커뮤니티 도메인(NewPost/Post, NewComment/Comment)과 일관성을 맞추기 위해 MatchingResult를 생성용 NewMatchingResult와 조회용 MatchingResult로 분리한다.

## 변경 파일
| 파일 | 변경 | 설명 |
|------|------|------|
| `domain/ma-domain-core/.../matching/domain/NewMatchingResult.kt` | 신규 | 생성 전용 도메인 모델 (id, excluded, matchRate, getRemainingDays 등 조회 전용 필드/행위 제거) |
| `domain/ma-domain-core/.../matching/domain/NewMatchingResults.kt` | 신규 | 생성용 일급 컬렉션 (filterNew, merge, targetInfoIds, createUniqueKey 행위 보유) |
| `domain/ma-domain-core/.../matching/domain/MatchingResult.kt` | 수정 | 조회 전용으로 정리 (createUniqueKey 제거, excluded를 val로 변경 불가 -- exclude/include 행위 유지) |
| `domain/ma-domain-core/.../matching/domain/MatchingResults.kt` | 수정 | filterNew, merge, targetInfoIds 제거 (조회 전용 행위만 유지: extractTargetEmails, combineWithProfiles) |
| `domain/ma-domain-core/.../matching/domain/TargetInfo.kt` | 수정 | makeMatchingResult 반환 타입을 NewMatchingResult로 변경 |
| `domain/ma-domain-core/.../matching/domain/TargetInfos.kt` | 수정 | makeMatchingResults 반환 타입을 NewMatchingResults로 변경 |
| `domain/ma-domain-core/.../matching/domain/port/MatchingResultRepository.kt` | 수정 | saveAll 파라미터를 `List<NewMatchingResult>`로 변경 |
| `infrastructure/.../matching/dao/MatchingResultCommandDao.kt` | 수정 | saveAll 파라미터를 `List<NewMatchingResult>`로 변경 |
| `infrastructure/.../matching/repository/MatchingResultCoreRepository.kt` | 수정 | saveAll 구현을 NewMatchingResult 사용으로 변경 |
| `boot/ma-boot-batch/.../matching/MatchingJobConfig.kt` | 수정 | processor/writer 타입을 `List<NewMatchingResult>`로 변경 |
| `domain/ma-domain-core/src/testFixtures/.../fixture/MatchingResultFixture.kt` | 수정 | NewMatchingResultFixture 추가 또는 기존 create를 NewMatchingResult용으로 분리 |
| `boot/ma-boot-batch/.../matching/MatchingProcessorTest.kt` | 수정 | NewMatchingResult 타입으로 검증 변경 |
| `domain/ma-domain-core/src/test/.../matching/domain/MatchingResultsTest.kt` | 수정 | filterNew, merge 테스트를 NewMatchingResultsTest로 이동 |
| `domain/ma-domain-core/src/test/.../matching/domain/TargetInfoTest.kt` | 수정 | makeMatchingResult 반환 타입 검증 변경 |

## 구현 순서
1. NewMatchingResult 도메인 모델 생성 (registerEmail, targetInfoId, targetEmail, 6개 matched 필드, showingExpiryDate, matchingExpiryDate, createUniqueKey)
2. NewMatchingResults 일급 컬렉션 생성 (기존 MatchingResults에서 filterNew, merge, targetInfoIds 이동)
3. MatchingResult에서 생성 전용 로직 제거 (createUniqueKey 제거), MatchingResults에서 생성 전용 행위 제거
4. TargetInfo.makeMatchingResult/makeMatchingResults 반환 타입을 New- 계열로 변경
5. MatchingResultRepository.saveAll 파라미터를 `List<NewMatchingResult>`로 변경
6. MatchingResultCommandDao.saveAll, MatchingResultCoreRepository.saveAll 구현 변경
7. MatchingJobConfig의 processor/writer 타입 파라미터를 `List<NewMatchingResult>`로 변경
8. Fixture 및 테스트 코드 수정

## 주의사항
- NewMatchingResult에는 id 필드가 없다 (DB 저장 전이므로)
- NewMatchingResult에는 excluded 필드가 없다 (생성 시 항상 false이므로 DAO에서 하드코딩)
- MatchingResult의 excluded는 var로 유지해야 한다 (exclude/include 행위가 조회 후 상태 변경에 해당)
- findExistingMatchingResults는 중복 체크용이므로 반환 타입은 기존 `List<MatchingResult>` 유지, NewMatchingResults.filterNew가 `MatchingResults`가 아닌 `List<MatchingResult>`를 받도록 설계
- MatchingResults의 createUniqueKeys는 filterNew에서만 사용되었으므로, NewMatchingResults로 이동 시 기존 MatchingResult에도 createUniqueKey가 필요 (조회용에서 키 추출은 filterNew의 비교 대상으로만 사용)
