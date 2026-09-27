# meet-again-backend

재회 매칭 앱 **meet-again** 의 백엔드입니다. 회원·인증, 매칭, 커뮤니티, 인연(포인트) 충전, X룸(추억 기록), 알림을 제공하는 REST API 서버와 배치로 구성됩니다.

- 백엔드 설계와 개발을 1인이 맡았습니다 (커밋 465/466).
- Kotlin · Spring Boot 3 · Jetbrains Exposed · MariaDB · Redis · Spring Batch · Kotest · Spring REST Docs

## 아키텍처 — 헥사고날(포트-어댑터) 멀티모듈

비즈니스 규칙은 `domain` 모듈의 순수 Kotlin 객체가 갖고, DB·Redis·SMS·결제 같은 외부 시스템은 도메인이 정의한 **포트(인터페이스)** 를 `infrastructure` 모듈이 구현합니다.

```
boot/
  ma-boot-web            REST API (Controller, Security, 요청/응답 DTO)
  ma-boot-batch          Spring Batch 잡
domain/
  ma-domain-core         도메인 모델 · 규칙 · 포트 · Service
infrastructure/
  storage/ma-db-core     MariaDB (Exposed) — 영속화 포트 구현
  storage/ma-redis-core  Redis 캐시
  support/ma-jwt-core · ma-crypto-core · ma-sms-sender
          ma-file-storage · ma-payment-core · ma-id-obfuscator   외부 시스템 하나당 모듈 하나
config/
  ma-config-logging      공용 로깅 설정
```

### 의존 방향

```
boot ──implementation──▶ domain ◀──implementation── infrastructure
boot ─────runtimeOnly─────────────────────────────▶ infrastructure
```

- `domain` 은 다른 프로젝트 모듈에 의존하지 않고, 웹·DB 기술(Spring Web, Exposed, Redis 클라이언트 등)도 모릅니다.
- `boot` 는 어댑터 모듈을 **`runtimeOnly`** 로만 의존합니다. Controller 가 DAO 나 테이블 클래스를 import 하면 **컴파일이 실패**하므로, 계층 경계를 규칙 문서가 아니라 빌드가 지킵니다 ([`boot/ma-boot-web/build.gradle.kts`](boot/ma-boot-web/build.gradle.kts)).
- 외부 시스템을 바꾸는 일(예: SMS 제공자, 결제 PG 교체)은 해당 어댑터 모듈 안에서 끝납니다.
- 도메인 패키지 사이의 의존 방향과 순환 여부는 ArchUnit 테스트가 검사합니다 ([`DomainDependencyTest`](domain/ma-domain-core/src/test/kotlin/com/konkuk/ma/architecture/DomainDependencyTest.kt)).

### 도메인 모델이 규칙을 가진다

규칙은 Service 의 `if` 문이 아니라 도메인 객체 안에 둡니다. 예를 들어 인연(포인트) 잔액은 값 객체가 스스로 음수와 잔액 부족을 막습니다.

```kotlin
class PointQuantity(val value: Int) {
    init {
        if (value < 0) throw InvalidValueException(PointQuantity::class, value, "인연 수량은 음수일 수 없습니다.")
    }

    operator fun minus(other: PointQuantity): PointQuantity {
        if (isLessThan(other)) throw InvalidStateException(PointQuantity::class, value, "인연 잔액이 부족합니다.")
        return PointQuantity(value - other.value)
    }
}

class MemberPoint(val id: Long?, val ownerId: Long, val balance: PointQuantity) {
    fun charge(quantity: PointQuantity) = MemberPoint(id, ownerId, balance + quantity)
    fun spend(quantity: PointQuantity) = MemberPoint(id, ownerId, balance - quantity)
}
```

값 객체(`Email`, `PhoneNumber`, `Money`), 일급 컬렉션(`Posts`, `Comments`, `MatchingResults`), 생성 전용 객체(`NewPost`, `NewMember`) 같은 도메인 객체는 Spring 컨텍스트 없이 단위 테스트합니다.

## 테스트

| 모듈 | 테스트 파일 | 내용 |
|---|---|---|
| `domain/ma-domain-core` | 95 | 도메인 객체·Service 단위 테스트, 아키텍처 테스트 |
| `boot/ma-boot-web` | 67 | API 테스트 + Spring REST Docs 문서 생성 |
| `boot/ma-boot-batch` | 6 | 배치 잡 통합 테스트 |
| `infrastructure/*` | 42 | DAO·Repository 테스트 |

```bash
./gradlew test                          # 전체 테스트
./gradlew bootRun -p boot/ma-boot-web   # API 서버 실행
```
