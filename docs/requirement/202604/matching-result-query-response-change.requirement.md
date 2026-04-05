# Design: 매칭 결과 조회 API 응답 변경

> 작성일: 2026-04-05
> 상태: Draft

## 1. 설계 개요

로그인한 사용자의 매칭 결과 목록을 조회하는 API(`GET /api/matching-results`)를 신규 구현한다. 응답에는 매칭 상대의 프로필 사진 URL, 이름, 닉네임, 남은 showing 일수, 매칭률, MatchingResultId를 포함한다. MatchingResultQueryDao는 Entity를 반환하고 Entity.toDomain()으로 변환하는 패턴을 적용하며, Service는 비즈니스 로직 없이 조합만 담당한다.

## 2. 아키텍처

```
┌──────────────────────────────────────────────────────────┐
│ boot/ma-boot-web                                         │
│                                                          │
│  MatchingResultQueryApi                                  │
│    GET /api/matching-results                             │
│    └── MatchingResultQueryService.findByRegisterEmail()  │
│    └── 결과를 MatchingResultResponse로 변환              │
└───────────────────────────┬──────────────────────────────┘
                            │ (port)
┌───────────────────────────▼──────────────────────────────┐
│ domain/ma-domain-core                                    │
│                                                          │
│  MatchingResultQueryService(@Service, 조합만 담당)       │
│    + findByRegisterEmail(email: String): MatchingResults │
│    └── matchingResultRepository.findByRegisterEmail()    │
│    └── memberPhotoRepository.findByEmails()              │
│    └── memberQueryRepository.findByEmails()              │
│    └── MatchingResults.withProfiles() 조합               │
│                                                          │
│  MatchingResultRepository(port)                          │
│    + findByRegisterEmail(email: String): MatchingResults │
│                                                          │
│  MemberPhotoRepository(port)                             │
│    + findByEmails(emails: Set<String>):                  │
│        Map<String, MemberPhoto>                          │
│                                                          │
│  MemberQueryRepository(port)                             │
│    + findByEmails(emails: Set<String>): List<Member>     │
│                                                          │
│  MatchingResult(도메인 객체)                              │
│    + id: Long (추가)                                     │
│    + getRemainingDays(): Long (기존)                     │
│    + matchRate: Int (기존 lazy)                          │
│                                                          │
│  MatchingResultWithProfile(도메인 조합 객체)             │
│    + matchingResult: MatchingResult                      │
│    + targetName: String                                  │
│    + targetNickname: String                              │
│    + profileImageUrl: String?                            │
└───────────────────────────┬──────────────────────────────┘
                            │ (implements)
┌───────────────────────────▼──────────────────────────────┐
│ infrastructure/storage/ma-db-core                        │
│                                                          │
│  MatchingResultQueryDao                                  │
│    + findByRegisterEmail(email):                         │
│        List<MatchingResultEntity>                        │
│                                                          │
│  MatchingResultEntity (신규)                             │
│    + from(row: ResultRow): MatchingResultEntity          │
│    + toDomain(): MatchingResult                          │
│                                                          │
│  MemberPhotoQueryDao                                     │
│    + findByEmails(emails: Set<String>):                  │
│        List<MemberPhotoEntity>                           │
│                                                          │
│  MemberQueryDao                                          │
│    + findByEmails(emails: Set<String>):                  │
│        List<MemberEntity>                                │
│                                                          │
│  MatchingResultCoreRepository                            │
│    entity.toDomain() 호출하여 도메인 변환                │
│                                                          │
│  MemberPhotoCoreRepository                               │
│    entity.toDomain() 호출하여 도메인 변환                │
└──────────────────────────────────────────────────────────┘
```

## 3. 상세 설계

### 3.1 Domain - MatchingResult (수정)

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/matching/domain/MatchingResult.kt`

```kotlin
package com.konkuk.ma.domain.matching.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class MatchingResult(
    val id: Long = 0L,  // 추가: DB PK
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
}
```

- `id`: MatchingResult의 DB PK. 응답에 matchingResultId로 노출하기 위해 추가
- 기존 생성 로직(배치 Job에서 `id` 없이 생성)과의 호환성을 위해 기본값 `0L` 설정
- 기존 메서드(`createUniqueKey`, `getRemainingDays`, `matchRate`)는 변경 없음

### 3.2 Domain - MatchingResultWithProfile (신규)

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/matching/domain/MatchingResultWithProfile.kt`

```kotlin
package com.konkuk.ma.domain.matching.domain

class MatchingResultWithProfile(
    val matchingResult: MatchingResult,
    val targetName: String,
    val targetNickname: String,
    val profileImageUrl: String?,
)
```

- 매칭 결과와 상대방 프로필 정보를 조합한 도메인 객체
- API 응답 변환의 소스 역할
- `profileImageUrl`은 nullable: 상대방이 사진을 업로드하지 않았을 수 있음

### 3.3 Domain - MatchingResultsWithProfiles (신규, 일급 컬렉션)

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/matching/domain/MatchingResultsWithProfiles.kt`

```kotlin
package com.konkuk.ma.domain.matching.domain

import com.konkuk.ma.domain.member.domain.Member
import com.konkuk.ma.domain.member.domain.photo.MemberPhoto

class MatchingResultsWithProfiles(
    val data: List<MatchingResultWithProfile>,
) {
    companion object {
        fun combine(
            matchingResults: MatchingResults,
            membersByEmail: Map<String, Member>,
            photosByEmail: Map<String, MemberPhoto>,
        ): MatchingResultsWithProfiles {
            val combined = matchingResults.data.mapNotNull { result ->
                val member = membersByEmail[result.targetEmail] ?: return@mapNotNull null
                val photo = photosByEmail[result.targetEmail]
                MatchingResultWithProfile(
                    matchingResult = result,
                    targetName = member.name,
                    targetNickname = member.nickname,
                    profileImageUrl = photo?.thumbnailPath,
                )
            }
            return MatchingResultsWithProfiles(combined)
        }
    }
}
```

- `combine`: 매칭 결과 리스트와 회원 정보, 사진 정보를 조합하는 팩토리 메서드
- `membersByEmail`에 없는 경우(탈퇴한 회원 등) `mapNotNull`로 안전하게 필터링
- `profileImageUrl`에는 `thumbnailPath`를 사용 (썸네일이 있으면 썸네일, 없으면 null)
- 비즈니스 로직(조합)이 도메인 객체 안에 위치하여 Service는 조합만 담당

### 3.4 Domain Port - MatchingResultRepository (수정)

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/matching/domain/port/MatchingResultRepository.kt`

```kotlin
package com.konkuk.ma.domain.matching.domain.port

import com.konkuk.ma.domain.matching.domain.MatchingResults
import java.time.LocalDate

interface MatchingResultRepository {
    fun saveAll(matchingResults: MatchingResults)
    fun findExistingMatchingResults(targetInfoIds: List<Long>): MatchingResults
    fun deleteExpiredMatchingResults(baseDate: LocalDate): Int
    fun findByRegisterEmail(email: String): MatchingResults  // 추가
}
```

- `findByRegisterEmail`: 로그인한 사용자의 모든 매칭 결과를 조회
- 반환 타입은 일급 컬렉션 `MatchingResults` (포트 규칙 준수)

### 3.5 Domain Port - MemberPhotoRepository (수정)

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/member/domain/port/MemberPhotoRepository.kt`

```kotlin
package com.konkuk.ma.domain.member.domain.port

import com.konkuk.ma.domain.member.domain.photo.MemberPhoto
import com.konkuk.ma.domain.member.domain.photo.NewPhoto

interface MemberPhotoRepository {
    fun save(newPhoto: NewPhoto): Long
    fun findByMemberEmail(email: String): MemberPhoto?
    fun deleteByMemberEmail(email: String)
    fun findByEmails(emails: Set<String>): Map<String, MemberPhoto>  // 추가
}
```

- `findByEmails`: 여러 이메일에 대한 프로필 사진을 벌크 조회하여 N+1 방지
- 반환 타입 `Map<String, MemberPhoto>`: email을 키로 하여 빠른 조회 가능

### 3.6 Domain Port - MemberQueryRepository (수정)

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/member/domain/port/MemberQueryRepository.kt`

```kotlin
package com.konkuk.ma.domain.member.domain.port

import com.konkuk.ma.domain.member.domain.Member

interface MemberQueryRepository {
    fun existsByNickname(nickname: String): Boolean
    fun existsByEmail(email: String): Boolean
    fun findByEmail(email: String): Member
    fun findByNames(names: Set<String>): List<Member>
    fun findByEmails(emails: Set<String>): List<Member>  // 추가
}
```

- `findByEmails`: 매칭 상대의 이름, 닉네임을 가져오기 위한 벌크 조회
- `findByNames`와 동일 패턴, 키만 email로 변경

### 3.7 Domain Service - MatchingResultQueryService (신규)

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/matching/application/MatchingResultQueryService.kt`

```kotlin
package com.konkuk.ma.domain.matching.application

import com.konkuk.ma.domain.matching.domain.MatchingResultsWithProfiles
import com.konkuk.ma.domain.matching.domain.port.MatchingResultRepository
import com.konkuk.ma.domain.member.domain.port.MemberPhotoRepository
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
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
        if (targetEmails.isEmpty()) return MatchingResultsWithProfiles(emptyList())

        val members = memberQueryRepository.findByEmails(targetEmails)
        val membersByEmail = members.associateBy { it.email }
        val photosByEmail = memberPhotoRepository.findByEmails(targetEmails)

        return MatchingResultsWithProfiles.combine(matchingResults, membersByEmail, photosByEmail)
    }
}
```

- Service는 포트만 참조 (규칙 10 준수)
- Service는 조합만 담당: 매칭결과 조회 -> 상대 이메일 추출 -> 회원정보/사진 벌크 조회 -> 도메인 객체에서 조합 (규칙 11 준수)
- `@Transactional(readOnly = true)`: 읽기 전용 트랜잭션
- 빈 결과 조기 반환으로 불필요한 DB 호출 방지

### 3.8 Domain - MatchingResults (수정)

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/matching/domain/MatchingResults.kt`

```kotlin
package com.konkuk.ma.domain.matching.domain

class MatchingResults(
    val data: List<MatchingResult>
) {
    fun targetInfoIds(): List<Long> {
        return data.map { it.targetInfoId }.distinct()
    }

    fun extractTargetEmails(): Set<String> {  // 추가
        return data.map { it.targetEmail }.toSet()
    }

    private fun createUniqueKeys(): Set<Pair<Long, String>> {
        return data.map { it.createUniqueKey() }.toSet()
    }

    fun filterNew(existing: MatchingResults): MatchingResults {
        val existingKeys = existing.createUniqueKeys()
        return MatchingResults(data.filter { it.createUniqueKey() !in existingKeys })
    }

    companion object {
        fun merge(dataList: List<MatchingResults>): MatchingResults {
            return MatchingResults(dataList.flatMap { it.data })
        }
    }
}
```

- `extractTargetEmails()` 추가: 매칭 상대 이메일 목록을 추출하여 벌크 조회에 사용

### 3.9 Infrastructure - MatchingResultEntity (신규)

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/matching/entity/MatchingResultEntity.kt`

```kotlin
package com.konkuk.ma.domain.matching.entity

import com.konkuk.ma.domain.matching.domain.MatchingResult
import com.konkuk.ma.domain.matching.entity.table.MatchingResultTable
import org.jetbrains.exposed.sql.ResultRow
import java.time.LocalDate
import java.time.LocalDateTime

class MatchingResultEntity(
    val id: Long,
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
    val matchingExpiryDate: LocalDate,
) {
    fun toDomain(): MatchingResult {
        return MatchingResult(
            id = id,
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
            matchingExpiryDate = matchingExpiryDate,
        )
    }

    companion object {
        fun from(row: ResultRow): MatchingResultEntity {
            return MatchingResultEntity(
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
                matchingExpiryDate = row[MatchingResultTable.matchingExpiryDate],
            )
        }
    }
}
```

- 규칙 8 준수: DAO는 Entity를 반환, Entity에서 `toDomain()` 변환
- `from(row: ResultRow)`: `companion object` 팩토리 메서드로 ResultRow에서 Entity 생성
- `toDomain()`: 인프라 관심사(컬럼 매핑)와 도메인 변환 로직 분리

### 3.10 Infrastructure - MatchingResultQueryDao (수정)

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/matching/dao/MatchingResultQueryDao.kt`

```kotlin
package com.konkuk.ma.domain.matching.dao

import com.konkuk.ma.domain.matching.entity.MatchingResultEntity
import com.konkuk.ma.domain.matching.entity.table.MatchingResultTable
import org.springframework.stereotype.Component

@Component
class MatchingResultQueryDao {
    fun findByTargetInfoIds(targetInfoIds: List<Long>): List<MatchingResultEntity> {  // 반환타입 변경
        if (targetInfoIds.isEmpty()) return emptyList()
        return MatchingResultTable
            .selectAll()
            .where { MatchingResultTable.targetInfoId inList targetInfoIds }
            .map { row -> MatchingResultEntity.from(row) }
    }

    fun findByRegisterEmail(email: String): List<MatchingResultEntity> {  // 추가
        return MatchingResultTable
            .selectAll()
            .where {
                (MatchingResultTable.registerEmail eq email) and
                    (MatchingResultTable.deleted eq false)
            }
            .map { row -> MatchingResultEntity.from(row) }
    }
}
```

- `findByTargetInfoIds`: 반환 타입을 `List<MatchingResult>` -> `List<MatchingResultEntity>`로 변경 (규칙 8)
- `findByRegisterEmail`: email 기반 조회 추가, `deleted eq false` 조건 포함
- `selectAll()`로 변경하여 `id`를 포함한 전체 컬럼 조회 (기존은 `select()`로 컬럼을 명시적으로 지정했으나, Entity 패턴에서는 `selectAll()`이 적절)
- `import org.jetbrains.exposed.sql.and` 추가 필요

### 3.11 Infrastructure - MatchingResultCoreRepository (수정)

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
        return MatchingResults(
            matchingResultQueryDao.findByTargetInfoIds(targetInfoIds)
                .map { it.toDomain() }  // Entity -> Domain 변환
        )
    }

    override fun deleteExpiredMatchingResults(baseDate: LocalDate): Int {
        return matchingResultCommandDao.deleteExpired(baseDate)
    }

    override fun findByRegisterEmail(email: String): MatchingResults {  // 추가
        return MatchingResults(
            matchingResultQueryDao.findByRegisterEmail(email)
                .map { it.toDomain() }
        )
    }
}
```

- `findExistingMatchingResults`: Entity.toDomain() 패턴으로 변환 (기존에는 DAO에서 직접 도메인 객체를 반환했으나 규칙 8에 맞게 수정)
- `findByRegisterEmail`: 신규 추가, 동일한 Entity -> Domain 변환 패턴

### 3.12 Infrastructure - MemberPhotoQueryDao (수정)

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/member/dao/MemberPhotoQueryDao.kt`

```kotlin
package com.konkuk.ma.domain.member.dao

import com.konkuk.ma.domain.member.entity.MemberPhotoEntity
import com.konkuk.ma.domain.member.entity.table.MemberPhotoTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Component

@Component
class MemberPhotoQueryDao {

    fun findByMemberEmail(email: String): MemberPhotoEntity? {
        return MemberPhotoTable.selectAll()
            .where { (MemberPhotoTable.memberEmail eq email) and (MemberPhotoTable.deleted eq false) }
            .limit(1)
            .firstOrNull()
            ?.let { MemberPhotoEntity.from(it) }
    }

    fun findByEmails(emails: Set<String>): List<MemberPhotoEntity> {  // 추가
        if (emails.isEmpty()) return emptyList()
        return MemberPhotoTable.selectAll()
            .where {
                (MemberPhotoTable.memberEmail inList emails) and
                    (MemberPhotoTable.deleted eq false)
            }
            .map { MemberPhotoEntity.from(it) }
    }
}
```

- `findByEmails`: 여러 이메일에 대한 사진을 한 번의 쿼리로 벌크 조회 (N+1 방지)
- 반환 타입 `List<MemberPhotoEntity>`: 규칙 8 준수
- `deleted eq false` 조건으로 삭제된 사진 제외

### 3.13 Infrastructure - MemberPhotoCoreRepository (수정)

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/member/repository/MemberPhotoCoreRepository.kt`

```kotlin
package com.konkuk.ma.domain.member.repository

import com.konkuk.ma.domain.member.dao.MemberPhotoCommandDao
import com.konkuk.ma.domain.member.dao.MemberPhotoQueryDao
import com.konkuk.ma.domain.member.domain.photo.MemberPhoto
import com.konkuk.ma.domain.member.domain.photo.NewPhoto
import com.konkuk.ma.domain.member.domain.port.MemberPhotoRepository
import org.springframework.stereotype.Repository

@Repository
class MemberPhotoCoreRepository(
    private val memberPhotoCommandDao: MemberPhotoCommandDao,
    private val memberPhotoQueryDao: MemberPhotoQueryDao
) : MemberPhotoRepository {

    override fun save(newPhoto: NewPhoto): Long {
        return memberPhotoCommandDao.save(newPhoto)
    }

    override fun findByMemberEmail(email: String): MemberPhoto? {
        return memberPhotoQueryDao.findByMemberEmail(email)?.toDomain()
    }

    override fun deleteByMemberEmail(email: String) {
        memberPhotoCommandDao.deleteByMemberEmail(email)
    }

    override fun findByEmails(emails: Set<String>): Map<String, MemberPhoto> {  // 추가
        return memberPhotoQueryDao.findByEmails(emails)
            .map { it.toDomain() }
            .associateBy { it.memberEmail }
    }
}
```

- `findByEmails`: Entity -> Domain 변환 후 `associateBy`로 email 키 맵으로 변환
- 한 회원당 사진은 1개이므로 `associateBy`에서 키 충돌 없음

### 3.14 Infrastructure - MemberQueryDao (수정)

기존 MemberQueryDao를 확인해야 합니다. `findByNames`와 동일한 패턴으로 `findByEmails`를 추가합니다.

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/member/dao/MemberQueryDao.kt`

```kotlin
// 기존 메서드들 유지

fun findByEmails(emails: Set<String>): List<MemberEntity> {  // 추가
    if (emails.isEmpty()) return emptyList()
    return MemberTable.selectAll()
        .where { (MemberTable.email inList emails) and (MemberTable.deleted eq false) }
        .map { RowEntityMapper.toMemberEntity(it) }
}
```

- `findByNames`와 동일한 패턴, 조건만 email로 변경
- `RowEntityMapper.toMemberEntity` 활용하여 기존 매핑 로직 재사용

### 3.15 Infrastructure - MemberQueryCoreRepository (수정)

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/member/repository/MemberQueryCoreRepository.kt`

```kotlin
// 기존 메서드들 유지

override fun findByEmails(emails: Set<String>): List<Member> {  // 추가
    return memberQueryDao.findByEmails(emails)
        .map { it.toDomain() }
}
```

- Entity -> Domain 변환 패턴 기존과 동일

### 3.16 Boot - MatchingResultQueryApi (신규)

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/matching/api/MatchingResultQueryApi.kt`

```kotlin
package com.konkuk.ma.domain.matching.api

import com.konkuk.ma.domain.matching.api.response.MatchingResultResponse
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
    fun findMyMatchingResults(
        @AuthenticationPrincipal email: String,
    ): MatchingResultsResponse {
        val results = matchingResultQueryService.findByRegisterEmail(email)
        return MatchingResultsResponse.from(results)
    }
}
```

- `@AuthenticationPrincipal email: String`: JWT 인증된 사용자 이메일 주입 (기존 패턴 따름)
- `findMyMatchingResults`: 동사로 시작하는 메서드명 (clean-code 규칙)
- 컨트롤러는 Service 호출 -> 응답 DTO 변환만 담당

### 3.17 Boot - MatchingResultsResponse (신규)

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/matching/api/response/MatchingResultsResponse.kt`

```kotlin
package com.konkuk.ma.domain.matching.api.response

import com.konkuk.ma.domain.matching.domain.MatchingResultsWithProfiles

class MatchingResultsResponse(
    val matchingResults: List<MatchingResultResponse>,
) {
    companion object {
        fun from(results: MatchingResultsWithProfiles): MatchingResultsResponse {
            return MatchingResultsResponse(
                matchingResults = results.data.map { MatchingResultResponse.from(it) }
            )
        }
    }
}
```

### 3.18 Boot - MatchingResultResponse (신규)

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/matching/api/response/MatchingResultResponse.kt`

```kotlin
package com.konkuk.ma.domain.matching.api.response

import com.konkuk.ma.domain.matching.domain.MatchingResultWithProfile

class MatchingResultResponse(
    val matchingResultId: Long,
    val targetName: String,
    val targetNickname: String,
    val profileImageUrl: String?,
    val remainingDays: Long,
    val matchRate: Int,
) {
    companion object {
        fun from(result: MatchingResultWithProfile): MatchingResultResponse {
            return MatchingResultResponse(
                matchingResultId = result.matchingResult.id,
                targetName = result.targetName,
                targetNickname = result.targetNickname,
                profileImageUrl = result.profileImageUrl,
                remainingDays = result.matchingResult.getRemainingDays(),
                matchRate = result.matchingResult.matchRate,
            )
        }
    }
}
```

- `matchingResultId`: 매칭 결과 PK
- `targetName`: 매칭 상대 이름 (TargetInfo.name 기반으로 가입한 회원의 name)
- `targetNickname`: 매칭 상대 닉네임
- `profileImageUrl`: 프로필 사진 (썸네일) URL, nullable
- `remainingDays`: 남은 showing 일수 (MatchingResult.getRemainingDays())
- `matchRate`: 매칭률 (MatchingResult.matchRate)

### 3.19 DDL - MATCHING_RESULTS 테이블 인덱스 추가

**파일**: `infrastructure/storage/ma-db-core/src/main/resources/script/ddl.sql`

기존 MATCHING_RESULTS 테이블에 `REGISTER_EMAIL` 인덱스를 추가합니다:

```sql
-- MATCHING RESULTS
CREATE TABLE MATCHING_RESULTS
(
    MATCHING_RESULT_ID    BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- MatchingResultTable 특화 컬럼들
    REGISTER_EMAIL        VARCHAR(255) NOT NULL,
    TARGET_INFO_ID        BIGINT       NOT NULL,
    TARGET_EMAIL          VARCHAR(255) NOT NULL,
    MIDDLE_NUMBER_MATCHED BOOLEAN      NOT NULL,
    LAST_NUMBER_MATCHED   BOOLEAN      NOT NULL,
    YEAR_MATCHED          BOOLEAN      NOT NULL,
    MONTH_MATCHED         BOOLEAN      NOT NULL,
    DAY_MATCHED           BOOLEAN      NOT NULL,
    REGION_MATCHED        BOOLEAN      NOT NULL,
    SHOWING_EXPIRY_DATE   DATETIME     NOT NULL,
    MATCHING_EXPIRY_DATE  DATE         NOT NULL,

    -- BaseTable 공통 컬럼들
    CREATED_DATE          DATETIME     DEFAULT CURRENT_TIMESTAMP,
    CREATED_BY            VARCHAR(255) DEFAULT 'MEET_AGAIN',
    LAST_MODIFIED_DATE    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    LAST_MODIFIED_BY      VARCHAR(255) DEFAULT 'MEET_AGAIN',
    DELETED               BOOLEAN      DEFAULT FALSE,

    -- 인덱스
    INDEX idx_matching_expiry_date (MATCHING_EXPIRY_DATE),
    INDEX idx_matching_register_email (REGISTER_EMAIL)  -- 추가
);
```

- `REGISTER_EMAIL` 인덱스 추가: 사용자별 매칭 결과 조회 성능 보장
- 기존 `MATCHING_EXPIRY_DATE` 인덱스 유지
- FK 사용 금지 규칙 준수

## 4. 구현 순서

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 1 | `domain/.../matching/domain/MatchingResult.kt` | 수정 | `id: Long = 0L` 필드 추가 |
| 2 | `domain/.../matching/domain/MatchingResults.kt` | 수정 | `extractTargetEmails()` 메서드 추가 |
| 3 | `domain/.../matching/domain/MatchingResultWithProfile.kt` | 신규 | 매칭결과 + 프로필 조합 도메인 객체 |
| 4 | `domain/.../matching/domain/MatchingResultsWithProfiles.kt` | 신규 | 일급 컬렉션 + combine 팩토리 메서드 |
| 5 | `domain/.../matching/domain/port/MatchingResultRepository.kt` | 수정 | `findByRegisterEmail()` 추가 |
| 6 | `domain/.../member/domain/port/MemberPhotoRepository.kt` | 수정 | `findByEmails()` 추가 |
| 7 | `domain/.../member/domain/port/MemberQueryRepository.kt` | 수정 | `findByEmails()` 추가 |
| 8 | `domain/.../matching/application/MatchingResultQueryService.kt` | 신규 | 조회 Service (조합만 담당) |
| 9 | `infrastructure/.../matching/entity/MatchingResultEntity.kt` | 신규 | Entity + toDomain() + from() |
| 10 | `infrastructure/.../matching/dao/MatchingResultQueryDao.kt` | 수정 | 반환타입 Entity로 변경 + findByRegisterEmail 추가 |
| 11 | `infrastructure/.../matching/repository/MatchingResultCoreRepository.kt` | 수정 | Entity.toDomain() 변환 + findByRegisterEmail 구현 |
| 12 | `infrastructure/.../member/dao/MemberPhotoQueryDao.kt` | 수정 | `findByEmails()` 추가 |
| 13 | `infrastructure/.../member/repository/MemberPhotoCoreRepository.kt` | 수정 | `findByEmails()` 구현 |
| 14 | `infrastructure/.../member/dao/MemberQueryDao.kt` | 수정 | `findByEmails()` 추가 |
| 15 | `infrastructure/.../member/repository/MemberQueryCoreRepository.kt` | 수정 | `findByEmails()` 구현 |
| 16 | `infrastructure/.../script/ddl.sql` | 수정 | REGISTER_EMAIL 인덱스 추가 |
| 17 | `boot/.../matching/api/response/MatchingResultResponse.kt` | 신규 | 개별 매칭결과 응답 DTO |
| 18 | `boot/.../matching/api/response/MatchingResultsResponse.kt` | 신규 | 매칭결과 목록 응답 DTO |
| 19 | `boot/.../matching/api/MatchingResultQueryApi.kt` | 신규 | GET /api/matching-results 컨트롤러 |

## 5. 고려사항

- **MatchingResultQueryDao 반환타입 변경 영향**: 기존 `findByTargetInfoIds`의 반환 타입을 `List<MatchingResult>`에서 `List<MatchingResultEntity>`로 변경한다. 이 메서드를 호출하는 `MatchingResultCoreRepository.findExistingMatchingResults`에서 `.map { it.toDomain() }` 변환이 필요하다. 배치 Job에서 간접적으로 사용되므로 기존 동작에 영향 없음을 확인해야 한다.

- **MatchingResult.id 추가 시 기존 호환성**: 기존 배치 Job에서 `MatchingResult`를 생성할 때 `id`를 전달하지 않으므로 기본값 `0L`로 처리된다. `createUniqueKey()`는 `targetInfoId`와 `targetEmail` 기반이므로 영향 없음.

- **profileImageUrl에 thumbnailPath 사용**: `MemberPhoto.filePath`(원본)가 아닌 `thumbnailPath`(썸네일)를 API 응답에 사용한다. 썸네일이 없으면(`null`) 클라이언트에서 기본 이미지로 대체할 수 있다. 필요시 `filePath`를 fallback으로 사용하는 로직을 `MemberPhoto` 도메인 객체에 추가할 수 있다.

- **N+1 쿼리 방지**: 매칭 결과에서 상대방 이메일을 추출 -> 벌크로 회원정보/사진 조회 -> 메모리에서 조합. DB 쿼리는 총 3회(매칭결과 1회, 회원정보 1회, 사진 1회)로 고정.

- **탈퇴한 회원 처리**: `membersByEmail`에 해당 이메일이 없는 경우(탈퇴 등) `MatchingResultsWithProfiles.combine`에서 `mapNotNull`로 해당 결과를 제외한다. 필요시 "알 수 없는 사용자"로 표시하는 정책으로 변경 가능.

- **REGISTER_EMAIL 인덱스**: 매칭 결과 조회 시 `WHERE REGISTER_EMAIL = ?` 쿼리의 성능을 위해 인덱스 추가. 기존에는 `MATCHING_EXPIRY_DATE` 인덱스만 있었음.

- **RowEntityMapper 활용**: `MatchingResultEntity`는 `companion object`의 `from(row)` 팩토리 메서드를 사용하고, `RowEntityMapper`에는 추가하지 않는다. `MemberPhotoEntity`와 동일한 패턴(Entity 내부에 `from` 정의)을 따르며, `RowEntityMapper`는 기존 Entity들에서만 사용하는 레거시 패턴으로 본다.
