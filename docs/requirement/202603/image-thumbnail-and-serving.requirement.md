# Design: 이미지 썸네일 생성 및 이미지 서빙 API

> 작성일: 2026-03-30
> 상태: Draft

## 1. 설계 개요

회원가입 시 업로드된 프로필 이미지에 대해 가로 400px 비율 유지 썸네일을 자동 생성하여 별도 파일로 저장하고, DB에 원본/썸네일 경로를 모두 관리한다. 또한 경로 기반 이미지 서빙 API(`GET /api/images/**`)를 제공하여 원본/썸네일 이미지를 클라이언트에 제공한다.

## 2. 아키텍처

```
┌──────────────────────────────────────────────────────────────────────┐
│ boot/ma-boot-web                                                     │
│                                                                      │
│  SignUpApi (기존) — 변경 없음                                         │
│    └── SignUpService.signUp(command, photoFile?)                     │
│                                                                      │
│  ImageServingApi (신규) — GET /api/images/**                         │
│    └── FileStorage.load(filePath): Resource                          │
│        └── Content-Type 헤더 설정, 바이너리 응답                      │
│                                                                      │
│  SecurityConfig (수정) — /api/images/** permitAll 추가               │
└──────────────────┬───────────────────────────────────────────────────┘
                   │ (port)
┌──────────────────▼───────────────────────────────────────────────────┐
│ domain/ma-domain-core                                                │
│                                                                      │
│  [port] FileStorage                                                  │
│    + load(filePath: String): Resource        (신규)                  │
│                                                                      │
│  [port] ThumbnailGenerator (신규 포트)                                │
│    + generate(source: ByteArray, width: Int): ByteArray              │
│                                                                      │
│  [domain] MemberPhotoUploader (수정)                                 │
│    └── upload() 내에서 썸네일 생성 + 썸네일 파일 저장 + DB 저장       │
│                                                                      │
│  [domain] NewPhoto (수정)                                            │
│    + thumbnailPath 필드 추가                                         │
│                                                                      │
│  [domain] MemberPhoto (수정)                                         │
│    + thumbnailPath 필드 추가                                         │
│    + originalImageUrl(): String                                      │
│    + thumbnailImageUrl(): String                                     │
│                                                                      │
│  [port] MemberPhotoRepository (수정)                                 │
│    기존 메서드 시그니처 유지 (NewPhoto에 필드가 추가되는 것이므로)      │
└──────────────────┬───────────────────────────────────────────────────┘
                   │ (implements)
┌──────────────────▼───────────────────────────────────────────────────┐
│ infrastructure/support/ma-file-storage                               │
│                                                                      │
│  LocalFileStorage (수정)                                             │
│    + load(filePath): Resource  — FileSystemResource 반환             │
│                                                                      │
│  JavaImageThumbnailGenerator (신규)                                  │
│    + generate(source, width): ByteArray — java.awt.image 활용       │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│ infrastructure/storage/ma-db-core                                    │
│                                                                      │
│  MemberPhotoTable (수정) — THUMBNAIL_PATH 컬럼 추가                  │
│  MemberPhotoEntity (수정) — thumbnailPath 필드 추가                  │
│  MemberPhotoCommandDao (수정) — save()에 thumbnailPath 삽입 추가     │
│                                                                      │
│  DDL — MEMBER_PHOTOS 테이블에 THUMBNAIL_PATH 컬럼 추가              │
└──────────────────────────────────────────────────────────────────────┘
```

## 3. 상세 설계

### 3.1 Domain Port — ThumbnailGenerator (신규)

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/common/domain/file/port/ThumbnailGenerator.kt`

```kotlin
package com.konkuk.ma.domain.common.domain.file.port

interface ThumbnailGenerator {
    fun generate(source: ByteArray, width: Int): ByteArray
}
```

- `source`: 원본 이미지의 바이트 배열. PhotoFile의 `content`를 그대로 전달
- `width`: 썸네일 가로 픽셀 수. 높이는 비율에 따라 자동 계산
- 반환값: 리사이즈된 이미지의 바이트 배열
- 포트 인터페이스이므로 Spring 의존성 없음. 도메인 계층에 위치

### 3.2 Domain Port — FileStorage 확장 (수정)

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/common/domain/file/port/FileStorage.kt`

```kotlin
package com.konkuk.ma.domain.common.domain.file.port

import com.konkuk.ma.domain.common.domain.file.PhotoFile

interface FileStorage {
    fun store(directory: String, photoFile: PhotoFile): String
    fun storeBytes(directory: String, content: ByteArray, fileName: String): String  // 추가
    fun delete(filePath: String)
    fun load(filePath: String): Any  // 추가 — 실제 타입은 Spring Resource이나 도메인에서는 Any로 선언
}
```

- `storeBytes`: 썸네일은 PhotoFile이 아닌 순수 바이트 배열이므로 별도 저장 메서드 필요. fileName에는 UUID 기반 파일명을 전달
- `load`: 파일 경로로부터 파일을 읽어 반환. 도메인 계층에서 Spring의 `Resource` 타입을 직접 사용할 수 없으므로 `Any`로 선언하고, boot 계층에서 캐스팅하여 사용

**설계 대안 검토**:
- `load`의 반환 타입을 `ByteArray`로 할 수도 있으나, 대용량 이미지의 경우 메모리 효율성이 떨어짐
- `Any`로 선언하면 타입 안전성이 낮아지지만, 도메인에 Spring 의존성을 추가하는 것보다 나은 트레이드오프
- 대안: `InputStream`을 반환하여 스트리밍 처리. 하지만 Spring의 `InputStreamResource`로 감싸야 하므로 `Any`와 큰 차이 없음

### 3.3 Domain — StorageUsageType 확장 (수정)

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/common/domain/file/StorageUsageType.kt`

```kotlin
package com.konkuk.ma.domain.common.domain.file

enum class StorageUsageType(val path: String) {
    PROFILE("profile"),
    THUMBNAIL("thumbnail"),  // 추가
}
```

- 썸네일 파일의 저장 디렉토리를 원본과 분리하기 위한 용도
- 저장 경로 예: `member/thumbnail/{email}/{uuid}.jpg`

### 3.4 Domain — NewPhoto 수정

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/member/domain/photo/NewPhoto.kt`

```kotlin
package com.konkuk.ma.domain.member.domain.photo

class NewPhoto(
    val memberEmail: String,
    val filePath: String,
    val originalFileName: String,
    val thumbnailPath: String?  // 추가
) {
    companion object {
        fun create(
            memberEmail: String,
            filePath: String,
            originalFileName: String,
            thumbnailPath: String? = null  // 추가
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

- `thumbnailPath`: 썸네일 파일의 저장 경로. nullable인 이유는 썸네일 생성 실패 시에도 원본 저장은 진행되어야 하기 때문

### 3.5 Domain — MemberPhoto 수정

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/member/domain/photo/MemberPhoto.kt`

```kotlin
package com.konkuk.ma.domain.member.domain.photo

class MemberPhoto(
    val id: Long,
    val memberEmail: String,
    val filePath: String,
    val originalFileName: String,
    val approvalStatus: ApprovalStatus,
    val thumbnailPath: String?  // 추가
) {
    fun belongsTo(email: String): Boolean = memberEmail == email

    fun hasOriginalImage(): Boolean = filePath.isNotBlank()  // 추가

    fun hasThumbnail(): Boolean = thumbnailPath != null  // 추가
}
```

- `hasOriginalImage()`: 원본 이미지 존재 여부를 객체 스스로 판단 (도메인 행위 부여)
- `hasThumbnail()`: 썸네일 존재 여부를 객체 스스로 판단

### 3.6 Domain — MemberPhotoUploader 수정

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/member/domain/photo/MemberPhotoUploader.kt`

```kotlin
package com.konkuk.ma.domain.member.domain.photo

import com.konkuk.ma.domain.common.domain.file.PhotoFile
import com.konkuk.ma.domain.common.domain.file.StorageDomainType
import com.konkuk.ma.domain.common.domain.file.StoragePath
import com.konkuk.ma.domain.common.domain.file.StorageUsageType
import com.konkuk.ma.domain.common.domain.file.port.FileStorage
import com.konkuk.ma.domain.common.domain.file.port.ThumbnailGenerator  // 추가
import com.konkuk.ma.domain.member.domain.port.MemberPhotoRepository
import org.springframework.stereotype.Component

@Component
class MemberPhotoUploader(
    private val fileStorage: FileStorage,
    private val memberPhotoRepository: MemberPhotoRepository,
    private val thumbnailGenerator: ThumbnailGenerator  // 추가
) {
    companion object {
        private const val THUMBNAIL_WIDTH = 400  // 추가
    }

    fun upload(email: String, photoFile: PhotoFile?) {
        photoFile ?: return
        deleteExisting(email)

        val directory = StoragePath.of(StorageDomainType.MEMBER, StorageUsageType.PROFILE, email)
        val filePath = fileStorage.store(directory.value, photoFile)
        val thumbnailPath = generateThumbnail(email, photoFile)  // 추가
        val newPhoto = NewPhoto.create(email, filePath, photoFile.originalFileName, thumbnailPath)  // 수정
        memberPhotoRepository.save(newPhoto)
    }

    private fun generateThumbnail(email: String, photoFile: PhotoFile): String? {  // 추가
        return try {
            val thumbnailBytes = thumbnailGenerator.generate(photoFile.content, THUMBNAIL_WIDTH)
            val thumbnailDir = StoragePath.of(StorageDomainType.MEMBER, StorageUsageType.THUMBNAIL, email)
            val thumbnailFileName = "thumb_${System.currentTimeMillis()}.${photoFile.extension.normalized}"
            fileStorage.storeBytes(thumbnailDir.value, thumbnailBytes, thumbnailFileName)
        } catch (e: Exception) {
            null
        }
    }

    private fun deleteExisting(email: String) {
        val existing = memberPhotoRepository.findByMemberEmail(email) ?: return
        fileStorage.delete(existing.filePath)
        if (existing.hasThumbnail()) {  // 추가
            fileStorage.delete(existing.thumbnailPath!!)
        }
        memberPhotoRepository.deleteByMemberEmail(email)
    }
}
```

- `THUMBNAIL_WIDTH = 400`: 썸네일 가로 픽셀 수를 상수로 선언 (하드코딩 지양 원칙)
- `generateThumbnail`: 썸네일 생성 실패 시 null 반환하여 원본 저장에 영향을 주지 않음
- `deleteExisting`: 기존 사진 삭제 시 썸네일도 함께 삭제. `hasThumbnail()` 메서드로 도메인 객체에 판단 위임
- `thumbnailFileName`: 타임스탬프 기반 파일명으로 캐시 무효화 용이

### 3.7 Infrastructure — JavaImageThumbnailGenerator (신규)

**파일**: `infrastructure/support/ma-file-storage/src/main/kotlin/com/konkuk/ma/storage/JavaImageThumbnailGenerator.kt`

```kotlin
package com.konkuk.ma.storage

import com.konkuk.ma.domain.common.domain.file.port.ThumbnailGenerator
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import org.springframework.stereotype.Component

@Component
class JavaImageThumbnailGenerator : ThumbnailGenerator {

    override fun generate(source: ByteArray, width: Int): ByteArray {
        val originalImage = ImageIO.read(ByteArrayInputStream(source))
            ?: throw IllegalArgumentException("이미지를 읽을 수 없습니다.")

        val scaledImage = scaleToWidth(originalImage, width)
        return toByteArray(scaledImage, determineFormat(originalImage))
    }

    private fun scaleToWidth(image: BufferedImage, targetWidth: Int): BufferedImage {
        if (image.width <= targetWidth) {
            return image
        }

        val ratio = targetWidth.toDouble() / image.width
        val targetHeight = (image.height * ratio).toInt()

        val resized = BufferedImage(targetWidth, targetHeight, image.type.coerceAtLeast(BufferedImage.TYPE_INT_RGB))
        val graphics = resized.createGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        graphics.drawImage(image, 0, 0, targetWidth, targetHeight, null)
        graphics.dispose()

        return resized
    }

    private fun determineFormat(image: BufferedImage): String {
        return if (image.colorModel.hasAlpha()) "png" else "jpg"
    }

    private fun toByteArray(image: BufferedImage, format: String): ByteArray {
        val outputStream = ByteArrayOutputStream()
        ImageIO.write(image, format, outputStream)
        return outputStream.toByteArray()
    }
}
```

- JDK 표준 라이브러리 `java.awt.image`와 `javax.imageio`만 사용하여 외부 라이브러리 의존성 없음
- `scaleToWidth`: 원본이 targetWidth보다 작으면 리사이즈하지 않음 (불필요한 업스케일 방지)
- `RenderingHints.VALUE_INTERPOLATION_BILINEAR`: 속도와 품질의 균형 잡힌 보간법
- `BufferedImage.TYPE_INT_RGB`: 투명도 없는 이미지 타입. `type`이 0(TYPE_CUSTOM)인 경우 대비 `coerceAtLeast` 사용
- `determineFormat`: 투명도가 있는 이미지는 PNG, 없으면 JPG로 출력

### 3.8 Infrastructure — LocalFileStorage 수정

**파일**: `infrastructure/support/ma-file-storage/src/main/kotlin/com/konkuk/ma/storage/LocalFileStorage.kt`

```kotlin
package com.konkuk.ma.storage

import com.konkuk.ma.domain.common.domain.file.PhotoFile
import com.konkuk.ma.domain.common.domain.file.port.FileStorage
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.stereotype.Component

@Component
class LocalFileStorage(
    @Value("\${file.upload.base-path:uploads}")
    private val basePath: String
) : FileStorage {

    override fun store(directory: String, photoFile: PhotoFile): String {
        val dir = Paths.get(basePath, directory)
        Files.createDirectories(dir)

        val storedFileName = "${UUID.randomUUID()}.${photoFile.extension.normalized}"
        val targetPath = dir.resolve(storedFileName)
        Files.write(targetPath, photoFile.content)

        return targetPath.toString()
    }

    override fun storeBytes(directory: String, content: ByteArray, fileName: String): String {  // 추가
        val dir = Paths.get(basePath, directory)
        Files.createDirectories(dir)

        val targetPath = dir.resolve(fileName)
        Files.write(targetPath, content)

        return targetPath.toString()
    }

    override fun delete(filePath: String) {
        val path = Paths.get(filePath)
        if (Files.exists(path)) {
            Files.delete(path)
        }
    }

    override fun load(filePath: String): Any {  // 추가
        val path = Paths.get(filePath)
        require(Files.exists(path)) { "파일을 찾을 수 없습니다: $filePath" }
        return FileSystemResource(path)
    }
}
```

- `storeBytes`: 썸네일 바이트 배열을 디렉토리에 저장. fileName은 호출자가 지정
- `load`: `FileSystemResource`를 반환하여 Spring MVC에서 바로 사용 가능. 파일 미존재 시 `IllegalArgumentException` 발생

### 3.9 Infrastructure DB — MemberPhotoTable 수정

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/member/entity/table/MemberPhotoTable.kt`

```kotlin
package com.konkuk.ma.domain.member.entity.table

import com.konkuk.ma.domain.common.entity.table.BaseTable
import com.konkuk.ma.domain.member.domain.photo.ApprovalStatus

object MemberPhotoTable : BaseTable("MEMBER_PHOTOS", "MEMBER_PHOTO_ID") {
    val memberEmail = varchar("MEMBER_EMAIL", 255)
    val filePath = varchar("FILE_PATH", 512)
    val originalFileName = varchar("ORIGINAL_FILE_NAME", 255)
    val approvalStatus = varchar("APPROVAL_STATUS", 32).clientDefault { ApprovalStatus.PENDING.name }
    val thumbnailPath = varchar("THUMBNAIL_PATH", 512).nullable()  // 추가
}
```

- `thumbnailPath`: nullable로 선언. 기존 데이터와의 호환성 및 썸네일 생성 실패 케이스 대응

### 3.10 Infrastructure DB — MemberPhotoEntity 수정

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
    val approvalStatus: String,
    val thumbnailPath: String?  // 추가
) {
    fun toDomain(): MemberPhoto {
        return MemberPhoto(
            id = id,
            memberEmail = memberEmail,
            filePath = filePath,
            originalFileName = originalFileName,
            approvalStatus = ApprovalStatus.valueOf(approvalStatus),
            thumbnailPath = thumbnailPath  // 추가
        )
    }

    companion object {
        fun from(row: ResultRow): MemberPhotoEntity {
            return MemberPhotoEntity(
                id = row[MemberPhotoTable.id].value,
                memberEmail = row[MemberPhotoTable.memberEmail],
                filePath = row[MemberPhotoTable.filePath],
                originalFileName = row[MemberPhotoTable.originalFileName],
                approvalStatus = row[MemberPhotoTable.approvalStatus],
                thumbnailPath = row[MemberPhotoTable.thumbnailPath]  // 추가
            )
        }
    }
}
```

### 3.11 Infrastructure DB — MemberPhotoCommandDao 수정

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
            it[thumbnailPath] = newPhoto.thumbnailPath  // 추가
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

### 3.12 Boot — ImageServingApi (신규)

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/common/api/ImageServingApi.kt`

```kotlin
package com.konkuk.ma.domain.common.api

import com.konkuk.ma.domain.common.domain.file.port.FileStorage
import jakarta.servlet.http.HttpServletRequest
import java.nio.file.Files
import java.nio.file.Paths
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/images")
class ImageServingApi(
    private val fileStorage: FileStorage
) {
    @GetMapping("/**")
    fun serveImage(request: HttpServletRequest): ResponseEntity<Resource> {
        val filePath = extractFilePath(request)
        val resource = fileStorage.load(filePath) as Resource

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, detectMediaType(filePath))
            .body(resource)
    }

    private fun extractFilePath(request: HttpServletRequest): String {
        val prefix = "/api/images/"
        return request.requestURI.removePrefix(prefix)
    }

    private fun detectMediaType(filePath: String): String {
        val contentType = Files.probeContentType(Paths.get(filePath))
        return contentType ?: MediaType.APPLICATION_OCTET_STREAM_VALUE
    }
}
```

- `GET /api/images/**`: 경로 기반 이미지 서빙. 예: `GET /api/images/member/profile/user@email.com/uuid.jpg`
- `extractFilePath`: 요청 URI에서 `/api/images/` 접두사를 제거하여 실제 파일 경로 추출
- `detectMediaType`: `Files.probeContentType`로 MIME 타입 자동 감지. 실패 시 `application/octet-stream` 기본값
- `fileStorage.load()` 반환값을 `Resource`로 캐스팅하여 Spring MVC의 스트리밍 응답 활용

**보안 고려사항**: `extractFilePath`에서 `..` (path traversal) 공격을 방지해야 함. 아래 3.13에서 별도 처리.

### 3.13 Boot — ImageServingApi 경로 검증 추가

`extractFilePath`에 경로 순회 공격 방지 로직을 추가한다.

```kotlin
private fun extractFilePath(request: HttpServletRequest): String {
    val prefix = "/api/images/"
    val rawPath = request.requestURI.removePrefix(prefix)
    require(!rawPath.contains("..")) { "잘못된 경로입니다." }
    return rawPath
}
```

- `..` 문자열 포함 여부 검증으로 디렉토리 탈출 방지
- `require` 실패 시 `IllegalArgumentException` 발생 → GlobalExceptionHandler의 `handleException`에서 500 응답
- 추가 보안이 필요하면 정규화된 경로가 basePath 하위인지 검증하는 로직을 `LocalFileStorage.load()`에서 수행

### 3.14 Boot — SecurityConfig 수정

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/config/SecurityConfig.kt`

```kotlin
// 기존 코드에 아래 한 줄 추가
.authorizeHttpRequests { authorize ->
    authorize
        .requestMatchers(HttpMethod.POST,"/api/auth/login").permitAll()
        .requestMatchers(HttpMethod.POST,"/api/auth/refresh-token").permitAll()
        .requestMatchers(HttpMethod.POST,"/api/sms/**").permitAll()
        .requestMatchers(HttpMethod.POST, "/api/auth/sign-up").permitAll()
        .requestMatchers(HttpMethod.POST, "/api/members/duplicated-**").permitAll()
        .requestMatchers(HttpMethod.GET, "/api/images/**").permitAll()  // 추가
        .requestMatchers("/h2-console/**").permitAll()
        .requestMatchers("/actuator/**").permitAll()
        .anyRequest().authenticated()
}
```

- 이미지 서빙은 인증 없이 접근 가능해야 함 (프로필 이미지를 외부에서 참조할 수 있도록)
- GET 메서드만 허용하여 POST/PUT/DELETE는 여전히 인증 필요

### 3.15 DDL 수정

**파일**: `infrastructure/storage/ma-db-core/src/main/resources/script/ddl.sql`

기존 `MEMBER_PHOTOS` 테이블에 `THUMBNAIL_PATH` 컬럼 추가:

```sql
-- MEMBER PHOTOS
CREATE TABLE MEMBER_PHOTOS
(
    MEMBER_PHOTO_ID    BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- MemberPhotoTable 특화 컬럼들
    MEMBER_EMAIL       VARCHAR(255) NOT NULL,
    FILE_PATH          VARCHAR(512) NOT NULL,
    ORIGINAL_FILE_NAME VARCHAR(255) NOT NULL,
    APPROVAL_STATUS    VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    THUMBNAIL_PATH     VARCHAR(512) NULL,

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

- `THUMBNAIL_PATH VARCHAR(512) NULL`: 기존 데이터와 호환성을 위해 nullable
- 별도 마이그레이션 스크립트: `ALTER TABLE MEMBER_PHOTOS ADD COLUMN THUMBNAIL_PATH VARCHAR(512) NULL AFTER APPROVAL_STATUS;`

### 3.16 프로필 조회 응답에 이미지 URL 포함

회원 프로필 조회 시 원본/썸네일 URL을 응답에 포함해야 한다. 현재 프로필 조회 API가 구현되어 있지 않으므로, 향후 구현 시 다음 패턴을 적용한다.

이미지 URL 생성 유틸:

```kotlin
// boot 계층 또는 응답 DTO에서 사용
object ImageUrlGenerator {
    private const val IMAGE_API_PREFIX = "/api/images/"

    fun toUrl(filePath: String?): String? {
        return filePath?.let { IMAGE_API_PREFIX + it }
    }
}
```

응답 DTO 예시:

```kotlin
data class MemberProfileResponse(
    val email: String,
    val nickname: String,
    val originalImageUrl: String?,
    val thumbnailImageUrl: String?
)
```

## 4. 구현 순서

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 1 | `domain/.../file/port/ThumbnailGenerator.kt` | 신규 | 썸네일 생성 포트 인터페이스 |
| 2 | `domain/.../file/port/FileStorage.kt` | 수정 | `storeBytes()`, `load()` 메서드 추가 |
| 3 | `domain/.../file/StorageUsageType.kt` | 수정 | `THUMBNAIL` enum 값 추가 |
| 4 | `domain/.../photo/NewPhoto.kt` | 수정 | `thumbnailPath` 필드 추가 |
| 5 | `domain/.../photo/MemberPhoto.kt` | 수정 | `thumbnailPath` 필드 + `hasThumbnail()` 메서드 추가 |
| 6 | `domain/.../photo/MemberPhotoUploader.kt` | 수정 | 썸네일 생성/저장 로직 추가 |
| 7 | `infrastructure/.../storage/JavaImageThumbnailGenerator.kt` | 신규 | `java.awt` 기반 썸네일 생성 구현체 |
| 8 | `infrastructure/.../storage/LocalFileStorage.kt` | 수정 | `storeBytes()`, `load()` 구현 |
| 9 | `infrastructure/.../entity/table/MemberPhotoTable.kt` | 수정 | `thumbnailPath` 컬럼 추가 |
| 10 | `infrastructure/.../entity/MemberPhotoEntity.kt` | 수정 | `thumbnailPath` 필드 + `toDomain()` 수정 |
| 11 | `infrastructure/.../dao/MemberPhotoCommandDao.kt` | 수정 | `save()`에 `thumbnailPath` 삽입 추가 |
| 12 | `infrastructure/.../resources/script/ddl.sql` | 수정 | `THUMBNAIL_PATH` 컬럼 추가 |
| 13 | `boot/.../common/api/ImageServingApi.kt` | 신규 | 이미지 서빙 API (`GET /api/images/**`) |
| 14 | `boot/.../config/SecurityConfig.kt` | 수정 | `/api/images/**` permitAll 추가 |

## 5. 고려사항

### 5.1 외부 라이브러리 의존성

- **썸네일 생성에 JDK 표준 `java.awt` + `javax.imageio`만 사용**: 외부 라이브러리(thumbnailator, imgscalr 등) 없이 구현 가능. 성능이 이슈가 되면 `net.coobird:thumbnailator`로 교체 가능 (포트 패턴이므로 구현체만 변경하면 됨)
- **SVG 주의**: `javax.imageio`는 SVG를 네이티브로 지원하지 않음. `AllowedExtension`에 SVG가 포함되어 있으므로, SVG 파일은 썸네일 생성을 건너뛰고 원본만 저장하는 처리 필요. `generateThumbnail`의 try-catch에서 자연스럽게 null 반환으로 처리됨

### 5.2 FileStorage 포트의 `load` 반환 타입

- `Any`로 선언하여 도메인에 Spring 의존성 유입을 방지했으나, 타입 안전성이 낮음
- 대안 1: `ByteArray` 반환 — 메모리 효율성 문제 (대용량 이미지)
- 대안 2: `InputStream` 반환 — java.io 표준이므로 도메인에서 사용 가능하나, 스트림 관리 부담
- 현재 선택: `Any`로 하되, boot 계층에서 `as Resource` 캐스팅. 향후 S3 전환 시에도 `S3Resource`로 동일 패턴 유지 가능

### 5.3 경로 순회 보안

- `extractFilePath`에서 `..` 포함 여부만 검증하는 것은 기본적인 방어
- 더 강화하려면 `LocalFileStorage.load()`에서 정규화된 절대 경로가 `basePath` 하위인지 검증:
  ```kotlin
  val normalizedPath = path.toRealPath()
  val baseDir = Paths.get(basePath).toRealPath()
  require(normalizedPath.startsWith(baseDir)) { "접근할 수 없는 경로입니다." }
  ```

### 5.4 성능

- 썸네일 생성은 업로드 시점에 동기 처리. 프로필 이미지 업로드 빈도가 높지 않으므로 비동기 처리 불필요
- 이미지 서빙 시 `FileSystemResource`는 Spring이 스트리밍 전송하므로 메모리 효율적
- 향후 트래픽 증가 시 CDN 또는 Nginx 정적 파일 서빙으로 전환 검토

### 5.5 기존 데이터 마이그레이션

- `THUMBNAIL_PATH` 컬럼이 nullable이므로 기존 데이터에 영향 없음
- 기존 업로드된 사진에 대해 썸네일을 생성하려면 별도 배치 Job 필요 (이번 스코프 외)

### 5.6 테스트 계획

- **MemberPhotoUploader 단위 테스트**: Mockk로 FileStorage, ThumbnailGenerator, MemberPhotoRepository를 모킹. 썸네일 생성 성공/실패 케이스 모두 검증
- **JavaImageThumbnailGenerator 단위 테스트**: 실제 테스트 이미지 파일로 리사이즈 결과 검증 (가로 400px, 비율 유지)
- **ImageServingApi 통합 테스트**: MockMvc로 이미지 서빙 API 호출, Content-Type 헤더 검증, 경로 순회 공격 방어 검증
- **LocalFileStorage 단위 테스트**: `storeBytes`, `load` 메서드 추가 테스트
