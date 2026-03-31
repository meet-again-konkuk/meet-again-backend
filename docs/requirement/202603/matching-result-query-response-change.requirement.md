# Design: MatchingResult 조회 API 응답 변경

> 작성일: 2026-03-31
> 상태: Draft
> 기반 문서: requirement/202603/matching-result-query-api.requirement.md

## 1. 설계 개요

기존 MatchingResult 조회 API의 응답에서 matched 필드들(middleNumberMatched, lastNumberMatched 등)을 제거하고, 매칭 상대의 프로필 사진/이름/닉네임, 매칭률(%), 남은 showing date, MatchingResultId를 반환하도록 변경한다. 매칭 상대 정보 조회 시 N+1을 방지하기 위해 `targetEmail` 목록으로 Member와 MemberPhoto를 일괄 조회한다.

## 2. 아키텍처

```
┌──────────────────────────────────────────────────────────────────────────┐
│ boot/ma-boot-web                                                        │
│                                                                         │
│  MatchingResultQueryApi (GET /api/matching-results)                     │
│    └── @AuthenticationPrincipal email                                   │
│    └── MatchingResultQueryService.findByEmail(email)                    │
│         → ClassifiedMatchingResults                                     │
│    └── 매칭 상대 프로필 조회를 위해                                       │
│         MatchingPartnerQueryService.findPartnerProfiles(targetEmails)   │
│         → Map<String, MatchingPartnerProfile>                           │
│    └── MatchingResultsResponse.from(classified, partnerProfileMap)      │
└──────────────────────┬───────────────────────────────────────────────────┘
                       │ (port)
┌──────────────────────▼───────────────────────────────────────────────────┐
│ domain/ma-domain-core                                                    │
│                                                                          │
│  MatchingResult (수정)                                                   │
│    + val id: Long                                                        │
│    + val matchRate: Int (기존 - lazy 계산)                                │
│    + getRemainingDays(): Long (기존)                                     │
│                                                                          │
│  MatchingPartnerProfile (신규 도메인 모델)                                │
│    val email: String                                                     │
│    val name: String                                                      │
│    val nickname: String                                                   │
│    val profileImageUrl: String?                                          │
│                                                                          │
│  MatchingPartnerQueryRepository (신규 포트)                               │
│    + findByEmails(emails: Set<String>): Map<String, MatchingPartnerProfile>│
└──────────────────────┬───────────────────────────────────────────────────┘
                       │ (implements)
┌──────────────────────▼───────────────────────────────────────────────────┐
│ infrastructure/storage/ma-db-core                                        │
│                                                                          │
│  MatchingPartnerQueryDao (신규)                                          │
│    + findByEmails(emails: Set<String>)                                   │
│      → Map<String, MatchingPartnerProfile>                               │
│      └── SELECT m.EMAIL, m.NAME, m.NICKNAME, mp.FILE_PATH               │
│          FROM MEMBERS m LEFT JOIN MEMBER_PHOTOS mp                       │
│              ON m.EMAIL = mp.MEMBER_EMAIL AND mp.DELETED = false         │
│          WHERE m.EMAIL IN (:emails)                                      │
│                                                                          │
│  MatchingResultQueryDao (수정)                                           │
│    + findByRegisterEmail: id 포함하여 반환                                │
│                                                                          │
│  MatchingPartnerQueryCoreRepository (신규)                               │
│    + findByEmails 위임                                                   │
└──────────────────────────────────────────────────────────────────────────┘
```

## 3. 상세 설계

### 3.1 Domain - MatchingResult id 필드 추가

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/matching/domain/MatchingResult.kt`

```kotlin
package com.konkuk.ma.domain.matching.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class MatchingResult(
    val id: Long = 0L,                                       // 추가
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

    fun isRecent(now: LocalDateTime): Boolean {
        val showingStartDate = showingExpiryDate.minusDays(SHOWING_EXPIRY_DAYS)
        return !now.isBefore(showingStartDate) && !now.isAfter(showingExpiryDate)
    }

    fun isOlder(now: LocalDateTime): Boolean {
        return now.isAfter(showingExpiryDate)
    }
}
```

- `id`: DB PK인 `MATCHING_RESULT_ID` 값. 기본값 `0L`은 새로 생성하는 경우(아직 저장 전)를 위함
- 기존 생성자 호출부(`saveAll` 등)에 영향 없음: `id`가 첫 번째 파라미터이지만 기본값이 있으므로 named parameter로 사용하는 코드에는 호환성 유지
- **주의**: 기존 `MatchingResultQueryDao.findByTargetInfoIds`와 `MatchingResultCommandDao.saveAll`에서 `id`를 세팅하지 않는 코드가 있으면 기본값 `0L`로 동작. 조회 DAO에서는 `id`를 함께 읽도록 수정 필요

### 3.2 Domain - MatchingPartnerProfile 신규 생성

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/matching/domain/MatchingPartnerProfile.kt`

```kotlin
package com.konkuk.ma.domain.matching.domain

class MatchingPartnerProfile(
    val email: String,
    val name: String,
    val nickname: String,
    val profileImageUrl: String?
)
```

- 매칭 상대의 프로필 정보를 담는 경량 도메인 모델
- `Member` 도메인을 그대로 사용하지 않는 이유: Member에는 password, phoneNumber 등 민감 정보가 포함되어 있고, 매칭 조회 컨텍스트에서 필요한 필드만 노출하기 위함
- `profileImageUrl`: `MemberPhoto.filePath`에서 가져옴. 사진이 없거나 삭제된 경우 `null`

### 3.3 Domain Port - MatchingPartnerQueryRepository 신규 생성

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/matching/domain/port/MatchingPartnerQueryRepository.kt`

```kotlin
package com.konkuk.ma.domain.matching.domain.port

import com.konkuk.ma.domain.matching.domain.MatchingPartnerProfile

interface MatchingPartnerQueryRepository {
    fun findByEmails(emails: Set<String>): Map<String, MatchingPartnerProfile>
}
```

- `emails`: 매칭 상대의 이메일 목록 (중복 제거를 위해 `Set`)
- 반환값: `email -> MatchingPartnerProfile` 맵. 호출 측에서 O(1)로 조회 가능
- 별도 포트로 분리하는 이유: `MemberQueryRepository`에 매칭 전용 조회 메서드를 추가하면 Member 도메인의 책임이 확대됨. 매칭 컨텍스트 전용 포트를 만들어 경계 유지

### 3.4 Domain Application - MatchingResultQueryService 수정

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/matching/application/MatchingResultQueryService.kt`

```kotlin
package com.konkuk.ma.domain.matching.application

import com.konkuk.ma.domain.matching.domain.ClassifiedMatchingResults
import com.konkuk.ma.domain.matching.domain.MatchingPartnerProfile
import com.konkuk.ma.domain.matching.domain.port.MatchingPartnerQueryRepository
import com.konkuk.ma.domain.matching.domain.port.MatchingResultRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class MatchingResultQueryService(
    private val matchingResultRepository: MatchingResultRepository,
    private val matchingPartnerQueryRepository: MatchingPartnerQueryRepository  // 추가
) {
    fun findByEmail(email: String): ClassifiedMatchingResults {
        val matchingResults = matchingResultRepository.findByRegisterEmail(email)
        return matchingResults.classifyByShowingExpiry(LocalDateTime.now())
    }

    fun findPartnerProfiles(                                                    // 추가
        targetEmails: Set<String>
    ): Map<String, MatchingPartnerProfile> {
        if (targetEmails.isEmpty()) return emptyMap()
        return matchingPartnerQueryRepository.findByEmails(targetEmails)
    }
}
```

- `findPartnerProfiles`: 매칭 상대 이메일 목록으로 프로필 정보를 일괄 조회
- Controller에서 `ClassifiedMatchingResults`의 모든 `targetEmail`을 수집하여 한 번에 호출 (N+1 방지)
- `targetEmails`가 비어있으면 DB 호출 없이 빈 맵 반환

### 3.5 Infrastructure DAO - MatchingPartnerQueryDao 신규 생성

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/matching/dao/MatchingPartnerQueryDao.kt`

```kotlin
package com.konkuk.ma.domain.matching.dao

import com.konkuk.ma.domain.matching.domain.MatchingPartnerProfile
import com.konkuk.ma.domain.member.entity.table.MemberPhotoTable
import com.konkuk.ma.domain.member.entity.table.MemberTable
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.and
import org.springframework.stereotype.Component

@Component
class MatchingPartnerQueryDao {

    fun findByEmails(emails: Set<String>): Map<String, MatchingPartnerProfile> {
        if (emails.isEmpty()) return emptyMap()

        return MemberTable
            .join(
                otherTable = MemberPhotoTable,
                joinType = JoinType.LEFT,
                onColumn = MemberTable.email,
                otherColumn = MemberPhotoTable.memberEmail,
                additionalConstraint = { MemberPhotoTable.deleted eq false }
            )
            .select(
                MemberTable.email,
                MemberTable.name,
                MemberTable.nickname,
                MemberPhotoTable.filePath
            )
            .where { MemberTable.email inList emails }
            .associate { row ->
                val email = row[MemberTable.email]
                email to MatchingPartnerProfile(
                    email = email,
                    name = row[MemberTable.name],
                    nickname = row[MemberTable.nickname],
                    profileImageUrl = row.getOrNull(MemberPhotoTable.filePath)
                )
            }
    }
}
```

- `JoinType.LEFT`: Member에 사진이 없어도 결과에 포함 (profileImageUrl이 null)
- `additionalConstraint`: soft-delete된 사진은 제외 (`DELETED = false`)
- `row.getOrNull(MemberPhotoTable.filePath)`: LEFT JOIN 시 사진이 없으면 null 반환
- `associate`: 한 번의 쿼리로 Map을 생성하여 N+1 방지
- 기술적 포인트: Exposed의 `join` DSL 사용. `additionalConstraint` 파라미터로 JOIN 조건에 `DELETED = false`를 추가하면 WHERE 절이 아닌 ON 절에 조건이 걸려 LEFT JOIN 의미가 유지됨

### 3.6 Infrastructure Repository - MatchingPartnerQueryCoreRepository 신규 생성

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/matching/repository/MatchingPartnerQueryCoreRepository.kt`

```kotlin
package com.konkuk.ma.domain.matching.repository

import com.konkuk.ma.domain.matching.dao.MatchingPartnerQueryDao
import com.konkuk.ma.domain.matching.domain.MatchingPartnerProfile
import com.konkuk.ma.domain.matching.domain.port.MatchingPartnerQueryRepository
import org.springframework.stereotype.Repository

@Repository
class MatchingPartnerQueryCoreRepository(
    private val matchingPartnerQueryDao: MatchingPartnerQueryDao
) : MatchingPartnerQueryRepository {

    override fun findByEmails(emails: Set<String>): Map<String, MatchingPartnerProfile> {
        return matchingPartnerQueryDao.findByEmails(emails)
    }
}
```

- DAO에 단순 위임. Entity 변환 없이 DAO에서 직접 도메인 모델을 생성하는 이유: MatchingPartnerProfile은 JOIN 쿼리의 결과물이므로 중간 Entity를 만들면 불필요한 복잡도만 증가

### 3.7 Infrastructure DAO - MatchingResultQueryDao 수정 (id 포함)

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/matching/dao/MatchingResultQueryDao.kt`

```kotlin
package com.konkuk.ma.domain.matching.dao

import com.konkuk.ma.domain.matching.domain.MatchingResult
import com.konkuk.ma.domain.matching.entity.table.MatchingResultTable
import org.springframework.stereotype.Component

@Component
class MatchingResultQueryDao {
    fun findByTargetInfoIds(targetInfoIds: List<Long>): List<MatchingResult> {
        if (targetInfoIds.isEmpty()) return emptyList()
        return MatchingResultTable
            .select(
                MatchingResultTable.id,                               // 추가
                MatchingResultTable.registerEmail,
                MatchingResultTable.targetInfoId,
                MatchingResultTable.targetEmail,
                MatchingResultTable.middleNumberMatched,
                MatchingResultTable.lastNumberMatched,
                MatchingResultTable.yearMatched,
                MatchingResultTable.monthMatched,
                MatchingResultTable.dayMatched,
                MatchingResultTable.regionMatched,
                MatchingResultTable.showingExpiryDate,
                MatchingResultTable.matchingExpiryDate
            )
            .where { MatchingResultTable.targetInfoId inList targetInfoIds }
            .map { row ->
                MatchingResult(
                    id = row[MatchingResultTable.id].value,           // 추가
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

    fun findByRegisterEmail(email: String): List<MatchingResult> {    // 추가
        return MatchingResultTable
            .select(
                MatchingResultTable.id,
                MatchingResultTable.registerEmail,
                MatchingResultTable.targetInfoId,
                MatchingResultTable.targetEmail,
                MatchingResultTable.middleNumberMatched,
                MatchingResultTable.lastNumberMatched,
                MatchingResultTable.yearMatched,
                MatchingResultTable.monthMatched,
                MatchingResultTable.dayMatched,
                MatchingResultTable.regionMatched,
                MatchingResultTable.showingExpiryDate,
                MatchingResultTable.matchingExpiryDate
            )
            .where { MatchingResultTable.registerEmail eq email }
            .map { row ->
                MatchingResult(
                    id = row[MatchingResultTable.id].value,
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

- 기존 `findByTargetInfoIds`에도 `id` 선택과 매핑 추가
- `findByRegisterEmail`: 기존 계획(3.7절)에서 설계한 메서드. `id` 포함하여 반환
- `MatchingResultTable.id.value`: Exposed의 `LongIdTable`은 `EntityID<Long>` 타입이므로 `.value`로 Long 추출

### 3.8 Infrastructure Repository - MatchingResultCoreRepository 수정

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
        return MatchingResults(matchingResultQueryDao.findByTargetInfoIds(targetInfoIds))
    }

    override fun deleteExpiredMatchingResults(baseDate: LocalDate): Int {
        return matchingResultCommandDao.deleteExpired(baseDate)
    }

    override fun findByRegisterEmail(email: String): MatchingResults {  // 추가
        return MatchingResults(matchingResultQueryDao.findByRegisterEmail(email))
    }
}
```

- `findByRegisterEmail`: DAO 결과를 `MatchingResults` 일급 컬렉션으로 감싸서 반환
- DAO가 이미 도메인 `MatchingResult`를 반환하므로 별도 변환 불필요

### 3.9 Domain - MatchingResults 일급 컬렉션에 targetEmails 추출 메서드 추가

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

    fun targetEmails(): Set<String> {                                     // 추가
        return data.map { it.targetEmail }.toSet()
    }

    private fun createUniqueKeys(): Set<Pair<Long, String>> {
        return data.map { it.createUniqueKey() }.toSet()
    }

    fun filterNew(existing: MatchingResults): MatchingResults {
        val existingKeys = existing.createUniqueKeys()
        return MatchingResults(data.filter { it.createUniqueKey() !in existingKeys })
    }

    fun classifyByShowingExpiry(now: LocalDateTime): ClassifiedMatchingResults {
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

- `targetEmails()`: 매칭 상대 이메일 목록을 `Set`으로 반환 (중복 제거)
- Controller에서 `ClassifiedMatchingResults`의 양쪽 결과에서 targetEmail을 수집할 때 사용

### 3.10 Domain - ClassifiedMatchingResults에 전체 targetEmails 추출 메서드 추가

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/matching/domain/ClassifiedMatchingResults.kt`

```kotlin
package com.konkuk.ma.domain.matching.domain

class ClassifiedMatchingResults(
    val recentResults: MatchingResults,
    val olderResults: MatchingResults
) {
    fun allTargetEmails(): Set<String> {                                  // 추가
        return recentResults.targetEmails() + olderResults.targetEmails()
    }
}
```

- `allTargetEmails()`: recent + older의 모든 targetEmail을 합산. `Set` 연산으로 중복 자동 제거
- Controller에서 한 번의 호출로 모든 매칭 상대 이메일을 수집할 수 있음

### 3.11 Boot API Response - MatchingResultResponse 변경

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/matching/api/response/MatchingResultResponse.kt`

```kotlin
package com.konkuk.ma.domain.matching.api.response

import com.konkuk.ma.domain.matching.domain.MatchingPartnerProfile
import com.konkuk.ma.domain.matching.domain.MatchingResult

class MatchingResultResponse(
    val matchingResultId: Long,
    val partnerProfileImageUrl: String?,
    val partnerName: String,
    val partnerNickname: String,
    val remainingDays: Long,
    val matchRate: Int
) {
    companion object {
        fun from(
            matchingResult: MatchingResult,
            partnerProfile: MatchingPartnerProfile?
        ): MatchingResultResponse {
            return MatchingResultResponse(
                matchingResultId = matchingResult.id,
                partnerProfileImageUrl = partnerProfile?.profileImageUrl,
                partnerName = partnerProfile?.name ?: "",
                partnerNickname = partnerProfile?.nickname ?: "",
                remainingDays = matchingResult.getRemainingDays(),
                matchRate = matchingResult.matchRate
            )
        }
    }
}
```

- 기존 `matched` 필드 6개 + `targetInfoId` + `targetEmail` 모두 제거
- `matchingResultId`: `MatchingResult.id` (DB PK)
- `partnerProfileImageUrl`: 매칭 상대의 프로필 사진 URL. 사진 미등록 시 `null`
- `partnerName`: 매칭 상대의 실명
- `partnerNickname`: 매칭 상대의 닉네임
- `remainingDays`: 기존 `MatchingResult.getRemainingDays()` 활용 (showingExpiryDate까지 남은 일수)
- `matchRate`: `MatchingResult.matchRate` (lazy 프로퍼티, MatchRateCalculator로 계산된 퍼센트)
- `partnerProfile`이 `null`인 경우 (탈퇴 회원 등): 이름/닉네임을 빈 문자열로 처리

### 3.12 Boot API Response - MatchingResultsResponse 변경

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/matching/api/response/MatchingResultsResponse.kt`

```kotlin
package com.konkuk.ma.domain.matching.api.response

import com.konkuk.ma.domain.matching.domain.ClassifiedMatchingResults
import com.konkuk.ma.domain.matching.domain.MatchingPartnerProfile

class MatchingResultsResponse(
    val recentMatchingResults: List<MatchingResultResponse>,
    val olderMatchingResults: List<MatchingResultResponse>
) {
    companion object {
        fun from(
            classified: ClassifiedMatchingResults,
            partnerProfileMap: Map<String, MatchingPartnerProfile>
        ): MatchingResultsResponse {
            return MatchingResultsResponse(
                recentMatchingResults = classified.recentResults.data
                    .map { MatchingResultResponse.from(it, partnerProfileMap[it.targetEmail]) },
                olderMatchingResults = classified.olderResults.data
                    .map { MatchingResultResponse.from(it, partnerProfileMap[it.targetEmail]) }
            )
        }
    }
}
```

- `partnerProfileMap`: email -> MatchingPartnerProfile 맵을 받아 각 MatchingResult의 targetEmail로 O(1) 조회
- `partnerProfileMap[it.targetEmail]`이 null인 경우: 탈퇴한 회원. `MatchingResultResponse.from`에서 안전하게 처리

### 3.13 Boot API Controller - MatchingResultQueryApi 변경

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
        val partnerProfileMap = matchingResultQueryService.findPartnerProfiles(
            classified.allTargetEmails()
        )
        return MatchingResultsResponse.from(classified, partnerProfileMap)
    }
}
```

- Controller에서 2번의 서비스 호출: (1) 매칭 결과 조회 + 분류, (2) 매칭 상대 프로필 일괄 조회
- N+1 방지: `classified.allTargetEmails()`로 모든 상대 이메일을 한 번에 수집 후 IN 쿼리 1회
- 총 DB 쿼리 수: 2회 (MATCHING_RESULTS SELECT 1회 + MEMBERS LEFT JOIN MEMBER_PHOTOS 1회)

### 3.14 Domain Port - MatchingResultRepository 확장

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/matching/domain/port/MatchingResultRepository.kt`

```kotlin
package com.konkuk.ma.domain.matching.domain.port

import com.konkuk.ma.domain.matching.domain.MatchingResults
import java.time.LocalDate

interface MatchingResultRepository {
    fun saveAll(matchingResults: MatchingResults)
    fun findExistingMatchingResults(targetInfoIds: List<Long>): MatchingResults
    fun deleteExpiredMatchingResults(baseDate: LocalDate): Int
    fun findByRegisterEmail(email: String): MatchingResults              // 추가
}
```

- 기존 계획에서 이미 설계된 메서드. 변경 없음

### 3.15 DDL 변경 - REGISTER_EMAIL 인덱스 추가

**파일**: `infrastructure/storage/ma-db-core/src/main/resources/script/ddl.sql`

기존 MATCHING_RESULTS 테이블의 인덱스 부분에 추가:

```sql
-- 기존
    INDEX idx_matching_expiry_date (MATCHING_EXPIRY_DATE)

-- 변경 후
    INDEX idx_matching_expiry_date (MATCHING_EXPIRY_DATE),
    INDEX idx_matching_result_register_email (REGISTER_EMAIL)
```

- `REGISTER_EMAIL`로 조회하는 새 DAO 메서드(`findByRegisterEmail`)가 추가되므로 인덱스 필요
- 인덱스 없으면 회원별 매칭 결과 조회 시 full table scan 발생

### 3.16 Test - MatchingResultQueryApi 컨트롤러 테스트 변경

**파일**: `boot/ma-boot-web/src/test/kotlin/com/konkuk/ma/domain/matching/api/MatchingResultQueryApiTest.kt`

```kotlin
package com.konkuk.ma.domain.matching.api

import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.matching.application.MatchingResultQueryService
import com.konkuk.ma.domain.matching.domain.ClassifiedMatchingResults
import com.konkuk.ma.domain.matching.domain.MatchingPartnerProfile
import com.konkuk.ma.domain.matching.domain.MatchingResult
import com.konkuk.ma.domain.matching.domain.MatchingResults
import com.konkuk.ma.extension.ARRAY
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
            id = 1L,
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
            id = 2L,
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

        val partnerProfileMap = mapOf(
            "target1@example.com" to MatchingPartnerProfile(
                email = "target1@example.com",
                name = "김철수",
                nickname = "철수닉네임",
                profileImageUrl = "/photos/target1.jpg"
            ),
            "target2@example.com" to MatchingPartnerProfile(
                email = "target2@example.com",
                name = "이영희",
                nickname = "영희닉네임",
                profileImageUrl = null
            )
        )

        every { matchingResultQueryService.findByEmail("test@example.com") } returns classified
        every {
            matchingResultQueryService.findPartnerProfiles(setOf("target1@example.com", "target2@example.com"))
        } returns partnerProfileMap

        // When & Then
        mockMvc.getJson("/api/matching-results") {}
            .andExpect {
                status { isOk() }
                jsonPath("$.recentMatchingResults").isArray
                jsonPath("$.recentMatchingResults[0].matchingResultId").value(1)
                jsonPath("$.recentMatchingResults[0].partnerName").value("김철수")
                jsonPath("$.recentMatchingResults[0].partnerNickname").value("철수닉네임")
                jsonPath("$.recentMatchingResults[0].partnerProfileImageUrl").value("/photos/target1.jpg")
                jsonPath("$.recentMatchingResults[0].matchRate").isNumber
                jsonPath("$.recentMatchingResults[0].remainingDays").isNumber
                jsonPath("$.olderMatchingResults").isArray
                jsonPath("$.olderMatchingResults[0].matchingResultId").value(2)
                jsonPath("$.olderMatchingResults[0].partnerName").value("이영희")
                jsonPath("$.olderMatchingResults[0].partnerProfileImageUrl").doesNotExist()
            }
            .andDocument(
                "matching/find-matching-results",
                responseBody(
                    "recentMatchingResults" responseType ARRAY means "아직 보여주는 기간인 매칭 결과 목록",
                    "recentMatchingResults[].matchingResultId" responseType NUMBER means "매칭 결과 ID",
                    "recentMatchingResults[].partnerProfileImageUrl" responseType STRING means "매칭 상대 프로필 사진 URL (없으면 null)",
                    "recentMatchingResults[].partnerName" responseType STRING means "매칭 상대 이름",
                    "recentMatchingResults[].partnerNickname" responseType STRING means "매칭 상대 닉네임",
                    "recentMatchingResults[].remainingDays" responseType NUMBER means "보여주기 만료까지 남은 일수",
                    "recentMatchingResults[].matchRate" responseType NUMBER means "매칭률 (%)",
                    "olderMatchingResults" responseType ARRAY means "보여주는 기간이 지난 매칭 결과 목록",
                    "olderMatchingResults[].matchingResultId" responseType NUMBER means "매칭 결과 ID",
                    "olderMatchingResults[].partnerProfileImageUrl" responseType STRING means "매칭 상대 프로필 사진 URL (없으면 null)",
                    "olderMatchingResults[].partnerName" responseType STRING means "매칭 상대 이름",
                    "olderMatchingResults[].partnerNickname" responseType STRING means "매칭 상대 닉네임",
                    "olderMatchingResults[].remainingDays" responseType NUMBER means "보여주기 만료까지 남은 일수",
                    "olderMatchingResults[].matchRate" responseType NUMBER means "매칭률 (%)"
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
        every { matchingResultQueryService.findPartnerProfiles(emptySet()) } returns emptyMap()

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

### 3.17 Test - MatchingResult 도메인 테스트 (matchRate 검증)

**파일**: `domain/ma-domain-core/src/test/kotlin/com/konkuk/ma/domain/matching/domain/MatchingResultTest.kt`

기존 테스트에 추가:

```kotlin
context("matchRate") {

    test("전화번호 중간자리 + 생년월일 전체 + 지역 일치 시 99% 반환") {
        val result = MatchingResult(
            registerEmail = "test@example.com",
            targetInfoId = 1L,
            targetEmail = "target@example.com",
            middleNumberMatched = true,
            lastNumberMatched = true,
            yearMatched = true,
            monthMatched = true,
            dayMatched = true,
            regionMatched = true
        )

        result.matchRate shouldBe 99
    }

    test("아무것도 일치하지 않으면 0% 반환") {
        val result = MatchingResult(
            registerEmail = "test@example.com",
            targetInfoId = 1L,
            targetEmail = "target@example.com",
            middleNumberMatched = false,
            lastNumberMatched = false,
            yearMatched = false,
            monthMatched = false,
            dayMatched = false,
            regionMatched = false
        )

        result.matchRate shouldBe 0
    }
}
```

## 4. 구현 순서

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 1 | `domain/.../matching/domain/MatchingResult.kt` | 수정 | `id: Long` 필드 추가 (기본값 0L) |
| 2 | `domain/.../matching/domain/MatchingPartnerProfile.kt` | 신규 | 매칭 상대 프로필 도메인 모델 |
| 3 | `domain/.../matching/domain/MatchingResults.kt` | 수정 | `targetEmails()` 메서드 추가 |
| 4 | `domain/.../matching/domain/ClassifiedMatchingResults.kt` | 수정 | `allTargetEmails()` 메서드 추가 |
| 5 | `domain/.../matching/domain/port/MatchingPartnerQueryRepository.kt` | 신규 | 매칭 상대 프로필 조회 포트 |
| 6 | `domain/.../matching/domain/port/MatchingResultRepository.kt` | 수정 | `findByRegisterEmail` 추가 |
| 7 | `domain/.../matching/application/MatchingResultQueryService.kt` | 수정 | `findPartnerProfiles` 메서드 추가, 생성자에 `MatchingPartnerQueryRepository` 추가 |
| 8 | `infrastructure/.../matching/dao/MatchingResultQueryDao.kt` | 수정 | `id` 포함 조회 + `findByRegisterEmail` 추가 |
| 9 | `infrastructure/.../matching/dao/MatchingPartnerQueryDao.kt` | 신규 | Member LEFT JOIN MemberPhoto 쿼리 |
| 10 | `infrastructure/.../matching/repository/MatchingResultCoreRepository.kt` | 수정 | `findByRegisterEmail` 위임 |
| 11 | `infrastructure/.../matching/repository/MatchingPartnerQueryCoreRepository.kt` | 신규 | 포트 구현체 (DAO 위임) |
| 12 | `infrastructure/.../script/ddl.sql` | 수정 | REGISTER_EMAIL 인덱스 추가 |
| 13 | `boot/.../matching/api/response/MatchingResultResponse.kt` | 수정 | 응답 필드 변경 (matched 제거, 프로필/매칭률 추가) |
| 14 | `boot/.../matching/api/response/MatchingResultsResponse.kt` | 수정 | `from` 파라미터에 `partnerProfileMap` 추가 |
| 15 | `boot/.../matching/api/MatchingResultQueryApi.kt` | 수정 | 매칭 상대 프로필 조회 호출 추가 |
| 16 | `domain/.../matching/domain/MatchingResultTest.kt` | 수정 | `matchRate` 테스트 추가 |
| 17 | `boot/.../matching/api/MatchingResultQueryApiTest.kt` | 수정 | 변경된 응답 구조에 맞게 테스트 수정 |

## 5. 고려사항

- **N+1 방지 전략**: Controller에서 `classified.allTargetEmails()`로 전체 상대 이메일을 한 번에 수집 후, `findPartnerProfiles`로 IN 쿼리 1회 실행. 총 DB 호출 2회(매칭 결과 1회 + 상대 프로필 1회)

- **MatchingPartnerProfile을 별도 도메인 모델로 분리한 이유**: `Member` 도메인을 직접 반환하면 password, phoneNumber 등 민감 정보가 매칭 컨텍스트로 유출됨. 매칭 조회에 필요한 필드만 포함하는 경량 모델을 사용하여 바운디드 컨텍스트 간 결합도를 낮춤

- **MatchingPartnerQueryRepository를 matching 도메인에 배치한 이유**: 이 포트는 "매칭 상대"라는 매칭 컨텍스트의 개념을 표현함. `MemberQueryRepository`에 추가하면 Member 도메인이 매칭 개념에 의존하게 됨

- **탈퇴 회원 처리**: 매칭 상대가 탈퇴한 경우 `partnerProfileMap`에 해당 이메일이 없으므로 `null`로 전달. `MatchingResultResponse.from`에서 이름/닉네임을 빈 문자열로 처리. 추후 "탈퇴한 회원" 표시가 필요하면 별도 플래그 추가 가능

- **profileImageUrl의 의미**: `MemberPhoto.filePath`를 사용. 이 값은 파일 저장 경로이므로, 클라이언트에서 실제 이미지를 로드하려면 파일 서빙 경로(예: `/api/photos/` prefix)를 합쳐야 할 수 있음. 현재는 DB에 저장된 값을 그대로 반환

- **DDL 인덱스 추가**: `REGISTER_EMAIL` 컬럼에 인덱스 추가. 회원별 매칭 결과 조회 빈도가 높을 것으로 예상되므로 필수. 기존 `idx_matching_expiry_date` 인덱스와 별도로 추가

- **MatchingResult.id 추가의 영향 범위**: 기본값 `0L`이 있으므로 기존 생성자 호출(`saveAll` 시 `MatchingResult` 생성)에는 영향 없음. 단, `findByTargetInfoIds` DAO에서 id를 읽도록 수정 필요 (3.7절)

- **Exposed LEFT JOIN 시 null 처리**: `row.getOrNull(MemberPhotoTable.filePath)` 사용. Exposed에서 LEFT JOIN 결과의 nullable 컬럼 접근 시 `getOrNull`을 사용하지 않으면 NPE 발생 가능. `MemberPhotoTable.filePath`는 원래 NOT NULL이지만 LEFT JOIN으로 인해 null이 될 수 있음

## 6. API 응답 예시

### 정상 응답 (매칭 결과 있음)

```json
{
  "recentMatchingResults": [
    {
      "matchingResultId": 1,
      "partnerProfileImageUrl": "/photos/target1.jpg",
      "partnerName": "김철수",
      "partnerNickname": "철수닉네임",
      "remainingDays": 25,
      "matchRate": 83
    }
  ],
  "olderMatchingResults": [
    {
      "matchingResultId": 2,
      "partnerProfileImageUrl": null,
      "partnerName": "이영희",
      "partnerNickname": "영희닉네임",
      "remainingDays": 0,
      "matchRate": 40
    }
  ]
}
```

### 빈 응답

```json
{
  "recentMatchingResults": [],
  "olderMatchingResults": []
}
```
