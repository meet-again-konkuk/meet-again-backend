# Design: Entity ID 난독화 Context 기반 Salt 분리

> 작성일: 2026-03-31
> 상태: Draft

## 1. 설계 개요

동일한 Long ID를 가진 서로 다른 Entity가 같은 인코딩 결과를 생성하는 보안 취약점을 해결하기 위해, `IdObfuscator` 포트에 `context` 파라미터를 추가하고, Jackson Serializer/Deserializer와 Spring Converter에서 필드명을 자동으로 context로 전달하여 Entity별 고유한 인코딩 결과를 생성한다.

---

## 2. 아키텍처

```
┌──────────────────────────────────────────────────────────────────────┐
│ boot/ma-boot-web                                                     │
│                                                                      │
│  Response DTO (@EncryptId val memberId)                              │
│    └── Jackson 직렬화 시 EncryptIdAnnotationIntrospector 호출        │
│         └── EncryptIdSerializer.serialize()                          │
│              └── gen.outputContext.currentName → context="memberId"  │
│                   └── idObfuscator.encode("memberId", 1L)           │
│                                                                      │
│  Request DTO (@EncryptId val memberId)                               │
│    └── Jackson 역직렬화 시 EncryptIdAnnotationIntrospector 호출      │
│         └── EncryptIdDeserializer.deserialize()                      │
│              └── p.currentName → context="memberId"                  │
│                   └── idObfuscator.decode("memberId", "kRnB9P3L")   │
│                                                                      │
│  Controller (@PathVariable @DecryptId memberId: Long)                │
│    └── DecryptIdConverter.convert()                                  │
│         └── @DecryptId("memberId") value → context="memberId"       │
│              └── idObfuscator.decode("memberId", "kRnB9P3L")        │
└──────────────────────┬───────────────────────────────────────────────┘
                       │ (port)
┌──────────────────────▼──────────────────────┐
│ domain/ma-domain-core                       │
│                                             │
│  IdObfuscator (port interface)              │
│    + encode(context: String, id: Long): String   │
│    + decode(context: String, encoded: String): Long │
└──────────────────────┬──────────────────────┘
                       │ (implements)
┌──────────────────────▼──────────────────────┐
│ infrastructure/support/ma-id-obfuscator     │
│                                             │
│  HashidsIdObfuscator                        │
│    - baseSalt: String                       │
│    - minLength: Int                         │
│    - hashidsCache: ConcurrentHashMap        │
│    + encode(context, id) → Hashids(baseSalt + ":" + context).encode(id) │
│    + decode(context, encoded) → Hashids(baseSalt + ":" + context).decode(encoded) │
└─────────────────────────────────────────────┘
```

---

## 3. 상세 설계

### 3.1 Domain Port - IdObfuscator

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/common/port/IdObfuscator.kt`

```kotlin
package com.konkuk.ma.domain.common.port

interface IdObfuscator {
    fun encode(context: String, id: Long): String
    fun decode(context: String, encoded: String): Long
}
```

- `context`: Entity 종류를 구분하는 문자열. 동일한 ID라도 context가 다르면 다른 인코딩 결과 생성
- 기존 `encode(id)`, `decode(encoded)` 시그니처 제거 (하위호환 불필요 -- 내부 인프라 코드이므로 한번에 변경)

---

### 3.2 Infrastructure - HashidsIdObfuscator

**파일**: `infrastructure/support/ma-id-obfuscator/src/main/kotlin/com/konkuk/ma/support/id/HashidsIdObfuscator.kt`

```kotlin
package com.konkuk.ma.support.id

import com.konkuk.ma.domain.common.exception.InvalidObfuscatedIdException
import com.konkuk.ma.domain.common.port.IdObfuscator
import org.hashids.Hashids
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class HashidsIdObfuscator(
    @Value("\${id-obfuscator.salt}") private val baseSalt: String,
    @Value("\${id-obfuscator.min-length:8}") private val minLength: Int
) : IdObfuscator {

    private val hashidsCache = ConcurrentHashMap<String, Hashids>()

    override fun encode(context: String, id: Long): String {
        return resolveHashids(context).encode(id)
    }

    override fun decode(context: String, encoded: String): Long {
        val decoded = resolveHashids(context).decode(encoded)
        if (decoded.isEmpty()) {
            throw InvalidObfuscatedIdException(encoded)
        }
        return decoded[0]
    }

    private fun resolveHashids(context: String): Hashids {
        return hashidsCache.computeIfAbsent(context) { ctx ->
            Hashids("$baseSalt:$ctx", minLength)
        }
    }
}
```

- `baseSalt`: 기존 `salt` 프로퍼티를 그대로 사용. 변수명만 `baseSalt`로 변경하여 의도 명확화
- `hashidsCache`: `ConcurrentHashMap`으로 context별 Hashids 인스턴스 캐싱. `computeIfAbsent`는 thread-safe
- `resolveHashids()`: `baseSalt + ":" + context` 조합으로 Hashids 인스턴스 생성. 구분자 `:`로 salt 충돌 방지
- 기존 `private val hashids` 단일 인스턴스 필드 제거

**기술적 포인트**:
- `ConcurrentHashMap.computeIfAbsent`는 같은 key에 대해 한 번만 factory를 실행하므로 동시성 안전
- context 종류가 필드명 개수로 한정되므로 (수십 개 이하) 메모리 부담 없음

---

### 3.3 Boot - EncryptId 어노테이션

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/support/id/EncryptId.kt`

```kotlin
package com.konkuk.ma.support.id

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class EncryptId(
    val value: String = ""
)
```

- `value`: 명시적 context 지정용. 빈 문자열이면 필드명을 자동으로 사용
- 사용 예: `@EncryptId val memberId: Long` (context = "memberId"), `@EncryptId("customContext") val id: Long` (context = "customContext")

---

### 3.4 Boot - DecryptId 어노테이션

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/support/id/DecryptId.kt`

```kotlin
package com.konkuk.ma.support.id

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class DecryptId(
    val value: String = ""
)
```

- `value`: 명시적 context 지정용. 빈 문자열이면 파라미터명을 자동으로 사용
- PathVariable 사용처에서 `@DecryptId("memberId")` 또는 `@DecryptId` 모두 가능

---

### 3.5 Boot - EncryptIdSerializer

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/support/id/EncryptIdSerializer.kt`

```kotlin
package com.konkuk.ma.support.id

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.konkuk.ma.domain.common.port.IdObfuscator

class EncryptIdSerializer(
    private val idObfuscator: IdObfuscator,
    private val explicitContext: String
) : JsonSerializer<Long>() {

    override fun serialize(value: Long, gen: JsonGenerator, serializers: SerializerProvider) {
        val context = resolveContext(gen)
        gen.writeString(idObfuscator.encode(context, value))
    }

    private fun resolveContext(gen: JsonGenerator): String {
        if (explicitContext.isNotEmpty()) {
            return explicitContext
        }
        return gen.outputContext.currentName
            ?: throw IllegalStateException("필드명을 추출할 수 없습니다. @EncryptId에 context를 명시하세요.")
    }
}
```

- `explicitContext`: `@EncryptId("memberId")`처럼 명시된 경우 해당 값 사용
- `gen.outputContext.currentName`: Jackson이 현재 직렬화 중인 JSON 필드명을 반환. `@EncryptId val memberId` 일 때 `"memberId"` 반환
- fallback으로 `IllegalStateException` -- 필드명을 추출할 수 없는 비정상 상황 대응

**기술적 포인트**:
- `JsonGenerator.outputContext`는 `JsonStreamContext` 타입으로, 현재 출력 위치의 컨텍스트 정보 보유
- `currentName`은 현재 필드를 write하기 직전에 세팅되므로, `serialize()` 호출 시점에 항상 유효

---

### 3.6 Boot - EncryptIdDeserializer

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/support/id/EncryptIdDeserializer.kt`

```kotlin
package com.konkuk.ma.support.id

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.konkuk.ma.domain.common.port.IdObfuscator

class EncryptIdDeserializer(
    private val idObfuscator: IdObfuscator,
    private val explicitContext: String
) : JsonDeserializer<Long>() {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Long {
        val context = resolveContext(p)
        return idObfuscator.decode(context, p.valueAsString)
    }

    private fun resolveContext(p: JsonParser): String {
        if (explicitContext.isNotEmpty()) {
            return explicitContext
        }
        return p.currentName
            ?: throw IllegalStateException("필드명을 추출할 수 없습니다. @EncryptId에 context를 명시하세요.")
    }
}
```

- `p.currentName`: Jackson `JsonParser`가 현재 파싱 중인 JSON 필드명 반환
- 나머지 구조는 Serializer와 대칭

---

### 3.7 Boot - EncryptIdAnnotationIntrospector

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/support/id/EncryptIdAnnotationIntrospector.kt`

```kotlin
package com.konkuk.ma.support.id

import com.fasterxml.jackson.databind.introspect.Annotated
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector
import com.konkuk.ma.domain.common.port.IdObfuscator

class EncryptIdAnnotationIntrospector(
    private val idObfuscator: IdObfuscator
) : NopAnnotationIntrospector() {

    override fun findSerializer(a: Annotated): Any? {
        val annotation = a.getAnnotation(EncryptId::class.java) ?: return null
        return EncryptIdSerializer(idObfuscator, annotation.value)
    }

    override fun findDeserializer(a: Annotated): Any? {
        val annotation = a.getAnnotation(EncryptId::class.java) ?: return null
        return EncryptIdDeserializer(idObfuscator, annotation.value)
    }
}
```

- 기존 `hasAnnotation` 체크 대신 `getAnnotation`으로 변경하여 어노테이션의 `value` 속성 추출
- `annotation.value`를 Serializer/Deserializer에 전달 (빈 문자열이면 필드명 자동 사용)

---

### 3.8 Boot - DecryptIdConverter

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/support/id/DecryptIdConverter.kt`

```kotlin
package com.konkuk.ma.support.id

import com.konkuk.ma.domain.common.port.IdObfuscator
import org.springframework.core.convert.TypeDescriptor
import org.springframework.core.convert.converter.ConditionalGenericConverter
import org.springframework.core.convert.converter.GenericConverter.ConvertiblePair

class DecryptIdConverter(
    private val idObfuscator: IdObfuscator
) : ConditionalGenericConverter {

    override fun matches(sourceType: TypeDescriptor, targetType: TypeDescriptor): Boolean {
        return targetType.hasAnnotation(DecryptId::class.java)
    }

    override fun getConvertibleTypes(): Set<ConvertiblePair> {
        return setOf(
            ConvertiblePair(String::class.java, Long::class.java),
            ConvertiblePair(String::class.java, Long::class.javaObjectType)
        )
    }

    override fun convert(source: Any?, sourceType: TypeDescriptor, targetType: TypeDescriptor): Any? {
        if (source == null) return null
        val context = resolveContext(targetType)
        return idObfuscator.decode(context, source as String)
    }

    private fun resolveContext(targetType: TypeDescriptor): String {
        val annotation = targetType.getAnnotation(DecryptId::class.java)
            ?: throw IllegalStateException("@DecryptId 어노테이션을 찾을 수 없습니다.")
        if (annotation.value.isNotEmpty()) {
            return annotation.value
        }
        return targetType.resolvableType.toString().substringAfterLast(".")
            .replaceFirstChar { it.lowercase() }
            .ifEmpty {
                throw IllegalStateException("파라미터명을 추출할 수 없습니다. @DecryptId에 context를 명시하세요.")
            }
    }
}
```

**주의 -- PathVariable에서 파라미터명 추출의 한계**:

`TypeDescriptor`에서는 Kotlin 파라미터명을 직접 추출하기 어렵다. `TypeDescriptor`는 타입 정보를 가지고 있지만 파라미터 이름은 포함하지 않는다.

**권장 대안**: PathVariable 사용처에서는 `@DecryptId`에 context를 명시적으로 지정한다.

```kotlin
// 변경 전
@GetMapping("/{memberId}")
fun getMember(@PathVariable @DecryptId memberId: Long)

// 변경 후 (context 명시)
@GetMapping("/{memberId}")
fun getMember(@PathVariable @DecryptId("memberId") memberId: Long)
```

따라서 `DecryptIdConverter`는 아래와 같이 단순화하는 것이 더 안전하다:

```kotlin
package com.konkuk.ma.support.id

import com.konkuk.ma.domain.common.port.IdObfuscator
import org.springframework.core.convert.TypeDescriptor
import org.springframework.core.convert.converter.ConditionalGenericConverter
import org.springframework.core.convert.converter.GenericConverter.ConvertiblePair

class DecryptIdConverter(
    private val idObfuscator: IdObfuscator
) : ConditionalGenericConverter {

    override fun matches(sourceType: TypeDescriptor, targetType: TypeDescriptor): Boolean {
        return targetType.hasAnnotation(DecryptId::class.java)
    }

    override fun getConvertibleTypes(): Set<ConvertiblePair> {
        return setOf(
            ConvertiblePair(String::class.java, Long::class.java),
            ConvertiblePair(String::class.java, Long::class.javaObjectType)
        )
    }

    override fun convert(source: Any?, sourceType: TypeDescriptor, targetType: TypeDescriptor): Any? {
        if (source == null) return null
        val context = resolveContext(targetType)
        return idObfuscator.decode(context, source as String)
    }

    private fun resolveContext(targetType: TypeDescriptor): String {
        val annotation = targetType.getAnnotation(DecryptId::class.java)
            ?: throw IllegalStateException("@DecryptId 어노테이션을 찾을 수 없습니다.")
        require(annotation.value.isNotEmpty()) {
            "@DecryptId의 value를 명시해야 합니다. 예: @DecryptId(\"memberId\")"
        }
        return annotation.value
    }
}
```

- `@DecryptId`는 PathVariable에서만 사용되므로, context 명시를 필수로 강제하는 것이 안전
- `TypeDescriptor`에서 파라미터명을 가져오는 방법이 Spring 버전에 따라 불안정하므로 명시적 지정이 더 견고함

---

### 3.9 Boot - ObfuscatedIdJacksonConfig (변경 없음)

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/support/id/ObfuscatedIdJacksonConfig.kt`

기존 코드 변경 없음. `EncryptIdAnnotationIntrospector` 내부 변경만으로 충분.

---

### 3.10 Boot - WebConfig (변경 없음)

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/config/WebConfig.kt`

기존 코드 변경 없음. `DecryptIdConverter` 내부 변경만으로 충분.

---

### 3.11 사용처 변경 - MemberQueryApi

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/member/api/MemberQueryApi.kt`

```kotlin
// 변경 전
@GetMapping("/{memberId}")
fun getMember(@PathVariable @DecryptId memberId: Long) {

// 변경 후
@GetMapping("/{memberId}")
fun getMember(@PathVariable @DecryptId("memberId") memberId: Long) {
```

- `@DecryptId`에 `"memberId"` context 명시 추가

---

### 3.12 사용처 변경 - Response DTO (변경 없음)

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/auth/api/response/SignUpResponse.kt`
**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/matching/api/response/NewTargetInfoResponse.kt`

변경 불필요. `@EncryptId val memberId`, `@EncryptId val targetInfoId` -- 필드명이 자동으로 context로 사용됨.
- `SignUpResponse.memberId` -> context = "memberId"
- `NewTargetInfoResponse.targetInfoId` -> context = "targetInfoId"

이로써 `Member(1L)`과 `TargetInfo(1L)`이 서로 다른 인코딩 결과를 생성한다.

---

### 3.13 테스트 - HashidsIdObfuscatorTest

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
        baseSalt = "test-salt",
        minLength = 8
    )

    Given("Long 타입 ID가 주어졌을 때") {
        val id = 42L
        val context = "memberId"

        When("인코딩하면") {
            val encoded = obfuscator.encode(context, id)

            Then("8자 이상의 문자열이 반환된다") {
                encoded shouldHaveMinLength 8
            }

            Then("같은 context로 디코딩하면 원래 값으로 복원된다") {
                obfuscator.decode(context, encoded) shouldBe id
            }
        }
    }

    Given("같은 ID를 다른 context로 인코딩할 때") {
        val id = 1L
        val context1 = "memberId"
        val context2 = "targetInfoId"

        When("각각 인코딩하면") {
            val encoded1 = obfuscator.encode(context1, id)
            val encoded2 = obfuscator.encode(context2, id)

            Then("서로 다른 문자열이 생성된다") {
                encoded1 shouldNotBe encoded2
            }
        }
    }

    Given("다른 context로 디코딩을 시도할 때") {
        val id = 42L
        val context = "memberId"
        val wrongContext = "targetInfoId"

        When("memberId context로 인코딩한 값을 targetInfoId context로 디코딩하면") {
            val encoded = obfuscator.encode(context, id)

            Then("InvalidObfuscatedIdException이 발생한다") {
                shouldThrow<InvalidObfuscatedIdException> {
                    obfuscator.decode(wrongContext, encoded)
                }
            }
        }
    }

    Given("서로 다른 ID가 같은 context로 주어졌을 때") {
        val id1 = 1L
        val id2 = 2L
        val context = "memberId"

        When("각각 인코딩하면") {
            val encoded1 = obfuscator.encode(context, id1)
            val encoded2 = obfuscator.encode(context, id2)

            Then("서로 다른 문자열이 생성된다") {
                encoded1 shouldNotBe encoded2
            }
        }
    }

    Given("잘못된 인코딩 문자열이 주어졌을 때") {
        val invalidEncoded = "!@#invalid"
        val context = "memberId"

        When("디코딩하면") {
            Then("InvalidObfuscatedIdException이 발생한다") {
                shouldThrow<InvalidObfuscatedIdException> {
                    obfuscator.decode(context, invalidEncoded)
                }
            }
        }
    }

    Given("동일한 salt와 context로 생성한 obfuscator는") {
        val anotherObfuscator = HashidsIdObfuscator(
            baseSalt = "test-salt",
            minLength = 8
        )
        val context = "memberId"

        When("같은 ID를 인코딩하면") {
            val encoded1 = obfuscator.encode(context, 100L)
            val encoded2 = anotherObfuscator.encode(context, 100L)

            Then("동일한 결과를 반환한다") {
                encoded1 shouldBe encoded2
            }
        }
    }

    Given("다른 salt로 생성한 obfuscator는") {
        val differentSaltObfuscator = HashidsIdObfuscator(
            baseSalt = "different-salt",
            minLength = 8
        )
        val context = "memberId"

        When("같은 ID를 인코딩하면") {
            val encoded1 = obfuscator.encode(context, 100L)
            val encoded2 = differentSaltObfuscator.encode(context, 100L)

            Then("다른 결과를 반환한다") {
                encoded1 shouldNotBe encoded2
            }
        }
    }
})
```

- 생성자 파라미터명 `salt` -> `baseSalt` 변경 반영
- 모든 `encode`/`decode` 호출에 `context` 파라미터 추가
- **신규 테스트 케이스**: 같은 ID를 다른 context로 인코딩 시 다른 결과 (핵심 보안 요구사항 검증)
- **신규 테스트 케이스**: 다른 context로 디코딩 시도 시 실패 (cross-entity 공격 시나리오 검증)

---

### 3.14 테스트 - EncryptIdSerializerTest

**파일**: `boot/ma-boot-web/src/test/kotlin/com/konkuk/ma/support/id/EncryptIdSerializerTest.kt`

```kotlin
package com.konkuk.ma.support.id

import com.konkuk.ma.domain.common.port.IdObfuscator
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonStreamContext

class EncryptIdSerializerTest : FunSpec({

    val idObfuscator = mockk<IdObfuscator>()

    context("serialize - 필드명을 context로 자동 사용") {

        test("Long 값을 필드명 기반 context로 인코딩된 문자열로 직렬화한다") {
            val serializer = EncryptIdSerializer(idObfuscator, explicitContext = "")
            val id = 42L
            val fieldName = "memberId"
            val encoded = "kRnB9P3L"
            every { idObfuscator.encode(fieldName, id) } returns encoded

            val outputContext = mockk<JsonStreamContext>()
            every { outputContext.currentName } returns fieldName

            val gen = mockk<JsonGenerator>()
            every { gen.outputContext } returns outputContext
            every { gen.writeString(any<String>()) } returns Unit

            serializer.serialize(id, gen, mockk())

            verify { gen.writeString(encoded) }
            verify { idObfuscator.encode(fieldName, id) }
        }
    }

    context("serialize - 명시적 context 사용") {

        test("@EncryptId에 지정된 context를 사용하여 인코딩한다") {
            val explicitContext = "customContext"
            val serializer = EncryptIdSerializer(idObfuscator, explicitContext = explicitContext)
            val id = 42L
            val encoded = "xYz12345"
            every { idObfuscator.encode(explicitContext, id) } returns encoded

            val gen = mockk<JsonGenerator>()
            every { gen.writeString(any<String>()) } returns Unit

            serializer.serialize(id, gen, mockk())

            verify { gen.writeString(encoded) }
            verify { idObfuscator.encode(explicitContext, id) }
        }
    }
})
```

- `EncryptIdSerializer` 생성자에 `explicitContext` 파라미터 추가 반영
- `gen.outputContext.currentName` 모킹으로 필드명 자동 추출 검증
- 명시적 context 사용 케이스 추가

---

### 3.15 테스트 - EncryptIdDeserializerTest

**파일**: `boot/ma-boot-web/src/test/kotlin/com/konkuk/ma/support/id/EncryptIdDeserializerTest.kt`

```kotlin
package com.konkuk.ma.support.id

import com.konkuk.ma.domain.common.exception.InvalidObfuscatedIdException
import com.konkuk.ma.domain.common.port.IdObfuscator
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import com.fasterxml.jackson.core.JsonParser

class EncryptIdDeserializerTest : FunSpec({

    val idObfuscator = mockk<IdObfuscator>()

    context("deserialize - 필드명을 context로 자동 사용") {

        test("인코딩된 문자열을 필드명 기반 context로 Long 값으로 역직렬화한다") {
            val deserializer = EncryptIdDeserializer(idObfuscator, explicitContext = "")
            val encoded = "kRnB9P3L"
            val decoded = 42L
            val fieldName = "memberId"
            every { idObfuscator.decode(fieldName, encoded) } returns decoded

            val parser = mockk<JsonParser>()
            every { parser.valueAsString } returns encoded
            every { parser.currentName } returns fieldName

            val result = deserializer.deserialize(parser, mockk())

            result shouldBe decoded
        }

        test("잘못된 인코딩 문자열이면 InvalidObfuscatedIdException이 발생한다") {
            val deserializer = EncryptIdDeserializer(idObfuscator, explicitContext = "")
            val invalidEncoded = "!@#invalid"
            val fieldName = "memberId"
            every { idObfuscator.decode(fieldName, invalidEncoded) } throws InvalidObfuscatedIdException(invalidEncoded)

            val parser = mockk<JsonParser>()
            every { parser.valueAsString } returns invalidEncoded
            every { parser.currentName } returns fieldName

            shouldThrow<InvalidObfuscatedIdException> {
                deserializer.deserialize(parser, mockk())
            }
        }
    }

    context("deserialize - 명시적 context 사용") {

        test("@EncryptId에 지정된 context를 사용하여 디코딩한다") {
            val explicitContext = "customContext"
            val deserializer = EncryptIdDeserializer(idObfuscator, explicitContext = explicitContext)
            val encoded = "xYz12345"
            val decoded = 42L
            every { idObfuscator.decode(explicitContext, encoded) } returns decoded

            val parser = mockk<JsonParser>()
            every { parser.valueAsString } returns encoded

            val result = deserializer.deserialize(parser, mockk())

            result shouldBe decoded
        }
    }
})
```

---

### 3.16 테스트 - DecryptIdConverterTest

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

    Given("유효한 인코딩된 ID와 context가 지정된 @DecryptId가 주어졌을 때") {
        val encoded = "kRnB9P3L"
        val decoded = 42L
        val context = "memberId"
        every { idObfuscator.decode(context, encoded) } returns decoded

        val sourceType = mockk<TypeDescriptor>()
        val targetType = mockk<TypeDescriptor>()
        val annotation = DecryptId("memberId")
        every { targetType.getAnnotation(DecryptId::class.java) } returns annotation

        When("convert를 호출하면") {
            val result = converter.convert(encoded, sourceType, targetType)

            Then("지정된 context로 디코딩된 Long 값을 반환한다") {
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
        val context = "memberId"
        every { idObfuscator.decode(context, invalidEncoded) } throws InvalidObfuscatedIdException(invalidEncoded)

        val sourceType = mockk<TypeDescriptor>()
        val targetType = mockk<TypeDescriptor>()
        val annotation = DecryptId("memberId")
        every { targetType.getAnnotation(DecryptId::class.java) } returns annotation

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

- 모든 `decode` 호출에 `context` 파라미터 추가
- `@DecryptId("memberId")` 어노테이션 인스턴스를 mockk 대신 직접 생성하여 `value` 검증

**주의**: `DecryptId("memberId")`로 어노테이션 인스턴스 직접 생성이 Kotlin에서 가능한지 확인 필요. 불가능하면 mockk으로 대체:

```kotlin
val annotation = mockk<DecryptId>()
every { annotation.value } returns "memberId"
```

---

### 3.17 테스트 - TestIdObfuscatorConfig (변경 없음)

**파일**: `boot/ma-boot-web/src/test/kotlin/com/konkuk/ma/support/id/TestIdObfuscatorConfig.kt`

```kotlin
// 변경 없음 -- HashidsIdObfuscator 내부에서 context별 캐싱이 자동 처리되므로
// TestIdObfuscatorConfig는 그대로 유지
```

단, `HashidsIdObfuscator`의 생성자 파라미터명이 `salt` -> `baseSalt`로 변경되므로 named argument 사용 시 수정 필요:

```kotlin
@Bean
@Primary
fun testIdObfuscator(): IdObfuscator {
    return HashidsIdObfuscator(
        baseSalt = "test-salt",
        minLength = 8
    )
}
```

---

### 3.18 테스트 - EncryptIdRequestBodyIntegrationTest

**파일**: `boot/ma-boot-web/src/test/kotlin/com/konkuk/ma/support/id/EncryptIdRequestBodyIntegrationTest.kt`

```kotlin
package com.konkuk.ma.support.id

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.common.port.IdObfuscator
import com.konkuk.ma.extension.postJson
import io.kotest.core.spec.style.FunSpec
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.ResponseEntity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import com.konkuk.ma.support.error.GlobalExceptionHandler

data class EncryptIdTestRequest(
    @field:EncryptId
    val id: Long,
    val name: String
)

data class EncryptIdTestResponse(
    val decodedId: Long,
    val name: String
)

@RestController
class EncryptIdTestController {

    @PostMapping("/test/encrypt-id-request-body")
    fun testEndpoint(@RequestBody request: EncryptIdTestRequest): ResponseEntity<EncryptIdTestResponse> {
        return ResponseEntity.ok(
            EncryptIdTestResponse(
                decodedId = request.id,
                name = request.name
            )
        )
    }
}

@WebMvcTest(EncryptIdTestController::class)
@BaseApiTest
@Import(GlobalExceptionHandler::class)
class EncryptIdRequestBodyIntegrationTest(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper,
    private val idObfuscator: IdObfuscator
) : FunSpec({

    context("@EncryptId @RequestBody 역직렬화") {

        test("인코딩된 ID가 포함된 JSON이 Long으로 역직렬화된다") {
            // Given
            val originalId = 42L
            val encodedId = idObfuscator.encode("id", originalId)  // context = 필드명 "id"
            val requestBody = mapper.writeValueAsString(
                mapOf(
                    "id" to encodedId,
                    "name" to "테스트"
                )
            )

            // When & Then
            mockMvc.postJson("/test/encrypt-id-request-body") {
                content = requestBody
            }
                .andExpect {
                    status { isOk() }
                    jsonPath("$.decodedId").value(originalId)
                    jsonPath("$.name").value("테스트")
                }
        }

        test("잘못된 인코딩 ID가 포함된 JSON이면 에러 응답이 반환된다") {
            // Given
            val requestBody = mapper.writeValueAsString(
                mapOf(
                    "id" to "!@#invalid",
                    "name" to "테스트"
                )
            )

            // When & Then
            mockMvc.postJson("/test/encrypt-id-request-body") {
                content = requestBody
            }
                .andExpect {
                    status { is5xxServerError() }
                }
        }
    }
})
```

- `idObfuscator.encode("id", originalId)`: 테스트 DTO의 필드명이 `id`이므로 context = `"id"`
- 나머지 로직 동일

---

## 4. 구현 순서

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 1 | `domain/ma-domain-core/.../port/IdObfuscator.kt` | 수정 | `encode(context, id)`, `decode(context, encoded)` 시그니처 변경 |
| 2 | `infrastructure/support/ma-id-obfuscator/.../HashidsIdObfuscator.kt` | 수정 | `baseSalt + context` 조합, `ConcurrentHashMap` 캐싱 |
| 3 | `boot/ma-boot-web/.../support/id/EncryptId.kt` | 수정 | `value: String = ""` 속성 추가 |
| 4 | `boot/ma-boot-web/.../support/id/DecryptId.kt` | 수정 | `value: String = ""` 속성 추가 |
| 5 | `boot/ma-boot-web/.../support/id/EncryptIdSerializer.kt` | 수정 | `explicitContext` 파라미터 추가, `gen.outputContext.currentName` 활용 |
| 6 | `boot/ma-boot-web/.../support/id/EncryptIdDeserializer.kt` | 수정 | `explicitContext` 파라미터 추가, `p.currentName` 활용 |
| 7 | `boot/ma-boot-web/.../support/id/EncryptIdAnnotationIntrospector.kt` | 수정 | `getAnnotation`으로 변경, `annotation.value` 전달 |
| 8 | `boot/ma-boot-web/.../support/id/DecryptIdConverter.kt` | 수정 | `annotation.value`에서 context 추출 |
| 9 | `boot/ma-boot-web/.../domain/member/api/MemberQueryApi.kt` | 수정 | `@DecryptId("memberId")` context 명시 |
| 10 | `infrastructure/.../HashidsIdObfuscatorTest.kt` | 수정 | context 파라미터 추가, cross-context 테스트 추가 |
| 11 | `boot/.../EncryptIdSerializerTest.kt` | 수정 | context 관련 모킹 추가 |
| 12 | `boot/.../EncryptIdDeserializerTest.kt` | 수정 | context 관련 모킹 추가 |
| 13 | `boot/.../DecryptIdConverterTest.kt` | 수정 | context 관련 모킹 추가 |
| 14 | `boot/.../TestIdObfuscatorConfig.kt` | 수정 | 생성자 파라미터명 `baseSalt` 반영 |
| 15 | `boot/.../EncryptIdRequestBodyIntegrationTest.kt` | 수정 | `encode("id", originalId)` context 추가 |

---

## 5. 고려사항

### 5.1 Jackson 필드명 추출 안정성

- **Serializer**: `gen.outputContext.currentName`은 Jackson이 `writeFieldName()` 호출 후 값을 쓰기 직전에 세팅된다. `@JsonProperty`로 필드명을 변경한 경우, JSON 출력 필드명이 context로 사용된다. 이는 의도된 동작 -- JSON 필드명이 곧 API 계약이므로 이를 기준으로 삼는 것이 자연스럽다.
- **Deserializer**: `p.currentName`은 현재 파싱 중인 JSON 필드명을 반환한다. JSON 요청 본문의 필드명이 context가 된다.
- **주의**: 배열 내부 등 특수한 JSON 구조에서는 `currentName`이 null일 수 있다. 현재 사용처에서는 모두 객체의 최상위 필드에 `@EncryptId`를 적용하므로 문제 없음.

### 5.2 DecryptIdConverter에서 파라미터명 자동 추출 불가

- Spring의 `TypeDescriptor`는 타입 메타데이터를 제공하지만, 메서드 파라미터의 이름은 포함하지 않는다.
- `HandlerMethodArgumentResolver`를 커스텀 구현하면 파라미터명 접근이 가능하지만, 기존 `ConditionalGenericConverter` 기반 구조를 크게 변경해야 한다.
- **결정**: `@DecryptId`는 PathVariable에서만 사용되며 사용처가 적으므로, `value` 명시를 필수로 강제하는 것이 구조 변경 대비 가성비가 높다.

### 5.3 Hashids 인스턴스 캐싱 메모리

- context 종류 = Entity 필드명 종류 (memberId, targetInfoId 등) = 수십 개 이하
- Hashids 인스턴스 하나당 수 KB 이하의 메모리 사용
- 전체 캐시 크기는 무시 가능한 수준

### 5.4 하위호환성

- `IdObfuscator` 포트 인터페이스의 시그니처가 변경되므로, 이를 사용하는 모든 곳을 동시에 수정해야 한다.
- 현재 `IdObfuscator`를 직접 호출하는 곳은 테스트 코드와 Serializer/Deserializer/Converter뿐이므로 영향 범위가 제한적이다.
- **기존 인코딩된 ID와의 호환성**: context 파라미터 추가로 인코딩 결과가 달라지므로, 기존에 클라이언트가 캐싱한 인코딩 ID는 무효화된다. 이는 보안 패치의 의도된 효과이다.

### 5.5 어노테이션 인스턴스 생성 (테스트)

- Kotlin에서 어노테이션은 인터페이스이므로 직접 인스턴스화할 수 없다.
- `DecryptIdConverterTest`에서 `@DecryptId("memberId")`의 `value`를 검증하려면 mockk을 사용해야 한다:
  ```kotlin
  val annotation = mockk<DecryptId>()
  every { annotation.value } returns "memberId"
  ```
