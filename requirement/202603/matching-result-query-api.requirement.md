# Design: 로그인한 회원의 MatchingResults 조회 API

> 작성일: 2025-03-21
> 상태: Draft

## 1. 설계 개요

로그인한 회원의 이메일을 기준으로 MatchingResult를 조회하고, `showingExpiryDate` 기준으로 3가지로 분류하여 반환하는 GET API를 구현한다.

- **recentMatchingResults**: 공개 기간 내 데이터 (`showingExpiryDate - 30일 <= now <= showingExpiryDate`)
- **olderMatchingResults**: 공개 기간이 지난 데이터 (`now > showingExpiryDate`)
- 공개 전 데이터 (`now < showingExpiryDate - 30일`): API 응답에서 제외

## 2. 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│ boot/ma-boot-web                                                │
│                                                                 │
│  MatchingResultQueryApi (GET /api/matching-results)             │
│    └── @AuthenticationPrincipal email                          │
│    └── MatchingResultQueryService.findByEmail(email)           │
│    └── MatchingResultsResponse.from(recentResults, olderResults)│
└──────────────────────┬──────────────────────────────────────────┘
                       │ (port)
┌──────────────────────▼──────────────────────────────────────────┐
│ domain/ma-domain-core                                           │
│                                                                 │
│  MatchingResultQueryService                                     │
│    + findByEmail(email: String): ClassifiedMatchingResults     │
│                                                                 │
│  MatchingResults (일급 컬렉션 확장)                               │
│    + classifyByShowingExpiry(now: LocalDateTime)                │
│      : ClassifiedMatchingResults                                │
│                                                                 │
│  ClassifiedMatchingResults (도메인 모델 신규)                     │
│    val recentResults: MatchingResults                            │
│    val olderResults: MatchingResults                             │
│                                                                 │
│  MatchingResult (도메인 모델 확장)                                │
│    + isRecent(now: LocalDateTime): Boolean                      │
│                                                                 │
│  MatchingResultRepository (포트 확장)                             │
│    + findByRegisterEmail(email: String): MatchingResults        │
└──────────────────────┬──────────────────────────────────────────┘
                       │ (implements)
┌──────────────────────▼──────────────────────────────────────────┐
│ infrastructure/storage/ma-db-core                               │
│                                                                 │
│  MatchingResultQueryDao                                         │
│    + findByRegisterEmail(email: String): List<MatchingResult>  │
│      └── WHERE REGISTER_EMAIL = :email                         │
│          AND MATCHING_EXPIRY_DATE >= :today                     │
│                                                                 │
│  MatchingResultCoreRepository                                   │
│    + findByRegisterEmail(email) 위임                            │
└─────────────────────────────────────────────────────────────────┘
```

## 3. 상세 설계

### 3.1 Domain - MatchingResult 행위 추가

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/matching/domain/MatchingResult.kt`

```kotlin
package com.konkuk.ma.domain.matching.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class MatchingResult(
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

    fun isRecent(now: LocalDateTime): Boolean {                   // 추가
        val showingStartDate = showingExpiryDate.minusDays(SHOWING_EXPIRY_DAYS)
        return !now.isBefore(showingStartDate) && !now.isAfter(showingExpiryDate)
    }

    fun isOlder(now: LocalDateTime): Boolean {                    // 추가
        return now.isAfter(showingExpiryDate)
    }
}
```

- `isRecent(now)`: 공개 시작 시점(`showingExpiryDate - 30일`) 이후이고 `showingExpiryDate` 이전이거나 같으면 `true`
  - 예: showingExpiryDate가 `2026-03-31 11:00`이면 → `2026-03-01 11:00 ~ 2026-03-31 11:00` 기간만 recent
- `isOlder(now)`: 현재 시각이 `showingExpiryDate` 이후이면 `true` (보여주는 기간이 지남)
- `isRecent`도 `isOlder`도 아닌 경우: 아직 공개 전 → API 응답에서 제외
- `now`를 외부에서 주입하는 이유: 테스트 용이성. `LocalDateTime.now()`를 직접 호출하지 않는다

### 3.2 Domain - ClassifiedMatchingResults 신규 생성

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/matching/domain/ClassifiedMatchingResults.kt`

```kotlin
package com.konkuk.ma.domain.matching.domain

class ClassifiedMatchingResults(
    val recentResults: MatchingResults,
    val olderResults: MatchingResults
)
```

- 두 그룹으로 분류된 MatchingResults를 담는 도메인 모델
- MatchingResults 일급 컬렉션의 `classifyByShowingExpiry` 메서드의 반환 타입으로 사용

### 3.3 Domain - MatchingResults 일급 컬렉션 확장

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/matching/domain/MatchingResults.kt`

```kotlin
package com.konkuk.ma.domain.matching.domain

import java.time.LocalDateTime

class MatchingResults(
    val data: List<MatchingResult>
) {
    fun targetInfoIds(): List<Long> {
        return data.map { it.targetInfoId }.distinct()
    }

    private fun createUniqueKeys(): Set<Pair<Long, String>> {
        return data.map { it.createUniqueKey() }.toSet()
    }

    fun filterNew(existing: MatchingResults): MatchingResults {
        val existingKeys = existing.createUniqueKeys()
        return MatchingResults(data.filter { it.createUniqueKey() !in existingKeys })
    }

    fun classifyByShowingExpiry(now: LocalDateTime): ClassifiedMatchingResults {  // 추가
        return ClassifiedMatchingResults(
            recentResults = MatchingResults(data.filter { it.isRecent(now) }),
            olderResults = MatchingResults(data.filter { it.isOlder(now) })
        )
    }

    companion object {
        fun merge(dataList: List<MatchingResults>): MatchingResults {
            return MatchingResults(dataList.flatMap { it.data })
        }
    }
}
```

- `classifyByShowingExpiry(now)`: `isRecent`와 `isOlder`로 각각 필터링하여 두 그룹으로 분리
- 공개 전 데이터(`isRecent`도 `isOlder`도 아닌 데이터)는 자동으로 제외됨
- `now`를 외부에서 주입하여 테스트 시 시간을 고정할 수 있음

### 3.4 Domain Port - MatchingResultRepository 확장

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/matching/domain/port/MatchingResultRepository.kt`

```kotlin
package com.konkuk.ma.domain.matching.domain.port

import com.konkuk.ma.domain.matching.domain.MatchingResults
import java.time.LocalDate

interface MatchingResultRepository {
    fun saveAll(matchingResults: MatchingResults)
    fun findExistingMatchingResults(targetInfoIds: List<Long>): MatchingResults
    fun deleteExpiredMatchingResults(baseDate: LocalDate): Int
    fun findByRegisterEmail(email: String): MatchingResults        // 추가
}
```

- `findByRegisterEmail`: 이메일 기준으로 매칭 결과를 조회
- 반환 타입은 일급 컬렉션 `MatchingResults` (포트 규칙 준수)
- DB 레벨에서 `matchingExpiryDate`가 지난 데이터는 이미 배치에서 삭제되므로, 만료 필터링은 불필요

### 3.5 Domain Application - MatchingResultQueryService 신규 생성

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/matching/application/MatchingResultQueryService.kt`

```kotlin
package com.konkuk.ma.domain.matching.application

import com.konkuk.ma.domain.matching.domain.ClassifiedMatchingResults
import com.konkuk.ma.domain.matching.domain.port.MatchingResultRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class MatchingResultQueryService(
    private val matchingResultRepository: MatchingResultRepository
) {
    fun findByEmail(email: String): ClassifiedMatchingResults {
        val matchingResults = matchingResultRepository.findByRegisterEmail(email)
        return matchingResults.classifyByShowingExpiry(LocalDateTime.now())
    }
}
```

- `@Transactional(readOnly = true)`: 조회 전용 서비스이므로 읽기 전용 트랜잭션 사용 (기존 `MemberQueryService` 패턴 준수)
- `classifyByShowingExpiry`에 `LocalDateTime.now()`를 전달하여 분류 기준 시각 설정
- 서비스 레이어는 얇게 유지: 포트 호출 + 도메인 행위 위임

### 3.6 Infrastructure Entity - MatchingResultEntity 신규 생성

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/matching/entity/MatchingResultEntity.kt`

```kotlin
package com.konkuk.ma.domain.matching.entity

import com.konkuk.ma.domain.matching.domain.MatchingResult
import com.konkuk.ma.domain.matching.entity.table.MatchingResultTable
import org.jetbrains.exposed.sql.ResultRow
import java.time.LocalDate
import java.time.LocalDateTime

class MatchingResultEntity(
    val registerEmail: String,
    val targetInfoId: Long,
    val targetEmail: String,
    val middleNumberMatched: Boolean,
    val lastNumberMatched: Boolean,
    val yearMatched: Boolean,
    val monthMatched: Boolean,
    val dayMatched: Boolean,
    val regionMatched: Boolean,
    val showingExpiryDate: LocalDateTime,
    val matchingExpiryDate: LocalDate
) {
    fun toDomain(): MatchingResult {
        return MatchingResult(
            registerEmail = registerEmail,
            targetInfoId = targetInfoId,
            targetEmail = targetEmail,
            middleNumberMatched = middleNumberMatched,
            lastNumberMatched = lastNumberMatched,
            yearMatched = yearMatched,
            monthMatched = monthMatched,
            dayMatched = dayMatched,
            regionMatched = regionMatched,
            showingExpiryDate = showingExpiryDate,
            matchingExpiryDate = matchingExpiryDate
        )
    }

    companion object {
        fun from(row: ResultRow): MatchingResultEntity {
            return MatchingResultEntity(
                registerEmail = row[MatchingResultTable.registerEmail],
                targetInfoId = row[MatchingResultTable.targetInfoId],
                targetEmail = row[MatchingResultTable.targetEmail],
                middleNumberMatched = row[MatchingResultTable.middleNumberMatched],
                lastNumberMatched = row[MatchingResultTable.lastNumberMatched],
                yearMatched = row[MatchingResultTable.yearMatched],
                monthMatched = row[MatchingResultTable.monthMatched],
                dayMatched = row[MatchingResultTable.dayMatched],
                regionMatched = row[MatchingResultTable.regionMatched],
                showingExpiryDate = row[MatchingResultTable.showingExpiryDate],
                matchingExpiryDate = row[MatchingResultTable.matchingExpiryDate]
            )
        }
    }
}
```

### 3.7 Infrastructure DAO - MatchingResultQueryDao 확장

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/matching/dao/MatchingResultQueryDao.kt`

```kotlin
package com.konkuk.ma.domain.matching.dao

import com.konkuk.ma.domain.matching.entity.MatchingResultEntity
import com.konkuk.ma.domain.matching.entity.table.MatchingResultTable
import org.springframework.stereotype.Component

@Component
class MatchingResultQueryDao {
    fun findByTargetInfoIds(targetInfoIds: List<Long>): List<MatchingResultEntity> {
        if (targetInfoIds.isEmpty()) return emptyList()
        return MatchingResultTable
            .selectAll()
            .where { MatchingResultTable.targetInfoId inList targetInfoIds }
            .map { MatchingResultEntity.from(it) }
    }

    fun findByRegisterEmail(email: String): List<MatchingResultEntity> {  // 추가
        return MatchingResultTable
            .selectAll()
            .where { MatchingResultTable.registerEmail eq email }
            .map { MatchingResultEntity.from(it) }
    }
}
```

- DAO는 `MatchingResultEntity`를 반환 (도메인 객체 직접 생성 X)
- `MatchingResultEntity.from(row)`으로 ResultRow → Entity 변환
- 기존 `findByTargetInfoIds`도 Entity 반환으로 리팩토링

### 3.8 Infrastructure Repository - MatchingResultCoreRepository 확장

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/matching/repository/MatchingResultCoreRepository.kt`

- Repository에서 `entity.toDomain()` 호출하여 도메인 객체로 변환
- `matchingExpiryDate` 필터링은 별도로 하지 않음: 배치 Job이 만료 데이터를 삭제하므로 DB에 남아있는 데이터는 모두 유효

### 3.7 Infrastructure Repository - MatchingResultCoreRepository 확장

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/matching/repository/MatchingResultCoreRepository.kt`

```kotlin
package com.konkuk.ma.domain.matching.repository

import com.konkuk.ma.domain.matching.dao.MatchingResultCommandDao
import com.konkuk.ma.domain.matching.dao.MatchingResultQueryDao
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
        return MatchingResults(matchingResultQueryDao.findByTargetInfoIds(targetInfoIds).map { it.toDomain() })
    }

    override fun deleteExpiredMatchingResults(baseDate: LocalDate): Int {
        return matchingResultCommandDao.deleteExpired(baseDate)
    }

    override fun findByRegisterEmail(email: String): MatchingResults {  // 추가
        return MatchingResults(matchingResultQueryDao.findByRegisterEmail(email).map { it.toDomain() })
    }
}
```

- Repository에서 `entity.toDomain()`을 호출하여 Entity → 도메인 변환
- DAO가 Entity를 반환하므로 `.map { it.toDomain() }` 체이닝

### 3.8 Boot API Response - MatchingResultResponse 신규 생성

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/matching/api/response/MatchingResultResponse.kt`

```kotlin
package com.konkuk.ma.domain.matching.api.response

import com.konkuk.ma.domain.matching.domain.MatchingResult

class MatchingResultResponse(
    val targetInfoId: Long,
    val targetEmail: String,
    val middleNumberMatched: Boolean,
    val lastNumberMatched: Boolean,
    val yearMatched: Boolean,
    val monthMatched: Boolean,
    val dayMatched: Boolean,
    val regionMatched: Boolean,
    val remainingDays: Long
) {
    companion object {
        fun from(matchingResult: MatchingResult): MatchingResultResponse {
            return MatchingResultResponse(
                targetInfoId = matchingResult.targetInfoId,
                targetEmail = matchingResult.targetEmail,
                middleNumberMatched = matchingResult.middleNumberMatched,
                lastNumberMatched = matchingResult.lastNumberMatched,
                yearMatched = matchingResult.yearMatched,
                monthMatched = matchingResult.monthMatched,
                dayMatched = matchingResult.dayMatched,
                regionMatched = matchingResult.regionMatched,
                remainingDays = matchingResult.getRemainingDays()
            )
        }
    }
}
```

- `remainingDays`: 기존 `MatchingResult.getRemainingDays()` 활용. 보여주기 만료까지 남은 일수
- `registerEmail`은 응답에 포함하지 않음: 로그인한 회원 본인의 데이터이므로 중복 정보

### 3.9 Boot API Response - MatchingResultsResponse 신규 생성

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/matching/api/response/MatchingResultsResponse.kt`

```kotlin
package com.konkuk.ma.domain.matching.api.response

import com.konkuk.ma.domain.matching.domain.ClassifiedMatchingResults

class MatchingResultsResponse(
    val recentMatchingResults: List<MatchingResultResponse>,
    val olderMatchingResults: List<MatchingResultResponse>
) {
    companion object {
        fun from(classified: ClassifiedMatchingResults): MatchingResultsResponse {
            return MatchingResultsResponse(
                recentMatchingResults = classified.recentResults.data
                    .map { MatchingResultResponse.from(it) },
                olderMatchingResults = classified.olderResults.data
                    .map { MatchingResultResponse.from(it) }
            )
        }
    }
}
```

- `from(classified)`: 도메인 모델 `ClassifiedMatchingResults`를 API 응답 DTO로 변환
- `recentMatchingResults`: 아직 보여주는 기간인 데이터 (showingExpiryDate 이전)
- `olderMatchingResults`: 보여주는 기간이 지난 데이터 (showingExpiryDate 이후)

### 3.10 Boot API Controller - MatchingResultQueryApi 신규 생성

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/matching/api/MatchingResultQueryApi.kt`

```kotlin
package com.konkuk.ma.domain.matching.api

import com.konkuk.ma.domain.matching.api.response.MatchingResultsResponse
import com.konkuk.ma.domain.matching.application.MatchingResultQueryService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/matching-results")
class MatchingResultQueryApi(
    private val matchingResultQueryService: MatchingResultQueryService
) {
    @GetMapping
    fun findMatchingResults(
        @AuthenticationPrincipal email: String
    ): MatchingResultsResponse {
        val classified = matchingResultQueryService.findByEmail(email)
        return MatchingResultsResponse.from(classified)
    }
}
```

- `@AuthenticationPrincipal email: String`: 기존 `TargetInfoCommandApi`와 동일한 패턴으로 JWT에서 회원 이메일 추출
- `@GetMapping`: 조회 API이므로 GET 메서드 사용
- 엔드포인트: `/api/matching-results` (리소스 중심 REST 네이밍)

### 3.11 Test - MatchingResult 도메인 테스트 확장

**파일**: `domain/ma-domain-core/src/test/kotlin/com/konkuk/ma/domain/matching/domain/MatchingResultTest.kt`

```kotlin
// 기존 테스트 유지 + 아래 컨텍스트 추가

context("isRecent") {

    test("현재 시각이 공개 기간 내이면 true 반환") {
        // Given - showingExpiryDate가 3/31 11:00이면 공개 시작은 3/1 11:00
        val now = LocalDateTime.of(2025, 3, 15, 11, 0)
        val result = MatchingResultFixture.create(
            showingExpiryDate = LocalDateTime.of(2025, 3, 31, 11, 0)
        )

        // When & Then
        result.isRecent(now) shouldBe true
    }

    test("현재 시각이 showingExpiryDate와 같으면 true 반환") {
        // Given
        val now = LocalDateTime.of(2025, 3, 31, 11, 0)
        val result = MatchingResultFixture.create(
            showingExpiryDate = LocalDateTime.of(2025, 3, 31, 11, 0)
        )

        // When & Then
        result.isRecent(now) shouldBe true
    }

    test("현재 시각이 공개 시작 시점과 같으면 true 반환") {
        // Given - showingExpiryDate가 3/31 11:00이면 공개 시작은 3/1 11:00
        val now = LocalDateTime.of(2025, 3, 1, 11, 0)
        val result = MatchingResultFixture.create(
            showingExpiryDate = LocalDateTime.of(2025, 3, 31, 11, 0)
        )

        // When & Then
        result.isRecent(now) shouldBe true
    }

    test("현재 시각이 공개 시작 이전이면 false 반환") {
        // Given - showingExpiryDate가 3/31 11:00이면 공개 시작은 3/1 11:00
        val now = LocalDateTime.of(2025, 3, 1, 6, 0)
        val result = MatchingResultFixture.create(
            showingExpiryDate = LocalDateTime.of(2025, 3, 31, 11, 0)
        )

        // When & Then
        result.isRecent(now) shouldBe false
    }

    test("현재 시각이 showingExpiryDate 이후이면 false 반환") {
        // Given
        val now = LocalDateTime.of(2025, 4, 1, 11, 0)
        val result = MatchingResultFixture.create(
            showingExpiryDate = LocalDateTime.of(2025, 3, 31, 11, 0)
        )

        // When & Then
        result.isRecent(now) shouldBe false
    }
}

context("isOlder") {

    test("현재 시각이 showingExpiryDate 이후이면 true 반환") {
        // Given
        val now = LocalDateTime.of(2025, 4, 1, 11, 0)
        val result = MatchingResultFixture.create(
            showingExpiryDate = LocalDateTime.of(2025, 3, 31, 11, 0)
        )

        // When & Then
        result.isOlder(now) shouldBe true
    }

    test("현재 시각이 showingExpiryDate와 같으면 false 반환") {
        // Given
        val now = LocalDateTime.of(2025, 3, 31, 11, 0)
        val result = MatchingResultFixture.create(
            showingExpiryDate = LocalDateTime.of(2025, 3, 31, 11, 0)
        )

        // When & Then
        result.isOlder(now) shouldBe false
    }
}
```

### 3.12 Test - MatchingResults 일급 컬렉션 테스트 확장

**파일**: `domain/ma-domain-core/src/test/kotlin/com/konkuk/ma/domain/matching/domain/MatchingResultsTest.kt`

```kotlin
// 기존 테스트 유지 + 아래 컨텍스트 추가

context("MatchingResults.classifyByShowingExpiry") {

    test("showingExpiryDate 기준으로 recent와 older를 분류한다") {
        // Given
        val now = LocalDateTime.of(2025, 3, 21, 11, 0)
        val recentResult = MatchingResultFixture.create(
            targetEmail = "recent@example.com",
            showingExpiryDate = LocalDateTime.of(2025, 3, 25, 11, 0)
        )
        val olderResult = MatchingResultFixture.create(
            targetEmail = "older@example.com",
            showingExpiryDate = LocalDateTime.of(2025, 3, 20, 11, 0)
        )
        val matchingResults = MatchingResults(listOf(recentResult, olderResult))

        // When
        val classified = matchingResults.classifyByShowingExpiry(now)

        // Then
        classified.recentResults.data shouldHaveSize 1
        classified.recentResults.data[0].targetEmail shouldBe "recent@example.com"
        classified.olderResults.data shouldHaveSize 1
        classified.olderResults.data[0].targetEmail shouldBe "older@example.com"
    }

    test("모두 recent인 경우 olderResults는 빈 리스트") {
        // Given
        val now = LocalDateTime.of(2025, 3, 21, 11, 0)
        val result = MatchingResultFixture.create(
            showingExpiryDate = LocalDateTime.of(2025, 4, 1, 11, 0)
        )
        val matchingResults = MatchingResults(listOf(result))

        // When
        val classified = matchingResults.classifyByShowingExpiry(now)

        // Then
        classified.recentResults.data shouldHaveSize 1
        classified.olderResults.data shouldHaveSize 0
    }

    test("빈 MatchingResults에 대해 양쪽 모두 빈 리스트 반환") {
        // Given
        val now = LocalDateTime.now()
        val matchingResults = MatchingResults(emptyList())

        // When
        val classified = matchingResults.classifyByShowingExpiry(now)

        // Then
        classified.recentResults.data shouldHaveSize 0
        classified.olderResults.data shouldHaveSize 0
    }
}
```

### 3.13 Test - MatchingResultQueryApi 컨트롤러 테스트

**파일**: `boot/ma-boot-web/src/test/kotlin/com/konkuk/ma/domain/matching/api/MatchingResultQueryApiTest.kt`

```kotlin
package com.konkuk.ma.domain.matching.api

import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.matching.application.MatchingResultQueryService
import com.konkuk.ma.domain.matching.domain.ClassifiedMatchingResults
import com.konkuk.ma.domain.matching.domain.MatchingResult
import com.konkuk.ma.domain.matching.domain.MatchingResults
import com.konkuk.ma.extension.ARRAY
import com.konkuk.ma.extension.BOOLEAN
import com.konkuk.ma.extension.NUMBER
import com.konkuk.ma.extension.STRING
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.getJson
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.extension.responseType
import com.konkuk.ma.support.security.WithAuthMember
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import java.time.LocalDate
import java.time.LocalDateTime

@WebMvcTest(MatchingResultQueryApi::class)
@BaseApiTest
@WithAuthMember(email = "test@example.com")
class MatchingResultQueryApiTest(
    private val mockMvc: MockMvc,
    @MockkBean private val matchingResultQueryService: MatchingResultQueryService
) : FunSpec({

    test("매칭 결과 조회 API 문서화") {
        // Given
        val recentResult = MatchingResult(
            registerEmail = "test@example.com",
            targetInfoId = 1L,
            targetEmail = "target1@example.com",
            middleNumberMatched = true,
            lastNumberMatched = false,
            yearMatched = true,
            monthMatched = true,
            dayMatched = false,
            regionMatched = true,
            showingExpiryDate = LocalDate.now().atTime(11, 0).plusDays(25),
            matchingExpiryDate = LocalDate.now().plusDays(200)
        )
        val olderResult = MatchingResult(
            registerEmail = "test@example.com",
            targetInfoId = 2L,
            targetEmail = "target2@example.com",
            middleNumberMatched = false,
            lastNumberMatched = true,
            yearMatched = false,
            monthMatched = false,
            dayMatched = true,
            regionMatched = false,
            showingExpiryDate = LocalDateTime.now().minusDays(5),
            matchingExpiryDate = LocalDate.now().plusDays(100)
        )

        val classified = ClassifiedMatchingResults(
            recentResults = MatchingResults(listOf(recentResult)),
            olderResults = MatchingResults(listOf(olderResult))
        )

        every { matchingResultQueryService.findByEmail("test@example.com") } returns classified

        // When & Then
        mockMvc.getJson("/api/matching-results") {}
            .andExpect {
                status { isOk() }
                jsonPath("$.recentMatchingResults").isArray
                jsonPath("$.recentMatchingResults[0].targetInfoId").value(1)
                jsonPath("$.recentMatchingResults[0].targetEmail").value("target1@example.com")
                jsonPath("$.olderMatchingResults").isArray
                jsonPath("$.olderMatchingResults[0].targetInfoId").value(2)
            }
            .andDocument(
                "matching/find-matching-results",
                responseBody(
                    "recentMatchingResults" responseType ARRAY means "아직 보여주는 기간인 매칭 결과 목록",
                    "recentMatchingResults[].targetInfoId" responseType NUMBER means "찾는 사람 정보 ID",
                    "recentMatchingResults[].targetEmail" responseType STRING means "매칭된 대상 이메일",
                    "recentMatchingResults[].middleNumberMatched" responseType BOOLEAN means "전화번호 중간자리 일치 여부",
                    "recentMatchingResults[].lastNumberMatched" responseType BOOLEAN means "전화번호 뒷자리 일치 여부",
                    "recentMatchingResults[].yearMatched" responseType BOOLEAN means "생년 일치 여부",
                    "recentMatchingResults[].monthMatched" responseType BOOLEAN means "생월 일치 여부",
                    "recentMatchingResults[].dayMatched" responseType BOOLEAN means "생일 일치 여부",
                    "recentMatchingResults[].regionMatched" responseType BOOLEAN means "지역 일치 여부",
                    "recentMatchingResults[].remainingDays" responseType NUMBER means "보여주기 만료까지 남은 일수",
                    "olderMatchingResults" responseType ARRAY means "보여주는 기간이 지난 매칭 결과 목록",
                    "olderMatchingResults[].targetInfoId" responseType NUMBER means "찾는 사람 정보 ID",
                    "olderMatchingResults[].targetEmail" responseType STRING means "매칭된 대상 이메일",
                    "olderMatchingResults[].middleNumberMatched" responseType BOOLEAN means "전화번호 중간자리 일치 여부",
                    "olderMatchingResults[].lastNumberMatched" responseType BOOLEAN means "전화번호 뒷자리 일치 여부",
                    "olderMatchingResults[].yearMatched" responseType BOOLEAN means "생년 일치 여부",
                    "olderMatchingResults[].monthMatched" responseType BOOLEAN means "생월 일치 여부",
                    "olderMatchingResults[].dayMatched" responseType BOOLEAN means "생일 일치 여부",
                    "olderMatchingResults[].regionMatched" responseType BOOLEAN means "지역 일치 여부",
                    "olderMatchingResults[].remainingDays" responseType NUMBER means "보여주기 만료까지 남은 일수"
                )
            )
    }

    test("매칭 결과가 없는 경우 빈 목록 반환") {
        // Given
        val classified = ClassifiedMatchingResults(
            recentResults = MatchingResults(emptyList()),
            olderResults = MatchingResults(emptyList())
        )

        every { matchingResultQueryService.findByEmail("test@example.com") } returns classified

        // When & Then
        mockMvc.getJson("/api/matching-results") {}
            .andExpect {
                status { isOk() }
                jsonPath("$.recentMatchingResults").isArray
                jsonPath("$.recentMatchingResults").isEmpty
                jsonPath("$.olderMatchingResults").isArray
                jsonPath("$.olderMatchingResults").isEmpty
            }
            .andDocument(
                "matching/find-matching-results-empty",
                responseBody(
                    "recentMatchingResults" responseType ARRAY means "빈 매칭 결과 목록",
                    "olderMatchingResults" responseType ARRAY means "빈 매칭 결과 목록"
                )
            )
    }
})
```

## 4. 구현 순서

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 1 | `domain/ma-domain-core/.../domain/MatchingResult.kt` | 수정 | `isRecent(now)` 메서드 추가 |
| 2 | `domain/ma-domain-core/.../domain/ClassifiedMatchingResults.kt` | 신규 | 분류된 결과를 담는 도메인 모델 |
| 3 | `domain/ma-domain-core/.../domain/MatchingResults.kt` | 수정 | `classifyByShowingExpiry(now)` 메서드 추가 |
| 4 | `domain/ma-domain-core/.../domain/port/MatchingResultRepository.kt` | 수정 | `findByRegisterEmail(email)` 메서드 추가 |
| 5 | `domain/ma-domain-core/.../application/MatchingResultQueryService.kt` | 신규 | 조회 서비스 |
| 6 | `infrastructure/.../dao/MatchingResultQueryDao.kt` | 수정 | `findByRegisterEmail` 추가 + `toMatchingResult` 리팩토링 |
| 7 | `infrastructure/.../repository/MatchingResultCoreRepository.kt` | 수정 | `findByRegisterEmail` 위임 구현 |
| 8 | `boot/ma-boot-web/.../response/MatchingResultResponse.kt` | 신규 | 단건 매칭 결과 응답 DTO |
| 9 | `boot/ma-boot-web/.../response/MatchingResultsResponse.kt` | 신규 | 분류된 매칭 결과 응답 DTO |
| 10 | `boot/ma-boot-web/.../api/MatchingResultQueryApi.kt` | 신규 | GET API 컨트롤러 |
| 11 | `domain/.../domain/MatchingResultTest.kt` | 수정 | `isRecent` 테스트 추가 |
| 12 | `domain/.../domain/MatchingResultsTest.kt` | 수정 | `classifyByShowingExpiry` 테스트 추가 |
| 13 | `boot/.../api/MatchingResultQueryApiTest.kt` | 신규 | API 문서화 테스트 |

## 5. 고려사항

- **인덱스**: `MATCHING_RESULTS` 테이블의 `REGISTER_EMAIL` 컬럼에 인덱스가 필요할 수 있음. 현재 DDL을 확인하여 인덱스가 없으면 추가 권장. 인덱스 없이 `REGISTER_EMAIL`로 조회 시 full table scan 발생 가능
- **데이터 규모**: 한 회원의 MatchingResult 건수가 극단적으로 많아질 가능성은 낮음 (TargetInfo 등록 건수에 비례). 현재는 페이징 없이 전체 조회로 충분. 추후 건수가 많아지면 페이징 도입 검토
- **배치와의 관계**: `ExpiredMatchingResultDeleteJobConfig`가 `matchingExpiryDate`가 지난 데이터를 삭제하므로, DB에 남아 있는 데이터는 모두 유효한 상태. DAO에서 별도 만료 필터링 불필요
- **showingExpiryDate 경계값**: `isRecent`에서 `!now.isAfter(showingExpiryDate)`를 사용하여 `now == showingExpiryDate`인 경우도 recent로 분류. 요구사항의 "showingExpiryDate 날짜까지"에 부합
- **REST Docs 응답 필드**: `responseType` 확장 함수가 자동으로 `data.` prefix를 추가하는 구조이므로, 테스트 작성 시 실제 JSON 구조와 REST Docs 스니펫의 필드 경로가 일치하는지 확인 필요. 만약 응답 래핑이 없는 구조라면 `responseType` 대신 별도 처리 필요
- **보안**: `@AuthenticationPrincipal`로 JWT에서 추출한 이메일만 사용하므로 다른 회원의 데이터 접근 불가. 별도 권한 검증 불필요
