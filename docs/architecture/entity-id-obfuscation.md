# Entity ID 난독화 (Obfuscation)

## 개요

API 응답의 Entity ID가 auto increment 정수여서 쉽게 추측 가능한 문제를 해결한다.
DB는 BIGINT AUTO_INCREMENT를 유지하고, API 레이어에서 Hashids 기반으로 인코딩/디코딩한다.

## 동작 원리

### @EncryptId — Response 인코딩 (서버 → 클라이언트)

API 응답에서 `Long` id를 인코딩된 `String`으로 자동 변환한다.

```
[서버] memberId = 42L
    ↓ Jackson 직렬화 시작
    ↓ AnnotationIntrospector가 @EncryptId 감지
    ↓ EncryptIdSerializer(idObfuscator) 사용
    ↓ idObfuscator.encode(42) → "kRnB9P3L"
[응답 JSON] { "memberId": "kRnB9P3L" }
```

**변환 엔진**: Jackson (`AnnotationIntrospector`)

**사용 예시:**
```kotlin
data class SignUpResponse(
    @EncryptId val memberId: Long  // 응답 시 자동으로 인코딩된 문자열
)
```

### @EncryptId — RequestBody 디코딩 (클라이언트 → 서버, JSON)

`@RequestBody`로 받는 JSON의 인코딩된 ID를 `Long`으로 자동 역직렬화한다.
`@EncryptId`가 Serializer와 Deserializer를 모두 연결하므로, 같은 어노테이션으로 양방향 변환이 된다.

```
[요청 JSON] { "targetId": "kRnB9P3L" }
    ↓ Jackson 역직렬화 시작
    ↓ AnnotationIntrospector가 @EncryptId 감지
    ↓ EncryptIdDeserializer(idObfuscator) 사용
    ↓ idObfuscator.decode("kRnB9P3L") → 42L
[서버] targetId = 42L
```

**변환 엔진**: Jackson (`AnnotationIntrospector`)

**사용 예시:**
```kotlin
data class SomeRequest(
    @EncryptId val targetId: Long  // JSON에서 인코딩된 문자열을 Long으로 자동 역직렬화
)
```

### @DecryptId — Request 디코딩 (클라이언트 → 서버, PathVariable/RequestParam)

URL 경로의 인코딩된 ID를 Controller 파라미터에서 `Long`으로 자동 변환한다.

```
[요청] GET /api/members/kRnB9P3L
    ↓ Spring MVC가 PathVariable "kRnB9P3L" 추출
    ↓ 파라미터에 @DecryptId 감지
    ↓ DecryptIdConverter.matches() → true
    ↓ DecryptIdConverter.convert("kRnB9P3L") → idObfuscator.decode() → 42L
[Controller] fun getMember(@PathVariable @DecryptId memberId: Long)
                                                      // memberId = 42L
```

**변환 엔진**: Spring MVC (`ConversionService`)

**사용 예시:**
```kotlin
@GetMapping("/members/{memberId}")
fun getMember(@PathVariable @DecryptId memberId: Long) {
    // memberId는 이미 디코딩된 42L
}
```

## 비교 정리

| 어노테이션 | 방향 | 적용 대상 | 변환 엔진 |
|---|---|---|---|
| `@EncryptId` (직렬화) | 서버 → 클라이언트 | Response DTO 필드 | Jackson (AnnotationIntrospector) |
| `@EncryptId` (역직렬화) | 클라이언트 → 서버 | RequestBody JSON 필드 | Jackson (AnnotationIntrospector) |
| `@DecryptId` | 클라이언트 → 서버 | PathVariable, RequestParam | Spring MVC (ConversionService) |

- `@EncryptId`: Jackson이 담당하는 JSON 직렬화/역직렬화 양방향 처리
- `@DecryptId`: Spring MVC가 담당하는 URL 파라미터 디코딩 전용

## 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│ boot/ma-boot-web                                            │
│                                                             │
│  @EncryptId (어노테이션)                                     │
│    ↓ (감지)                                                  │
│  EncryptIdAnnotationIntrospector(idObfuscator)              │
│    ↓ (연결)                                                  │
│  EncryptIdSerializer / EncryptIdDeserializer                │
│    ↓ (Jackson 직렬화/역직렬화)                               │
│  ObfuscatedIdJacksonConfig → ObjectMapper에 Introspector 등록│
│                                                             │
│  @DecryptId (어노테이션)                                     │
│    ↓ (감지)                                                  │
│  DecryptIdConverter (ConditionalGenericConverter)            │
│    ↓ (String → Long 변환)                                    │
│  WebConfig → ConversionService에 Converter 등록             │
│                                                             │
│  모두 IdObfuscator 포트를 통해 인코딩/디코딩                 │
└──────────────┬──────────────────────────────────────────────┘
               │ (port)
┌──────────────▼──────────────────────────────────────────────┐
│ domain/ma-domain-core                                       │
│                                                             │
│  [port] IdObfuscator                                        │
│    + encode(id: Long): String                               │
│    + decode(encodedId: String): Long                        │
│                                                             │
│  [exception] InvalidObfuscatedIdException                   │
└──────────────┬──────────────────────────────────────────────┘
               │ (implements)
┌──────────────▼──────────────────────────────────────────────┐
│ infrastructure/support/ma-id-obfuscator                     │
│                                                             │
│  HashidsIdObfuscator                                        │
│    implements IdObfuscator                                   │
│    salt: 환경변수 주입                                       │
│    minLength: 8                                             │
└─────────────────────────────────────────────────────────────┘
```

## 클래스 목록

### Domain Layer
| 클래스 | 역할 |
|--------|------|
| `IdObfuscator` | 포트 인터페이스 (encode/decode) |
| `InvalidObfuscatedIdException` | 디코딩 실패 예외 (400 Bad Request) |

### Infrastructure Layer (ma-id-obfuscator 모듈)
| 클래스 | 역할 |
|--------|------|
| `HashidsIdObfuscator` | Hashids 기반 IdObfuscator 구현체 |

### Boot/Web Layer
| 클래스 | 역할 |
|--------|------|
| `@EncryptId` | Response 인코딩 마커 어노테이션 |
| `@DecryptId` | PathVariable/RequestParam 디코딩 마커 어노테이션 |
| `EncryptIdSerializer` | Jackson Long → String 직렬화 |
| `EncryptIdDeserializer` | Jackson String → Long 역직렬화 |
| `EncryptIdAnnotationIntrospector` | @EncryptId 감지하여 Serializer/Deserializer 연결 |
| `ObfuscatedIdJacksonConfig` | ObjectMapper에 Introspector 등록 (@Configuration) |
| `DecryptIdConverter` | ConditionalGenericConverter (String → Long 디코딩) |
| `WebConfig` | ConversionService에 DecryptIdConverter 등록 |

## 새 API에 적용하는 방법

### Response에 인코딩 추가
```kotlin
data class SomeResponse(
    @EncryptId val someId: Long  // @EncryptId만 붙이면 끝
)
```

### PathVariable 디코딩 추가
```kotlin
@GetMapping("/some/{someId}")
fun getSome(@PathVariable @DecryptId someId: Long) { ... }
```

### 알고리즘 교체
`HashidsIdObfuscator` 대신 새 `IdObfuscator` 구현체를 만들면 된다.
도메인/컨트롤러 코드 변경 없음.
