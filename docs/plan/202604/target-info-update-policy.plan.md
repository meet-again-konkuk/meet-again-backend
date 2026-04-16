# Plan: TargetInfo 수정 정책 변경

> 작성일: 2026-04-13

## 1. 개요

TargetInfo 수정 API에 name 필드를 다시 추가하고, MatchingResult가 존재하거나 생성 후 하루가 경과한 TargetInfo는 수정을 차단하는 정책을 도입한다.

## 2. 변경 전략

### 2-1. name 필드 수정 복원

| 레이어 | 현재 | 변경 후 | 변환 위치 |
|--------|------|---------|-----------|
| UpdateTargetInfo (Domain) | name 필드 없음 | `targetName: String?` 추가 | 생성자 |
| UpdateTargetInfoRequest (Boot) | name 필드 없음 | `name: String?` 추가 (Bean Validation 포함) | Request DTO |
| TargetInfoCommandDao.update | name 컬럼 미갱신 | `name` 컬럼 갱신 추가 | DAO update 함수 |

### 2-2. 수정 가능 여부 검증

| 레이어 | 현재 | 변경 후 | 변환 위치 |
|--------|------|---------|-----------|
| TargetInfo (Domain) | `createdDate` 필드 없음 | `createdDate: LocalDateTime` 추가 | 생성자 |
| TargetInfo (Domain) | 수정 검증 행위 없음 | `validateUpdatable(hasMatchingResult: Boolean)` 추가 | 도메인 메서드 |
| TargetInfoEntity (Infra) | `toDomain()`에서 createdDate 미전달 | `createdDate` 전달 추가 | `toDomain()` |
| MatchingResultRepository (Port) | `existsByTargetInfoId` 없음 | `existsByTargetInfoId(targetInfoId: Long): Boolean` 추가 | 포트 인터페이스 |
| MatchingResultQueryDao (Infra) | 해당 메서드 없음 | `existsByTargetInfoId(targetInfoId: Long): Boolean` 추가 | DAO |
| MatchingResultCoreRepository (Infra) | 해당 메서드 없음 | `existsByTargetInfoId` 구현 | Repository 구현체 |
| TargetInfoCommandService | 검증 없이 바로 update | `validateUpdatable` 호출 후 update | Service |

### 2-3. 검증 규칙 (TargetInfo.validateUpdatable)

- **MatchingResult 존재 여부**: 파라미터 `hasMatchingResult: Boolean`이 true이면 `InvalidStateException` 발생
- **생성 후 하루 경과**: `createdDate`가 현재 시각 기준 24시간 이상 경과했으면 `InvalidStateException` 발생
- 검증 순서: MatchingResult 존재 확인 -> 하루 경과 확인 (더 명확한 사유를 먼저 안내)

### 2-4. Service 조합 흐름

1. `targetInfoQueryRepository.findOne(id)` -> TargetInfo 조회
2. `targetInfo.validateOwnership(memberEmail)` -> 소유자 확인
3. `matchingResultRepository.existsByTargetInfoId(id)` -> MatchingResult 존재 여부 조회
4. `targetInfo.validateUpdatable(hasMatchingResult)` -> 수정 가능 여부 검증
5. `targetInfoCommandRepository.update(...)` -> 수정 수행

## 3. 변경 파일 목록

### Phase 1: Domain 모델 변경

| # | 파일 | 내용 |
|---|------|------|
| 1 | `domain/.../matching/domain/TargetInfo.kt` | `createdDate: LocalDateTime` 필드 추가, `validateUpdatable(hasMatchingResult: Boolean)` 메서드 추가 |
| 2 | `domain/.../matching/domain/UpdateTargetInfo.kt` | `targetName: String?` 필드 추가 |

### Phase 2: Port 인터페이스 변경

| # | 파일 | 내용 |
|---|------|------|
| 3 | `domain/.../matching/domain/port/MatchingResultRepository.kt` | `existsByTargetInfoId(targetInfoId: Long): Boolean` 메서드 추가 |

### Phase 3: Infrastructure 변경

| # | 파일 | 내용 |
|---|------|------|
| 4 | `infrastructure/.../matching/entity/TargetInfoEntity.kt` | `toDomain()`에서 `createdDate` 전달 추가 |
| 5 | `infrastructure/.../matching/dao/MatchingResultQueryDao.kt` | `existsByTargetInfoId(targetInfoId: Long): Boolean` 메서드 추가 (MatchingResultTable에서 targetInfoId로 exists 쿼리) |
| 6 | `infrastructure/.../matching/repository/MatchingResultCoreRepository.kt` | `existsByTargetInfoId` 구현 추가 (DAO 위임) |
| 7 | `infrastructure/.../matching/dao/TargetInfoCommandDao.kt` | `update` 메서드에서 `name` 컬럼 갱신 추가 (`updateTargetInfo.targetName`이 non-null일 때) |

### Phase 4: Boot (API) 변경

| # | 파일 | 내용 |
|---|------|------|
| 8 | `boot/.../matching/api/request/UpdateTargetInfoRequest.kt` | `name: String?` 필드 추가 (`@Pattern(regexp = ValidationPatterns.NAME, message = ValidationMessages.NAME_INVALID)`), `toUpdateTargetInfo()`에 `targetName = name` 전달 |

### Phase 5: Service 변경

| # | 파일 | 내용 |
|---|------|------|
| 9 | `domain/.../matching/application/TargetInfoCommandService.kt` | `update` 메서드에서 `matchingResultRepository.existsByTargetInfoId` 호출 후 `targetInfo.validateUpdatable(hasMatchingResult)` 호출 추가 |

## 4. 고려사항

- **createdDate 전달 범위**: `TargetInfo` 도메인 객체에 `createdDate`를 추가하면 조회 목록/상세에서도 노출 가능. 현재 `TargetInfoResponse`에 이미 필요한 필드만 매핑하고 있으므로 외부 노출 영향 없음
- **하루 기준**: `LocalDateTime.now()`와 비교하여 `createdDate.plusDays(1).isBefore(now)` 판단. 서버 시간 기준이므로 타임존 이슈 없음 (DB 서버와 앱 서버 동일 타임존 가정)
- **name 수정 nullable**: `UpdateTargetInfo.targetName`이 null이면 기존 name 유지. DAO에서 non-null일 때만 업데이트하도록 조건부 갱신
- **기존 findExistingMatchingResults와의 차이**: 기존 메서드는 `List<Long>` 받아 `List<MatchingResult>` 반환. 새 `existsByTargetInfoId`는 단건 Boolean 반환으로 단순하고 효율적 (전체 데이터 로딩 불필요)
- **InvalidStateException 사용**: 프로젝트에 이미 존재하는 예외 클래스. `type = TargetInfo::class`, `value = targetInfoId`, `reason` 에 구체적 사유 전달

## 5. 검증 항목

- [ ] `TargetInfo.validateUpdatable`: MatchingResult 존재 시 InvalidStateException 발생 확인
- [ ] `TargetInfo.validateUpdatable`: createdDate 기준 24시간 경과 시 InvalidStateException 발생 확인
- [ ] `TargetInfo.validateUpdatable`: 조건 미충족 시 정상 통과 확인
- [ ] `TargetInfoCommandService.update`: name 포함 수정 정상 동작 확인
- [ ] `TargetInfoCommandService.update`: 수정 불가 조건에서 예외 발생 확인
- [ ] `UpdateTargetInfoRequest`: name 필드 Bean Validation 동작 확인
- [ ] `TargetInfoCommandDao.update`: name 컬럼이 non-null일 때만 갱신되는지 확인
- [ ] 빌드 성공: `./gradlew build`
