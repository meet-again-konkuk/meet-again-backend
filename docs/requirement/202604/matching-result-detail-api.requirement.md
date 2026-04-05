# Design: MatchingResult 상세 조회 API

> 작성일: 2026-04-06
> 상태: Draft

## 1. 설계 개요

MatchingResultId를 PathVariable로 받아 매칭 결과의 상세 정보(항목별 일치 여부, 매칭률, 상대방 프로필)를 반환하는 GET API를 구현한다. 소유권 검증을 통해 본인의 매칭 결과만 조회 가능하도록 한다.

## 2. 아키텍처

```
┌──────────────────────────────────────────────────────────────┐
│ boot/ma-boot-web                                             │
│                                                              │
│  MatchingResultQueryApi                                      │
│    GET /api/matching-results/{matchingResultId}              │
│    @DecryptId → Long 변환                                    │
│    @AuthenticationPrincipal → email                          │
│    └── MatchingResultQueryService.findDetailById(id, email)  │
│    └── MatchingResultDetailResponse.from(result)             │
└───────────────────────────┬──────────────────────────────────┘
                            │ (port)
┌───────────────────────────▼──────────────────────────────────┐
│ domain/ma-domain-core                                        │
│                                                              │
│  MatchingResultQueryService                                  │
│    + findDetailById(matchingResultId: Long, email: String)   │
│      : MatchingResultWithProfile                             │
│                                                              │
│  MatchingResultRepository (port)                             │
│    + findById(matchingResultId: Long): MatchingResult?       │
│                                                              │
│  MatchingResult (domain)                                     │
│    + validateOwnership(email: String)                        │
│                                                              │
│  MatchingResultAccessDeniedException (exception, 신규)       │
└───────────────────────────┬──────────────────────────────────┘
                            │ (implements)
┌───────────────────────────▼──────────────────────────────────┐
│ infrastructure/storage/ma-db-core                            │
│                                                              │
│  MatchingResultQueryDao                                      │
│    + findById(id: Long): MatchingResultEntity?               │
│                                                              │
│  MatchingResultCoreRepository                                │
│    + findById(matchingResultId: Long): MatchingResult?       │
└──────────────────────────────────────────────────────────────┘
```

## 3. 상세 설계

### 3.1 Domain - MatchingResultAccessDeniedException (신규)

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/matching/exception/MatchingResultAccessDeniedException.kt`

```kotlin
package com.konkuk.ma.domain.matching.exception

import com.konkuk.ma.exception.BusinessException

class MatchingResultAccessDeniedException(
    matchingResultId: Long,
    email: String,
) : BusinessException(
    message = "매칭 결과에 대한 접근 권한이 없습니다.",
    dataMessage = "matchingResultId: $matchingResultId, requestEmail: $email",
    logLevel = LogLevel.WARN,
)
```

- `matchingResultId`: 접근 시도한 매칭 결과 ID (로깅용)
- `email`: 접근 시도한 사용자 email (로깅용)
- `LogLevel.WARN`: 권한 없는 접근이므로 WARN 레벨 로깅

### 3.2 Domain - MatchingResult 소유권 검증 메서드 추가

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/matching/domain/MatchingResult.kt`

```kotlin
package com.konkuk.ma.domain.matching.domain

import com.konkuk.ma.domain.matching.exception.MatchingResultAccessDeniedException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class MatchingResult(
    val id: Long = 0L,
    val registerEmail: String,
    val targetInfoId: Long,
    val targetEmail: String,

    val middleNumberMatched: Boolean,
    val lastNumberMatched: Boolean,
    val yearMatched: Boolean,
    val monthMatched: Boolean,
    val dayMatched: Boolean,
    val regionMatched: Boolean,

    val showingExpiryDate: LocalDateTime = LocalDate.now()
        .atTime(SHOWING_START_HOUR, 0)
        .plusDays(SHOWING_EXPIRY_DAYS),
    val matchingExpiryDate: LocalDate = LocalDate.now()
        .plusDays(MATCHING_EXPIRY_DAYS),
) {
    val matchRate: Int by lazy {
        MatchRateCalculator(
            groups = listOf(
                MatchingGroup.Phone(middleNumberMatched, lastNumberMatched),
                MatchingGroup.Birth(yearMatched, monthMatched, dayMatched),
            ),
            regionMatched = regionMatched,
        ).calculate()
    }

    companion object {
        private const val SHOWING_EXPIRY_DAYS = 30L
        private const val MATCHING_EXPIRY_DAYS = 210L
        private const val SHOWING_START_HOUR = 11
    }

    fun createUniqueKey(): Pair<Long, String> {
        return Pair(targetInfoId, targetEmail)
    }

    fun getRemainingDays(): Long {
        val now = LocalDate.now()
        return ChronoUnit.DAYS.between(now, showingExpiryDate)
            .coerceAtLeast(0)
    }

    fun validateOwnership(email: String) {  // 추가
        if (registerEmail != email) {
            throw MatchingResultAccessDeniedException(id, email)
        }
    }
}
```

- `validateOwnership(email)`: 요청자의 email과 등록자 email을 비교하여 소유권 검증
- 도메인 객체 내부에서 상태 검증 수행 (OOP 원칙: 상태 검증은 객체 내부에서)
- 불일치 시 `MatchingResultAccessDeniedException` 발생

### 3.3 Domain Port - MatchingResultRepository findById 추가

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/matching/domain/port/MatchingResultRepository.kt`

```kotlin
package com.konkuk.ma.domain.matching.domain.port

import com.konkuk.ma.domain.matching.domain.MatchingResults
import java.time.LocalDate

interface MatchingResultRepository {
    fun saveAll(matchingResults: MatchingResults)
    fun findExistingMatchingResults(targetInfoIds: List<Long>): MatchingResults
    fun deleteExpiredMatchingResults(baseDate: LocalDate): Int
    fun findByRegisterEmail(email: String): MatchingResults
    fun findById(matchingResultId: Long): MatchingResult?  // 추가
}
```

- 반환타입 `MatchingResult?`: 존재하지 않는 ID일 수 있으므로 nullable
- 파라미터명 `matchingResultId`: 의미를 명확히 전달

> **주의**: import문에 `MatchingResult` 추가 필요 (`com.konkuk.ma.domain.matching.domain.MatchingResult`)

### 3.4 Domain Service - MatchingResultQueryService findDetailById 추가

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/matching/application/MatchingResultQueryService.kt`

```kotlin
package com.konkuk.ma.domain.matching.application

import com.konkuk.ma.domain.matching.domain.MatchingResultWithProfile
import com.konkuk.ma.domain.matching.domain.MatchingResultsWithProfiles
import com.konkuk.ma.domain.matching.domain.port.MatchingResultRepository
import com.konkuk.ma.domain.member.domain.port.MemberPhotoRepository
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import com.konkuk.ma.exception.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MatchingResultQueryService(
    private val matchingResultRepository: MatchingResultRepository,
    private val memberQueryRepository: MemberQueryRepository,
    private val memberPhotoRepository: MemberPhotoRepository,
) {
    fun findByRegisterEmail(email: String): MatchingResultsWithProfiles {
        val matchingResults = matchingResultRepository.findByRegisterEmail(email)
        val targetEmails = matchingResults.extractTargetEmails()

        val members = memberQueryRepository.findByEmails(targetEmails)
        val photos = memberPhotoRepository.findByEmails(targetEmails)

        return matchingResults.combineWithProfiles(members, photos)
    }

    fun findDetailById(matchingResultId: Long, email: String): MatchingResultWithProfile {  // 추가
        val matchingResult = matchingResultRepository.findById(matchingResultId)
            ?: throw EntityNotFoundException("MatchingResult", "id", matchingResultId.toString())

        matchingResult.validateOwnership(email)

        val targetEmail = matchingResult.targetEmail
        val members = memberQueryRepository.findByEmails(setOf(targetEmail))
        val photos = memberPhotoRepository.findByEmails(setOf(targetEmail))

        val member = members.findByEmail(targetEmail)
        val photo = photos.findByEmail(targetEmail)

        return MatchingResultWithProfile(
            matchingResult = matchingResult,
            targetMemberId = member?.id,
            targetName = member?.name,
            targetNickname = member?.nickname,
            profileImageUrl = photo?.thumbnailPath,
        )
    }
}
```

- `findDetailById(matchingResultId, email)`: 단건 조회 + 소유권 검증 + 프로필 조합
- Service는 조합만 담당: findById -> validateOwnership -> findByEmails -> 조합
- 존재하지 않는 경우 `EntityNotFoundException` (기존 패턴과 동일)
- 소유권 검증은 도메인 객체(`MatchingResult.validateOwnership`)에 위임
- `MemberPhotos.findByEmail` 반환 타입 확인 필요 (아래서 확인)

### 3.5 Infrastructure - MatchingResultQueryDao findById 추가

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/matching/dao/MatchingResultQueryDao.kt`

```kotlin
package com.konkuk.ma.domain.matching.dao

import com.konkuk.ma.domain.matching.entity.MatchingResultEntity
import com.konkuk.ma.domain.matching.entity.table.MatchingResultTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Component

@Component
class MatchingResultQueryDao {
    fun findByTargetInfoIds(targetInfoIds: List<Long>): List<MatchingResultEntity> {
        if (targetInfoIds.isEmpty()) return emptyList()
        return MatchingResultTable
            .selectAll()
            .where { MatchingResultTable.targetInfoId inList targetInfoIds }
            .map { row -> MatchingResultEntity.from(row) }
    }

    fun findByRegisterEmail(email: String): List<MatchingResultEntity> {
        return MatchingResultTable
            .selectAll()
            .where {
                (MatchingResultTable.registerEmail eq email) and
                    (MatchingResultTable.deleted eq false)
            }
            .map { row -> MatchingResultEntity.from(row) }
    }

    fun findById(id: Long): MatchingResultEntity? {  // 추가
        return MatchingResultTable
            .selectAll()
            .where {
                (MatchingResultTable.id eq id) and
                    (MatchingResultTable.deleted eq false)
            }
            .map { row -> MatchingResultEntity.from(row) }
            .singleOrNull()
    }
}
```

- `findById(id)`: PK로 단건 조회, soft delete 필터링 포함
- `singleOrNull()`: 결과가 없으면 null, 있으면 단건 반환
- `deleted eq false` 조건: 기존 `findByRegisterEmail`과 동일한 soft delete 필터링 패턴

### 3.6 Infrastructure - MatchingResultCoreRepository findById 구현

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/matching/repository/MatchingResultCoreRepository.kt`

```kotlin
package com.konkuk.ma.domain.matching.repository

import com.konkuk.ma.domain.matching.dao.MatchingResultCommandDao
import com.konkuk.ma.domain.matching.dao.MatchingResultQueryDao
import com.konkuk.ma.domain.matching.domain.MatchingResult
import com.konkuk.ma.domain.matching.domain.MatchingResults
import com.konkuk.ma.domain.matching.domain.port.MatchingResultRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class MatchingResultCoreRepository(
    private val matchingResultCommandDao: MatchingResultCommandDao,
    private val matchingResultQueryDao: MatchingResultQueryDao
) : MatchingResultRepository {
    override fun saveAll(matchingResults: MatchingResults) {
        matchingResultCommandDao.saveAll(matchingResults.data)
    }

    override fun findExistingMatchingResults(targetInfoIds: List<Long>): MatchingResults {
        return MatchingResults(
            matchingResultQueryDao.findByTargetInfoIds(targetInfoIds)
                .map { it.toDomain() }
        )
    }

    override fun deleteExpiredMatchingResults(baseDate: LocalDate): Int {
        return matchingResultCommandDao.deleteExpired(baseDate)
    }

    override fun findByRegisterEmail(email: String): MatchingResults {
        return MatchingResults(
            matchingResultQueryDao.findByRegisterEmail(email)
                .map { it.toDomain() }
        )
    }

    override fun findById(matchingResultId: Long): MatchingResult? {  // 추가
        return matchingResultQueryDao.findById(matchingResultId)
            ?.toDomain()
    }
}
```

- `findById`: DAO에서 Entity를 받아 `toDomain()`으로 도메인 객체로 변환 (기존 패턴과 동일)
- nullable 체이닝: `?.toDomain()`

### 3.7 Boot - MatchingResultDetailResponse (신규)

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/matching/api/response/MatchingResultDetailResponse.kt`

```kotlin
package com.konkuk.ma.domain.matching.api.response

import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.matching.domain.MatchingResultWithProfile
import com.konkuk.ma.support.id.EncryptId

class MatchingResultDetailResponse(
    @EncryptId(ObfuscationType.MATCHING_RESULT)
    val matchingResultId: Long,
    @EncryptId(ObfuscationType.MEMBER)
    val targetMemberId: Long?,
    val targetName: String?,
    val targetNickname: String?,
    val profileImageUrl: String?,
    val remainingDays: Long,
    val matchRate: Int,
    val isWithdrawn: Boolean,
    val middleNumberMatched: Boolean,
    val lastNumberMatched: Boolean,
    val yearMatched: Boolean,
    val monthMatched: Boolean,
    val dayMatched: Boolean,
    val regionMatched: Boolean,
) {
    companion object {
        fun from(result: MatchingResultWithProfile): MatchingResultDetailResponse {
            return MatchingResultDetailResponse(
                matchingResultId = result.matchingResult.id,
                targetMemberId = result.targetMemberId,
                targetName = result.targetName,
                targetNickname = result.targetNickname,
                profileImageUrl = result.profileImageUrl,
                remainingDays = result.matchingResult.getRemainingDays(),
                matchRate = result.matchingResult.matchRate,
                isWithdrawn = result.isWithdrawn,
                middleNumberMatched = result.matchingResult.middleNumberMatched,
                lastNumberMatched = result.matchingResult.lastNumberMatched,
                yearMatched = result.matchingResult.yearMatched,
                monthMatched = result.matchingResult.monthMatched,
                dayMatched = result.matchingResult.dayMatched,
                regionMatched = result.matchingResult.regionMatched,
            )
        }
    }
}
```

- 기존 `MatchingResultResponse`의 필드 + 매칭 항목별 일치 여부(6개 Boolean) 추가
- `@EncryptId`: 기존 패턴과 동일하게 ID 암호화
- `from()` 팩토리 메서드: 도메인 -> 응답 변환 (기존 패턴과 동일)

### 3.8 Boot - MatchingResultQueryApi 상세 조회 엔드포인트 추가

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/matching/api/MatchingResultQueryApi.kt`

```kotlin
package com.konkuk.ma.domain.matching.api

import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.matching.api.response.MatchingResultDetailResponse
import com.konkuk.ma.domain.matching.api.response.MatchingResultsResponse
import com.konkuk.ma.domain.matching.application.MatchingResultQueryService
import com.konkuk.ma.support.id.DecryptId
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/matching-results")
class MatchingResultQueryApi(
    private val matchingResultQueryService: MatchingResultQueryService
) {
    @GetMapping
    fun findMyMatchingResults(
        @AuthenticationPrincipal email: String,
    ): MatchingResultsResponse {
        val results = matchingResultQueryService.findByRegisterEmail(email)
        return MatchingResultsResponse.from(results)
    }

    @GetMapping("/{matchingResultId}")  // 추가
    fun findMatchingResultDetail(
        @AuthenticationPrincipal email: String,
        @PathVariable @DecryptId(ObfuscationType.MATCHING_RESULT) matchingResultId: Long,
    ): MatchingResultDetailResponse {
        val result = matchingResultQueryService.findDetailById(matchingResultId, email)
        return MatchingResultDetailResponse.from(result)
    }
}
```

- `@PathVariable @DecryptId(ObfuscationType.MATCHING_RESULT)`: URL에서 암호화된 ID를 받아 Long으로 디코딩
- `@AuthenticationPrincipal email`: JWT에서 인증된 사용자 email 추출
- 메서드명 `findMatchingResultDetail`: 동사(`find`) + 목적어(`MatchingResultDetail`)

### 3.9 Boot - GlobalExceptionHandler에 MatchingResultAccessDeniedException 핸들러 추가

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/support/error/GlobalExceptionHandler.kt`

```kotlin
// 기존 import에 추가
import com.konkuk.ma.domain.matching.exception.MatchingResultAccessDeniedException

// 기존 핸들러들 유지, 아래 핸들러 추가

@ExceptionHandler(MatchingResultAccessDeniedException::class)
fun handleMatchingResultAccessDeniedException(
    e: MatchingResultAccessDeniedException
): ResponseEntity<String> {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.message)
}
```

- HTTP 403 Forbidden 반환: 인증은 되었으나 권한(소유권)이 없는 경우
- 기존 `GlobalExceptionHandler` 패턴을 따라 `ResponseEntity<String>` 반환

### 3.10 Test - MatchingVocabulary에 상세 응답 필드 추가

**파일**: `boot/ma-boot-web/src/test/kotlin/com/konkuk/ma/vocabulary/MatchingVocabulary.kt`

```kotlin
// 기존 코드 유지, 파일 하단에 추가

// --- 매칭 결과 상세 관련 필드 ---

fun detailMatchingResultId(fieldName: String = "matchingResultId") =
    fieldName responseType STRING means "매칭 결과 ID (인코딩)" example "abc123"

fun detailTargetMemberId(fieldName: String = "targetMemberId") =
    fieldName responseType STRING means "매칭된 상대의 회원 ID (인코딩, 탈퇴 시 null)" example "abc123"

fun detailTargetName(fieldName: String = "targetName") =
    fieldName responseType STRING means "매칭된 상대의 이름" example "김만남"

fun detailTargetNickname(fieldName: String = "targetNickname") =
    fieldName responseType STRING means "매칭된 상대의 닉네임" example "테스트닉네임"

fun detailProfileImageUrl(fieldName: String = "profileImageUrl") =
    fieldName responseType STRING means "매칭된 상대의 프로필 이미지 URL" example "https://example.com/image.jpg"

fun detailRemainingDays(fieldName: String = "remainingDays") =
    fieldName responseType NUMBER means "매칭 결과 노출 잔여일" example "25"

fun detailMatchRate(fieldName: String = "matchRate") =
    fieldName responseType NUMBER means "매칭률 (%)" example "75"

fun detailIsWithdrawn(fieldName: String = "isWithdrawn") =
    fieldName responseType BOOLEAN means "탈퇴 회원 여부" example "false"

fun middleNumberMatched(fieldName: String = "middleNumberMatched") =
    fieldName responseType BOOLEAN means "전화번호 중간자리 일치 여부" example "true"

fun lastNumberMatched(fieldName: String = "lastNumberMatched") =
    fieldName responseType BOOLEAN means "전화번호 뒷자리 일치 여부" example "true"

fun yearMatched(fieldName: String = "yearMatched") =
    fieldName responseType BOOLEAN means "생년 일치 여부" example "true"

fun monthMatched(fieldName: String = "monthMatched") =
    fieldName responseType BOOLEAN means "생월 일치 여부" example "false"

fun dayMatched(fieldName: String = "dayMatched") =
    fieldName responseType BOOLEAN means "생일 일치 여부" example "false"

fun regionMatched(fieldName: String = "regionMatched") =
    fieldName responseType BOOLEAN means "지역 일치 여부" example "true"
```

- 기존 목록 조회용 vocabulary는 `matchingResults[].` prefix가 있음 (배열 내부)
- 상세 조회는 단건이므로 prefix 없이 `data.` prefix만 자동 추가됨
- `middleNumberMatched` ~ `regionMatched`: 6개 매칭 항목별 일치 여부 필드

### 3.11 Test - MatchingResultQueryServiceTest에 findDetailById 테스트 추가

**파일**: `domain/ma-domain-core/src/test/kotlin/com/konkuk/ma/domain/matching/application/MatchingResultQueryServiceTest.kt`

```kotlin
// 기존 import에 추가
import com.konkuk.ma.domain.matching.exception.MatchingResultAccessDeniedException
import com.konkuk.ma.exception.EntityNotFoundException
import io.kotest.assertions.throwables.shouldThrow

// 기존 context("findByRegisterEmail") 블록 아래에 추가

context("findDetailById") {

    test("매칭 결과 ID로 상세 정보를 조회하고 프로필 정보를 조합하여 반환한다") {
        // Given
        val email = "register@example.com"
        val matchingResultId = 1L
        val matchingResult = MatchingResultFixture.create(
            registerEmail = email,
            targetEmail = "target@example.com",
        ).let { it.copy(id = matchingResultId) }  // id 주입 주의 - 아래 고려사항 참조

        val member = MemberFixture.create(email = matchingResult.targetEmail)
        val photo = MemberPhotoFixture.create(
            memberEmail = matchingResult.targetEmail,
            thumbnailPath = "thumb/photo.jpg"
        )

        every { matchingResultRepository.findById(matchingResultId) } returns matchingResult
        every { memberQueryRepository.findByEmails(setOf(matchingResult.targetEmail)) } returns Members(listOf(member))
        every { memberPhotoRepository.findByEmails(setOf(matchingResult.targetEmail)) } returns MemberPhotos(listOf(photo))

        // When
        val result = service.findDetailById(matchingResultId, email)

        // Then
        result.matchingResult.id shouldBe matchingResultId
        result.targetName shouldBe member.name
        result.targetNickname shouldBe member.nickname
        result.profileImageUrl shouldBe photo.thumbnailPath
    }

    test("존재하지 않는 매칭 결과 ID면 EntityNotFoundException을 발생시킨다") {
        // Given
        val matchingResultId = 999L
        every { matchingResultRepository.findById(matchingResultId) } returns null

        // When & Then
        shouldThrow<EntityNotFoundException> {
            service.findDetailById(matchingResultId, "register@example.com")
        }
    }

    test("본인의 매칭 결과가 아니면 MatchingResultAccessDeniedException을 발생시킨다") {
        // Given
        val matchingResultId = 1L
        val matchingResult = MatchingResultFixture.create(
            registerEmail = "owner@example.com",
            targetEmail = "target@example.com",
        )

        every { matchingResultRepository.findById(matchingResultId) } returns matchingResult

        // When & Then
        shouldThrow<MatchingResultAccessDeniedException> {
            service.findDetailById(matchingResultId, "other@example.com")
        }
    }
}
```

- 3가지 케이스: 정상 조회 / 존재하지 않는 ID / 소유권 불일치
- `MatchingResultFixture.create`에서 id 파라미터 주입 필요 (아래 고려사항 참조)

### 3.12 Test - MatchingResultTest에 validateOwnership 테스트 추가

**파일**: `domain/ma-domain-core/src/test/kotlin/com/konkuk/ma/domain/matching/domain/MatchingResultTest.kt`

기존 테스트 파일에 아래 context 블록을 추가한다:

```kotlin
// 기존 import에 추가
import com.konkuk.ma.domain.matching.exception.MatchingResultAccessDeniedException
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow

context("validateOwnership") {
    test("등록자 본인의 email이면 예외가 발생하지 않는다") {
        // Given
        val matchingResult = MatchingResultFixture.create(registerEmail = "owner@example.com")

        // When & Then
        shouldNotThrow<MatchingResultAccessDeniedException> {
            matchingResult.validateOwnership("owner@example.com")
        }
    }

    test("등록자가 아닌 email이면 MatchingResultAccessDeniedException이 발생한다") {
        // Given
        val matchingResult = MatchingResultFixture.create(registerEmail = "owner@example.com")

        // When & Then
        shouldThrow<MatchingResultAccessDeniedException> {
            matchingResult.validateOwnership("other@example.com")
        }
    }
}
```

### 3.13 Test - MatchingResultQueryApiTest에 상세 조회 API 문서화 테스트 추가

**파일**: `boot/ma-boot-web/src/test/kotlin/com/konkuk/ma/domain/matching/api/MatchingResultQueryApiTest.kt`

기존 테스트 클래스 내부에 아래 테스트를 추가한다:

```kotlin
// 기존 import에 추가
import com.konkuk.ma.domain.matching.domain.MatchingResultWithProfile
import com.konkuk.ma.vocabulary.detailMatchingResultId
import com.konkuk.ma.vocabulary.detailTargetMemberId
import com.konkuk.ma.vocabulary.detailTargetName
import com.konkuk.ma.vocabulary.detailTargetNickname
import com.konkuk.ma.vocabulary.detailProfileImageUrl
import com.konkuk.ma.vocabulary.detailRemainingDays
import com.konkuk.ma.vocabulary.detailMatchRate
import com.konkuk.ma.vocabulary.detailIsWithdrawn
import com.konkuk.ma.vocabulary.middleNumberMatched
import com.konkuk.ma.vocabulary.lastNumberMatched
import com.konkuk.ma.vocabulary.yearMatched
import com.konkuk.ma.vocabulary.monthMatched
import com.konkuk.ma.vocabulary.dayMatched
import com.konkuk.ma.vocabulary.regionMatched
import com.konkuk.ma.extension.pathVariables
import com.konkuk.ma.extension.requestParam

test("매칭 결과 상세 조회 API 문서화") {
    // Given
    val matchingResult = MatchingResult(
        id = 1L,
        registerEmail = "test@example.com",
        targetInfoId = 10L,
        targetEmail = "target@example.com",
        middleNumberMatched = true,
        lastNumberMatched = true,
        yearMatched = true,
        monthMatched = false,
        dayMatched = false,
        regionMatched = true,
        showingExpiryDate = LocalDateTime.now().plusDays(25),
        matchingExpiryDate = LocalDate.now().plusDays(200),
    )
    val resultWithProfile = MatchingResultWithProfile(
        matchingResult = matchingResult,
        targetMemberId = 1L,
        targetName = "김만남",
        targetNickname = "테스트닉네임",
        profileImageUrl = "https://example.com/image.jpg",
    )

    every { matchingResultQueryService.findDetailById(1L, "test@example.com") } returns resultWithProfile

    // When & Then
    mockMvc.getJson("/api/matching-results/{matchingResultId}", "1") {}
        .andExpect { status { isOk() } }
        .andDocument(
            "matching/find-matching-result-detail",
            pathVariables(
                "matchingResultId" requestParam "매칭 결과 ID (인코딩)",
            ),
            responseBody(
                detailMatchingResultId(),
                detailTargetMemberId(),
                detailTargetName(),
                detailTargetNickname(),
                detailProfileImageUrl(),
                detailRemainingDays(),
                detailMatchRate(),
                detailIsWithdrawn(),
                middleNumberMatched(),
                lastNumberMatched(),
                yearMatched(),
                monthMatched(),
                dayMatched(),
                regionMatched(),
            )
        )
}
```

> **주의**: `mockMvc.getJson`은 현재 URI 문자열만 받는 구조다. PathVariable을 사용하는 API 테스트가 프로젝트에 처음이므로, `MockMvcExtensions.kt`에 PathVariable 지원 확장 함수가 필요할 수 있다. 아래 고려사항에서 상세히 다룬다.

## 4. 구현 순서

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 1 | `domain/.../matching/exception/MatchingResultAccessDeniedException.kt` | 신규 | 소유권 검증 실패 예외 클래스 |
| 2 | `domain/.../matching/domain/MatchingResult.kt` | 수정 | `validateOwnership(email)` 메서드 추가 |
| 3 | `domain/.../matching/domain/port/MatchingResultRepository.kt` | 수정 | `findById(matchingResultId)` 메서드 추가 |
| 4 | `infrastructure/.../matching/dao/MatchingResultQueryDao.kt` | 수정 | `findById(id)` 쿼리 구현 |
| 5 | `infrastructure/.../matching/repository/MatchingResultCoreRepository.kt` | 수정 | `findById` 포트 구현 (DAO 위임) |
| 6 | `domain/.../matching/application/MatchingResultQueryService.kt` | 수정 | `findDetailById(matchingResultId, email)` 메서드 추가 |
| 7 | `boot/.../matching/api/response/MatchingResultDetailResponse.kt` | 신규 | 상세 응답 DTO (매칭 항목별 일치 여부 포함) |
| 8 | `boot/.../matching/api/MatchingResultQueryApi.kt` | 수정 | `GET /{matchingResultId}` 엔드포인트 추가 |
| 9 | `boot/.../support/error/GlobalExceptionHandler.kt` | 수정 | `MatchingResultAccessDeniedException` 핸들러 추가 (403) |
| 10 | `domain/.../matching/domain/MatchingResultTest.kt` | 수정 | `validateOwnership` 테스트 추가 |
| 11 | `domain/.../matching/application/MatchingResultQueryServiceTest.kt` | 수정 | `findDetailById` 테스트 추가 (정상/404/403) |
| 12 | `boot/.../vocabulary/MatchingVocabulary.kt` | 수정 | 상세 응답 필드 vocabulary 추가 |
| 13 | `boot/.../matching/api/MatchingResultQueryApiTest.kt` | 수정 | 상세 조회 API 문서화 테스트 |

## 5. 고려사항

### 5.1 MockMvc PathVariable 지원

현재 `MockMvcExtensions.kt`의 `getJson`은 단순 URI 문자열만 받는다. PathVariable이 있는 URL 테스트를 위해 두 가지 선택지가 있다:

- **선택지 A**: `getJson` 오버로드 추가 — `fun MockMvc.getJson(urlTemplate: String, vararg uriVars: Any, setup: ...)`
- **선택지 B**: 기존 `mockMvc.get` (Spring의 MockMvc Kotlin DSL)을 직접 사용

`@DecryptId`가 `ConditionalGenericConverter`로 등록되어 있고, 테스트에서는 `addFilters = false`이므로 `@DecryptId` 변환이 정상 동작하는지 확인이 필요하다. **`TestIdObfuscatorConfig`에서 변환이 identity(입력=출력)로 동작한다면**, 단순히 `mockMvc.get("/api/matching-results/1")`로 테스트할 수 있다.

### 5.2 MatchingResultFixture.create에 id 파라미터

현재 `MatchingResultFixture.create`는 id 파라미터가 없다 (기본값 `0L` 사용). `findDetailById` 테스트에서는 id가 필요하므로:

- **권장**: `MatchingResultFixture.create`에 `id: Long = 0L` 파라미터 추가
- `MatchingResult`는 data class가 아니므로 `copy()` 불가 — Fixture 수정이 필요

### 5.3 MemberPhotos.findByEmail 반환 타입 확인

`MemberPhotos` 일급 컬렉션의 `findByEmail` 메서드가 `MemberPhoto?`를 반환하는지 확인 필요. Service에서 nullable로 처리하고 있으므로 맞을 것으로 예상하지만, 구현 전 확인 권장.

### 5.4 FK 안전성

이번 변경은 읽기(SELECT) 전용이므로 FK 관련 이슈 없음.

### 5.5 성능

- `findById`는 PK 조회이므로 인덱스 활용 → 성능 문제 없음
- `findByEmails`는 단건(1개 email)이므로 N+1 이슈 없음

### 5.6 디미터 법칙 관련

`MatchingResultDetailResponse.from()`에서 `result.matchingResult.middleNumberMatched`처럼 체이닝이 발생한다. 이는 `MatchingResultWithProfile`이 DTO 성격의 조합 객체이므로 허용 가능한 수준이다. 만약 엄격하게 적용하려면 `MatchingResultWithProfile`에 위임 메서드를 추가할 수 있으나, 응답 DTO 변환 코드에서는 과도한 설계로 판단된다.
