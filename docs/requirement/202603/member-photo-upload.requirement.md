# Design: 회원가입 시 프로필 사진 업로드 통합

> 작성일: 2026-03-22
> 상태: Draft

## 1. 설계 개요

기존 회원가입 API(`POST /api/auth/sign-up`)를 `@RequestBody` JSON에서 `multipart/form-data`로 변환하여, 회원 정보(JSON part)와 프로필 사진(file part, 선택사항)을 한 번의 요청으로 처리한다. 파일 저장은 포트/어댑터 패턴으로 추상화하여 로컬 파일시스템에서 S3 등으로 교체 가능하게 설계한다.

## 2. 아키텍처

```
┌──────────────────────────────────────────────────────────────────────────────┐
│ boot/ma-boot-web                                                             │
│                                                                              │
│  SignUpApi (Controller) — multipart/form-data 수신                           │
│    @RequestPart("request") SignUpRequest (JSON)                              │
│    @RequestPart("photo", required=false) MultipartFile? (파일)               │
│      │                                                                       │
│      ├── MultipartFile → PhotoFile 변환 (Spring 의존성을 boot에 격리)         │
│      └── SignUpService.signUp(command, photoFile?)                           │
└──────────┬───────────────────────────────────────────────────────────────────┘
           │ (port)
┌──────────▼───────────────────────────────────────────────────────────────────┐
│ domain/ma-domain-core                                                        │
│                                                                              │
│  [domain] member/domain/photo/AllowedExtension (Value Object)                │
│  [domain] member/domain/photo/ApprovalStatus (Enum)                          │
│  [domain] member/domain/photo/PhotoFile (Value Object — 크기/확장자 검증)     │
│  [domain] member/domain/photo/NewPhoto (생성 전용 도메인 객체)                │
│  [domain] member/domain/photo/MemberPhoto (도메인 모델)                       │
│                                                                              │
│  [port] member/domain/photo/port/MemberPhotoRepository                       │
│    + save(newPhoto: NewPhoto): Long                                          │
│    + findByMemberEmail(email: String): MemberPhoto?                          │
│    + deleteByMemberEmail(email: String)                                      │
│                                                                              │
│  [port] member/domain/photo/port/FileStorage                                 │
│    + store(email: String, file: PhotoFile): String                           │
│    + delete(filePath: String)                                                │
│                                                                              │
│  [application] auth/application/SignUpService — photoFile 파라미터 추가       │
│    signUp(command: SignUpCommand, photoFile: PhotoFile?): Long                │
└──────────┬──────────────────────┬────────────────────────────────────────────┘
           │ (implements)         │ (implements)
┌──────────▼──────────────┐  ┌───▼──────────────────────────────────────┐
│ infrastructure/storage/  │  │ infrastructure/support/                  │
│ ma-db-core               │  │ ma-file-storage (신규 모듈)              │
│                          │  │                                         │
│  MemberPhotoTable        │  │  LocalFileStorage                       │
│  MemberPhotoEntity       │  │    implements FileStorage               │
│  MemberPhotoCommandDao   │  │    저장: uploads/{email}/{uuid}.ext     │
│  MemberPhotoQueryDao     │  │    삭제: Files.delete()                 │
│  MemberPhotoCoreRepo     │  │                                         │
└──────────────────────────┘  └─────────────────────────────────────────┘
```

## 3. 상세 설계

---

### 3.1 Domain - AllowedExtension (Value Object)

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/member/domain/photo/AllowedExtension.kt`

```kotlin
package com.konkuk.ma.domain.member.domain.photo

class AllowedExtension(val value: String) {

    init {
        val normalized = value.lowercase().removePrefix(".")
        require(normalized in ALLOWED_EXTENSIONS) {
            "허용되지 않는 파일 형식입니다: $value (허용: ${ALLOWED_EXTENSIONS.joinToString()})"
        }
    }

    val normalized: String get() = value.lowercase().removePrefix(".")

    companion object {
        private val ALLOWED_EXTENSIONS = setOf("jpeg", "jpg", "png", "svg", "webp")

        fun from(originalFileName: String): AllowedExtension {
            val extension = originalFileName.substringAfterLast('.', "")
            return AllowedExtension(extension)
        }
    }
}
```

- 원시값 포장 패턴 적용 (규칙 2)
- 생성 시점에 유효성 검증하여 잘못된 확장자가 도메인에 유입되는 것을 차단
- `from(originalFileName)` 팩토리 메서드로 파일명에서 확장자를 추출하여 생성 (규칙 7)

---

### 3.2 Domain - ApprovalStatus (Enum)

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/member/domain/photo/ApprovalStatus.kt`

```kotlin
package com.konkuk.ma.domain.member.domain.photo

import com.konkuk.ma.domain.common.domain.EnumWithDisplayName

enum class ApprovalStatus(override val displayName: String) : EnumWithDisplayName {
    PENDING("대기"),
    APPROVED("승인"),
    REJECTED("거절");

    fun isPending(): Boolean = this == PENDING
}
```

- 기존 `EnumWithDisplayName` 인터페이스를 구현하여 프로젝트 패턴과 일관성 유지
- 상태 판단 행위를 enum 내부에 부여 (규칙 1)

---

### 3.3 Domain - PhotoFile (Value Object)

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/member/domain/photo/PhotoFile.kt`

```kotlin
package com.konkuk.ma.domain.member.domain.photo

class PhotoFile(
    val originalFileName: String,
    val extension: AllowedExtension,
    val sizeInBytes: Long,
    val content: ByteArray
) {
    init {
        require(sizeInBytes <= MAX_FILE_SIZE_BYTES) {
            "파일 크기는 ${MAX_FILE_SIZE_MB}MB를 초과할 수 없습니다: ${sizeInBytes}bytes"
        }
        require(content.isNotEmpty()) {
            "파일 내용이 비어있습니다."
        }
    }

    companion object {
        private const val MAX_FILE_SIZE_MB = 10
        private const val MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024L * 1024L

        fun create(originalFileName: String, sizeInBytes: Long, content: ByteArray): PhotoFile {
            return PhotoFile(
                originalFileName = originalFileName,
                extension = AllowedExtension.from(originalFileName),
                sizeInBytes = sizeInBytes,
                content = content
            )
        }
    }
}
```

- 파일 크기 제한(10MB)을 도메인 객체 내부에서 검증 (규칙 5: 상태 검증은 객체 내부에서)
- `ByteArray`를 포함하여 실제 파일 내용까지 캡슐화
- Spring의 `MultipartFile`에 의존하지 않음 (도메인 레이어 NO Spring 원칙)

---

### 3.4 Domain - MemberPhoto (도메인 모델)

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/member/domain/photo/MemberPhoto.kt`

```kotlin
package com.konkuk.ma.domain.member.domain.photo

class MemberPhoto(
    val id: Long,
    val memberEmail: String,
    val filePath: String,
    val originalFileName: String,
    val approvalStatus: ApprovalStatus
) {
    fun belongsTo(email: String): Boolean = memberEmail == email
}
```

- `belongsTo`: 소유 여부 판단 행위를 객체에 부여 (규칙 1)

---

### 3.5 Domain - NewPhoto (생성 전용 도메인 객체)

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/member/domain/photo/NewPhoto.kt`

```kotlin
package com.konkuk.ma.domain.member.domain.photo

class NewPhoto(
    val memberEmail: String,
    val filePath: String,
    val originalFileName: String
) {
    companion object {
        fun create(
            memberEmail: String,
            filePath: String,
            originalFileName: String
        ): NewPhoto {
            return NewPhoto(
                memberEmail = memberEmail,
                filePath = filePath,
                originalFileName = originalFileName
            )
        }
    }
}
```

- 기존 `NewMember`, `NewTargetInfo` 패턴을 따름
- 팩토리 메서드로 생성 의도를 명확히 드러냄 (규칙 7)

---

### 3.6 Domain Port - MemberPhotoRepository

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/member/domain/photo/port/MemberPhotoRepository.kt`

```kotlin
package com.konkuk.ma.domain.member.domain.photo.port

import com.konkuk.ma.domain.member.domain.photo.MemberPhoto
import com.konkuk.ma.domain.member.domain.photo.NewPhoto

interface MemberPhotoRepository {
    fun save(newPhoto: NewPhoto): Long
    fun findByMemberEmail(email: String): MemberPhoto?
    fun deleteByMemberEmail(email: String)
}
```

- 1장만 저장하므로 `findByMemberEmail`은 nullable 단건 반환
- 교체 시 기존 사진 삭제를 위해 `deleteByMemberEmail` 제공
- 포트 인터페이스는 도메인 타입 사용 (규칙 5)

---

### 3.7 Domain Port - FileStorage

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/member/domain/photo/port/FileStorage.kt`

```kotlin
package com.konkuk.ma.domain.member.domain.photo.port

import com.konkuk.ma.domain.member.domain.photo.PhotoFile

interface FileStorage {
    fun store(email: String, file: PhotoFile): String
    fun delete(filePath: String)
}
```

- `store` 반환값: 저장된 파일의 경로(String) -- DB에 저장할 참조 경로
- `email` 파라미터: 사용자별 디렉토리 분리를 위해 필요
- 이 포트를 통해 로컬 파일시스템 -> S3 -> GCS 등으로 어댑터 교체 가능

---

### 3.8 Domain Application - SignUpService 수정

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/auth/application/SignUpService.kt`

```kotlin
package com.konkuk.ma.domain.auth.application

import com.konkuk.ma.domain.auth.application.command.SignUpCommand
import com.konkuk.ma.domain.auth.domain.SignUpValidator
import com.konkuk.ma.domain.auth.domain.port.PasswordEncryptor
import com.konkuk.ma.domain.member.domain.photo.NewPhoto
import com.konkuk.ma.domain.member.domain.photo.PhotoFile
import com.konkuk.ma.domain.member.domain.photo.port.FileStorage
import com.konkuk.ma.domain.member.domain.photo.port.MemberPhotoRepository
import com.konkuk.ma.domain.member.domain.port.MemberCommandRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SignUpService(
    private val memberCommandRepository: MemberCommandRepository,
    private val signUpValidator: SignUpValidator,
    private val passwordEncryptor: PasswordEncryptor,
    private val fileStorage: FileStorage,                          // 추가
    private val memberPhotoRepository: MemberPhotoRepository       // 추가
) {
    fun signUp(signUpCommand: SignUpCommand, photoFile: PhotoFile?): Long {  // 시그니처 변경
        val newMember = signUpCommand.toNewMember(passwordEncryptor)
        signUpValidator.validate(newMember)
        val memberId = memberCommandRepository.save(newMember)

        if (photoFile != null) {                                   // 추가
            savePhoto(newMember.email, photoFile)
        }

        return memberId
    }

    private fun savePhoto(email: String, photoFile: PhotoFile) {   // 추가
        val filePath = fileStorage.store(email, photoFile)
        val newPhoto = NewPhoto.create(
            memberEmail = email,
            filePath = filePath,
            originalFileName = photoFile.originalFileName
        )
        memberPhotoRepository.save(newPhoto)
    }
}
```

- 기존 `signUp(signUpCommand)` 시그니처를 `signUp(signUpCommand, photoFile?)` 로 변경
- `photoFile`이 null이면 사진 저장을 건너뜀 (사진은 선택사항)
- 파일 저장 -> DB 저장 순서: 파일 저장 실패 시 트랜잭션 롤백으로 DB 정합성 유지
- `savePhoto`는 private 메서드로 분리하여 함수의 단일 책임 원칙 유지 (Clean Code 규칙 2)

---

### 3.9 Boot - SignUpApi 수정

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/auth/api/SignUpApi.kt`

```kotlin
package com.konkuk.ma.domain.auth.api

import com.konkuk.ma.domain.auth.api.request.SignUpRequest
import com.konkuk.ma.domain.auth.api.response.SignUpResponse
import com.konkuk.ma.domain.auth.application.SignUpService
import com.konkuk.ma.domain.member.domain.photo.PhotoFile
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/auth")
class SignUpApi(
    private val signUpService: SignUpService
) {
    @PostMapping("/sign-up", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun signUp(
        @Valid @RequestPart("request") request: SignUpRequest,
        @RequestPart("photo", required = false) photo: MultipartFile?
    ): SignUpResponse {
        val photoFile = photo?.let {
            PhotoFile.create(
                originalFileName = it.originalFilename ?: "unknown",
                sizeInBytes = it.size,
                content = it.bytes
            )
        }

        val memberId = signUpService.signUp(request.toCommand(), photoFile)

        return SignUpResponse(
            memberId = memberId,
            email = request.email,
            nickname = request.nickname
        )
    }
}
```

**변경 포인트:**
- `@RequestBody` -> `@RequestPart("request")`: JSON part를 multipart의 일부로 수신
- `@RequestPart("photo", required = false)`: 사진 파일을 선택적으로 수신
- `consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]`: Content-Type 명시
- `MultipartFile` -> `PhotoFile` 변환을 컨트롤러에서 수행 (Spring 의존성을 boot 레이어에 격리)
- Spring의 `@Valid`는 `@RequestPart` 바인딩 시에도 `SignUpRequest`의 Bean Validation 어노테이션을 정상 적용함

**클라이언트 요청 형식:**
```
POST /api/auth/sign-up
Content-Type: multipart/form-data; boundary=----

------
Content-Disposition: form-data; name="request"
Content-Type: application/json

{"email": "...", "password": "...", ...}
------
Content-Disposition: form-data; name="photo"; filename="profile.jpg"
Content-Type: image/jpeg

(binary data)
------
```

---

### 3.10 Boot - SecurityConfig 확인

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/config/SecurityConfig.kt`

```kotlin
// 변경 불필요
// 기존 .requestMatchers(HttpMethod.POST, "/api/auth/sign-up").permitAll() 이 그대로 적용됨
// URL과 HTTP method가 동일하므로 Content-Type 변경은 Security 설정에 영향 없음
```

---

### 3.11 Boot - GlobalExceptionHandler 수정

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/support/error/GlobalExceptionHandler.kt`

```kotlin
package com.konkuk.ma.support.error

import com.konkuk.ma.domain.auth.exception.RefreshTokenExpiredException
import com.konkuk.ma.domain.common.exception.InvalidValueException
import com.konkuk.ma.domain.member.exception.DuplicateEmailException
import com.konkuk.ma.domain.member.exception.DuplicateNicknameException
import com.konkuk.ma.domain.member.exception.PasswordMismatchException
import com.konkuk.ma.domain.member.exception.SmsNotVerifiedException
import com.konkuk.ma.exception.BusinessException
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.logger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.multipart.MaxUploadSizeExceededException              // 추가

@RestControllerAdvice
class GlobalExceptionHandler {

    // 기존 핸들러 유지

    @ExceptionHandler(EntityNotFoundException::class)
    fun handleEntityNotFoundException(e: EntityNotFoundException): ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.message)
    }

    @ExceptionHandler(DuplicateNicknameException::class, DuplicateEmailException::class)
    fun handleDuplicateException(e: BusinessException): ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.message)
    }

    @ExceptionHandler(PasswordMismatchException::class, RefreshTokenExpiredException::class)
    fun handleUnauthorizedException(e: BusinessException): ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.message)
    }

    @ExceptionHandler(InvalidValueException::class, SmsNotVerifiedException::class)
    fun handleBadRequestException(e: BusinessException): ResponseEntity<String> {
        return ResponseEntity.badRequest().body(e.message)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(e: MethodArgumentNotValidException): ResponseEntity<String> {
        val message = e.bindingResult.allErrors.firstOrNull()?.defaultMessage ?: "Invalid Request"
        return ResponseEntity.badRequest().body(message)
    }

    @ExceptionHandler(MaxUploadSizeExceededException::class)                          // 추가
    fun handleMaxUploadSizeExceededException(e: MaxUploadSizeExceededException): ResponseEntity<String> {
        return ResponseEntity.badRequest().body("파일 크기가 허용 한도를 초과했습니다.")
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<String> {
        logger.error(e) { "예상하지 못한 에러가 발생했습니다." }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 내부 오류가 발생했습니다.")
    }
}
```

- `MaxUploadSizeExceededException` 핸들러 추가: Spring의 multipart 크기 제한 초과 시 400 응답
- 기존 `Exception` 핸들러보다 위에 위치하여 우선 처리됨

---

### 3.12 Boot - application.yml 수정

**파일**: `boot/ma-boot-web/src/main/resources/application.yml`

```yaml
spring:
  profiles:
    active: local
  servlet:                          # 추가
    multipart:
      max-file-size: 10MB
      max-request-size: 20MB

file:                               # 추가
  upload:
    base-path: uploads
```

- `max-file-size: 10MB`: 단일 파일 크기 제한
- `max-request-size: 20MB`: 전체 요청 크기 제한 (JSON part + 파일을 합산)
- `file.upload.base-path`: 로컬 파일 저장 기본 경로

---

### 3.13 Infrastructure - DDL 추가

**파일**: `infrastructure/storage/ma-db-core/src/main/resources/script/ddl.sql`

```sql
-- 기존 DDL 유지 (MEMBERS, REFRESH_TOKENS, TARGET_INFOS, MATCHING_RESULTS)

-- MEMBER PHOTOS
CREATE TABLE MEMBER_PHOTOS
(
    MEMBER_PHOTO_ID    BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- MemberPhotoTable 특화 컬럼들
    MEMBER_EMAIL       VARCHAR(255) NOT NULL,
    FILE_PATH          VARCHAR(512) NOT NULL,
    ORIGINAL_FILE_NAME VARCHAR(255) NOT NULL,
    APPROVAL_STATUS    VARCHAR(32)  NOT NULL DEFAULT 'PENDING',

    -- BaseTable 공통 컬럼들
    CREATED_DATE       DATETIME     DEFAULT CURRENT_TIMESTAMP,
    CREATED_BY         VARCHAR(255) DEFAULT 'MEET_AGAIN',
    LAST_MODIFIED_DATE DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    LAST_MODIFIED_BY   VARCHAR(255) DEFAULT 'MEET_AGAIN',
    DELETED            BOOLEAN      DEFAULT FALSE,

    -- 인덱스
    INDEX idx_member_photo_email (MEMBER_EMAIL)
);
```

- 기존 DDL 스타일(컬럼 정렬, 주석 패턴, BaseTable 공통 컬럼 순서)을 정확히 따름
- FK 사용 금지 -- `MEMBER_EMAIL`은 INDEX로만 관리
- `MEMBER_EMAIL`에 INDEX 추가: 사용자별 사진 조회 성능 보장

---

### 3.14 Infrastructure - MemberPhotoTable (Exposed Table)

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/member/entity/table/MemberPhotoTable.kt`

```kotlin
package com.konkuk.ma.domain.member.entity.table

import com.konkuk.ma.domain.common.entity.table.BaseTable

object MemberPhotoTable : BaseTable("MEMBER_PHOTOS", "MEMBER_PHOTO_ID") {
    val memberEmail = varchar("MEMBER_EMAIL", 255)
    val filePath = varchar("FILE_PATH", 512)
    val originalFileName = varchar("ORIGINAL_FILE_NAME", 255)
    val approvalStatus = varchar("APPROVAL_STATUS", 32).clientDefault { "PENDING" }
}
```

- 기존 `MemberTable`, `TargetInfoTable` 패턴과 동일하게 `BaseTable` 상속
- `approvalStatus`는 varchar로 저장하여 enum 확장에 유연 대응
- 패키지: `com.konkuk.ma.domain.member.entity.table` (기존 `MemberTable`과 동일 패키지)

---

### 3.15 Infrastructure - MemberPhotoEntity

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/member/entity/MemberPhotoEntity.kt`

```kotlin
package com.konkuk.ma.domain.member.entity

import com.konkuk.ma.domain.member.domain.photo.ApprovalStatus
import com.konkuk.ma.domain.member.domain.photo.MemberPhoto
import com.konkuk.ma.domain.member.entity.table.MemberPhotoTable
import org.jetbrains.exposed.sql.ResultRow

class MemberPhotoEntity(
    val id: Long,
    val memberEmail: String,
    val filePath: String,
    val originalFileName: String,
    val approvalStatus: String
) {
    fun toDomain(): MemberPhoto {
        return MemberPhoto(
            id = id,
            memberEmail = memberEmail,
            filePath = filePath,
            originalFileName = originalFileName,
            approvalStatus = ApprovalStatus.valueOf(approvalStatus)
        )
    }

    companion object {
        fun from(row: ResultRow): MemberPhotoEntity {
            return MemberPhotoEntity(
                id = row[MemberPhotoTable.id].value,
                memberEmail = row[MemberPhotoTable.memberEmail],
                filePath = row[MemberPhotoTable.filePath],
                originalFileName = row[MemberPhotoTable.originalFileName],
                approvalStatus = row[MemberPhotoTable.approvalStatus]
            )
        }
    }
}
```

- DAO는 Entity를 반환, Entity에서 도메인으로 변환 패턴 준수 (규칙 8)
- `toDomain()` 메서드에서 `ApprovalStatus.valueOf`로 문자열 -> enum 변환
- `companion object`의 `from(row: ResultRow)` 팩토리 메서드로 `ResultRow`에서 생성 (규칙 8)
- 패키지: `com.konkuk.ma.domain.member.entity` (기존 `MemberEntity`와 동일 패키지)

**참고**: 기존 `RowEntityMapper` 오브젝트에 메서드를 추가하는 대신, Entity 클래스의 `companion object`에 `from(row)` 팩토리 메서드를 두는 방식으로 설계했다. 이는 규칙 8의 권장 패턴을 따르며, `RowEntityMapper`의 비대화를 방지한다.

---

### 3.16 Infrastructure - MemberPhotoCommandDao

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/member/dao/MemberPhotoCommandDao.kt`

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
            it[memberEmail] = newPhoto.memberEmail
            it[filePath] = newPhoto.filePath
            it[originalFileName] = newPhoto.originalFileName
            it[createdBy] = newPhoto.memberEmail
            it[lastModifiedBy] = newPhoto.memberEmail
        }.value
    }

    fun deleteByMemberEmail(email: String) {
        MemberPhotoTable.deleteWhere {
            MemberPhotoTable.memberEmail eq email
        }
    }
}
```

- 기존 `MemberCommandDao` 패턴 준수: `@Component`, `insertAndGetId`, Exposed DSL
- `createdBy`, `lastModifiedBy`에 `memberEmail` 설정 (기존 `MemberCommandDao.save`와 동일 패턴)

---

### 3.17 Infrastructure - MemberPhotoQueryDao

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/member/dao/MemberPhotoQueryDao.kt`

```kotlin
package com.konkuk.ma.domain.member.dao

import com.konkuk.ma.domain.member.entity.MemberPhotoEntity
import com.konkuk.ma.domain.member.entity.table.MemberPhotoTable
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
}
```

- 기존 `MemberQueryDao` 패턴 준수
- `deleted eq false` 조건으로 소프트 삭제 레코드 제외
- 1장만 저장하므로 단건 nullable 반환
- `MemberPhotoEntity.from(row)` 팩토리 메서드 사용 (규칙 8)

---

### 3.18 Infrastructure - MemberPhotoCoreRepository

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/member/repository/MemberPhotoCoreRepository.kt`

```kotlin
package com.konkuk.ma.domain.member.repository

import com.konkuk.ma.domain.member.dao.MemberPhotoCommandDao
import com.konkuk.ma.domain.member.dao.MemberPhotoQueryDao
import com.konkuk.ma.domain.member.domain.photo.MemberPhoto
import com.konkuk.ma.domain.member.domain.photo.NewPhoto
import com.konkuk.ma.domain.member.domain.photo.port.MemberPhotoRepository
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
}
```

- 기존 `MemberCommandCoreRepository`, `MemberQueryCoreRepository` 패턴 준수
- DAO에서 Entity를 받아 `toDomain()`으로 도메인 객체 변환 (규칙 8)

---

### 3.19 Infrastructure - LocalFileStorage (새 모듈: ma-file-storage)

**모듈 구조**: `infrastructure/support/ma-file-storage/`

**파일**: `infrastructure/support/ma-file-storage/build.gradle.kts`

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation(project(":domain:ma-domain-core"))
}
```

**파일**: `infrastructure/support/ma-file-storage/src/main/kotlin/com/konkuk/ma/storage/LocalFileStorage.kt`

```kotlin
package com.konkuk.ma.storage

import com.konkuk.ma.domain.member.domain.photo.PhotoFile
import com.konkuk.ma.domain.member.domain.photo.port.FileStorage
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID

@Component
class LocalFileStorage(
    @Value("\${file.upload.base-path:uploads}")
    private val basePath: String
) : FileStorage {

    override fun store(email: String, file: PhotoFile): String {
        val directory = Paths.get(basePath, email)
        Files.createDirectories(directory)

        val storedFileName = "${UUID.randomUUID()}.${file.extension.normalized}"
        val targetPath = directory.resolve(storedFileName)
        Files.write(targetPath, file.content)

        return targetPath.toString()
    }

    override fun delete(filePath: String) {
        val path = Paths.get(filePath)
        if (Files.exists(path)) {
            Files.delete(path)
        }
    }
}
```

- UUID 기반 파일명으로 충돌 방지
- `email`별 디렉토리 분리로 파일 관리 용이
- `basePath`는 `@Value`로 외부 설정 주입 (규칙 9: 변동 가능성 높은 값은 파라미터로)
- 향후 S3 어댑터로 교체 시 `FileStorage` 인터페이스만 새로 구현하면 됨

---

### 3.20 Build Configuration 수정

**파일**: `settings.gradle.kts` (루트)

```kotlin
rootProject.name = "meet-again"

include("boot:ma-boot-web")
include("boot:ma-boot-batch")
include("domain:ma-domain-core")
include("infrastructure:storage:ma-db-core")
include("infrastructure:storage:ma-redis-core")
include("infrastructure:support:ma-sms-sender")
include("infrastructure:support:ma-crypto-core")
include("infrastructure:support:ma-jwt-core")
include("infrastructure:support:ma-file-storage")    // 추가
include("config:ma-config-yaml-importer")
include("config:ma-config-logging")
```

**파일**: `boot/ma-boot-web/build.gradle.kts` (의존성 추가)

```kotlin
// 기존 runtimeOnly 블록에 추가
runtimeOnly(project(":infrastructure:support:ma-file-storage"))    // 추가
```

---

### 3.21 Boot - SignUpApiTest 수정

**파일**: `boot/ma-boot-web/src/test/kotlin/com/konkuk/ma/domain/auth/api/SignUpApiTest.kt`

기존 테스트가 `postJson`으로 JSON을 전송하고 있으므로, multipart/form-data 방식으로 전면 변경해야 한다.

```kotlin
package com.konkuk.ma.domain.auth.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.auth.application.SignUpService
import com.konkuk.ma.domain.auth.application.command.SignUpCommand
import com.konkuk.ma.domain.member.domain.Gender
import com.konkuk.ma.domain.member.domain.Region
import com.konkuk.ma.domain.member.domain.photo.PhotoFile
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.requestPart
import com.konkuk.ma.extension.requestParts
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.vocabulary.email
import com.konkuk.ma.vocabulary.memberId
import com.konkuk.ma.vocabulary.message
import com.konkuk.ma.vocabulary.nickname
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import java.time.LocalDate
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath

@WebMvcTest(SignUpApi::class)
@BaseApiTest
class SignUpApiTest(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper,
    @MockkBean private val signUpService: SignUpService
) : FunSpec({

    test("signUp - 유효한 회원가입 요청시 성공한다 (사진 포함)") {
        // Given
        val memberId = 1L
        val request = mapOf(
            "email" to "test@example.com",
            "password" to "password123",
            "phoneNumber" to "01012345678",
            "nickname" to "testuser",
            "gender" to Gender.MALE.name,
            "name" to "김테스트",
            "birthDate" to "1990-01-01",
            "region" to "SEOUL",
            "highSchool" to "테스트고등학교",
            "university" to "테스트대학교"
        )

        val requestPart = MockMultipartFile(
            "request",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            mapper.writeValueAsBytes(request)
        )

        val photoPart = MockMultipartFile(
            "photo",
            "profile.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "fake-image-content".toByteArray()
        )

        every {
            signUpService.signUp(
                SignUpCommand(
                    email = "test@example.com",
                    password = "password123",
                    nickname = "testuser",
                    gender = Gender.MALE,
                    phoneNumber = "01012345678",
                    name = "김테스트",
                    birthDate = LocalDate.of(1990, 1, 1),
                    region = Region.SEOUL,
                    highSchool = "테스트고등학교",
                    university = "테스트대학교"
                ),
                any<PhotoFile>()
            )
        } returns memberId

        // When & Then
        mockMvc.multipart("/api/auth/sign-up") {
            file(requestPart)
            file(photoPart)
            accept = MediaType.APPLICATION_JSON
        }
            .andExpect {
                status { isCreated() }
                jsonPath("$.memberId").value(memberId)
                jsonPath("$.email").value("test@example.com")
                jsonPath("$.nickname").value("testuser")
                jsonPath("$.message").value("회원가입이 완료되었습니다.")
            }
            .andDocument(
                "sign-up-with-photo",
                requestParts(
                    "request" requestPart "회원가입 정보 (JSON)",
                    "photo" requestPart "프로필 사진 파일 (선택, 10MB 이하, jpeg/jpg/png/svg/webp)" isOptional true,
                ),
                responseBody(
                    memberId(),
                    email(),
                    nickname(),
                    message(),
                )
            )
    }

    test("signUp - 사진 없이 회원가입 요청시 성공한다") {
        // Given
        val memberId = 2L
        val request = mapOf(
            "email" to "test2@example.com",
            "password" to "password123",
            "phoneNumber" to "01098765432",
            "nickname" to "testuser2",
            "gender" to Gender.FEMALE.name,
            "name" to "이테스트",
            "birthDate" to "1995-06-15",
            "region" to "BUSAN"
        )

        val requestPart = MockMultipartFile(
            "request",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            mapper.writeValueAsBytes(request)
        )

        every {
            signUpService.signUp(
                SignUpCommand(
                    email = "test2@example.com",
                    password = "password123",
                    nickname = "testuser2",
                    gender = Gender.FEMALE,
                    phoneNumber = "01098765432",
                    name = "이테스트",
                    birthDate = LocalDate.of(1995, 6, 15),
                    region = Region.BUSAN,
                    highSchool = null,
                    university = null
                ),
                null
            )
        } returns memberId

        // When & Then
        mockMvc.multipart("/api/auth/sign-up") {
            file(requestPart)
            accept = MediaType.APPLICATION_JSON
        }
            .andExpect {
                status { isCreated() }
                jsonPath("$.memberId").value(memberId)
                jsonPath("$.email").value("test2@example.com")
                jsonPath("$.nickname").value("testuser2")
            }
            .andDocument(
                "sign-up-without-photo",
                requestParts(
                    "request" requestPart "회원가입 정보 (JSON)",
                ),
                responseBody(
                    memberId(),
                    email(),
                    nickname(),
                    message(),
                )
            )
    }
})
```

**변경 포인트:**
- `postJson` -> `mockMvc.multipart`: multipart/form-data 테스트로 전환
- `MockMultipartFile`로 JSON part와 파일 part를 각각 생성
- `requestParts` (REST Docs snippet)를 사용하여 multipart part 문서화
- 기존 `requestBody` snippet은 `requestParts`로 교체 (multipart에서는 `requestFields` 사용 불가)
- 기존 프로젝트에 `requestPart` DSL 함수와 `requestParts` 함수가 이미 구현되어 있음 (`RestDocsExtensions.kt`, `CustomDescriptor.kt`)

---

### 3.22 Domain Test - PhotoFile 단위 테스트

**파일**: `domain/ma-domain-core/src/test/kotlin/com/konkuk/ma/domain/member/domain/photo/PhotoFileTest.kt`

```kotlin
package com.konkuk.ma.domain.member.domain.photo

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class PhotoFileTest : BehaviorSpec({

    Given("유효한 파일 정보가 주어졌을 때") {
        val content = "fake-image".toByteArray()

        When("PhotoFile을 생성하면") {
            val photoFile = PhotoFile.create(
                originalFileName = "profile.jpg",
                sizeInBytes = content.size.toLong(),
                content = content
            )

            Then("정상 생성된다") {
                photoFile.originalFileName shouldBe "profile.jpg"
                photoFile.extension.normalized shouldBe "jpg"
            }
        }
    }

    Given("10MB를 초과하는 파일이 주어졌을 때") {
        val overSizedBytes = 10 * 1024 * 1024L + 1

        When("PhotoFile을 생성하면") {
            Then("예외가 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    PhotoFile.create(
                        originalFileName = "large.jpg",
                        sizeInBytes = overSizedBytes,
                        content = ByteArray(1) { 0 }
                    )
                }
            }
        }
    }

    Given("빈 파일 내용이 주어졌을 때") {
        When("PhotoFile을 생성하면") {
            Then("예외가 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    PhotoFile.create(
                        originalFileName = "empty.jpg",
                        sizeInBytes = 0,
                        content = byteArrayOf()
                    )
                }
            }
        }
    }
})
```

---

### 3.23 Domain Test - AllowedExtension 단위 테스트

**파일**: `domain/ma-domain-core/src/test/kotlin/com/konkuk/ma/domain/member/domain/photo/AllowedExtensionTest.kt`

```kotlin
package com.konkuk.ma.domain.member.domain.photo

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class AllowedExtensionTest : BehaviorSpec({

    Given("허용된 확장자가 주어졌을 때") {
        listOf("jpeg", "jpg", "png", "svg", "webp").forEach { ext ->
            When("${ext} 확장자로 생성하면") {
                val extension = AllowedExtension(ext)

                Then("정상 생성된다") {
                    extension.normalized shouldBe ext
                }
            }
        }
    }

    Given("파일명에서 확장자를 추출할 때") {
        When("profile.PNG 파일명이 주어지면") {
            val extension = AllowedExtension.from("profile.PNG")

            Then("소문자로 정규화된 확장자가 생성된다") {
                extension.normalized shouldBe "png"
            }
        }
    }

    Given("허용되지 않는 확장자가 주어졌을 때") {
        When("gif 확장자로 생성하면") {
            Then("예외가 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    AllowedExtension("gif")
                }
            }
        }
    }
})
```

---

## 4. 구현 순서

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 1 | `domain/.../member/domain/photo/AllowedExtension.kt` | 신규 | 허용 확장자 Value Object |
| 2 | `domain/.../member/domain/photo/ApprovalStatus.kt` | 신규 | 승인 상태 enum |
| 3 | `domain/.../member/domain/photo/PhotoFile.kt` | 신규 | 파일 Value Object (크기 검증 포함) |
| 4 | `domain/.../member/domain/photo/MemberPhoto.kt` | 신규 | 사진 도메인 모델 |
| 5 | `domain/.../member/domain/photo/NewPhoto.kt` | 신규 | 생성 전용 도메인 객체 |
| 6 | `domain/.../member/domain/photo/port/MemberPhotoRepository.kt` | 신규 | DB 포트 인터페이스 |
| 7 | `domain/.../member/domain/photo/port/FileStorage.kt` | 신규 | 파일 저장 포트 인터페이스 |
| 8 | `domain/.../auth/application/SignUpService.kt` | **수정** | photoFile 파라미터 추가, savePhoto 로직 추가 |
| 9 | `ddl.sql` | 수정 | MEMBER_PHOTOS 테이블 DDL 추가 |
| 10 | `infra/.../member/entity/table/MemberPhotoTable.kt` | 신규 | Exposed Table 정의 |
| 11 | `infra/.../member/entity/MemberPhotoEntity.kt` | 신규 | Entity + toDomain() + from(row) |
| 12 | `infra/.../member/dao/MemberPhotoCommandDao.kt` | 신규 | 쓰기 DAO |
| 13 | `infra/.../member/dao/MemberPhotoQueryDao.kt` | 신규 | 읽기 DAO |
| 14 | `infra/.../member/repository/MemberPhotoCoreRepository.kt` | 신규 | 포트 구현체 |
| 15 | `settings.gradle.kts` | 수정 | ma-file-storage 모듈 등록 |
| 16 | `infra/support/ma-file-storage/build.gradle.kts` | 신규 | 모듈 빌드 설정 |
| 17 | `infra/support/ma-file-storage/.../LocalFileStorage.kt` | 신규 | 로컬 파일 저장 어댑터 |
| 18 | `boot/ma-boot-web/build.gradle.kts` | 수정 | ma-file-storage 의존성 추가 |
| 19 | `boot/.../auth/api/SignUpApi.kt` | **수정** | @RequestBody -> @RequestPart, multipart 수신 |
| 20 | `boot/.../support/error/GlobalExceptionHandler.kt` | 수정 | MaxUploadSize 예외 핸들러 추가 |
| 21 | `boot/.../resources/application.yml` | 수정 | multipart 및 file 설정 추가 |
| 22 | `domain/.../member/domain/photo/AllowedExtensionTest.kt` | 신규 | 확장자 검증 단위 테스트 |
| 23 | `domain/.../member/domain/photo/PhotoFileTest.kt` | 신규 | 파일 검증 단위 테스트 |
| 24 | `boot/.../auth/api/SignUpApiTest.kt` | **수정** | multipart 테스트로 전면 변경 |

---

## 5. 고려사항

- **@RequestPart + @Valid 동작**: Spring Boot 3.x에서 `@Valid @RequestPart("request") SignUpRequest`는 `application/json` Content-Type으로 전송된 multipart part를 `HttpMessageConverter`로 역직렬화한 뒤 Bean Validation을 수행한다. 기존 `SignUpRequest`의 `@NotBlank`, `@Email`, `@Pattern` 어노테이션이 그대로 동작한다. 단, 클라이언트가 request part의 Content-Type을 `application/json`으로 명시해야 한다.

- **기존 API 호환성 깨짐 (Breaking Change)**: 기존 클라이언트가 `application/json`으로 회원가입 요청을 보내고 있다면, `multipart/form-data`로 변경 후 기존 요청이 실패한다. 클라이언트(프론트엔드/모바일)와 동시에 배포해야 한다.

- **FK 미사용**: `MEMBER_PHOTOS.MEMBER_EMAIL`은 `MEMBERS.EMAIL`을 참조하지만 FK 제약조건 없이 INDEX만 설정했다. 참조 무결성은 애플리케이션 레벨에서 보장한다.

- **파일 저장 -> DB 저장 순서**: `SignUpService.savePhoto()`에서 파일 저장 -> DB 저장 순서로 진행한다. 파일 저장 성공 후 DB 저장 실패(트랜잭션 롤백) 시 파일이 남는 문제가 있으나, 회원가입 실패 시 전체 트랜잭션이 롤백되므로 고아 파일이 생길 수 있다. 이는 주기적 배치 정리 또는 즉각 정리(try-catch)로 대응 가능하다. 현재 단계에서는 단순성을 우선한다.

- **포트/어댑터 교체 가능성**: `FileStorage` 인터페이스를 통해 로컬 파일시스템 어댑터(`LocalFileStorage`)를 구현했다. 향후 S3로 전환 시 `S3FileStorage`를 새로 구현하고 `@Profile` 또는 `@ConditionalOnProperty`로 전환하면 된다. 도메인 코드 변경 불필요.

- **MultipartFile -> PhotoFile 변환 위치**: 컨트롤러(boot 레이어)에서 수행한다. 도메인 레이어에 Spring 의존성(`MultipartFile`)이 유입되는 것을 방지한다.

- **1장 제한**: 회원당 사진 1장만 저장한다. 회원가입 시 최초 저장만 수행하며, 교체/삭제는 향후 별도 API로 대응한다.

- **승인 상태 변경 API**: 현재 설계에는 관리자가 승인/거절하는 API는 포함하지 않았다. 별도 관리자 기능으로 분리하여 구현하는 것을 권장한다.

- **Exposed 버전 호환성**: Exposed 0.57.0 기준으로 `insertAndGetId`, `deleteWhere`, `selectAll`, `clientDefault` 모두 지원 확인됨.

- **MemberTable.profileImageUrl 컬럼**: 기존 MEMBERS 테이블에 `PROFILE_IMAGE_URL` 컬럼이 있으나, 이번 설계에서는 별도 `MEMBER_PHOTOS` 테이블을 사용한다. 하위 호환성을 위해 당장 제거하지 않는다.

- **SignUpService 생성자 변경**: `FileStorage`와 `MemberPhotoRepository` 의존성이 추가된다. 기존 `SignUpService`를 사용하는 테스트에서 mock 주입이 필요하다.

- **별도 MemberPhotoService 불필요**: 사진 업로드가 회원가입에 통합되므로 별도 서비스 없이 `SignUpService`에서 직접 처리한다. 향후 사진 교체/삭제 API가 필요하면 그때 `MemberPhotoService`를 분리한다.

---

## 6. 테스트 계획

| # | 테스트 대상 | 테스트 유형 | 주요 검증 내용 |
|---|------------|------------|---------------|
| 1 | `AllowedExtension` | 단위 (KoTest BehaviorSpec) | 허용 확장자 통과, 비허용 확장자 예외, 대소문자 정규화 |
| 2 | `PhotoFile` | 단위 (KoTest BehaviorSpec) | 10MB 초과 시 예외, 빈 파일 예외, 정상 생성 |
| 3 | `SignUpApiTest` | 통합 (Spring REST Docs) | multipart 회원가입 (사진 포함/미포함), 문서 생성 |
| 4 | `LocalFileStorage` | 통합 | 파일 저장/삭제 실제 동작 확인 (향후 구현) |

---

## 7. 삭제되는 항목 (기존 설계 대비)

기존 설계에서 제거된 항목:

| 삭제 항목 | 사유 |
|-----------|------|
| `MemberPhotoApi` (별도 Controller) | 회원가입 API에 통합 |
| `MemberPhotoService` (별도 Service) | SignUpService에 통합 |
| `MemberPhotoResponse`, `MemberPhotoDetailResponse` | 별도 사진 API 제거에 따라 불필요 |
| `DELETE /api/members/photos` | 별도 사진 삭제 API 제거 (향후 필요 시 추가) |
| `GET /api/members/photos` | 별도 사진 조회 API 제거 (향후 필요 시 추가) |
| 대표 사진 기능 | 요구사항에서 명시적으로 제외 |
| `RowEntityMapper` 수정 | Entity의 `companion object`에 `from(row)` 메서드를 두는 방식으로 대체 |
