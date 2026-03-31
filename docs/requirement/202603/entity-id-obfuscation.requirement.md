# Design: Entity ID 난독화 (ID Obfuscation)

> 작성일: 2026-03-31
> 최종 수정일: 2026-03-31
> 상태: Draft (v2 - ConditionalGenericConverter 방식으로 변경)

## 1. 설계 개요

DB는 기존 BIGINT AUTO_INCREMENT PK를 유지하면서, API 요청 시 `@DecryptId` + `ConditionalGenericConverter`로 인코딩된 ID를 자동 디코딩하고, API 응답 시 `@EncryptId` + Jackson Serializer로 Long을 자동 인코딩한다. Hashids 라이브러리를 사용하며, 컨트롤러에서 수동 encode/decode 코드가 불필요하다.

---

## 2. 접근 방식 비교 분석

### 방식 1: DB는 auto increment 유지 + API 레이어에서 인코딩/디코딩

| 항목 | 내용 |
|------|------|
| **원리** | DB PK는 BIGINT AUTO_INCREMENT 그대로. Controller/Response에서 Long -> 인코딩 문자열, Request에서 인코딩 문자열 -> Long 변환 |
| **장점** | DB 스키마 변경 없음, 기존 내부 로직(JOIN, 참조) 전부 유지, 인덱스 성능 최적(BIGINT), 배치 Job 등 내부 시스템은 Long 그대로 사용 |
| **단점** | API 레이어에 인코딩/디코딩 로직 추가 필요, 잘못된 인코딩값 입력 시 예외 처리 필요 |

### 방식 2: DB에 UUID 등 인코딩된 값을 PK로 사용

| 항목 | 내용 |
|------|------|
| **원리** | PK를 VARCHAR(36) UUID 또는 ULID로 변경 |
| **장점** | API 레이어 변환 불필요, 분산 환경에서 ID 충돌 없음 |
| **단점** | **모든 테이블 PK 타입 변경** (BIGINT -> VARCHAR), **Exposed ORM의 BaseTable이 LongIdTable 상속** 중이라 전면 재설계, JOIN/인덱스 성능 저하 (문자열 비교), 기존 Long 기반 도메인 객체 전부 변경, DDL 마이그레이션 필요, 배치 Job의 NoOffset 페이징이 Long 기반이라 전부 변경 |

### 추천: 방식 1

**근거:**

1. **BaseTable이 LongIdTable 상속**: Exposed ORM의 `LongIdTable`을 사용 중이므로 UUID PK 전환은 `BaseTable` 전면 재설계 + 모든 테이블/Entity/DAO 변경을 요구한다. 영향 범위가 너무 크다.
2. **NoOffset 페이징**: `HasCursorId<Long>` 기반 cursor 페이징이 구현되어 있어 Long PK 유지가 필수적이다.
3. **내부 참조 안정성**: `MatchingResult.targetInfoId`, `TargetInfoTable.references()` 등 내부적으로 Long 기반 참조가 전체에 걸쳐 있다.
4. **성능**: BIGINT PK의 인덱스 성능은 VARCHAR UUID 대비 월등하다 (B-Tree 비교 연산, 저장 공간).
5. **변경 최소화**: API 경계에서만 변환하면 되므로 도메인/인프라 계층 변경이 거의 없다.

---

## 3. 기술 선택: Hashids

### 왜 Hashids인가

| 대안 | 평가 |
|------|------|
| Base64 | 디코딩이 쉬워 보안 효과 미미 |
| UUID Random | Long <-> UUID 양방향 매핑 불가 |
| AES 암호화 | 과도한 복잡성, 키 관리 부담 |
| **Hashids** | **Long <-> String 양방향 변환, salt 기반 난독화, URL-safe, 짧은 문자열** |

### Hashids 특징
- 라이브러리: `org.hashids:hashids:1.0.3` (Java 구현체)
- Long 값을 salt 기반으로 인코딩하여 `"kRnB9P3L"` 같은 짧고 URL-safe한 문자열 생성
- 동일 salt면 동일 결과 (deterministic) -> 양방향 변환 가능
- salt가 다르면 전혀 다른 결과 -> salt를 비밀로 유지하면 외부에서 추측 불가

---

## 4. Request/Response 변환 방식 비교

### v1 (기존 계획): PathVariable String 수동 디코딩 + Jackson Deserializer

```kotlin
// Controller에서 수동 decode 필요
@GetMapping("/{memberId}")
fun getMember(@PathVariable("memberId") encodedId: String) {
    val memberId = idObfuscator.decode(encodedId)
}
```

### v2 (변경): @DecryptId + ConditionalGenericConverter 자동 디코딩

```kotlin
// Controller에서 수동 decode 불필요 - 어노테이션만 붙이면 자동 변환
@GetMapping("/{memberId}")
fun getMember(@PathVariable @DecryptId memberId: Long) { ... }
```

### v2 채택 근거

1. **컨트롤러 코드 간결화**: 수동 `idObfuscator.decode()` 호출 제거
2. **일관된 패턴**: 모든 ID 디코딩이 어노테이션 하나로 통일
3. **실수 방지**: 개발자가 decode 호출을 빠뜨리는 실수 원천 차단
4. **ConditionalGenericConverter**: `@DecryptId` 어노테이션이 있는 파라미터만 선택적으로 변환하므로, 기존 String -> Long 변환에 영향 없음

### Request/Response 변환 전략 요약

| 방향 | 방식 | 적용 대상 |
|------|------|-----------|
| **Request 디코딩** | `@DecryptId` + `ConditionalGenericConverter` | `@PathVariable`, `@RequestParam` |
| **Request Body 디코딩** | `@EncryptId` + Jackson Deserializer | `@RequestBody` JSON 필드 |
| **Response 인코딩** | `@EncryptId` + Jackson Serializer | Response DTO의 Long 필드 |

---

## 5. 아키텍처

```
┌───────────────────────────────────────────────────────────────┐
│ boot/ma-boot-web                                              │
│                                                               │
│  [Request 흐름 - PathVariable/RequestParam]                   │
│  @DecryptId + DecryptIdConverter (ConditionalGenericConverter) │
│    "kRnB9P3L" ──(String→Long)──> 42L                         │
│    └── IdObfuscator.decode() 호출                             │
│                                                               │
│  [Request 흐름 - RequestBody JSON]                            │
│  @EncryptId + EncryptIdDeserializer (Jackson)                 │
│    "kRnB9P3L" ──(JSON→Long)──> 42L                           │
│    └── IdObfuscator.decode() 호출                             │
│                                                               │
│  [Response 흐름]                                              │
│  @EncryptId + EncryptIdSerializer (Jackson)                   │
│    42L ──(Long→JSON)──> "kRnB9P3L"                           │
│    └── IdObfuscator.encode() 호출                             │
│                                                               │
│  WebConfig (WebMvcConfigurer)                                 │
│    └── addFormatters()에 DecryptIdConverter 등록               │
│                                                               │
│  ObfuscatedIdJacksonConfig (@Configuration)                   │
│    └── @PostConstruct에서 EncryptIdHolder 초기화              │
└──────────────────────┬────────────────────────────────────────┘
                       │ (port)
┌──────────────────────▼────────────────────────────────────────┐
│ domain/ma-domain-core                                         │
│                                                               │
│  port/IdObfuscator (interface)                                │
│    + encode(id: Long): String                                 │
│    + decode(encoded: String): Long                            │
│                                                               │
│  Domain 객체들 변경 없음 (Long PK 유지)                       │
└──────────────────────┬────────────────────────────────────────┘
                       │ (implements)
┌──────────────────────▼────────────────────────────────────────┐
│ infrastructure/support/ma-id-obfuscator                       │
│                                                               │
│  HashidsIdObfuscator (@Component)                             │
│    └── Hashids(salt, minLength) 사용                          │
└───────────────────────────────────────────────────────────────┘
```

---

## 6. 상세 설계

### 6.1 Infrastructure - Gradle 모듈 생성

**파일**: `infrastructure/support/ma-id-obfuscator/build.gradle.kts`

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.hashids:hashids:1.0.3")

    implementation(project(":domain:ma-domain-core"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
```

- `org.hashids:hashids:1.0.3`: Hashids Java 구현체. Long <-> String 양방향 변환 제공
- `domain:ma-domain-core` 의존: `IdObfuscator` 포트 인터페이스 구현을 위해 필요

**파일**: `settings.gradle.kts` (기존 파일에 include 추가)

```kotlin
// 기존 include 목록에 추가
include("infrastructure:support:ma-id-obfuscator")
```

### 6.2 Domain Port - IdObfuscator 인터페이스

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/common/port/IdObfuscator.kt`

```kotlin
package com.konkuk.ma.domain.common.port

interface IdObfuscator {
    fun encode(id: Long): String
    fun decode(encoded: String): Long
}
```

- `encode(id: Long): String`: DB의 Long PK를 API 노출용 문자열로 변환
- `decode(encoded: String): Long`: API에서 받은 문자열을 DB PK Long으로 변환
- 도메인 포트이므로 Spring 의존성 없음
- 디코딩 실패 시 예외는 구현체에서 던지고, boot 레이어의 `GlobalExceptionHandler`에서 처리

### 6.3 Domain Exception - InvalidObfuscatedIdException

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/common/exception/InvalidObfuscatedIdException.kt`

```kotlin
package com.konkuk.ma.domain.common.exception

import com.konkuk.ma.exception.BusinessException

class InvalidObfuscatedIdException(
    encodedValue: String
) : BusinessException(
    message = "유효하지 않은 ID입니다.",
    dataMessage = "encoded: $encodedValue",
    logLevel = LogLevel.WARN
)
```

- 기존 `BusinessException`을 상속하여 프로젝트의 예외 처리 패턴을 따름
- `logLevel = LogLevel.WARN`: 잘못된 ID 입력은 클라이언트 오류이므로 WARN 레벨
- API 사용자에게는 `message`만 노출, `dataMessage`는 서버 로그에만 기록

### 6.4 Infrastructure - HashidsIdObfuscator 구현체

**파일**: `infrastructure/support/ma-id-obfuscator/src/main/kotlin/com/konkuk/ma/support/id/HashidsIdObfuscator.kt`

```kotlin
package com.konkuk.ma.support.id

import com.konkuk.ma.domain.common.exception.InvalidObfuscatedIdException
import com.konkuk.ma.domain.common.port.IdObfuscator
import org.hashids.Hashids
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class HashidsIdObfuscator(
    @Value("\${id-obfuscator.salt}") salt: String,
    @Value("\${id-obfuscator.min-length:8}") minLength: Int
) : IdObfuscator {

    private val hashids = Hashids(salt, minLength)

    override fun encode(id: Long): String {
        return hashids.encode(id)
    }

    override fun decode(encoded: String): Long {
        val decoded = hashids.decode(encoded)
        if (decoded.isEmpty()) {
            throw InvalidObfuscatedIdException(encoded)
        }
        return decoded[0]
    }
}
```

- `salt`: application.yml에서 주입. 환경별로 다른 salt 사용 가능 (dev/prod 분리)
- `minLength`: 최소 인코딩 문자열 길이. 기본값 8 (예: `"kRnB9P3L"`)
- `Hashids(salt, minLength)`: Hashids 인스턴스. salt와 minLength가 같으면 동일한 인코딩 결과
- `decode()` 결과가 빈 배열이면 잘못된 인코딩값이므로 `InvalidObfuscatedIdException` 발생
- `@Component`로 Spring Bean 등록하여 boot 모듈에서 자동 주입

### 6.5 Boot/Web - @DecryptId 어노테이션 (Request 디코딩용)

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/support/id/DecryptId.kt`

```kotlin
package com.konkuk.ma.support.id

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class DecryptId
```

- `@PathVariable`이나 `@RequestParam` 파라미터에 붙여서 자동 디코딩을 트리거
- `AnnotationTarget.VALUE_PARAMETER`: 메서드 파라미터에만 적용 가능
- Jackson과 무관하게 Spring의 `ConditionalGenericConverter`에서 사용

### 6.6 Boot/Web - DecryptIdConverter (ConditionalGenericConverter)

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/support/id/DecryptIdConverter.kt`

```kotlin
package com.konkuk.ma.support.id

import com.konkuk.ma.domain.common.port.IdObfuscator
import org.springframework.core.convert.TypeDescriptor
import org.springframework.core.convert.converter.ConditionalGenericConverter
import org.springframework.core.convert.converter.GenericConverter.ConvertiblePair
import org.springframework.stereotype.Component

@Component
class DecryptIdConverter(
    private val idObfuscator: IdObfuscator
) : ConditionalGenericConverter {

    override fun matches(sourceType: TypeDescriptor, targetType: TypeDescriptor): Boolean {
        return targetType.hasAnnotation(DecryptId::class.java)
    }

    override fun getConvertibleTypes(): Set<ConvertiblePair> {
        return setOf(ConvertiblePair(String::class.java, Long::class.java))
    }

    override fun convert(source: Any?, sourceType: TypeDescriptor, targetType: TypeDescriptor): Any? {
        if (source == null) return null
        return idObfuscator.decode(source as String)
    }
}
```

- `matches()`: `targetType`에 `@DecryptId` 어노테이션이 있는 경우에만 변환 수행. 이로 인해 기존 String -> Long 변환에는 영향을 주지 않음
- `getConvertibleTypes()`: `String` -> `Long` 변환만 지원. `ConvertiblePair`로 소스/타겟 타입 쌍을 명시
- `convert()`: `idObfuscator.decode()`를 호출하여 인코딩된 문자열을 Long으로 변환. 디코딩 실패 시 `InvalidObfuscatedIdException`이 발생하고 `GlobalExceptionHandler`에서 처리
- `@Component`로 등록: `WebConfig`에서 이 Bean을 주입받아 `FormatterRegistry`에 등록

### 6.7 Boot/Web - WebConfig (WebMvcConfigurer)

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/config/WebConfig.kt`

```kotlin
package com.konkuk.ma.config

import com.konkuk.ma.support.id.DecryptIdConverter
import org.springframework.context.annotation.Configuration
import org.springframework.format.FormatterRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val decryptIdConverter: DecryptIdConverter
) : WebMvcConfigurer {

    override fun addFormatters(registry: FormatterRegistry) {
        registry.addConverter(decryptIdConverter)
    }
}
```

- `WebMvcConfigurer.addFormatters()`: Spring MVC의 타입 변환 시스템에 `DecryptIdConverter`를 등록
- `DecryptIdConverter`를 생성자 주입으로 받음 (DI). `new`로 생성하지 않고 Spring Bean을 주입받아 `IdObfuscator` 의존성 해결
- 이 설정으로 `@PathVariable @DecryptId Long memberId` 또는 `@RequestParam @DecryptId Long id` 사용 시 자동 변환 작동

### 6.8 Boot/Web - @EncryptId 어노테이션 (Response 인코딩 + Request Body 디코딩 겸용)

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/support/id/EncryptId.kt`

```kotlin
package com.konkuk.ma.support.id

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = EncryptIdSerializer::class)
@JsonDeserialize(using = EncryptIdDeserializer::class)
annotation class EncryptId
```

- `@JacksonAnnotationsInside`: 이 어노테이션 하나로 Serializer와 Deserializer를 동시 적용
- Response DTO의 `Long` 필드에 `@EncryptId`를 붙이면 JSON 직렬화 시 자동 인코딩
- Request Body DTO의 `Long` 필드에 `@EncryptId`를 붙이면 JSON 역직렬화 시 자동 디코딩
- `@DecryptId`와 역할 분리: `@DecryptId`는 `@PathVariable`/`@RequestParam`용, `@EncryptId`는 JSON Body용

### 6.9 Boot/Web - EncryptIdSerializer (Response: Long -> String)

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/support/id/EncryptIdSerializer.kt`

```kotlin
package com.konkuk.ma.support.id

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider

class EncryptIdSerializer : JsonSerializer<Long>() {

    override fun serialize(value: Long, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeString(EncryptIdHolder.idObfuscator.encode(value))
    }
}
```

- `JsonSerializer<Long>`: Long 타입 필드를 직렬화할 때 호출
- `EncryptIdHolder.idObfuscator.encode(value)`: Long -> 인코딩된 문자열로 변환하여 JSON에 String으로 출력
- Jackson Serializer는 Spring DI 컨테이너 밖에서 생성되므로 `EncryptIdHolder` (static object)를 통해 `IdObfuscator` 접근

### 6.10 Boot/Web - EncryptIdDeserializer (Request Body: String -> Long)

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/support/id/EncryptIdDeserializer.kt`

```kotlin
package com.konkuk.ma.support.id

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer

class EncryptIdDeserializer : JsonDeserializer<Long>() {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Long {
        return EncryptIdHolder.idObfuscator.decode(p.valueAsString)
    }
}
```

- `JsonDeserializer<Long>`: JSON 문자열을 Long으로 역직렬화
- `EncryptIdHolder.idObfuscator.decode()`: 인코딩된 문자열 -> Long PK. 실패 시 `InvalidObfuscatedIdException`
- `@RequestBody` JSON에서 인코딩된 ID를 받을 때 사용 (예: `{ "targetInfoId": "kRnB9P3L" }`)

### 6.11 Boot/Web - EncryptIdHolder (Jackson Serializer에 IdObfuscator 제공)

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/support/id/EncryptIdHolder.kt`

```kotlin
package com.konkuk.ma.support.id

import com.konkuk.ma.domain.common.port.IdObfuscator

object EncryptIdHolder {
    lateinit var idObfuscator: IdObfuscator
}
```

- Jackson Serializer/Deserializer는 Spring DI 컨테이너 밖에서 인스턴스가 생성됨
- `EncryptIdHolder`는 `object` (Kotlin singleton)로 `IdObfuscator`를 static하게 보관
- `ObfuscatedIdJacksonConfig`의 `@PostConstruct`에서 초기화됨
- **트레이드오프**: 순수 DI 위반이지만, Jackson의 제약 내에서 가장 실용적인 방식

### 6.12 Boot/Web - ObfuscatedIdJacksonConfig (EncryptIdHolder 초기화)

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/support/id/ObfuscatedIdJacksonConfig.kt`

```kotlin
package com.konkuk.ma.support.id

import com.konkuk.ma.domain.common.port.IdObfuscator
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration

@Configuration
class ObfuscatedIdJacksonConfig(
    private val idObfuscator: IdObfuscator
) {
    @PostConstruct
    fun initializeEncryptIdHolder() {
        EncryptIdHolder.idObfuscator = idObfuscator
    }
}
```

- `@PostConstruct`에서 `EncryptIdHolder`에 `IdObfuscator` Bean을 주입
- Spring 컨텍스트 초기화 시 한 번만 실행
- `EncryptIdSerializer`/`EncryptIdDeserializer`가 Jackson에 의해 인스턴스화될 때 `EncryptIdHolder`를 통해 `IdObfuscator`에 접근 가능

### 6.13 Boot/Web - GlobalExceptionHandler에 예외 처리 추가

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/support/error/GlobalExceptionHandler.kt`

```kotlin
// 기존 코드에 import 추가
import com.konkuk.ma.domain.common.exception.InvalidObfuscatedIdException

// 기존 클래스 내부에 메서드 추가
@ExceptionHandler(InvalidObfuscatedIdException::class)
fun handleInvalidObfuscatedId(e: InvalidObfuscatedIdException): ResponseEntity<String> {
    return ResponseEntity.badRequest().body(e.message)
}
```

- `InvalidObfuscatedIdException`은 `BusinessException`을 상속하므로 기존 패턴과 일관
- 기존 `handleBadRequestException`에 추가하는 대신 별도 핸들러로 분리: `InvalidValueException`/`SmsNotVerifiedException`과 성격이 다르므로 독립적으로 관리
- HTTP 400 Bad Request로 응답, 메시지는 "유효하지 않은 ID입니다."

### 6.14 Config - application.yml 설정 추가

인프라 모듈의 `config/` 디렉토리에 설정 파일을 추가한다. `MaEnvironmentPostProcessor`가 `classpath*:config/application-*` 패턴으로 자동 로드하므로, profile별 파일을 만든다.

**파일**: `infrastructure/support/ma-id-obfuscator/src/main/resources/config/application-local.yml`

```yaml
spring:
  config:
    activate:
      on-profile: local

id-obfuscator:
  salt: "meet-again-dev-salt-do-not-use-in-prod"
  min-length: 8
```

**파일**: `infrastructure/support/ma-id-obfuscator/src/main/resources/config/application-test.yml`

```yaml
spring:
  config:
    activate:
      on-profile: test

id-obfuscator:
  salt: "meet-again-test-salt"
  min-length: 8
```

- `salt`: profile별로 다른 값 사용. 운영 환경에서는 환경변수로 주입
- `min-length`: 인코딩 결과의 최소 문자열 길이 (8자)
- 기존 `ma-jwt-core`의 yml 파일 패턴과 동일한 구조

### 6.15 Boot/Web - build.gradle.kts 의존성 추가

**파일**: `boot/ma-boot-web/build.gradle.kts`

```kotlin
// 기존 runtimeOnly 블록에 추가
runtimeOnly(project(":infrastructure:support:ma-id-obfuscator"))
```

### 6.16 Boot/Web - Controller 적용 예시 (PathVariable)

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/member/api/MemberQueryApi.kt`

```kotlin
package com.konkuk.ma.domain.member.api

import com.konkuk.ma.domain.member.api.request.DuplicatedEmailRequest
import com.konkuk.ma.domain.member.api.request.DuplicatedNicknameRequest
import com.konkuk.ma.domain.member.api.response.CheckDuplicatedEmailResponse
import com.konkuk.ma.domain.member.api.response.CheckDuplicatedNicknameResponse
import com.konkuk.ma.domain.member.application.MemberQueryService
import com.konkuk.ma.support.id.DecryptId                        // 추가
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/members")
class MemberQueryApi(
    private val memberQueryService: MemberQueryService
) {
    // 기존 메서드 유지 (checkDuplicatedNickname, checkDuplicatedEmail)

    @GetMapping("/{memberId}")
    fun getMember(
        @PathVariable @DecryptId memberId: Long    // 변경: String -> Long, @DecryptId 추가
    ) {
        // memberId는 이미 Long으로 디코딩된 상태
        // TODO: 회원 조회 로직 구현
    }
}
```

- **변경 전**: `@PathVariable("memberId") encodedId: String` + 수동 `idObfuscator.decode()`
- **변경 후**: `@PathVariable @DecryptId memberId: Long` (자동 디코딩)
- Controller에 `IdObfuscator` 의존성 주입 불필요 (v1 대비 개선)

### 6.17 Boot/Web - Response DTO 적용 예시

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/auth/api/response/SignUpResponse.kt`

```kotlin
package com.konkuk.ma.domain.auth.api.response

import com.konkuk.ma.support.id.EncryptId                        // 추가

class SignUpResponse(
    @EncryptId                                                    // 추가
    val memberId: Long,       // JSON 출력 시 "kRnB9P3L" 같은 문자열로 자동 변환
    val email: String,
    val nickname: String,
    val message: String = "회원가입이 완료되었습니다."
)
```

- 기존 `Long` 타입 유지, `@EncryptId`만 추가
- Service/Domain 레이어 변경 없음

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/matching/api/response/NewTargetInfoResponse.kt`

```kotlin
package com.konkuk.ma.domain.matching.api.response

import com.konkuk.ma.support.id.EncryptId                        // 추가

class NewTargetInfoResponse(
    @EncryptId                                                    // 추가
    val targetInfoId: Long,   // JSON 출력 시 인코딩된 문자열로 자동 변환
    val registerEmail: String
)
```

### 6.18 Test - HashidsIdObfuscator 단위 테스트

**파일**: `infrastructure/support/ma-id-obfuscator/src/test/kotlin/com/konkuk/ma/support/id/HashidsIdObfuscatorTest.kt`

```kotlin
package com.konkuk.ma.support.id

import com.konkuk.ma.domain.common.exception.InvalidObfuscatedIdException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldHaveMinLength

class HashidsIdObfuscatorTest : BehaviorSpec({

    val obfuscator = HashidsIdObfuscator(
        salt = "test-salt",
        minLength = 8
    )

    Given("Long 타입 ID가 주어졌을 때") {
        val id = 42L

        When("인코딩하면") {
            val encoded = obfuscator.encode(id)

            Then("8자 이상의 문자열이 반환된다") {
                encoded shouldHaveMinLength 8
            }

            Then("디코딩하면 원래 값으로 복원된다") {
                obfuscator.decode(encoded) shouldBe id
            }
        }
    }

    Given("서로 다른 ID가 주어졌을 때") {
        val id1 = 1L
        val id2 = 2L

        When("각각 인코딩하면") {
            val encoded1 = obfuscator.encode(id1)
            val encoded2 = obfuscator.encode(id2)

            Then("서로 다른 문자열이 생성된다") {
                encoded1 shouldNotBe encoded2
            }
        }
    }

    Given("잘못된 인코딩 문자열이 주어졌을 때") {
        val invalidEncoded = "!@#invalid"

        When("디코딩하면") {
            Then("InvalidObfuscatedIdException이 발생한다") {
                shouldThrow<InvalidObfuscatedIdException> {
                    obfuscator.decode(invalidEncoded)
                }
            }
        }
    }

    Given("동일한 salt로 생성한 obfuscator는") {
        val anotherObfuscator = HashidsIdObfuscator(
            salt = "test-salt",
            minLength = 8
        )

        When("같은 ID를 인코딩하면") {
            val encoded1 = obfuscator.encode(100L)
            val encoded2 = anotherObfuscator.encode(100L)

            Then("동일한 결과를 반환한다") {
                encoded1 shouldBe encoded2
            }
        }
    }

    Given("다른 salt로 생성한 obfuscator는") {
        val differentSaltObfuscator = HashidsIdObfuscator(
            salt = "different-salt",
            minLength = 8
        )

        When("같은 ID를 인코딩하면") {
            val encoded1 = obfuscator.encode(100L)
            val encoded2 = differentSaltObfuscator.encode(100L)

            Then("다른 결과를 반환한다") {
                encoded1 shouldNotBe encoded2
            }
        }
    }
})
```

### 6.19 Test - DecryptIdConverter 단위 테스트

**파일**: `boot/ma-boot-web/src/test/kotlin/com/konkuk/ma/support/id/DecryptIdConverterTest.kt`

```kotlin
package com.konkuk.ma.support.id

import com.konkuk.ma.domain.common.exception.InvalidObfuscatedIdException
import com.konkuk.ma.domain.common.port.IdObfuscator
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.core.convert.TypeDescriptor

class DecryptIdConverterTest : BehaviorSpec({

    val idObfuscator = mockk<IdObfuscator>()
    val converter = DecryptIdConverter(idObfuscator)

    Given("@DecryptId 어노테이션이 있는 타겟 타입일 때") {
        val sourceType = mockk<TypeDescriptor>()
        val targetType = mockk<TypeDescriptor>()
        every { targetType.hasAnnotation(DecryptId::class.java) } returns true

        When("matches를 호출하면") {
            val result = converter.matches(sourceType, targetType)

            Then("true를 반환한다") {
                result shouldBe true
            }
        }
    }

    Given("@DecryptId 어노테이션이 없는 타겟 타입일 때") {
        val sourceType = mockk<TypeDescriptor>()
        val targetType = mockk<TypeDescriptor>()
        every { targetType.hasAnnotation(DecryptId::class.java) } returns false

        When("matches를 호출하면") {
            val result = converter.matches(sourceType, targetType)

            Then("false를 반환한다") {
                result shouldBe false
            }
        }
    }

    Given("유효한 인코딩된 ID가 주어졌을 때") {
        val encoded = "kRnB9P3L"
        val decoded = 42L
        every { idObfuscator.decode(encoded) } returns decoded

        val sourceType = mockk<TypeDescriptor>()
        val targetType = mockk<TypeDescriptor>()

        When("convert를 호출하면") {
            val result = converter.convert(encoded, sourceType, targetType)

            Then("디코딩된 Long 값을 반환한다") {
                result shouldBe decoded
            }
        }
    }

    Given("null이 주어졌을 때") {
        val sourceType = mockk<TypeDescriptor>()
        val targetType = mockk<TypeDescriptor>()

        When("convert를 호출하면") {
            val result = converter.convert(null, sourceType, targetType)

            Then("null을 반환한다") {
                result shouldBe null
            }
        }
    }

    Given("잘못된 인코딩된 ID가 주어졌을 때") {
        val invalidEncoded = "!@#invalid"
        every { idObfuscator.decode(invalidEncoded) } throws InvalidObfuscatedIdException(invalidEncoded)

        val sourceType = mockk<TypeDescriptor>()
        val targetType = mockk<TypeDescriptor>()

        When("convert를 호출하면") {
            Then("InvalidObfuscatedIdException이 발생한다") {
                shouldThrow<InvalidObfuscatedIdException> {
                    converter.convert(invalidEncoded, sourceType, targetType)
                }
            }
        }
    }
})
```

### 6.20 Test - API 테스트에서의 처리

기존 API 테스트에서 `jsonPath("$.targetInfoId").value(1L)` 같은 검증은 인코딩된 문자열로 변경해야 한다.

테스트에서는 `EncryptIdHolder.idObfuscator`를 테스트용 구현체로 설정한다.

```kotlin
// 테스트 설정 예시 - @BeforeEach 또는 BehaviorSpec의 beforeSpec에서 초기화
EncryptIdHolder.idObfuscator = HashidsIdObfuscator(
    salt = "test-salt",
    minLength = 8
)
```

또는 `BaseApiTest`에 `@Import`로 테스트용 설정을 추가하는 방식을 고려한다:

**파일**: `boot/ma-boot-web/src/test/kotlin/com/konkuk/ma/config/BaseApiTest.kt`

```kotlin
package com.konkuk.ma.config

import com.konkuk.ma.auth.JwtManager
import com.konkuk.ma.support.id.TestIdObfuscatorConfig             // 추가
import com.konkuk.ma.support.security.RoutingAwareEntryPoint
import com.konkuk.ma.support.security.WithAuthMember
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.context.annotation.Import

@Import(
    SecurityConfig::class,
    RoutingAwareEntryPoint::class,
    JwtManager::class,
    TestIdObfuscatorConfig::class                                   // 추가
)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
@Target(AnnotationTarget.CLASS)
@Retention
@WithAuthMember
annotation class BaseApiTest
```

**파일**: `boot/ma-boot-web/src/test/kotlin/com/konkuk/ma/support/id/TestIdObfuscatorConfig.kt`

```kotlin
package com.konkuk.ma.support.id

import com.konkuk.ma.domain.common.port.IdObfuscator
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TestIdObfuscatorConfig {

    @Bean
    fun idObfuscator(): IdObfuscator {
        return HashidsIdObfuscator(
            salt = "test-salt",
            minLength = 8
        )
    }

    @PostConstruct
    fun initializeEncryptIdHolder() {
        EncryptIdHolder.idObfuscator = idObfuscator()
    }
}
```

- 테스트에서 고정된 salt를 사용하여 인코딩 결과를 예측 가능하게 만듦
- `BaseApiTest`에 `@Import`로 추가하여 모든 API 테스트에서 자동 적용
- `HashidsIdObfuscator`가 `infrastructure` 모듈에 있으므로, `boot/ma-boot-web/build.gradle.kts`에 `testImplementation(project(":infrastructure:support:ma-id-obfuscator"))` 추가 필요

### 6.21 Boot/Web - build.gradle.kts 테스트 의존성 추가

**파일**: `boot/ma-boot-web/build.gradle.kts`

```kotlin
// 기존 의존성에 추가
runtimeOnly(project(":infrastructure:support:ma-id-obfuscator"))
testImplementation(project(":infrastructure:support:ma-id-obfuscator"))  // 추가: 테스트에서 HashidsIdObfuscator 사용
```

---

## 7. 구현 순서

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 1 | `settings.gradle.kts` | 수정 | `ma-id-obfuscator` 모듈 include |
| 2 | `infrastructure/support/ma-id-obfuscator/build.gradle.kts` | 신규 | Hashids 의존성, domain-core 의존 |
| 3 | `domain/.../common/port/IdObfuscator.kt` | 신규 | 포트 인터페이스 정의 |
| 4 | `domain/.../common/exception/InvalidObfuscatedIdException.kt` | 신규 | 디코딩 실패 예외 (BusinessException 상속) |
| 5 | `infrastructure/.../id/HashidsIdObfuscator.kt` | 신규 | Hashids 기반 구현체 |
| 6 | `infrastructure/.../resources/config/application-local.yml` | 신규 | local 프로파일 설정 (salt, min-length) |
| 7 | `infrastructure/.../resources/config/application-test.yml` | 신규 | test 프로파일 설정 |
| 8 | `infrastructure/.../id/HashidsIdObfuscatorTest.kt` | 신규 | 단위 테스트 |
| 9 | `boot/ma-boot-web/build.gradle.kts` | 수정 | runtimeOnly + testImplementation 추가 |
| 10 | `boot/.../support/id/DecryptId.kt` | 신규 | Request 디코딩용 어노테이션 |
| 11 | `boot/.../support/id/DecryptIdConverter.kt` | 신규 | ConditionalGenericConverter 구현 |
| 12 | `boot/.../config/WebConfig.kt` | 신규 | WebMvcConfigurer - Converter 등록 |
| 13 | `boot/.../support/id/EncryptId.kt` | 신규 | Response 인코딩 + Request Body 디코딩용 어노테이션 |
| 14 | `boot/.../support/id/EncryptIdHolder.kt` | 신규 | static IdObfuscator 홀더 |
| 15 | `boot/.../support/id/EncryptIdSerializer.kt` | 신규 | Jackson Serializer (Long -> String) |
| 16 | `boot/.../support/id/EncryptIdDeserializer.kt` | 신규 | Jackson Deserializer (String -> Long) |
| 17 | `boot/.../support/id/ObfuscatedIdJacksonConfig.kt` | 신규 | @PostConstruct로 EncryptIdHolder 초기화 |
| 18 | `boot/.../support/error/GlobalExceptionHandler.kt` | 수정 | InvalidObfuscatedIdException 핸들러 추가 |
| 19 | `boot/.../auth/api/response/SignUpResponse.kt` | 수정 | `@EncryptId` 적용 |
| 20 | `boot/.../matching/api/response/NewTargetInfoResponse.kt` | 수정 | `@EncryptId` 적용 |
| 21 | `boot/.../member/api/MemberQueryApi.kt` | 수정 | `@PathVariable @DecryptId Long` 적용 |
| 22 | `boot/ma-boot-web/src/test/.../support/id/DecryptIdConverterTest.kt` | 신규 | Converter 단위 테스트 |
| 23 | `boot/ma-boot-web/src/test/.../support/id/TestIdObfuscatorConfig.kt` | 신규 | 테스트용 IdObfuscator 설정 |
| 24 | `boot/ma-boot-web/src/test/.../config/BaseApiTest.kt` | 수정 | TestIdObfuscatorConfig @Import 추가 |
| 25 | 기존 API 테스트 파일들 | 수정 | 인코딩된 ID 검증으로 변경 |

---

## 8. 고려사항

### 8.1 Hashids 라이브러리 호환성
- `org.hashids:hashids:1.0.3`은 Java 8+, Kotlin 호환. 별도의 native 의존성 없음
- Long 범위(0 ~ Long.MAX_VALUE) 전체 지원. 음수는 미지원이나 auto increment PK는 항상 양수

### 8.2 Salt 보안
- salt가 노출되면 인코딩/디코딩이 가능해지므로, **운영 환경에서는 반드시 환경변수로 주입**
- salt 변경 시 기존 인코딩값이 모두 무효화됨. 프론트엔드에서 캐싱한 ID가 있다면 갱신 필요
- **salt를 코드에 하드코딩하지 않도록 주의** (yml의 기본값은 dev/test 전용)

### 8.3 성능
- Hashids 인코딩/디코딩은 O(1) 연산, 무시할 수 있는 수준의 오버헤드
- 별도의 DB 조회나 네트워크 호출 없음
- `ConditionalGenericConverter.matches()`는 매 요청마다 호출되지만, 어노테이션 체크는 O(1)이므로 성능 영향 없음

### 8.4 ConditionalGenericConverter vs ArgumentResolver

| 항목 | ConditionalGenericConverter | HandlerMethodArgumentResolver |
|------|---------------------------|-------------------------------|
| 적용 범위 | `@PathVariable`, `@RequestParam`, `@ModelAttribute` | 커스텀 파라미터 전체 |
| 기존 어노테이션 유지 | `@PathVariable` 그대로 사용 | `@PathVariable` 제거하고 커스텀 어노테이션으로 대체 |
| Spring 타입 변환 통합 | FormatterRegistry에 자연스럽게 통합 | 별도 resolver 체인에 추가 |
| 선택적 적용 | `matches()`로 `@DecryptId` 있을 때만 | `supportsParameter()`로 판단 |

**ConditionalGenericConverter 채택 근거**: `@PathVariable`을 그대로 유지하면서 `@DecryptId`만 추가하면 되므로, 기존 Spring MVC 패턴과의 호환성이 가장 좋다.

### 8.5 @DecryptId vs @EncryptId 분리 이유

| 어노테이션 | 적용 대상 | 변환 엔진 | 이유 |
|-----------|-----------|-----------|------|
| `@DecryptId` | `@PathVariable`, `@RequestParam` 파라미터 | `ConditionalGenericConverter` (Spring MVC) | Spring의 타입 변환 시스템이 처리 |
| `@EncryptId` | Response DTO 필드, `@RequestBody` JSON 필드 | Jackson Serializer/Deserializer | Jackson의 JSON 직렬화/역직렬화가 처리 |

두 어노테이션을 하나로 합치지 않는 이유: `@PathVariable`의 String -> Long 변환은 Spring MVC의 `ConversionService`가 담당하고, JSON Body의 직렬화/역직렬화는 Jackson이 담당한다. 서로 다른 엔진이므로 어노테이션을 분리하는 것이 역할이 명확하다.

### 8.6 배치 Job 영향 없음
- 배치 Job은 API 레이어를 거치지 않으므로 Long PK를 그대로 사용
- `NoOffsetReader`, `HasCursorId<Long>` 등 기존 배치 인프라 변경 불필요

### 8.7 FK 안전성
- DB 스키마 변경 없음. 기존 참조 관계에 영향 없음

### 8.8 프론트엔드 영향
- API 응답의 id 필드가 `Long` -> `String`으로 타입이 변경됨
- 프론트엔드에서 ID를 숫자로 처리하던 로직이 있다면 수정 필요
- **프론트엔드 팀과 사전 협의 필수**

### 8.9 Jackson Serializer에 Spring Bean 주입 문제
- Jackson의 Serializer/Deserializer는 Spring의 DI 컨테이너 밖에서 생성됨
- `EncryptIdHolder` (static object)를 통해 `IdObfuscator`를 제공하는 것은 순수 DI 위반이지만, Jackson의 제약 내에서 가장 실용적인 방식
- 테스트 시 `EncryptIdHolder.idObfuscator`를 테스트용 구현체로 교체 가능

### 8.10 추후 확장
- 엔티티별로 다른 salt를 사용하고 싶다면, `IdObfuscator`를 엔티티 타입별로 분리하여 `encode(entityType, id)` 시그니처로 확장 가능
- 현 단계에서는 단일 salt로 충분 (테이블 간 ID 충돌 시 잘못된 엔티티를 조회할 수 있지만, API endpoint가 다르므로 실질적 보안 위험은 없음)
