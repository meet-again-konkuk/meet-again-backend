# Design: Email Value Object 도입

> 작성일: 2026-04-09
> 상태: Draft

## 1. 설계 개요

프로젝트 전반에서 `email: String`으로 사용되는 이메일 값을 `Email` Value Object로 원시값 포장하여, 타입 안전성을 확보하고 이메일 유효성 검증을 도메인 레이어에서 일관되게 수행한다.

## 2. 아키텍처

### 2.1 Email Value Object 배치 위치

`Email`은 `member` 도메인뿐 아니라 `matching`, `community`, `auth` 등 모든 도메인에서 사용되므로, **공통 도메인 패키지**(`common.domain`)에 배치한다.

```
domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/common/domain/
  └── Email.kt
```

### 2.2 레이어별 변환 전략

```
┌─────────────────────────────────────────────────────────┐
│ boot/ma-boot-web                                        │
│                                                         │
│  Request DTO: email: String (Bean Validation 유지)      │
│    └── toCommand() 에서 Email(email)로 변환             │
│                                                         │
│  Controller: @AuthenticationPrincipal email: String      │
│    └── Email(email)로 변환 후 Service에 전달            │
│                                                         │
│  Response DTO: email: String (JSON 직렬화 유지)         │
│    └── Email.value로 언패킹                             │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│ domain/ma-domain-core                                   │
│                                                         │
│  Domain Model: email: Email (타입 안전)                 │
│  Port Interface: email: Email                           │
│  Application Service: email: Email                      │
│  Command: email: Email                                  │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│ infrastructure                                          │
│                                                         │
│  JwtManager: email: String (JWT subject)                │
│    └── getEmailFromToken() → String 반환 유지           │
│    └── generateAccessToken(email: Email)                │
│         → email.value를 JWT subject로 저장              │
│                                                         │
│  Entity: email: String (DB 컬럼 매핑)                   │
│    └── toDomain()에서 Email(email)로 변환               │
│                                                         │
│  DAO: email: String (Exposed DSL 쿼리)                  │
│    └── Repository에서 email.value로 언패킹 후 전달      │
└─────────────────────────────────────────────────────────┘
```

### 2.3 경계별 변환 요약

| 경계 | 입력 | 출력 | 변환 위치 |
|------|------|------|-----------|
| HTTP Request → Controller | `String` | `Email` | Controller에서 `Email(email)` |
| JWT → Controller | `String` (principal) | `Email` | Controller에서 `Email(email)` |
| Controller → Service | `Email` | - | 이미 Email |
| Service → Port | `Email` | - | 이미 Email |
| Port → Repository(구현체) | `Email` | `String` | Repository에서 `email.value` |
| Repository → DAO | `String` | `String` | 그대로 전달 |
| DAO → Entity | `String` | `String` | Entity 생성 |
| Entity → Domain | `String` | `Email` | `toDomain()`에서 `Email(email)` |
| Domain → Response DTO | `Email` | `String` | Response에서 `email.value` |

## 3. 상세 설계

### 3.1 Domain Common - Email Value Object

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/common/domain/Email.kt`
**변경 유형**: 신규

```kotlin
package com.konkuk.ma.domain.common.domain

data class Email(val value: String) {
    init {
        require(value.isNotBlank()) { "이메일은 비어있을 수 없습니다." }
        require(EMAIL_REGEX.matches(value)) { "유효하지 않은 이메일 형식입니다: $value" }
    }

    override fun toString(): String = value

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}
```

- `data class`로 선언하여 `equals()`, `hashCode()` 자동 생성 (Map key, Set 요소로 안전하게 사용 가능)
- `toString()`을 `value`로 오버라이드하여 로그/예외 메시지에서 자연스럽게 출력
- 기존 `FourDigit`, `Year`, `Month`, `Day`와 동일한 패턴 (init 블록에서 유효성 검증)
- 정규식은 Bean Validation의 `@Email`보다 엄격하지 않게 설정 (도메인 레벨 기본 검증)

### 3.2 Domain - Member

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/member/domain/Member.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.member.domain

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.common.domain.date.Day
import com.konkuk.ma.domain.common.domain.date.Month
import com.konkuk.ma.domain.common.domain.date.Year
import java.time.LocalDate

class Member(
    val id: Long = 0L,
    val email: Email,           // String → Email
    val password: String,
    val nickname: String,
    val gender: Gender,
    val phoneNumber: PhoneNumber,
    val name: String,
    val region: Region,
    val birthDate: LocalDate,
    val highSchool: String?,
    val university: String?
) {
    companion object {
        fun create(
            id: Long = 0L,
            email: String,          // 팩토리는 String을 받아 Email로 변환
            password: String,
            nickname: String,
            gender: Gender,
            phoneNumber: String,
            name: String,
            region: Region,
            birthDate: LocalDate,
            highSchool: String?,
            university: String?
        ): Member {
            return Member(
                id = id,
                email = Email(email),   // 변환
                password = password,
                nickname = nickname,
                gender = gender,
                phoneNumber = PhoneNumber(phoneNumber),
                name = name,
                region = region,
                birthDate = birthDate,
                highSchool = highSchool,
                university = university
            )
        }
    }

    fun getOtherGender(): Gender {
        return gender.getOtherGender()
    }

    fun getYear(): Year {
        return Year(birthDate.year)
    }

    fun getMonth(): Month {
        return Month(birthDate.monthValue)
    }

    fun getDay(): Day {
        return Day(birthDate.dayOfMonth)
    }
}
```

- `Member.create()` 팩토리 메서드의 `email` 파라미터는 `String`을 유지 → Entity의 `toDomain()`에서 호출하기 편리
- `val email: Email`로 변경하여 도메인 내부에서는 항상 `Email` 타입 사용
- `Member.email` 참조하는 모든 곳에서 `.value`가 필요한 경우 확인 필요

### 3.3 Domain - NewMember

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/member/domain/NewMember.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.member.domain

import com.konkuk.ma.domain.common.domain.Email  // 추가
import java.time.LocalDate

class NewMember(
    val email: Email,           // String → Email
    val password: String,
    val nickname: String,
    val gender: Gender,
    val phoneNumber: PhoneNumber,
    val name: String,
    val birthDate: LocalDate,
    val region: Region,
    val highSchool: String?,
    val university: String?
)
```

### 3.4 Domain - Members (일급 컬렉션)

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/member/domain/Members.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.member.domain

import com.konkuk.ma.domain.common.domain.Email  // 추가

class Members(val data: List<Member>) {

    private val nicknameByEmail: Map<Email, String> by lazy {  // String → Email
        data.associate { it.email to it.nickname }
    }

    fun findOne(email: Email): Member? = data.find { it.email == email }  // String → Email

    fun findNickname(email: Email): String {  // String → Email
        return nicknameByEmail[email] ?: UNKNOWN_NICKNAME
    }

    companion object {
        private const val UNKNOWN_NICKNAME = "알 수 없음"
    }
}
```

### 3.5 Domain - MemberPhoto

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/member/domain/photo/MemberPhoto.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.member.domain.photo

import com.konkuk.ma.domain.common.domain.Email  // 추가

class MemberPhoto(
    val id: Long,
    val memberEmail: Email,     // String → Email
    val filePath: String,
    val originalFileName: String,
    val approvalStatus: ApprovalStatus,
    val thumbnailPath: String? = null
) {
    fun belongsTo(email: Email): Boolean = memberEmail == email  // String → Email

    fun hasThumbnail(): Boolean = thumbnailPath != null
}
```

### 3.6 Domain - MemberPhotos (일급 컬렉션)

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/member/domain/photo/MemberPhotos.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.member.domain.photo

import com.konkuk.ma.domain.common.domain.Email  // 추가

class MemberPhotos(val data: List<MemberPhoto>) {

    fun findOne(email: Email): MemberPhoto? = data.find { it.memberEmail == email }  // String → Email
}
```

### 3.7 Domain - NewPhoto

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/member/domain/photo/NewPhoto.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.member.domain.photo

import com.konkuk.ma.domain.common.domain.Email  // 추가

class NewPhoto(
    val memberEmail: Email,     // String → Email
    val filePath: String,
    val originalFileName: String,
    val thumbnailPath: String? = null
) {
    companion object {
        fun create(
            memberEmail: Email,     // String → Email
            filePath: String,
            originalFileName: String,
            thumbnailPath: String? = null
        ): NewPhoto {
            return NewPhoto(
                memberEmail = memberEmail,
                filePath = filePath,
                originalFileName = originalFileName,
                thumbnailPath = thumbnailPath
            )
        }
    }
}
```

### 3.8 Domain - MemberPhotoProcessor

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/member/domain/photo/MemberPhotoProcessor.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.member.domain.photo

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.common.domain.file.PhotoFile
import com.konkuk.ma.domain.common.domain.file.StorageDomainType
import com.konkuk.ma.domain.common.domain.file.StoragePath
import com.konkuk.ma.domain.common.domain.file.StorageUsageType
import com.konkuk.ma.domain.common.domain.file.port.FileStorage
import com.konkuk.ma.domain.common.domain.file.port.ThumbnailGenerator
import com.konkuk.ma.logger
import org.springframework.stereotype.Component

@Component
class MemberPhotoProcessor(
    private val fileStorage: FileStorage,
    private val thumbnailGenerator: ThumbnailGenerator
) {

    fun process(email: Email, photoFile: PhotoFile): ProcessedPhoto {  // String → Email
        val filePath = storeOriginal(email, photoFile)
        val thumbnailPath = storeThumbnail(email, photoFile)
        return ProcessedPhoto(filePath, thumbnailPath)
    }

    fun deleteFiles(photo: MemberPhoto) {
        fileStorage.delete(photo.filePath)
        if (photo.hasThumbnail()) {
            fileStorage.delete(photo.thumbnailPath!!)
        }
    }

    private fun storeOriginal(email: Email, photoFile: PhotoFile): String {  // String → Email
        val directory = StoragePath.of(StorageDomainType.MEMBER, StorageUsageType.PROFILE, email.value)  // .value로 언패킹
        return fileStorage.store(directory.value, photoFile)
    }

    private fun storeThumbnail(email: Email, photoFile: PhotoFile): String? {  // String → Email
        return try {
            val thumbnailBytes = thumbnailGenerator.generate(photoFile.content, THUMBNAIL_WIDTH)
            val directory = StoragePath.of(StorageDomainType.MEMBER, StorageUsageType.THUMBNAIL, email.value)  // .value로 언패킹
            fileStorage.storeBytes(directory.value, "thumb_${photoFile.originalFileName}", thumbnailBytes)
        } catch (e: Exception) {
            logger.warn { "썸네일 생성 실패 (email=${email.value}): ${e.message}" }
            null
        }
    }

    companion object {
        private const val THUMBNAIL_WIDTH = 400
    }
}
```

- `StoragePath.of()`는 파일 경로 조합이므로 `email.value` (String)를 전달
- `StoragePath.of()`와 `StoragePath.withDate()`의 `email` 파라미터도 `Email`로 변경 가능하나, 파일 경로는 인프라 관심사이므로 `String`을 유지하고 호출부에서 `.value`로 언패킹

### 3.9 Domain - Port: MemberQueryRepository

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/member/domain/port/MemberQueryRepository.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.member.domain.port

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.member.domain.Member

interface MemberQueryRepository {
    fun existsByNickname(nickname: String): Boolean
    fun existsByEmail(email: Email): Boolean            // String → Email
    fun findOne(email: Email): Member                   // String → Email
    fun findByNames(names: Set<String>): List<Member>
    fun findByEmails(emails: Set<Email>): List<Member>  // Set<String> → Set<Email>
}
```

### 3.10 Domain - Port: MemberPhotoRepository

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/member/domain/port/MemberPhotoRepository.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.member.domain.port

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.member.domain.photo.MemberPhoto
import com.konkuk.ma.domain.member.domain.photo.NewPhoto

interface MemberPhotoRepository {
    fun save(newPhoto: NewPhoto): Long
    fun findOne(email: Email): MemberPhoto?     // String → Email
    fun delete(email: Email)                    // String → Email
    fun find(emails: Set<Email>): List<MemberPhoto>  // Set<String> → Set<Email>
}
```

### 3.11 Domain - Port: MatchingResultRepository

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/matching/domain/port/MatchingResultRepository.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.matching.domain.port

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.matching.domain.MatchingResult
import com.konkuk.ma.domain.matching.domain.NewMatchingResult
import java.time.LocalDate

interface MatchingResultRepository {
    fun saveAll(matchingResults: List<NewMatchingResult>)
    fun findExistingMatchingResults(targetInfoIds: List<Long>): List<MatchingResult>
    fun deleteExpiredMatchingResults(baseDate: LocalDate): Int
    fun deleteExcludedExpiredMatchingResults(baseDate: LocalDate): Int
    fun find(email: Email, excluded: Boolean = false): List<MatchingResult>  // String → Email
    fun findOne(matchingResultId: Long): MatchingResult
    fun updateExcluded(matchingResult: MatchingResult)
}
```

### 3.12 Domain - Port: RefreshTokenRepository

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/auth/domain/port/RefreshTokenRepository.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.auth.domain.port

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.auth.domain.RefreshToken

interface RefreshTokenRepository {
    fun save(refreshToken: RefreshToken)
    fun delete(email: Email)            // String → Email
    fun findOne(email: Email): RefreshToken  // String → Email
}
```

### 3.13 Domain - Port: TokenManager

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/auth/domain/port/TokenManager.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.auth.domain.port

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.auth.domain.RefreshToken

interface TokenManager {
    fun generateAccessToken(email: Email): String       // String → Email
    fun generateRefreshToken(email: Email): RefreshToken // String → Email
    fun validateToken(token: String): Boolean
    fun getEmailFromToken(token: String): Email         // 반환 String → Email
}
```

- `getEmailFromToken()`의 반환 타입을 `Email`로 변경: JWT에서 추출한 String을 Email로 감싸서 반환
- 이렇게 하면 JwtAuthenticationFilter에서도 `Email` 타입을 사용할 수 있으나, Spring Security의 `principal`은 `Object`이므로 `.value`로 다시 String 저장

### 3.14 Domain - Port: PostLikeRepository

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/community/domain/port/PostLikeRepository.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.community.domain.port

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.community.domain.PostLike

interface PostLikeRepository {
    fun save(postLike: PostLike): Long
    fun delete(postId: Long, memberEmail: Email)  // String → Email
}
```

### 3.15 Domain - Port: CommentLikeRepository

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/community/domain/port/CommentLikeRepository.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.community.domain.port

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.community.domain.CommentLike

interface CommentLikeRepository {
    fun save(commentLike: CommentLike): Long
    fun delete(commentId: Long, memberEmail: Email)  // String → Email
}
```

### 3.16 Domain - Auth: LoginInfo

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/auth/domain/LoginInfo.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.auth.domain

import com.konkuk.ma.domain.common.domain.Email  // 추가

class LoginInfo(
    val email: Email,       // String → Email
    val nickname: String,
    val accessToken: String,
    val refreshToken: RefreshToken
)
```

### 3.17 Domain - Auth: RefreshToken

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/auth/domain/RefreshToken.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.auth.domain

import com.konkuk.ma.domain.common.domain.Email  // 추가
import java.time.LocalDateTime

class RefreshToken(
    val email: Email,       // String → Email
    val expirationDate: LocalDateTime,
    val token: String,
) {
    fun isExpired(): Boolean {
        return !LocalDateTime.now().isBefore(expirationDate)
    }
}
```

### 3.18 Domain - Auth: SignUpCommand

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/auth/application/command/SignUpCommand.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.auth.application.command

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.member.domain.Gender
import com.konkuk.ma.domain.member.domain.NewMember
import com.konkuk.ma.domain.member.domain.PhoneNumber
import com.konkuk.ma.domain.member.domain.Region
import com.konkuk.ma.domain.auth.domain.port.PasswordEncryptor
import java.time.LocalDate

data class SignUpCommand(
    val email: Email,           // String → Email
    val password: String,
    val nickname: String,
    val gender: Gender,
    val phoneNumber: String,
    val name: String,
    val birthDate: LocalDate,
    val region: Region,
    val highSchool: String?,
    val university: String?
) {
    fun toNewMember(passwordEncryptor: PasswordEncryptor): NewMember {
        return NewMember(
            email = email,              // 이미 Email 타입
            password = passwordEncryptor.encode(password),
            nickname = nickname,
            gender = gender,
            phoneNumber = PhoneNumber(phoneNumber),
            name = name,
            birthDate = birthDate,
            region = region,
            highSchool = highSchool,
            university = university
        )
    }
}
```

### 3.19 Domain - Auth: LoginCommand

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/auth/application/command/LoginCommand.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.auth.application.command

import com.konkuk.ma.domain.common.domain.Email  // 추가

class LoginCommand(
    val email: Email,       // String → Email
    val password: String
)
```

### 3.20 Domain - Auth: SignUpValidator

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/auth/domain/SignUpValidator.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.auth.domain

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.auth.domain.port.SmsRepository
import com.konkuk.ma.domain.member.domain.NewMember
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import com.konkuk.ma.domain.member.exception.DuplicateEmailException
import com.konkuk.ma.domain.member.exception.DuplicateNicknameException
import com.konkuk.ma.domain.member.exception.SmsNotVerifiedException
import org.springframework.stereotype.Component

@Component
class SignUpValidator(
    private val memberQueryRepository: MemberQueryRepository,
    private val smsRepository: SmsRepository,
) {
    fun validate(newMember: NewMember) {
        checkDuplicatedNickname(newMember.nickname)
        checkDuplicatedEmail(newMember.email)
        checkSmsVerification(newMember.phoneNumber.fullNumber)
    }

    private fun checkDuplicatedNickname(nickname: String) {
        if (memberQueryRepository.existsByNickname(nickname)) {
            throw DuplicateNicknameException(nickname)
        }
    }

    private fun checkDuplicatedEmail(email: Email) {  // String → Email
        if (memberQueryRepository.existsByEmail(email)) {
            throw DuplicateEmailException(email.value)  // 예외에는 String 전달
        }
    }

    private fun checkSmsVerification(phoneNumber: String) {
        if (!smsRepository.getConfirmed(phoneNumber)) {
            throw SmsNotVerifiedException(phoneNumber)
        }
    }
}
```

### 3.21 Domain - Auth: RefreshTokenGenerator

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/auth/domain/RefreshTokenGenerator.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.auth.domain

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.auth.domain.port.RefreshTokenRepository
import com.konkuk.ma.domain.auth.domain.port.TokenManager
import org.springframework.stereotype.Component

@Component
class RefreshTokenGenerator(
    private val tokenManager: TokenManager,
    private val refreshTokenRepository: RefreshTokenRepository
) {
    fun generate(email: Email): RefreshToken {  // String → Email
        refreshTokenRepository.delete(email)
        val refreshToken = tokenManager.generateRefreshToken(email)
        refreshTokenRepository.save(refreshToken)
        return refreshToken
    }
}
```

### 3.22 Domain - Auth: LoginService

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/auth/application/LoginService.kt`
**변경 유형**: 수정 (email 타입 전파에 따른 변경)

```kotlin
package com.konkuk.ma.domain.auth.application

import com.konkuk.ma.domain.auth.application.command.LoginCommand
import com.konkuk.ma.domain.auth.domain.LoginInfo
import com.konkuk.ma.domain.auth.domain.PasswordVerifier
import com.konkuk.ma.domain.auth.domain.RefreshTokenGenerator
import com.konkuk.ma.domain.auth.domain.port.TokenManager
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class LoginService(
    private val memberQueryRepository: MemberQueryRepository,
    private val passwordVerifier: PasswordVerifier,
    private val tokenManager: TokenManager,
    private val refreshTokenGenerator: RefreshTokenGenerator
) {
    fun login(loginCommand: LoginCommand): LoginInfo {
        val member = memberQueryRepository.findOne(loginCommand.email)
        passwordVerifier.verify(loginCommand.password, member)

        val accessToken = tokenManager.generateAccessToken(member.email)   // member.email은 이미 Email
        val refreshToken = refreshTokenGenerator.generate(member.email)    // member.email은 이미 Email

        return LoginInfo(
            accessToken = accessToken,
            refreshToken = refreshToken,
            email = member.email,       // 이미 Email
            nickname = member.nickname
        )
    }
}
```

- `loginCommand.email`이 `Email` 타입이므로, `memberQueryRepository.findOne(Email)`과 자연스럽게 연결
- `member.email`이 `Email` 타입이므로, `tokenManager.generateAccessToken(Email)`과 자연스럽게 연결

### 3.23 Domain - Auth: RefreshTokenService

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/auth/application/RefreshTokenService.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.auth.application

import com.konkuk.ma.domain.auth.domain.LoginInfo
import com.konkuk.ma.domain.auth.domain.RefreshTokenGenerator
import com.konkuk.ma.domain.auth.domain.RefreshTokenValidator
import com.konkuk.ma.domain.auth.domain.port.RefreshTokenRepository
import com.konkuk.ma.domain.auth.domain.port.TokenManager
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class RefreshTokenService(
    private val tokenManager: TokenManager,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val refreshTokenValidator: RefreshTokenValidator,
    private val refreshTokenGenerator: RefreshTokenGenerator,
    private val memberQueryRepository: MemberQueryRepository
) {
    fun refreshToken(inputRefreshToken: String): LoginInfo {
        val email = tokenManager.getEmailFromToken(inputRefreshToken)  // 이제 Email 반환
        val refreshToken = refreshTokenRepository.findOne(email)
        refreshTokenValidator.validate(refreshToken)
        val accessToken = tokenManager.generateAccessToken(email)
        val newRefreshToken = refreshTokenGenerator.generate(refreshToken.email)
        val member = memberQueryRepository.findOne(email)
        return LoginInfo(
            email, member.nickname, accessToken, newRefreshToken
        )
    }
}
```

- `tokenManager.getEmailFromToken()`이 `Email`을 반환하므로 모든 하위 호출이 자연스럽게 연결

### 3.24 Domain - Auth: PasswordMismatchException

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/auth/exception/PasswordMismatchException.kt`
**변경 유형**: 수정 (파라미터를 Email로 받도록)

```kotlin
package com.konkuk.ma.domain.auth.exception

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.exception.BusinessException

class PasswordMismatchException(
    email: Email            // String → Email
) : BusinessException(
    message = "비밀번호가 올바르지 않습니다.",
    dataMessage = "email: ${email.value}",  // .value로 언패킹
    logLevel = LogLevel.WARN
)
```

### 3.25 Domain - Auth: RefreshTokenExpiredException

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/auth/exception/RefreshTokenExpiredException.kt`
**변경 유형**: 수정 (email 파라미터가 있다면 Email로 변경)

현재 코드를 확인하겠습니다.

```kotlin
// 현재 코드 확인 필요 — email 파라미터가 있다면 Email로 변경
```

### 3.26 Domain - Member: DuplicateEmailException

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/member/exception/DuplicateEmailException.kt`
**변경 유형**: 유지 (String 파라미터 유지)

```kotlin
// 변경 없음 — 예외 클래스는 로깅/메시지 목적이므로 String을 받는 것이 적절
// 호출부에서 email.value로 전달
```

### 3.27 Domain - Member Application: MemberPhotoService

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/member/application/MemberPhotoService.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.member.application

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.common.domain.file.PhotoFile
import com.konkuk.ma.domain.member.domain.photo.MemberPhotoProcessor
import com.konkuk.ma.domain.member.domain.photo.NewPhoto
import com.konkuk.ma.domain.member.domain.port.MemberPhotoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class MemberPhotoService(
    private val memberPhotoProcessor: MemberPhotoProcessor,
    private val memberPhotoRepository: MemberPhotoRepository
) {
    fun upload(email: Email, photoFile: PhotoFile) {  // String → Email
        delete(email)
        val processed = memberPhotoProcessor.process(email, photoFile)
        val newPhoto = NewPhoto.create(email, processed.filePath, photoFile.originalFileName, processed.thumbnailPath)
        memberPhotoRepository.save(newPhoto)
    }

    fun delete(email: Email) {  // String → Email
        val existing = memberPhotoRepository.findOne(email) ?: return
        memberPhotoProcessor.deleteFiles(existing)
        memberPhotoRepository.delete(email)
    }
}
```

### 3.28 Domain - Member Application: MemberQueryService

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/member/application/MemberQueryService.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.member.application

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MemberQueryService(
    private val memberQueryRepository: MemberQueryRepository
) {
    fun checkDuplicatedNickname(nickname: String): Boolean {
        return memberQueryRepository.existsByNickname(nickname)
    }

    fun checkDuplicatedEmail(email: Email): Boolean {  // String → Email
        return memberQueryRepository.existsByEmail(email)
    }
}
```

### 3.29 Domain - Matching: MatchingResult

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/matching/domain/MatchingResult.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.matching.domain

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.matching.exception.MatchingResultAccessDeniedException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class MatchingResult(
    val id: Long,
    val registerEmail: Email,       // String → Email
    override val targetInfoId: Long,
    override val targetEmail: Email, // String → Email

    val middleNumberMatched: Boolean,
    val lastNumberMatched: Boolean,
    val yearMatched: Boolean,
    val monthMatched: Boolean,
    val dayMatched: Boolean,
    val regionMatched: Boolean,

    val showingExpiryDate: LocalDateTime,
    val matchingExpiryDate: LocalDate,
    excluded: Boolean,
) : HasMatchingKey {
    var excluded: Boolean = excluded
        private set
    val matchRate: Int by lazy {
        MatchRateCalculator(
            groups = listOf(
                MatchingGroup.Phone(middleNumberMatched, lastNumberMatched),
                MatchingGroup.Birth(yearMatched, monthMatched, dayMatched),
            ),
            regionMatched = regionMatched,
        ).calculate()
    }

    fun getRemainingDays(): Long {
        val now = LocalDate.now()
        return ChronoUnit.DAYS.between(now, showingExpiryDate)
            .coerceAtLeast(0)
    }

    fun validateOwnership(email: Email) {  // String → Email
        if (registerEmail != email) {
            throw MatchingResultAccessDeniedException(id, registerEmail.value, email.value)
        }
    }

    fun exclude() {
        excluded = true
    }

    fun include() {
        excluded = false
    }
}
```

### 3.30 Domain - Matching: NewMatchingResult

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/matching/domain/NewMatchingResult.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.matching.domain

import com.konkuk.ma.domain.common.domain.Email  // 추가
import java.time.LocalDate
import java.time.LocalDateTime

class NewMatchingResult(
    val registerEmail: Email,       // String → Email
    override val targetInfoId: Long,
    override val targetEmail: Email, // String → Email

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
) : HasMatchingKey {
    companion object {
        private const val SHOWING_EXPIRY_DAYS = 30L
        private const val MATCHING_EXPIRY_DAYS = 210L
        private const val SHOWING_START_HOUR = 11
    }
}
```

### 3.31 Domain - Matching: HasMatchingKey

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/matching/domain/HasMatchingKey.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.matching.domain

import com.konkuk.ma.domain.common.domain.Email  // 추가

interface HasMatchingKey {
    val targetInfoId: Long
    val targetEmail: Email  // String → Email

    fun createUniqueKey(): Pair<Long, Email> = Pair(targetInfoId, targetEmail)  // Pair<Long, String> → Pair<Long, Email>
}
```

### 3.32 Domain - Matching: Target

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/matching/domain/Target.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.matching.domain

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.common.domain.date.Day
import com.konkuk.ma.domain.common.domain.date.Month
import com.konkuk.ma.domain.common.domain.date.Year
import com.konkuk.ma.domain.member.domain.FourDigit
import com.konkuk.ma.domain.member.domain.Gender
import com.konkuk.ma.domain.member.domain.Member
import com.konkuk.ma.domain.member.domain.Region

class Target(
    val email: Email,       // String → Email
    val name: String,
    val gender: Gender,
    val middleNumber: FourDigit,
    val lastNumber: FourDigit,

    val year: Year,
    val month: Month,
    val day: Day,

    val region: Region
) {
    fun matchesNameAndGender(name: String, gender: Gender): Boolean {
        return this.name == name && this.gender == gender
    }

    companion object {
        fun create(member: Member): Target {
            return Target(
                email = member.email,       // 이미 Email
                name = member.name,
                gender = member.gender,
                middleNumber = member.phoneNumber.middleNumber,
                lastNumber = member.phoneNumber.lastNumber,
                year = member.getYear(),
                month = member.getMonth(),
                day = member.getDay(),
                region = member.region
            )
        }
    }
}
```

### 3.33 Domain - Matching: NewTargetInfo

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/matching/domain/NewTargetInfo.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.matching.domain

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.common.domain.date.Day
import com.konkuk.ma.domain.common.domain.date.Month
import com.konkuk.ma.domain.common.domain.date.Year
import com.konkuk.ma.domain.member.domain.FourDigit
import com.konkuk.ma.domain.member.domain.Region

class NewTargetInfo(
    val registerEmail: Email,   // String → Email
    val targetName: String,
    val middleNumber: FourDigit?,
    val lastNumber: FourDigit?,

    val year: Year?,
    val month: Month?,
    val day: Day?,

    val region: Region?
)
```

### 3.34 Domain - Matching: TargetInfo

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/matching/domain/TargetInfo.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.matching.domain

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.common.domain.date.Day
import com.konkuk.ma.domain.common.domain.date.Month
import com.konkuk.ma.domain.common.domain.date.Year
import com.konkuk.ma.domain.member.domain.FourDigit
import com.konkuk.ma.domain.member.domain.Gender
import com.konkuk.ma.domain.member.domain.Region

class TargetInfo(
    val targetInfoId: Long,
    val registerEmail: Email,   // String → Email
    val targetName: String,
    val targetGender: Gender,

    val middleNumber: FourDigit?,
    val lastNumber: FourDigit?,

    val year: Year?,
    val month: Month?,
    val day: Day?,

    val region: Region?
) {
    fun makeMatchingResults(targets: Targets): NewMatchingResults {
        val results = targets
            .filterCandidates(targetName, targetGender)
            .map { makeMatchingResult(it) }
        return NewMatchingResults(results)
    }

    private fun makeMatchingResult(target: Target): NewMatchingResult {
        val middleNumberMatched = middleNumber == target.middleNumber
        val lastNumberMatched = lastNumber == target.lastNumber

        val yearMatched = year == target.year
        val monthMatched = month == target.month
        val dayMatched = day == target.day

        val regionMatched = region == target.region

        return NewMatchingResult(
            registerEmail = registerEmail,      // 이미 Email
            targetInfoId = targetInfoId,
            targetEmail = target.email,         // 이미 Email
            middleNumberMatched = middleNumberMatched,
            lastNumberMatched = lastNumberMatched,
            yearMatched = yearMatched,
            monthMatched = monthMatched,
            dayMatched = dayMatched,
            regionMatched = regionMatched,
        )
    }
}
```

### 3.35 Domain - Matching: MatchingResults (일급 컬렉션)

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/matching/domain/MatchingResults.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.matching.domain

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.member.domain.Members
import com.konkuk.ma.domain.member.domain.photo.MemberPhotos

class MatchingResults(
    val data: List<MatchingResult>
) {
    fun extractTargetEmails(): Set<Email> {  // Set<String> → Set<Email>
        return data.map { it.targetEmail }.toSet()
    }

    fun combineWithProfiles(members: Members, photos: MemberPhotos): MatchingResultsWithProfiles {
        val combined = data.map { result ->
            val member = members.findOne(result.targetEmail)    // Email 전달
            val photo = photos.findOne(result.targetEmail)      // Email 전달
            MatchingResultWithProfile(
                matchingResult = result,
                targetMemberId = member?.id,
                targetName = member?.name,
                targetNickname = member?.nickname,
                profileImageUrl = photo?.thumbnailPath,
            )
        }
        return MatchingResultsWithProfiles(combined)
    }
}
```

### 3.36 Domain - Matching Application: MatchingResultQueryService

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/matching/application/MatchingResultQueryService.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.matching.application

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.matching.domain.MatchingResult
import com.konkuk.ma.domain.matching.domain.MatchingResults
import com.konkuk.ma.domain.matching.domain.MatchingResultsWithProfiles
import com.konkuk.ma.domain.matching.domain.port.MatchingResultRepository
import com.konkuk.ma.domain.member.domain.Members
import com.konkuk.ma.domain.member.domain.photo.MemberPhotos
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
    fun find(email: Email, excluded: Boolean = false): MatchingResultsWithProfiles {  // String → Email
        val matchingResults = MatchingResults(matchingResultRepository.find(email, excluded))
        val targetEmails = matchingResults.extractTargetEmails()

        val members = Members(memberQueryRepository.findByEmails(targetEmails))
        val photos = MemberPhotos(memberPhotoRepository.find(targetEmails))

        return matchingResults.combineWithProfiles(members, photos)
    }

    fun findDetail(matchingResultId: Long, email: Email): MatchingResult {  // String → Email
        val matchingResult = matchingResultRepository.findOne(matchingResultId)
        matchingResult.validateOwnership(email)
        return matchingResult
    }
}
```

### 3.37 Domain - Matching Application: MatchingResultCommandService

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/matching/application/MatchingResultCommandService.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.matching.application

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.matching.domain.port.MatchingResultRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class MatchingResultCommandService(
    private val matchingResultRepository: MatchingResultRepository,
) {
    fun exclude(matchingResultId: Long, email: Email) {  // String → Email
        val matchingResult = matchingResultRepository.findOne(matchingResultId)
        matchingResult.validateOwnership(email)
        matchingResult.exclude()
        matchingResultRepository.updateExcluded(matchingResult)
    }

    fun include(matchingResultId: Long, email: Email) {  // String → Email
        val matchingResult = matchingResultRepository.findOne(matchingResultId)
        matchingResult.validateOwnership(email)
        matchingResult.include()
        matchingResultRepository.updateExcluded(matchingResult)
    }
}
```

### 3.38 Domain - Community: Comment

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/community/domain/Comment.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.community.exception.CommentAccessDeniedException
import com.konkuk.ma.domain.community.exception.NotRootCommentException
import com.konkuk.ma.domain.community.exception.ReplyDepthExceededException
import java.time.LocalDateTime

class Comment(
    val id: Long = 0L,
    val postId: Long,
    val authorEmail: Email,     // String → Email
    val content: String,
    val parentCommentId: Long? = null,
    val likes: Int = 0,
    val createdDate: LocalDateTime = LocalDateTime.now(),
    val deleted: Boolean = false,
) {
    fun displayContent(): String {
        if (deleted) return DELETED_CONTENT
        return content
    }

    fun hasParent(): Boolean = parentCommentId != null

    fun validateCanBeParent() {
        if (hasParent()) {
            throw ReplyDepthExceededException(id)
        }
    }

    fun validateIsRootComment() {
        if (hasParent()) {
            throw NotRootCommentException(id)
        }
    }

    fun validateOwnership(email: Email) {  // String → Email
        if (authorEmail != email) {
            throw CommentAccessDeniedException(id, authorEmail.value, email.value)
        }
    }

    companion object {
        private const val DELETED_CONTENT = "삭제된 댓글입니다."
    }
}
```

### 3.39 Domain - Community: NewComment

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/community/domain/NewComment.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.common.domain.Email  // 추가

class NewComment(
    val postId: Long,
    val authorEmail: Email,     // String → Email
    val content: String,
    val parentCommentId: Long? = null,
) {
    init {
        validateContent()
    }

    private fun validateContent() {
        require(content.isNotBlank()) { "댓글 내용은 비어있을 수 없습니다." }
        require(content.length <= MAX_CONTENT_LENGTH) { "댓글 내용은 ${MAX_CONTENT_LENGTH}자 이하여야 합니다." }
    }

    fun hasParent(): Boolean = parentCommentId != null

    companion object {
        const val MAX_CONTENT_LENGTH = 500
    }
}
```

### 3.40 Domain - Community: Post

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/community/domain/Post.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.common.domain.Email  // 추가
import java.time.LocalDateTime

class Post(
    val id: Long = 0L,
    val authorEmail: Email,     // String → Email
    val category: PostCategory,
    val title: String,
    val content: String,
    val likes: Int = 0,
    val createdDate: LocalDateTime = LocalDateTime.now(),
)
```

### 3.41 Domain - Community: NewPost

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/community/domain/NewPost.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.common.domain.Email  // 추가

class NewPost(
    val authorEmail: Email,     // String → Email
    val category: PostCategory,
    val title: String,
    val content: String,
) {
    init {
        validateTitle()
        validateContent()
    }

    private fun validateTitle() {
        require(title.isNotBlank()) { "게시글 제목은 비어있을 수 없습니다." }
        require(title.length <= MAX_TITLE_LENGTH) { "게시글 제목은 ${MAX_TITLE_LENGTH}자 이하여야 합니다." }
    }

    private fun validateContent() {
        require(content.isNotBlank()) { "게시글 내용은 비어있을 수 없습니다." }
        require(content.length <= MAX_CONTENT_LENGTH) { "게시글 내용은 ${MAX_CONTENT_LENGTH}자 이하여야 합니다." }
    }

    companion object {
        const val MAX_TITLE_LENGTH = 30
        const val MAX_CONTENT_LENGTH = 2000
    }
}
```

### 3.42 Domain - Community: PostLike

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/community/domain/PostLike.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.common.domain.Email  // 추가

class PostLike(
    val id: Long = 0L,
    val postId: Long,
    val memberEmail: Email,     // String → Email
)
```

### 3.43 Domain - Community: CommentLike

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/community/domain/CommentLike.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.common.domain.Email  // 추가

class CommentLike(
    val id: Long = 0L,
    val commentId: Long,
    val memberEmail: Email,     // String → Email
)
```

### 3.44 Domain - Community Application: CommentCommandService

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/community/application/CommentCommandService.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.community.application

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.community.domain.CommentValidator
import com.konkuk.ma.domain.community.domain.NewComment
import com.konkuk.ma.domain.community.domain.port.CommentCommandRepository
import com.konkuk.ma.domain.community.domain.port.CommentQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CommentCommandService(
    private val commentCommandRepository: CommentCommandRepository,
    private val commentQueryRepository: CommentQueryRepository,
    private val commentValidator: CommentValidator,
) {
    fun create(newComment: NewComment): Long {
        commentValidator.validate(newComment)
        return commentCommandRepository.save(newComment)
    }

    fun delete(commentId: Long, email: Email) {  // String → Email
        val comment = commentQueryRepository.findOne(commentId)
        comment.validateOwnership(email)
        commentCommandRepository.delete(commentId)
    }
}
```

### 3.45 Domain - Community Application: PostLikeService

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/community/application/PostLikeService.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.community.application

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.community.domain.PostLike
import com.konkuk.ma.domain.community.domain.PostLikeResult
import com.konkuk.ma.domain.community.domain.port.PostCommandRepository
import com.konkuk.ma.domain.community.domain.port.PostLikeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PostLikeService(
    private val postLikeRepository: PostLikeRepository,
    private val postCommandRepository: PostCommandRepository,
) {
    fun like(postId: Long, memberEmail: Email): PostLikeResult {  // String → Email
        postLikeRepository.save(PostLike(postId = postId, memberEmail = memberEmail))
        val likeCount = postCommandRepository.increaseLikes(postId)
        return PostLikeResult.liked(likeCount)
    }

    fun unlike(postId: Long, memberEmail: Email): PostLikeResult {  // String → Email
        postLikeRepository.delete(postId, memberEmail)
        val likeCount = postCommandRepository.decreaseLikes(postId)
        return PostLikeResult.unliked(likeCount)
    }
}
```

### 3.46 Domain - Community Application: CommentLikeService

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/community/application/CommentLikeService.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.community.application

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.community.domain.CommentLike
import com.konkuk.ma.domain.community.domain.CommentLikeResult
import com.konkuk.ma.domain.community.domain.port.CommentCommandRepository
import com.konkuk.ma.domain.community.domain.port.CommentLikeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CommentLikeService(
    private val commentLikeRepository: CommentLikeRepository,
    private val commentCommandRepository: CommentCommandRepository,
) {
    fun like(commentId: Long, memberEmail: Email): CommentLikeResult {  // String → Email
        commentLikeRepository.save(CommentLike(commentId = commentId, memberEmail = memberEmail))
        val likeCount = commentCommandRepository.increaseLikes(commentId)
        return CommentLikeResult.liked(likeCount)
    }

    fun unlike(commentId: Long, memberEmail: Email): CommentLikeResult {  // String → Email
        commentLikeRepository.delete(commentId, memberEmail)
        val likeCount = commentCommandRepository.decreaseLikes(commentId)
        return CommentLikeResult.unliked(likeCount)
    }
}
```

### 3.47 Domain - Common: StoragePath

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/common/domain/file/StoragePath.kt`
**변경 유형**: 유지

```kotlin
// 변경 없음 — StoragePath는 파일 시스템 경로를 조합하는 인프라 관심사
// email은 디렉토리명으로 사용되므로 String이 적절
// 호출부(MemberPhotoProcessor 등)에서 email.value로 전달
```

---

### Infrastructure 레이어

### 3.48 Infrastructure - JwtManager

**파일**: `infrastructure/support/ma-jwt-core/src/main/kotlin/com/konkuk/ma/auth/JwtManager.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.auth

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.auth.domain.RefreshToken
import com.konkuk.ma.domain.auth.domain.port.TokenManager
import com.konkuk.ma.domain.auth.exception.AuthTokenException
import com.konkuk.ma.domain.auth.exception.JwtExceptionType
import com.konkuk.ma.exception.BusinessException.LogLevel
import com.konkuk.ma.logger
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.SignatureException
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import javax.crypto.SecretKey

@Component
class JwtManager(
    @Value("\${jwt.secret}")
    private val secretKey: String,
    @Value("\${jwt.access-token-expiration}")
    private val accessTokenExpiration: Long,
    @Value("\${jwt.refresh-token-expiration}")
    private val refreshTokenExpiration: Long
) : TokenManager {

    private val key: SecretKey = Keys.hmacShaKeyFor(secretKey.toByteArray())

    override fun generateAccessToken(email: Email): String {  // String → Email
        logger.info { "Generating access token for email: ${email.value}" }
        val generated = generateJwt(email.value, accessTokenExpiration)  // .value로 언패킹
        return generated.token
    }

    override fun generateRefreshToken(email: Email): RefreshToken {  // String → Email
        logger.info { "Generating refresh token for email: ${email.value}" }
        val generated = generateJwt(email.value, refreshTokenExpiration)  // .value로 언패킹
        return RefreshToken(
            email = email,          // 이미 Email
            expirationDate = generated.expiry,
            token = generated.token
        )
    }

    private data class GeneratedJwt(
        val token: String,
        val expiry: LocalDateTime
    )

    private fun generateJwt(email: String, expirationMs: Long): GeneratedJwt {  // 내부 메서드는 String 유지
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now()
        val expiry = now.plus(Duration.ofMillis(expirationMs))

        val issuedDate = Date.from(now.atZone(zone).toInstant())
        val expiryDate = Date.from(expiry.atZone(zone).toInstant())

        val token = generateToken(email, issuedDate, expiryDate)
        logger.info { "generated token: $token, email: $email" }
        return GeneratedJwt(token = token, expiry = expiry)
    }

    private fun generateToken(email: String, issuedAt: Date, expiryDate: Date): String {
        return Jwts.builder()
            .setSubject(email)
            .setIssuedAt(issuedAt)
            .setExpiration(expiryDate)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    override fun validateToken(token: String): Boolean {
        getClaimsFromToken(token)
        return true
    }

    override fun getEmailFromToken(token: String): Email {  // 반환 String → Email
        val claims = getClaimsFromToken(token)
        return Email(claims.subject)    // String → Email로 감싸서 반환
    }

    private fun getClaimsFromToken(token: String): Claims {
        // 기존 코드 유지 (변경 없음)
        try {
            return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .body
        } catch (e: ExpiredJwtException) {
            throw AuthTokenException(token, JwtExceptionType.EXPIRED, e, LogLevel.INFO, this::getClaimsFromToken)
        } catch (e: MalformedJwtException) {
            throw AuthTokenException(token, JwtExceptionType.MALFORMED, e, LogLevel.ERROR, this::getClaimsFromToken)
        } catch (e: UnsupportedJwtException) {
            throw AuthTokenException(token, JwtExceptionType.UNSUPPORTED, e, LogLevel.ERROR, this::getClaimsFromToken)
        } catch (e: SignatureException) {
            throw AuthTokenException(token, JwtExceptionType.SIGNATURE, e, LogLevel.ERROR, this::getClaimsFromToken)
        } catch (e: IllegalArgumentException) {
            throw AuthTokenException(token, JwtExceptionType.ILLEGAL_ARGUMENT, e, LogLevel.ERROR, this::getClaimsFromToken)
        } catch (e: JwtException) {
            throw AuthTokenException(token, JwtExceptionType.ETC, e, LogLevel.ERROR, this::getClaimsFromToken)
        }
    }
}
```

- 외부 인터페이스(override 메서드)는 `Email` 파라미터/반환
- 내부 메서드(`generateJwt`, `generateToken`)는 JWT 라이브러리에 전달할 `String` 유지
- `getEmailFromToken()`은 JWT subject(String)를 `Email`로 감싸서 반환

### 3.49 Infrastructure - JwtAuthenticationFilter

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/support/security/JwtAuthenticationFilter.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.support.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.domain.auth.domain.port.TokenManager
import com.konkuk.ma.domain.auth.exception.AuthTokenException
import com.konkuk.ma.support.payload.response.ApiError
import com.konkuk.ma.support.payload.response.ErrorCode
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val tokenManager: TokenManager,
    private val mapper: ObjectMapper
) : OncePerRequestFilter() {
    companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
        const val BEARER_PREFIX = "Bearer "
    }

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val authHeader = request.getHeader(AUTHORIZATION_HEADER)
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response)
            return
        }
        val jwt = authHeader.substring(BEARER_PREFIX.length)
        try {
            val email = tokenManager.getEmailFromToken(jwt)  // 이제 Email 반환
            val authentication = UsernamePasswordAuthenticationToken(email.value, null, emptyList<SimpleGrantedAuthority>())  // .value로 String 저장
            authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
            SecurityContextHolder.getContext().authentication = authentication
            filterChain.doFilter(request, response)
        } catch (e: AuthTokenException) {
            // 기존 에러 핸들링 유지 (변경 없음)
            if (e.isExpired()) {
                writeApiError(response, ErrorCode.EXPIRED_TOKEN, HttpServletResponse.SC_UNAUTHORIZED)
                return
            }
            if (e.isMalformed()) {
                writeApiError(response, ErrorCode.MALFORMED_TOKEN, HttpServletResponse.SC_BAD_REQUEST)
                return
            }
            if (e.isInvalid()) {
                writeApiError(response, ErrorCode.INVALID_TOKEN, HttpServletResponse.SC_BAD_REQUEST)
                return
            }
            if (e.isOtherError()) {
                writeApiError(response, ErrorCode.OTHER_TOKEN_ERROR, HttpServletResponse.SC_BAD_REQUEST)
            }
        }
    }

    // writeApiError 메서드 변경 없음
    private fun writeApiError(
        response: HttpServletResponse,
        errorCode: ErrorCode,
        httpStatus: Int,
    ) {
        val apiError = ApiError(errorCode)
        response.status = httpStatus
        response.contentType = "application/json"
        response.characterEncoding = "UTF-8"
        response.writer.use { out ->
            out.write(mapper.writeValueAsString(apiError))
            out.flush()
        }
    }
}
```

- `tokenManager.getEmailFromToken(jwt)`이 `Email`을 반환하지만, Spring Security `principal`에는 `email.value` (String)를 저장
- 컨트롤러의 `@AuthenticationPrincipal email: String`은 계속 String으로 받고, 컨트롤러에서 `Email(email)`로 변환

### 3.50 Infrastructure - Entity: MemberEntity

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/member/entity/MemberEntity.kt`
**변경 유형**: 유지

```kotlin
// 변경 없음 — Entity는 DB 컬럼 매핑이므로 email: String 유지
// toDomain()에서 Member.create(email = email)를 호출하면 Member 내부에서 Email(email) 변환
```

### 3.51 Infrastructure - Entity: MatchingResultEntity

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/matching/entity/MatchingResultEntity.kt`
**변경 유형**: 수정 (toDomain에서 Email 감싸기)

```kotlin
package com.konkuk.ma.domain.matching.entity

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.matching.domain.MatchingResult
import com.konkuk.ma.domain.matching.entity.table.MatchingResultTable
import org.jetbrains.exposed.sql.ResultRow
import java.time.LocalDate
import java.time.LocalDateTime

class MatchingResultEntity(
    val id: Long,
    val registerEmail: String,      // DB 매핑이므로 String 유지
    val targetInfoId: Long,
    val targetEmail: String,        // DB 매핑이므로 String 유지
    val middleNumberMatched: Boolean,
    val lastNumberMatched: Boolean,
    val yearMatched: Boolean,
    val monthMatched: Boolean,
    val dayMatched: Boolean,
    val regionMatched: Boolean,
    val showingExpiryDate: LocalDateTime,
    val matchingExpiryDate: LocalDate,
    val excluded: Boolean,
) {
    fun toDomain(): MatchingResult {
        return MatchingResult(
            id = id,
            registerEmail = Email(registerEmail),   // String → Email 변환
            targetInfoId = targetInfoId,
            targetEmail = Email(targetEmail),        // String → Email 변환
            middleNumberMatched = middleNumberMatched,
            lastNumberMatched = lastNumberMatched,
            yearMatched = yearMatched,
            monthMatched = monthMatched,
            dayMatched = dayMatched,
            regionMatched = regionMatched,
            showingExpiryDate = showingExpiryDate,
            matchingExpiryDate = matchingExpiryDate,
            excluded = excluded,
        )
    }

    companion object {
        fun from(row: ResultRow): MatchingResultEntity {
            // 변경 없음 — ResultRow에서 String으로 읽음
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
                excluded = row[MatchingResultTable.excluded],
            )
        }
    }
}
```

### 3.52 Infrastructure - Entity: RefreshTokenEntity

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/auth/entity/RefreshTokenEntity.kt`
**변경 유형**: 수정 (toDomain에서 Email 감싸기)

```kotlin
package com.konkuk.ma.domain.auth.entity

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.auth.domain.RefreshToken
import java.time.LocalDateTime

class RefreshTokenEntity(
    val email: String,          // DB 매핑이므로 String 유지
    val expirationDate: LocalDateTime,
    val token: String
) {
    fun toDomain(): RefreshToken {
        return RefreshToken(
            email = Email(email),   // String → Email 변환
            expirationDate = expirationDate,
            token = token
        )
    }
}
```

### 3.53 Infrastructure - Entity: MemberPhotoEntity

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/member/entity/MemberPhotoEntity.kt`
**변경 유형**: 수정 (toDomain에서 Email 감싸기)

```kotlin
package com.konkuk.ma.domain.member.entity

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.member.domain.photo.ApprovalStatus
import com.konkuk.ma.domain.member.domain.photo.MemberPhoto
import com.konkuk.ma.domain.member.entity.table.MemberPhotoTable
import org.jetbrains.exposed.sql.ResultRow

class MemberPhotoEntity(
    val id: Long,
    val memberEmail: String,        // DB 매핑이므로 String 유지
    val filePath: String,
    val originalFileName: String,
    val approvalStatus: String,
    val thumbnailPath: String?
) {
    fun toDomain(): MemberPhoto {
        return MemberPhoto(
            id = id,
            memberEmail = Email(memberEmail),   // String → Email 변환
            filePath = filePath,
            originalFileName = originalFileName,
            approvalStatus = ApprovalStatus.valueOf(approvalStatus),
            thumbnailPath = thumbnailPath
        )
    }

    companion object {
        fun from(row: ResultRow): MemberPhotoEntity {
            // 변경 없음
            return MemberPhotoEntity(
                id = row[MemberPhotoTable.id].value,
                memberEmail = row[MemberPhotoTable.memberEmail],
                filePath = row[MemberPhotoTable.filePath],
                originalFileName = row[MemberPhotoTable.originalFileName],
                approvalStatus = row[MemberPhotoTable.approvalStatus],
                thumbnailPath = row[MemberPhotoTable.thumbnailPath]
            )
        }
    }
}
```

### 3.54 Infrastructure - Entity: PostEntity

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/community/entity/PostEntity.kt`
**변경 유형**: 수정 (toDomain에서 Email 감싸기)

```kotlin
package com.konkuk.ma.domain.community.entity

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.community.domain.Post
import com.konkuk.ma.domain.community.domain.PostCategory
import com.konkuk.ma.domain.community.entity.table.PostTable
import org.jetbrains.exposed.sql.ResultRow
import java.time.LocalDateTime

class PostEntity(
    val id: Long,
    val authorEmail: String,        // DB 매핑이므로 String 유지
    val category: PostCategory,
    val title: String,
    val content: String,
    val likes: Int,
    val createdDate: LocalDateTime,
) {
    fun toDomain(): Post {
        return Post(
            id = id,
            authorEmail = Email(authorEmail),   // String → Email 변환
            category = category,
            title = title,
            content = content,
            likes = likes,
            createdDate = createdDate,
        )
    }

    companion object {
        fun from(row: ResultRow): PostEntity {
            // 변경 없음
            return PostEntity(
                id = row[PostTable.id].value,
                authorEmail = row[PostTable.authorEmail],
                category = PostCategory.valueOf(row[PostTable.category]),
                title = row[PostTable.title],
                content = row[PostTable.content],
                likes = row[PostTable.likes],
                createdDate = row[PostTable.createdDate],
            )
        }
    }
}
```

### 3.55 Infrastructure - Entity: CommentEntity

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/community/entity/CommentEntity.kt`
**변경 유형**: 수정 (toDomain에서 Email 감싸기)

```kotlin
package com.konkuk.ma.domain.community.entity

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.community.domain.Comment
import com.konkuk.ma.domain.community.entity.table.CommentTable
import org.jetbrains.exposed.sql.ResultRow
import java.time.LocalDateTime

class CommentEntity(
    val id: Long,
    val postId: Long,
    val authorEmail: String,        // DB 매핑이므로 String 유지
    val content: String,
    val parentCommentId: Long?,
    val likes: Int,
    val createdDate: LocalDateTime,
    val deleted: Boolean,
) {
    fun toDomain(): Comment {
        return Comment(
            id = id,
            postId = postId,
            authorEmail = Email(authorEmail),   // String → Email 변환
            content = content,
            parentCommentId = parentCommentId,
            likes = likes,
            createdDate = createdDate,
            deleted = deleted,
        )
    }

    companion object {
        fun from(row: ResultRow): CommentEntity {
            // 변경 없음
            return CommentEntity(
                id = row[CommentTable.id].value,
                postId = row[CommentTable.postId],
                authorEmail = row[CommentTable.authorEmail],
                content = row[CommentTable.content],
                parentCommentId = row[CommentTable.parentCommentId],
                likes = row[CommentTable.likes],
                createdDate = row[CommentTable.createdDate],
                deleted = row[CommentTable.deleted],
            )
        }
    }
}
```

### 3.56 Infrastructure - DAO: MatchingResultQueryDao

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/matching/dao/MatchingResultQueryDao.kt`
**변경 유형**: 유지

```kotlin
// 변경 없음 — DAO는 String을 받아 Exposed DSL에 전달
// Repository 구현체에서 email.value로 언패킹하여 전달
```

### 3.57 Infrastructure - DAO: MatchingResultCommandDao

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/matching/dao/MatchingResultCommandDao.kt`
**변경 유형**: 수정 (NewMatchingResult.registerEmail이 Email이므로 .value 필요)

```kotlin
package com.konkuk.ma.domain.matching.dao

import com.konkuk.ma.domain.matching.domain.MatchingResult
import com.konkuk.ma.domain.matching.domain.NewMatchingResult
import com.konkuk.ma.domain.matching.entity.table.MatchingResultTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.update
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class MatchingResultCommandDao {
    fun deleteExpired(baseDate: LocalDate): Int {
        // 변경 없음
        return MatchingResultTable.deleteWhere {
            (matchingExpiryDate less baseDate) and (excluded eq false)
        }
    }

    fun deleteExcludedExpired(baseDate: LocalDate): Int {
        // 변경 없음
        return MatchingResultTable.deleteWhere {
            (excluded eq true) and (matchingExpiryDate less baseDate)
        }
    }

    fun saveAll(matchingResults: List<NewMatchingResult>) {
        MatchingResultTable.batchInsert(matchingResults) {
            this[MatchingResultTable.registerEmail] = it.registerEmail.value    // .value 추가
            this[MatchingResultTable.targetInfoId] = it.targetInfoId
            this[MatchingResultTable.targetEmail] = it.targetEmail.value        // .value 추가
            this[MatchingResultTable.middleNumberMatched] = it.middleNumberMatched
            this[MatchingResultTable.lastNumberMatched] = it.lastNumberMatched
            this[MatchingResultTable.yearMatched] = it.yearMatched
            this[MatchingResultTable.monthMatched] = it.monthMatched
            this[MatchingResultTable.dayMatched] = it.dayMatched
            this[MatchingResultTable.regionMatched] = it.regionMatched
            this[MatchingResultTable.showingExpiryDate] = it.showingExpiryDate
            this[MatchingResultTable.matchingExpiryDate] = it.matchingExpiryDate
            this[MatchingResultTable.createdBy] = it.registerEmail.value        // .value 추가
            this[MatchingResultTable.lastModifiedBy] = it.registerEmail.value   // .value 추가
            this[MatchingResultTable.excluded] = false
        }
    }

    fun updateExcluded(matchingResult: MatchingResult) {
        // 변경 없음 — id로 조회하므로 email 미사용
        MatchingResultTable.update({ MatchingResultTable.id eq matchingResult.id }) {
            it[excluded] = matchingResult.excluded
        }
    }
}
```

### 3.58 Infrastructure - DAO: MemberCommandDao

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/member/dao/MemberCommandDao.kt`
**변경 유형**: 수정 (NewMember.email이 Email이므로 .value 필요)

```kotlin
package com.konkuk.ma.domain.member.dao

import com.konkuk.ma.domain.member.domain.NewMember
import com.konkuk.ma.domain.member.entity.table.MemberTable
import org.jetbrains.exposed.sql.insertAndGetId
import org.springframework.stereotype.Component

@Component
class MemberCommandDao {

    fun save(newMember: NewMember): Long {
        return MemberTable.insertAndGetId {
            it[email] = newMember.email.value           // .value 추가
            it[password] = newMember.password
            it[nickname] = newMember.nickname
            it[phoneNumber] = newMember.phoneNumber.fullNumber
            it[gender] = newMember.gender.name
            it[name] = newMember.name
            it[region] = newMember.region.name
            it[birthDate] = newMember.birthDate
            it[highSchool] = newMember.highSchool
            it[university] = newMember.university
            it[createdBy] = newMember.email.value       // .value 추가
            it[lastModifiedBy] = newMember.email.value   // .value 추가
        }.value
    }
}
```

### 3.59 Infrastructure - DAO: MemberPhotoCommandDao

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/member/dao/MemberPhotoCommandDao.kt`
**변경 유형**: 수정 (NewPhoto.memberEmail이 Email이므로 .value 필요)

```kotlin
package com.konkuk.ma.domain.member.dao

import com.konkuk.ma.domain.member.domain.photo.NewPhoto
import com.konkuk.ma.domain.member.entity.table.MemberPhotoTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.springframework.stereotype.Component

@Component
class MemberPhotoCommandDao {

    fun save(newPhoto: NewPhoto): Long {
        return MemberPhotoTable.insertAndGetId {
            it[memberEmail] = newPhoto.memberEmail.value        // .value 추가
            it[filePath] = newPhoto.filePath
            it[originalFileName] = newPhoto.originalFileName
            it[thumbnailPath] = newPhoto.thumbnailPath
            it[createdBy] = newPhoto.memberEmail.value          // .value 추가
            it[lastModifiedBy] = newPhoto.memberEmail.value     // .value 추가
        }.value
    }

    fun delete(email: String) {     // String 유지 — Repository에서 .value로 전달
        MemberPhotoTable.deleteWhere {
            MemberPhotoTable.memberEmail eq email
        }
    }
}
```

### 3.60 Infrastructure - DAO: RefreshTokenDao

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/auth/dao/RefreshTokenDao.kt`
**변경 유형**: 수정 (RefreshToken.email이 Email이므로 .value 필요)

```kotlin
package com.konkuk.ma.domain.auth.dao

import com.konkuk.ma.domain.auth.domain.RefreshToken
import com.konkuk.ma.domain.auth.entity.RefreshTokenEntity
import com.konkuk.ma.domain.auth.entity.table.RefreshTokenTable
import com.konkuk.ma.domain.common.RowEntityMapper
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.exception.EntityType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Component

@Component
class RefreshTokenDao {
    fun save(refreshToken: RefreshToken) {
        RefreshTokenTable.insert {
            it[email] = refreshToken.email.value            // .value 추가
            it[token] = refreshToken.token
            it[expirationDate] = refreshToken.expirationDate
            it[createdBy] = refreshToken.email.value        // .value 추가
            it[lastModifiedBy] = refreshToken.email.value   // .value 추가
        }
    }

    fun delete(email: String) {     // String 유지 — Repository에서 .value로 전달
        RefreshTokenTable.deleteWhere {
            RefreshTokenTable.email eq email
        }
    }

    fun findOne(email: String): RefreshTokenEntity {    // String 유지 — Repository에서 .value로 전달
        return RefreshTokenTable.selectAll()
            .where { RefreshTokenTable.email eq email }
            .limit(1)
            .firstOrNull()
            ?.let { RowEntityMapper.toRefreshTokenEntity(it) }
            ?: throw EntityNotFoundException(EntityType.REFRESH_TOKEN, email)
    }
}
```

### 3.61 Infrastructure - DAO: PostCommandDao

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/community/dao/PostCommandDao.kt`
**변경 유형**: 수정 (NewPost.authorEmail이 Email이므로 .value 필요)

```kotlin
package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.domain.community.domain.NewPost
import com.konkuk.ma.domain.community.entity.table.PostTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.update
import org.springframework.stereotype.Component

@Component
class PostCommandDao {
    fun save(newPost: NewPost): Long {
        return PostTable.insertAndGetId {
            it[authorEmail] = newPost.authorEmail.value     // .value 추가
            it[category] = newPost.category.name
            it[title] = newPost.title
            it[content] = newPost.content
            it[createdBy] = newPost.authorEmail.value       // .value 추가
            it[lastModifiedBy] = newPost.authorEmail.value  // .value 추가
        }.value
    }

    // increaseLikes, decreaseLikes 변경 없음
    fun increaseLikes(postId: Long): Int { /* 변경 없음 */ }
    fun decreaseLikes(postId: Long): Int { /* 변경 없음 */ }
    private fun findLikeCount(postId: Long): Int { /* 변경 없음 */ }
}
```

### 3.62 Infrastructure - DAO: CommentCommandDao

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/community/dao/CommentCommandDao.kt`
**변경 유형**: 수정 (NewComment.authorEmail이 Email이므로 .value 필요)

```kotlin
package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.domain.community.domain.NewComment
import com.konkuk.ma.domain.community.entity.table.CommentTable
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.update
import org.springframework.stereotype.Component

@Component
class CommentCommandDao {
    fun save(newComment: NewComment): Long {
        return CommentTable.insertAndGetId {
            it[postId] = newComment.postId
            it[authorEmail] = newComment.authorEmail.value      // .value 추가
            it[content] = newComment.content
            it[parentCommentId] = newComment.parentCommentId
            it[createdBy] = newComment.authorEmail.value        // .value 추가
            it[lastModifiedBy] = newComment.authorEmail.value   // .value 추가
        }.value
    }

    // increaseLikes, decreaseLikes, delete 변경 없음
    fun increaseLikes(commentId: Long): Int { /* 변경 없음 */ }
    fun decreaseLikes(commentId: Long): Int { /* 변경 없음 */ }
    fun delete(id: Long) { /* 변경 없음 */ }
    private fun findLikeCount(commentId: Long): Int { /* 변경 없음 */ }
}
```

### 3.63 Infrastructure - DAO: PostLikeDao

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/community/dao/PostLikeDao.kt`
**변경 유형**: 유지

```kotlin
// 변경 없음 — DAO 메서드 파라미터는 String 유지
// Repository 구현체에서 postLike.memberEmail.value로 전달
```

### 3.64 Infrastructure - DAO: CommentLikeDao

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/community/dao/CommentLikeDao.kt`
**변경 유형**: 유지

```kotlin
// 변경 없음 — DAO 메서드 파라미터는 String 유지
// Repository 구현체에서 commentLike.memberEmail.value로 전달
```

### 3.65 Infrastructure - DAO: TargetInfoCommandDao

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/matching/dao/TargetInfoCommandDao.kt`
**변경 유형**: 수정 (NewTargetInfo.registerEmail이 Email이므로 .value 필요)

```kotlin
package com.konkuk.ma.domain.matching.dao

import com.konkuk.ma.domain.matching.domain.NewTargetInfo
import com.konkuk.ma.domain.matching.entity.table.TargetInfoTable
import com.konkuk.ma.domain.member.domain.Gender
import org.jetbrains.exposed.sql.insertAndGetId
import org.springframework.stereotype.Component

@Component
class TargetInfoCommandDao {
    fun save(newTargetInfo: NewTargetInfo, targetGender: Gender): Long {
        return TargetInfoTable.insertAndGetId {
            it[TargetInfoTable.registerEmail] = newTargetInfo.registerEmail.value    // .value 추가
            it[name] = newTargetInfo.targetName
            it[TargetInfoTable.targetGender] = targetGender.name
            it[middleNumber] = newTargetInfo.middleNumber?.value
            it[lastNumber] = newTargetInfo.lastNumber?.value
            it[year] = newTargetInfo.year?.value
            it[month] = newTargetInfo.month?.value
            it[day] = newTargetInfo.day?.value
            it[region] = newTargetInfo.region?.name
            it[createdBy] = newTargetInfo.registerEmail.value               // .value 추가
            it[lastModifiedBy] = newTargetInfo.registerEmail.value          // .value 추가
        }.value
    }
}
```

### 3.66 Infrastructure - Repository: MatchingResultCoreRepository

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/matching/repository/MatchingResultCoreRepository.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.matching.repository

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.matching.dao.MatchingResultCommandDao
import com.konkuk.ma.domain.matching.dao.MatchingResultQueryDao
import com.konkuk.ma.domain.matching.domain.MatchingResult
import com.konkuk.ma.domain.matching.domain.NewMatchingResult
import com.konkuk.ma.domain.matching.domain.port.MatchingResultRepository
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.exception.EntityType
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class MatchingResultCoreRepository(
    private val matchingResultCommandDao: MatchingResultCommandDao,
    private val matchingResultQueryDao: MatchingResultQueryDao
) : MatchingResultRepository {
    override fun saveAll(matchingResults: List<NewMatchingResult>) {
        matchingResultCommandDao.saveAll(matchingResults)
    }

    override fun findExistingMatchingResults(targetInfoIds: List<Long>): List<MatchingResult> {
        return matchingResultQueryDao.find(targetInfoIds)
            .map { it.toDomain() }
    }

    override fun deleteExpiredMatchingResults(baseDate: LocalDate): Int {
        return matchingResultCommandDao.deleteExpired(baseDate)
    }

    override fun deleteExcludedExpiredMatchingResults(baseDate: LocalDate): Int {
        return matchingResultCommandDao.deleteExcludedExpired(baseDate)
    }

    override fun find(email: Email, excluded: Boolean): List<MatchingResult> {  // String → Email
        return matchingResultQueryDao.find(email.value, excluded)    // .value로 언패킹
            .map { it.toDomain() }
    }

    override fun findOne(matchingResultId: Long): MatchingResult {
        return matchingResultQueryDao.findOne(matchingResultId)
            ?.toDomain()
            ?: throw EntityNotFoundException(EntityType.MATCHING_RESULT, matchingResultId.toString())
    }

    override fun updateExcluded(matchingResult: MatchingResult) {
        matchingResultCommandDao.updateExcluded(matchingResult)
    }
}
```

### 3.67 Infrastructure - Repository: MemberQueryCoreRepository

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/member/repository/MemberQueryCoreRepository.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.member.repository

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.member.dao.MemberQueryDao
import com.konkuk.ma.domain.member.domain.Member
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import org.springframework.stereotype.Repository

@Repository
class MemberQueryCoreRepository(
    private val memberQueryDao: MemberQueryDao
) : MemberQueryRepository {
    override fun existsByNickname(nickname: String): Boolean {
        return memberQueryDao.existsByNickname(nickname)
    }

    override fun existsByEmail(email: Email): Boolean {     // String → Email
        return memberQueryDao.existsByEmail(email.value)    // .value로 언패킹
    }

    override fun findOne(email: Email): Member {            // String → Email
        return memberQueryDao.findOne(email.value)          // .value로 언패킹
            .toDomain()
    }

    override fun findByNames(names: Set<String>): List<Member> {
        return memberQueryDao.findByNames(names)
            .map { it.toDomain() }
    }

    override fun findByEmails(emails: Set<Email>): List<Member> {   // Set<String> → Set<Email>
        return memberQueryDao.findByEmails(emails.map { it.value }.toSet())  // .value로 언패킹
            .map { it.toDomain() }
    }
}
```

### 3.68 Infrastructure - Repository: MemberPhotoCoreRepository

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/member/repository/MemberPhotoCoreRepository.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.member.repository

import com.konkuk.ma.domain.common.domain.Email  // 추가
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

    override fun findOne(email: Email): MemberPhoto? {      // String → Email
        return memberPhotoQueryDao.findOne(email.value)?.toDomain()  // .value로 언패킹
    }

    override fun delete(email: Email) {                     // String → Email
        memberPhotoCommandDao.delete(email.value)           // .value로 언패킹
    }

    override fun find(emails: Set<Email>): List<MemberPhoto> {  // Set<String> → Set<Email>
        return memberPhotoQueryDao.find(emails.map { it.value }.toSet())  // .value로 언패킹
            .map { it.toDomain() }
    }
}
```

### 3.69 Infrastructure - Repository: RefreshTokenCoreRepository

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/auth/repository/RefreshTokenCoreRepository.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.auth.repository

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.auth.domain.RefreshToken
import com.konkuk.ma.domain.auth.domain.port.RefreshTokenRepository
import com.konkuk.ma.domain.auth.dao.RefreshTokenDao
import org.springframework.stereotype.Repository

@Repository
class RefreshTokenCoreRepository(
    private val refreshTokenDao: RefreshTokenDao
) : RefreshTokenRepository {
    override fun save(refreshToken: RefreshToken) {
        refreshTokenDao.save(refreshToken)
    }

    override fun delete(email: Email) {             // String → Email
        refreshTokenDao.delete(email.value)         // .value로 언패킹
    }

    override fun findOne(email: Email): RefreshToken {  // String → Email
        return refreshTokenDao.findOne(email.value)     // .value로 언패킹
            .toDomain()
    }
}
```

### 3.70 Infrastructure - Repository: PostLikeCoreRepository

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/community/repository/PostLikeCoreRepository.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.community.repository

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.community.dao.PostLikeDao
import com.konkuk.ma.domain.community.domain.PostLike
import com.konkuk.ma.domain.community.domain.port.PostLikeRepository
import org.springframework.stereotype.Repository

@Repository
class PostLikeCoreRepository(
    private val postLikeDao: PostLikeDao,
) : PostLikeRepository {
    override fun save(postLike: PostLike): Long {
        return postLikeDao.save(postLike.postId, postLike.memberEmail.value)  // .value 추가
    }

    override fun delete(postId: Long, memberEmail: Email) {     // String → Email
        postLikeDao.delete(postId, memberEmail.value)           // .value로 언패킹
    }
}
```

### 3.71 Infrastructure - Repository: CommentLikeCoreRepository

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/community/repository/CommentLikeCoreRepository.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.community.repository

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.community.dao.CommentLikeDao
import com.konkuk.ma.domain.community.domain.CommentLike
import com.konkuk.ma.domain.community.domain.port.CommentLikeRepository
import org.springframework.stereotype.Repository

@Repository
class CommentLikeCoreRepository(
    private val commentLikeDao: CommentLikeDao,
) : CommentLikeRepository {
    override fun save(commentLike: CommentLike): Long {
        return commentLikeDao.save(commentLike.commentId, commentLike.memberEmail.value)  // .value 추가
    }

    override fun delete(commentId: Long, memberEmail: Email) {  // String → Email
        commentLikeDao.delete(commentId, memberEmail.value)     // .value로 언패킹
    }
}
```

---

### Boot 레이어

### 3.72 Boot - Controller: MatchingResultQueryApi

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/matching/api/MatchingResultQueryApi.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.matching.api

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.matching.api.response.MatchingResultDetailResponse
import com.konkuk.ma.domain.matching.api.response.MatchingResultsResponse
import com.konkuk.ma.domain.matching.application.MatchingResultQueryService
import com.konkuk.ma.support.id.DecryptId
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/matching-results")
class MatchingResultQueryApi(
    private val matchingResultQueryService: MatchingResultQueryService
) {
    @GetMapping
    fun findMyMatchingResults(
        @AuthenticationPrincipal email: String,     // Spring Security는 String으로 유지
        @RequestParam(defaultValue = "false") excluded: Boolean,
    ): MatchingResultsResponse {
        val results = matchingResultQueryService.find(Email(email), excluded)  // Email 변환
        return MatchingResultsResponse.from(results)
    }

    @GetMapping("/{matchingResultId}")
    fun findMatchingResultDetail(
        @AuthenticationPrincipal email: String,     // Spring Security는 String으로 유지
        @PathVariable @DecryptId(ObfuscationType.MATCHING_RESULT) matchingResultId: Long,
    ): MatchingResultDetailResponse {
        val matchingResult = matchingResultQueryService.findDetail(matchingResultId, Email(email))  // Email 변환
        return MatchingResultDetailResponse.from(matchingResult)
    }
}
```

### 3.73 Boot - Controller: MatchingResultCommandApi

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/matching/api/MatchingResultCommandApi.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.matching.api

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.matching.application.MatchingResultCommandService
import com.konkuk.ma.support.id.DecryptId
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/matching-results")
class MatchingResultCommandApi(
    private val matchingResultCommandService: MatchingResultCommandService,
) {
    @PatchMapping("/{matchingResultId}/exclude")
    fun exclude(
        @AuthenticationPrincipal email: String,
        @PathVariable @DecryptId(ObfuscationType.MATCHING_RESULT) matchingResultId: Long,
    ) {
        matchingResultCommandService.exclude(matchingResultId, Email(email))  // Email 변환
    }

    @PatchMapping("/{matchingResultId}/include")
    fun include(
        @AuthenticationPrincipal email: String,
        @PathVariable @DecryptId(ObfuscationType.MATCHING_RESULT) matchingResultId: Long,
    ) {
        matchingResultCommandService.include(matchingResultId, Email(email))  // Email 변환
    }
}
```

### 3.74 Boot - Controller: MemberPhotoApi

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/member/api/MemberPhotoApi.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.member.api

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.common.domain.file.PhotoFile
import com.konkuk.ma.domain.member.api.response.MemberPhotoResponse
import com.konkuk.ma.domain.member.application.MemberPhotoService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/members/photos")
class MemberPhotoApi(
    private val memberPhotoService: MemberPhotoService
) {
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun uploadPhoto(
        @AuthenticationPrincipal email: String,
        @RequestPart("photo") photo: MultipartFile
    ): MemberPhotoResponse {
        val photoFile = PhotoFile.create(photo.originalFilename, photo.size, photo.bytes)
        memberPhotoService.upload(Email(email), photoFile)  // Email 변환
        return MemberPhotoResponse.uploaded()
    }

    @DeleteMapping
    fun deletePhoto(
        @AuthenticationPrincipal email: String
    ): MemberPhotoResponse {
        memberPhotoService.delete(Email(email))  // Email 변환
        return MemberPhotoResponse.deleted()
    }
}
```

### 3.75 Boot - Controller: TargetInfoCommandApi

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/matching/api/TargetInfoCommandApi.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.matching.api

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.matching.api.request.NewTargetInfoRequest
import com.konkuk.ma.domain.matching.api.response.NewTargetInfoResponse
import com.konkuk.ma.domain.matching.application.TargetInfoCommandService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/target-infos")
class TargetInfoCommandApi(
    private val targetInfoCommandService: TargetInfoCommandService
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun save(
        @AuthenticationPrincipal email: String,
        @Valid @RequestBody request: NewTargetInfoRequest
    ): NewTargetInfoResponse {
        val newTargetInfo = request.toNewTargetInfo(Email(email))  // String → Email 변환
        val targetInfoId = targetInfoCommandService.register(newTargetInfo)

        return NewTargetInfoResponse(
            targetInfoId = targetInfoId,
            registerEmail = email       // Response는 String 유지
        )
    }
}
```

### 3.76 Boot - Controller: CommentCommandApi

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/community/api/CommentCommandApi.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.community.api

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.community.api.request.NewCommentRequest
import com.konkuk.ma.domain.community.api.response.NewCommentResponse
import com.konkuk.ma.domain.community.application.CommentCommandService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/community/posts/{postId}/comments")
class CommentCommandApi(
    private val commentCommandService: CommentCommandService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal email: String,
        @PathVariable postId: Long,
        @Valid @RequestBody request: NewCommentRequest,
    ): NewCommentResponse {
        val commentId = commentCommandService.create(request.toNewComment(Email(email), postId))  // Email 변환
        return NewCommentResponse(commentId = commentId)
    }

    @DeleteMapping("/{commentId}")
    fun delete(
        @AuthenticationPrincipal email: String,
        @PathVariable postId: Long,
        @PathVariable commentId: Long,
    ) {
        commentCommandService.delete(commentId, Email(email))  // Email 변환
    }
}
```

### 3.77 Boot - Controller: PostCommandApi

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/community/api/PostCommandApi.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.community.api

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.community.api.request.NewPostRequest
import com.konkuk.ma.domain.community.api.response.NewPostResponse
import com.konkuk.ma.domain.community.application.PostCommandService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/community/posts")
class PostCommandApi(
    private val postCommandService: PostCommandService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal email: String,
        @Valid @RequestBody request: NewPostRequest,
    ): NewPostResponse {
        val postId = postCommandService.create(request.toNewPost(Email(email)))  // Email 변환
        return NewPostResponse(postId = postId)
    }
}
```

### 3.78 Boot - Controller: PostLikeApi

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/community/api/PostLikeApi.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.community.api

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.community.api.response.PostLikeResponse
import com.konkuk.ma.domain.community.application.PostLikeService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/community/posts/{postId}/likes")
class PostLikeApi(
    private val postLikeService: PostLikeService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun like(
        @AuthenticationPrincipal email: String,
        @PathVariable postId: Long,
    ): PostLikeResponse {
        val result = postLikeService.like(postId, Email(email))  // Email 변환
        return PostLikeResponse.from(result)
    }

    @DeleteMapping
    fun unlike(
        @AuthenticationPrincipal email: String,
        @PathVariable postId: Long,
    ): PostLikeResponse {
        val result = postLikeService.unlike(postId, Email(email))  // Email 변환
        return PostLikeResponse.from(result)
    }
}
```

### 3.79 Boot - Controller: CommentLikeApi

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/community/api/CommentLikeApi.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.community.api

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.community.api.response.CommentLikeResponse
import com.konkuk.ma.domain.community.application.CommentLikeService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/community/comments/{commentId}/likes")
class CommentLikeApi(
    private val commentLikeService: CommentLikeService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun like(
        @AuthenticationPrincipal email: String,
        @PathVariable commentId: Long,
    ): CommentLikeResponse {
        val result = commentLikeService.like(commentId, Email(email))  // Email 변환
        return CommentLikeResponse.from(result)
    }

    @DeleteMapping
    fun unlike(
        @AuthenticationPrincipal email: String,
        @PathVariable commentId: Long,
    ): CommentLikeResponse {
        val result = commentLikeService.unlike(commentId, Email(email))  // Email 변환
        return CommentLikeResponse.from(result)
    }
}
```

### 3.80 Boot - Request DTO: SignUpRequest

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/auth/api/request/SignUpRequest.kt`
**변경 유형**: 수정 (toCommand에서 Email 변환)

```kotlin
package com.konkuk.ma.domain.auth.api.request

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.auth.application.command.SignUpCommand
import com.konkuk.ma.support.validation.ValidationMessages
import com.konkuk.ma.support.validation.ValidationPatterns
import com.konkuk.ma.domain.member.domain.Gender
import com.konkuk.ma.domain.member.domain.Region
import jakarta.validation.constraints.Email as EmailAnnotation  // 이름 충돌 방지
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import java.time.LocalDate

class SignUpRequest(
    @field:NotBlank(message = ValidationMessages.EMAIL_REQUIRED)
    @field:EmailAnnotation(message = ValidationMessages.EMAIL_INVALID)  // 어노테이션 이름 변경
    val email: String,      // Request DTO는 String 유지 (Bean Validation)

    @field:NotBlank(message = ValidationMessages.PASSWORD_REQUIRED)
    @field:Pattern(regexp = ValidationPatterns.PASSWORD, message = ValidationMessages.PASSWORD_INVALID)
    val password: String,

    @field:NotBlank(message = ValidationMessages.PHONE_NUMBER_REQUIRED)
    @field:Pattern(regexp = ValidationPatterns.PHONE_NUMBER, message = ValidationMessages.PHONE_NUMBER_INVALID)
    val phoneNumber: String,

    @field:Pattern(regexp = ValidationPatterns.NICKNAME, message = ValidationMessages.NICKNAME_INVALID)
    val nickname: String,

    @field:NotBlank(message = ValidationMessages.NAME_REQUIRED)
    @field:Pattern(regexp = ValidationPatterns.NAME, message = ValidationMessages.NAME_INVALID)
    val name: String,

    val gender: Gender,

    @field:NotNull(message = ValidationMessages.BIRTH_DATE_REQUIRED)
    val birthDate: LocalDate,

    @field:NotNull(message = ValidationMessages.REGION_REQUIRED)
    val region: Region,

    val highSchool: String?,

    val university: String?,
) {
    fun toCommand(): SignUpCommand {
        return SignUpCommand(
            email = Email(this.email),      // String → Email 변환
            password = this.password,
            nickname = this.nickname,
            gender = this.gender,
            phoneNumber = this.phoneNumber,
            name = this.name,
            birthDate = this.birthDate,
            region = this.region,
            highSchool = this.highSchool,
            university = this.university
        )
    }
}
```

- **중요**: `jakarta.validation.constraints.Email`과 도메인 `Email` 클래스의 이름이 충돌하므로, import alias 사용 (`import jakarta.validation.constraints.Email as EmailAnnotation`)

### 3.81 Boot - Request DTO: LoginRequest

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/auth/api/request/LoginRequest.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.auth.api.request

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.auth.application.command.LoginCommand
import com.konkuk.ma.support.validation.ValidationMessages
import jakarta.validation.constraints.Email as EmailAnnotation  // 이름 충돌 방지
import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank(message = ValidationMessages.EMAIL_REQUIRED)
    @field:EmailAnnotation(message = ValidationMessages.EMAIL_INVALID)  // 어노테이션 이름 변경
    val email: String,      // Request DTO는 String 유지

    @field:NotBlank(message = ValidationMessages.PASSWORD_REQUIRED)
    val password: String
) {
    fun toCommand() = LoginCommand(Email(email), password)  // String → Email 변환
}
```

### 3.82 Boot - Request DTO: DuplicatedEmailRequest

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/member/api/request/DuplicatedEmailRequest.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.member.api.request

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.support.validation.ValidationMessages
import jakarta.validation.constraints.Email as EmailAnnotation  // 이름 충돌 방지
import jakarta.validation.constraints.NotBlank

class DuplicatedEmailRequest(
    @field:NotBlank(message = ValidationMessages.EMAIL_REQUIRED)
    @field:EmailAnnotation(message = ValidationMessages.EMAIL_INVALID)
    val email: String       // Request DTO는 String 유지
) {
    fun toEmail(): Email = Email(email)     // 변환 메서드 추가
}
```

### 3.83 Boot - Request DTO: NewTargetInfoRequest

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/matching/api/request/NewTargetInfoRequest.kt`
**변경 유형**: 수정 (toNewTargetInfo 파라미터 타입)

```kotlin
package com.konkuk.ma.domain.matching.api.request

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.support.validation.ValidationMessages
import com.konkuk.ma.support.validation.ValidationPatterns
import com.konkuk.ma.domain.common.domain.date.Day
import com.konkuk.ma.domain.common.domain.date.Month
import com.konkuk.ma.domain.common.domain.date.Year
import com.konkuk.ma.domain.matching.domain.NewTargetInfo
import com.konkuk.ma.domain.member.domain.FourDigit
import com.konkuk.ma.domain.member.domain.Region
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

class NewTargetInfoRequest(
    @field:NotBlank(message = ValidationMessages.NAME_REQUIRED)
    @field:Pattern(regexp = ValidationPatterns.NAME, message = ValidationMessages.NAME_INVALID)
    val name: String,

    @field:Pattern(regexp = ValidationPatterns.FOUR_DIGIT, message = ValidationMessages.FOUR_DIGIT_MIDDLE_INVALID)
    val middleNumber: String?,

    @field:Pattern(regexp = ValidationPatterns.FOUR_DIGIT, message = ValidationMessages.FOUR_DIGIT_LAST_INVALID)
    val lastNumber: String?,

    val year: Int?,
    val month: Int?,
    val day: Int?,

    val region: Region?
) {
    fun toNewTargetInfo(registerEmail: Email): NewTargetInfo {  // String → Email
        return NewTargetInfo(
            registerEmail = registerEmail,      // 이미 Email
            targetName = name,
            middleNumber = middleNumber?.let { FourDigit(it) },
            lastNumber = lastNumber?.let { FourDigit(it) },
            year = year?.let { Year(it) },
            month = month?.let { Month(it) },
            day = day?.let { Day(it) },
            region = region
        )
    }
}
```

### 3.84 Boot - Request DTO: NewCommentRequest

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/community/api/request/NewCommentRequest.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.community.api.request

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.community.domain.NewComment
import com.konkuk.ma.support.validation.ValidationMessages
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

class NewCommentRequest(
    @field:NotBlank(message = ValidationMessages.COMMENT_CONTENT_REQUIRED)
    @field:Size(max = NewComment.MAX_CONTENT_LENGTH, message = ValidationMessages.COMMENT_CONTENT_SIZE)
    val content: String,

    val parentCommentId: Long? = null,
) {
    fun toNewComment(authorEmail: Email, postId: Long): NewComment {  // String → Email
        return NewComment(
            postId = postId,
            authorEmail = authorEmail,      // 이미 Email
            content = content,
            parentCommentId = parentCommentId,
        )
    }
}
```

### 3.85 Boot - Request DTO: NewPostRequest

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/community/api/request/NewPostRequest.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.community.api.request

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.community.domain.NewPost
import com.konkuk.ma.domain.community.domain.PostCategory
import com.konkuk.ma.support.validation.ValidationMessages
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

class NewPostRequest(
    val category: PostCategory,

    @field:NotBlank(message = ValidationMessages.POST_TITLE_REQUIRED)
    @field:Size(max = NewPost.MAX_TITLE_LENGTH, message = ValidationMessages.POST_TITLE_SIZE)
    val title: String,

    @field:NotBlank(message = ValidationMessages.POST_CONTENT_REQUIRED)
    @field:Size(max = NewPost.MAX_CONTENT_LENGTH, message = ValidationMessages.POST_CONTENT_SIZE)
    val content: String,
) {
    fun toNewPost(authorEmail: Email): NewPost {  // String → Email
        return NewPost(
            authorEmail = authorEmail,      // 이미 Email
            category = category,
            title = title,
            content = content,
        )
    }
}
```

### 3.86 Boot - Response DTO: LoginResponse

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/auth/api/response/LoginResponse.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.auth.api.response

import com.konkuk.ma.domain.auth.domain.LoginInfo

class LoginResponse(
    val email: String,      // Response는 String 유지 (JSON 직렬화)
    val nickname: String,
    val accessToken: String,
    val refreshToken: String
) {
    constructor(loginInfo: LoginInfo) : this(
        email = loginInfo.email.value,      // .value로 언패킹
        nickname = loginInfo.nickname,
        accessToken = loginInfo.accessToken,
        refreshToken = loginInfo.refreshToken.token
    )
}
```

### 3.87 Boot - Response DTO: SignUpResponse

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/auth/api/response/SignUpResponse.kt`
**변경 유형**: 유지

```kotlin
// 변경 없음 — email: String은 Controller에서 직접 String으로 전달
```

---

### Test 레이어

### 3.88 Test - testFixtures: NewMemberFixture

**파일**: `domain/ma-domain-core/src/testFixtures/kotlin/com/konkuk/ma/domain/member/fixture/NewMemberFixture.kt`
**변경 유형**: 수정

```kotlin
package com.konkuk.ma.domain.member.fixture

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.member.domain.Gender
import com.konkuk.ma.domain.member.domain.NewMember
import com.konkuk.ma.domain.member.domain.PhoneNumber
import com.konkuk.ma.domain.member.domain.Region
import java.time.LocalDate

object NewMemberFixture {
    fun create(
        email: String = "test@example.com",     // 편의를 위해 String 유지
        password: String = "password123",
        nickname: String = "testuser",
        gender: Gender = Gender.MALE,
        phoneNumber: PhoneNumber = PhoneNumber("01012345678"),
        name: String = "김테스트",
        birthDate: LocalDate = LocalDate.of(1990, 1, 1),
        region: Region = Region.SEOUL,
        highSchool: String? = null,
        university: String? = null
    ): NewMember {
        return NewMember(
            email = Email(email),       // String → Email 변환
            password = password,
            nickname = nickname,
            gender = gender,
            phoneNumber = phoneNumber,
            name = name,
            birthDate = birthDate,
            region = region,
            highSchool = highSchool,
            university = university
        )
    }
}
```

### 3.89 Test - testFixtures: MemberFixture

**파일**: `domain/ma-domain-core/src/testFixtures/kotlin/com/konkuk/ma/domain/matching/fixture/MemberFixture.kt`
**변경 유형**: 유지

```kotlin
// 변경 없음 — Member.create()의 팩토리 메서드가 String을 받아 내부에서 Email로 변환하므로
// Member(email = ..., phoneNumber = PhoneNumber(...)) 생성자를 직접 사용하는 경우에만 변경 필요
```

실제로 이 Fixture는 `Member` 생성자를 직접 사용하므로 수정이 필요합니다:

```kotlin
package com.konkuk.ma.domain.matching.fixture

import com.konkuk.ma.domain.common.domain.Email  // 추가
import com.konkuk.ma.domain.member.domain.Gender
import com.konkuk.ma.domain.member.domain.Member
import com.konkuk.ma.domain.member.domain.PhoneNumber
import com.konkuk.ma.domain.member.domain.Region
import java.time.LocalDate

object MemberFixture {
    fun create(
        email: String = "target@example.com",
        password: String = "password",
        nickname: String = "nickname",
        gender: Gender = Gender.MALE,
        phoneNumber: String = "01012345678",
        name: String = "홍길동",
        region: Region = Region.SEOUL,
        birthDate: LocalDate = LocalDate.of(1999, 12, 31),
        highSchool: String? = null,
        university: String? = null
    ): Member {
        return Member(
            email = Email(email),       // String → Email 변환
            password = password,
            nickname = nickname,
            gender = gender,
            phoneNumber = PhoneNumber(phoneNumber),
            name = name,
            region = region,
            birthDate = birthDate,
            highSchool = highSchool,
            university = university
        )
    }
}
```

### 3.90 Test - Email Value Object 테스트

**파일**: `domain/ma-domain-core/src/test/kotlin/com/konkuk/ma/domain/common/domain/EmailTest.kt`
**변경 유형**: 신규

```kotlin
package com.konkuk.ma.domain.common.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class EmailTest : StringSpec({
    "유효한 이메일로 Email을 생성할 수 있다" {
        val email = Email("test@example.com")
        email.value shouldBe "test@example.com"
    }

    "빈 문자열로 Email을 생성하면 예외가 발생한다" {
        shouldThrow<IllegalArgumentException> {
            Email("")
        }
    }

    "공백 문자열로 Email을 생성하면 예외가 발생한다" {
        shouldThrow<IllegalArgumentException> {
            Email("   ")
        }
    }

    "@ 없는 문자열로 Email을 생성하면 예외가 발생한다" {
        shouldThrow<IllegalArgumentException> {
            Email("testexample.com")
        }
    }

    "도메인 없는 문자열로 Email을 생성하면 예외가 발생한다" {
        shouldThrow<IllegalArgumentException> {
            Email("test@")
        }
    }

    "같은 값의 Email은 동등하다" {
        val email1 = Email("test@example.com")
        val email2 = Email("test@example.com")
        email1 shouldBe email2
    }

    "toString()은 이메일 값을 반환한다" {
        val email = Email("test@example.com")
        email.toString() shouldBe "test@example.com"
    }
})
```

---

## 4. 구현 순서

의존성 순서를 고려하여 **도메인 → 인프라 → boot** 순으로 구현한다. 같은 레이어 내에서는 의존되는 쪽을 먼저 수정한다.

### Phase 1: Email Value Object 생성 + 테스트

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 1 | `domain/.../common/domain/Email.kt` | 신규 | Email Value Object 클래스 |
| 2 | `domain/.../common/domain/EmailTest.kt` | 신규 | Email 유효성 검증 테스트 |

### Phase 2: Domain Model 변경

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 3 | `domain/.../member/domain/Member.kt` | 수정 | `email: String` → `email: Email` |
| 4 | `domain/.../member/domain/NewMember.kt` | 수정 | `email: String` → `email: Email` |
| 5 | `domain/.../member/domain/Members.kt` | 수정 | email 파라미터/키 타입 변경 |
| 6 | `domain/.../member/domain/photo/MemberPhoto.kt` | 수정 | `memberEmail: String` → `memberEmail: Email` |
| 7 | `domain/.../member/domain/photo/MemberPhotos.kt` | 수정 | email 파라미터 타입 변경 |
| 8 | `domain/.../member/domain/photo/NewPhoto.kt` | 수정 | `memberEmail: String` → `memberEmail: Email` |
| 9 | `domain/.../member/domain/photo/MemberPhotoProcessor.kt` | 수정 | email 파라미터 타입 변경 |
| 10 | `domain/.../auth/domain/LoginInfo.kt` | 수정 | `email: String` → `email: Email` |
| 11 | `domain/.../auth/domain/RefreshToken.kt` | 수정 | `email: String` → `email: Email` |
| 12 | `domain/.../matching/domain/HasMatchingKey.kt` | 수정 | `targetEmail: String` → `targetEmail: Email` |
| 13 | `domain/.../matching/domain/MatchingResult.kt` | 수정 | registerEmail, targetEmail 타입 변경 |
| 14 | `domain/.../matching/domain/NewMatchingResult.kt` | 수정 | registerEmail, targetEmail 타입 변경 |
| 15 | `domain/.../matching/domain/Target.kt` | 수정 | `email: String` → `email: Email` |
| 16 | `domain/.../matching/domain/TargetInfo.kt` | 수정 | `registerEmail: String` → `registerEmail: Email` |
| 17 | `domain/.../matching/domain/NewTargetInfo.kt` | 수정 | `registerEmail: String` → `registerEmail: Email` |
| 18 | `domain/.../matching/domain/MatchingResults.kt` | 수정 | extractTargetEmails 반환 타입 변경 |
| 19 | `domain/.../community/domain/Comment.kt` | 수정 | `authorEmail: String` → `authorEmail: Email` |
| 20 | `domain/.../community/domain/NewComment.kt` | 수정 | `authorEmail: String` → `authorEmail: Email` |
| 21 | `domain/.../community/domain/Post.kt` | 수정 | `authorEmail: String` → `authorEmail: Email` |
| 22 | `domain/.../community/domain/NewPost.kt` | 수정 | `authorEmail: String` → `authorEmail: Email` |
| 23 | `domain/.../community/domain/PostLike.kt` | 수정 | `memberEmail: String` → `memberEmail: Email` |
| 24 | `domain/.../community/domain/CommentLike.kt` | 수정 | `memberEmail: String` → `memberEmail: Email` |

### Phase 3: Domain Port 변경

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 25 | `domain/.../member/domain/port/MemberQueryRepository.kt` | 수정 | email 파라미터 타입 변경 |
| 26 | `domain/.../member/domain/port/MemberPhotoRepository.kt` | 수정 | email 파라미터 타입 변경 |
| 27 | `domain/.../matching/domain/port/MatchingResultRepository.kt` | 수정 | email 파라미터 타입 변경 |
| 28 | `domain/.../auth/domain/port/RefreshTokenRepository.kt` | 수정 | email 파라미터 타입 변경 |
| 29 | `domain/.../auth/domain/port/TokenManager.kt` | 수정 | email 파라미터/반환 타입 변경 |
| 30 | `domain/.../community/domain/port/PostLikeRepository.kt` | 수정 | memberEmail 파라미터 타입 변경 |
| 31 | `domain/.../community/domain/port/CommentLikeRepository.kt` | 수정 | memberEmail 파라미터 타입 변경 |

### Phase 4: Domain Command + Service + Component 변경

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 32 | `domain/.../auth/application/command/SignUpCommand.kt` | 수정 | `email: String` → `email: Email` |
| 33 | `domain/.../auth/application/command/LoginCommand.kt` | 수정 | `email: String` → `email: Email` |
| 34 | `domain/.../auth/domain/SignUpValidator.kt` | 수정 | email 파라미터 타입 변경 |
| 35 | `domain/.../auth/domain/RefreshTokenGenerator.kt` | 수정 | email 파라미터 타입 변경 |
| 36 | `domain/.../auth/exception/PasswordMismatchException.kt` | 수정 | email 파라미터 타입 변경 |
| 37 | `domain/.../member/exception/DuplicateEmailException.kt` | 유지 | String 파라미터 유지 (로깅 목적) |

### Phase 5: Domain Application Service 변경

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 38 | `domain/.../auth/application/LoginService.kt` | 수정 | 타입 전파에 따른 자동 변경 |
| 39 | `domain/.../auth/application/RefreshTokenService.kt` | 수정 | 타입 전파에 따른 자동 변경 |
| 40 | `domain/.../member/application/MemberPhotoService.kt` | 수정 | email 파라미터 타입 변경 |
| 41 | `domain/.../member/application/MemberQueryService.kt` | 수정 | email 파라미터 타입 변경 |
| 42 | `domain/.../matching/application/MatchingResultQueryService.kt` | 수정 | email 파라미터 타입 변경 |
| 43 | `domain/.../matching/application/MatchingResultCommandService.kt` | 수정 | email 파라미터 타입 변경 |
| 44 | `domain/.../community/application/CommentCommandService.kt` | 수정 | email 파라미터 타입 변경 |
| 45 | `domain/.../community/application/PostLikeService.kt` | 수정 | email 파라미터 타입 변경 |
| 46 | `domain/.../community/application/CommentLikeService.kt` | 수정 | email 파라미터 타입 변경 |

### Phase 6: Infrastructure Entity (toDomain 변환) 변경

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 47 | `infra/.../matching/entity/MatchingResultEntity.kt` | 수정 | toDomain에서 Email 감싸기 |
| 48 | `infra/.../auth/entity/RefreshTokenEntity.kt` | 수정 | toDomain에서 Email 감싸기 |
| 49 | `infra/.../member/entity/MemberPhotoEntity.kt` | 수정 | toDomain에서 Email 감싸기 |
| 50 | `infra/.../community/entity/PostEntity.kt` | 수정 | toDomain에서 Email 감싸기 |
| 51 | `infra/.../community/entity/CommentEntity.kt` | 수정 | toDomain에서 Email 감싸기 |

### Phase 7: Infrastructure DAO (.value 언패킹) 변경

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 52 | `infra/.../matching/dao/MatchingResultCommandDao.kt` | 수정 | .value 언패킹 추가 |
| 53 | `infra/.../member/dao/MemberCommandDao.kt` | 수정 | .value 언패킹 추가 |
| 54 | `infra/.../member/dao/MemberPhotoCommandDao.kt` | 수정 | .value 언패킹 추가 |
| 55 | `infra/.../auth/dao/RefreshTokenDao.kt` | 수정 | .value 언패킹 추가 |
| 56 | `infra/.../community/dao/PostCommandDao.kt` | 수정 | .value 언패킹 추가 |
| 57 | `infra/.../community/dao/CommentCommandDao.kt` | 수정 | .value 언패킹 추가 |
| 58 | `infra/.../matching/dao/TargetInfoCommandDao.kt` | 수정 | .value 언패킹 추가 |

### Phase 8: Infrastructure Repository (.value 언패킹) 변경

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 59 | `infra/.../matching/repository/MatchingResultCoreRepository.kt` | 수정 | .value 언패킹 추가 |
| 60 | `infra/.../member/repository/MemberQueryCoreRepository.kt` | 수정 | .value 언패킹 추가 |
| 61 | `infra/.../member/repository/MemberPhotoCoreRepository.kt` | 수정 | .value 언패킹 추가 |
| 62 | `infra/.../auth/repository/RefreshTokenCoreRepository.kt` | 수정 | .value 언패킹 추가 |
| 63 | `infra/.../community/repository/PostLikeCoreRepository.kt` | 수정 | .value 언패킹 추가 |
| 64 | `infra/.../community/repository/CommentLikeCoreRepository.kt` | 수정 | .value 언패킹 추가 |

### Phase 9: Infrastructure - JwtManager

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 65 | `infra/.../ma-jwt-core/.../JwtManager.kt` | 수정 | Email 파라미터/반환 타입 변경 |

### Phase 10: Boot - JwtAuthenticationFilter

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 66 | `boot/.../support/security/JwtAuthenticationFilter.kt` | 수정 | email.value로 principal 저장 |

### Phase 11: Boot - Controller 변경

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 67 | `boot/.../matching/api/MatchingResultQueryApi.kt` | 수정 | Email(email) 변환 추가 |
| 68 | `boot/.../matching/api/MatchingResultCommandApi.kt` | 수정 | Email(email) 변환 추가 |
| 69 | `boot/.../member/api/MemberPhotoApi.kt` | 수정 | Email(email) 변환 추가 |
| 70 | `boot/.../matching/api/TargetInfoCommandApi.kt` | 수정 | Email(email) 변환 추가 |
| 71 | `boot/.../community/api/CommentCommandApi.kt` | 수정 | Email(email) 변환 추가 |
| 72 | `boot/.../community/api/PostCommandApi.kt` | 수정 | Email(email) 변환 추가 |
| 73 | `boot/.../community/api/PostLikeApi.kt` | 수정 | Email(email) 변환 추가 |
| 74 | `boot/.../community/api/CommentLikeApi.kt` | 수정 | Email(email) 변환 추가 |

### Phase 12: Boot - Request/Response DTO 변경

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 75 | `boot/.../auth/api/request/SignUpRequest.kt` | 수정 | toCommand에서 Email 변환 + import alias |
| 76 | `boot/.../auth/api/request/LoginRequest.kt` | 수정 | toCommand에서 Email 변환 + import alias |
| 77 | `boot/.../member/api/request/DuplicatedEmailRequest.kt` | 수정 | toEmail() 변환 메서드 추가 |
| 78 | `boot/.../matching/api/request/NewTargetInfoRequest.kt` | 수정 | registerEmail 파라미터 타입 변경 |
| 79 | `boot/.../community/api/request/NewCommentRequest.kt` | 수정 | authorEmail 파라미터 타입 변경 |
| 80 | `boot/.../community/api/request/NewPostRequest.kt` | 수정 | authorEmail 파라미터 타입 변경 |
| 81 | `boot/.../auth/api/response/LoginResponse.kt` | 수정 | .value로 언패킹 |

### Phase 13: Test Fixture 변경

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 82 | `domain/.../member/fixture/NewMemberFixture.kt` | 수정 | Email(email) 변환 |
| 83 | `domain/.../matching/fixture/MemberFixture.kt` | 수정 | Email(email) 변환 |

### Phase 14: 기존 테스트 수정

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 84-97 | `boot/ma-boot-web/src/test/...` (약 14개 파일) | 수정 | 테스트 코드에서 email을 Email()로 감싸거나 .value 사용 |
| 98-107 | `infra/.../src/test/...` (약 10개 파일) | 수정 | DAO 테스트에서 email String 유지 확인 |

---

## 5. 고려사항

### 5.1 `jakarta.validation.constraints.Email`과 도메인 `Email` 이름 충돌

- Request DTO에서 `@Email` 어노테이션과 도메인 `Email` 클래스의 fully qualified name이 충돌
- **해결**: import alias 사용 — `import jakarta.validation.constraints.Email as EmailAnnotation`
- **대안**: 도메인 Email 클래스를 `EmailAddress`로 네이밍할 수도 있으나, 프로젝트 전반에서 `email`이라는 이름으로 사용되므로 `Email`이 더 자연스러움

### 5.2 Spring Security principal은 String 유지

- `@AuthenticationPrincipal email: String`은 Spring Security가 `SecurityContext`에 저장한 `principal`을 바인딩
- `JwtAuthenticationFilter`에서 `UsernamePasswordAuthenticationToken(email.value, ...)`로 String을 저장하므로 컨트롤러에서 String으로 받는 것이 안전
- 컨트롤러에서 `Email(email)`로 즉시 변환하여 서비스에 전달

### 5.3 `data class` 사용 이유

- `Email`을 `data class`로 선언하여 `equals()`, `hashCode()` 자동 생성
- `Map<Email, String>` (Members의 nicknameByEmail), `Set<Email>` (extractTargetEmails) 등에서 정상 동작 보장
- 기존 `FourDigit`도 `data class`로 선언되어 있어 패턴 일관성 유지

### 5.4 Email 정규식 엄격도

- 도메인 `Email`의 정규식은 기본적인 형식 검증만 수행 (@ 포함, 도메인 존재 여부)
- Bean Validation의 `@Email`이 이미 API 경계에서 더 상세한 검증을 수행
- 도메인 Email은 "프로그래밍 오류 방지" 수준의 가드 역할

### 5.5 성능 영향

- `Email` 객체 생성 시 정규식 검증이 추가되지만, 이미 Bean Validation에서 검증된 값이므로 실패 가능성 낮음
- `data class`이므로 객체 비교 시 `equals()`가 `value` 비교로 동작하여 기존 String 비교와 동일한 성능
- DAO 레벨에서 `.value`로 언패킹하므로 DB 쿼리에는 영향 없음

### 5.6 마이그레이션 안전성

- DB 스키마 변경 없음 (email 컬럼은 여전히 VARCHAR)
- DDL 변경 없음
- API 응답 형식 변경 없음 (Response DTO에서 `.value`로 String 반환)
- 기존 JWT 토큰 호환성 유지 (subject는 여전히 String)

### 5.7 TargetInfoEntity (batch에서 사용)

- TargetInfoEntity의 `toDomain()`에서도 `registerEmail`을 `Email`로 감싸야 함
- batch 모듈에서 TargetInfo를 사용하는 경우 해당 코드도 확인 필요
- 현재 코드에서는 batch Job에서 TargetInfo를 직접 생성하는 부분이 있으므로 추가 확인 필요

### 5.8 PasswordVerifier 확인 필요

- `PasswordVerifier.verify()` 메서드가 `Member`를 받으므로 `Member.email`이 `Email`로 변경되면 내부에서 예외 생성 시 `email.value` 전달 필요
- `PasswordMismatchException(email: Email)`로 변경하거나, `PasswordMismatchException(member.email.value)`로 호출

### 5.9 batch 모듈 영향 범위

- `boot/ma-boot-batch`에서 TargetInfo, MatchingResult 등을 사용하는 Job이 있을 수 있음
- Phase 구현 시 batch 모듈의 email 사용처도 반드시 확인 필요
