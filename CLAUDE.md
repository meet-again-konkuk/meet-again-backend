# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew build                         # Full build with tests
./gradlew test                          # Run all tests
./gradlew test --tests "ClassName"      # Run specific test class
./gradlew test -p boot/ma-boot-web      # Run tests for specific module
./gradlew bootRun -p boot/ma-boot-web   # Run web application
./gradlew bootRun -p boot/ma-boot-batch # Run batch application
./gradlew clean                         # Clean build artifacts
```

## Architecture

This is a Kotlin Spring Boot multi-module project using **hexagonal/ports-and-adapters architecture**:

```
meet-again/
├── boot/                    # Executable Spring Boot applications
│   ├── ma-boot-web/        # REST API server (controllers, security config)
│   └── ma-boot-batch/      # Batch job application
├── domain/
│   └── ma-domain-core/     # Framework-independent business logic
│       ├── model/          # Domain classes with business rules
│       └── port/           # Interfaces for external dependencies
├── infrastructure/
│   ├── storage/
│   │   ├── ma-db-core/     # MariaDB with Exposed ORM
│   │   └── ma-redis-core/  # Redis caching
│   └── support/
│       ├── ma-jwt-core/    # JWT token handling
│       ├── ma-crypto-core/ # Password encryption
│       └── ma-sms-sender/  # SMS service (Nurigo)
└── config/                  # Shared configuration modules
```

**Key principle**: Domain layer has NO Spring Boot dependencies. Infrastructure modules implement domain ports.

## Tech Stack

- **Language**: Kotlin 1.9.25, Java 21
- **Framework**: Spring Boot 3.3.4, Spring Security, Spring Batch
- **Database**: MariaDB with Jetbrains Exposed ORM (NOT JPA/Hibernate)
- **Cache**: Redis
- **Auth**: JWT (stateless, filter-based)
- **Testing**: KoTest + Mockk + Spring REST Docs
- **Docs**: API docs auto-generated via REST Docs → AsciiDoc

## Testing Conventions

- Use KoTest spec styles (BehaviorSpec, StringSpec, etc.)
- Use Mockk for mocking (not Mockito)
- API tests extend common test configuration with `@BaseApiTest`
- REST Docs snippets generate API documentation

## Database Access

Uses **Jetbrains Exposed** ORM with DSL syntax, not JPA annotations:
```kotlin
// Entity definition pattern
object TargetInfoTable : LongIdTable("target_info") {
    val name = varchar("name", 50)
}
```

## API Documentation

- Source: `boot/ma-boot-web/src/docs/asciidoc/`
- Generated from test snippets to `src/main/resources/static/docs/`
- Manual testing: HTTP files in `/http/web-api/`

## Required Skills

코드를 작성하거나 수정하는 모든 에이전트는 다음 스킬을 반드시 참조한다:

- `clean-code` — Robert C. Martin의 Clean Code 원칙 (네이밍, 함수, 주석, 포매팅, 에러 핸들링)
- `code-implementation-rules` — 이 프로젝트의 OOP 원칙과 구현 패턴 (도메인 행위 부여, 원시값 포장, 일급 컬렉션, 포트 규칙)

## OOP Principles

코드 작성 시 다음 객체지향 원칙을 반드시 따른다:

- **도메인 객체에 행위를 부여**: 외부에서 getter로 꺼내서 판단하지 말고, 객체 스스로 판단/행동하게 한다
- **원시값 포장**: 도메인에서 의미 있는 값은 Value Object로 감싼다 (예: `FourDigit`, `Year`, `Month`)
- **일급 컬렉션**: 컬렉션을 감싸는 도메인 객체를 활용하여 관련 로직을 응집시킨다. 멤버 변수명은 `val data`로 통일한다
- **디미터 법칙**: `a.b.c.doSomething()` 같은 체이닝을 피하고, 직접 협력하는 객체에게만 메시지를 보낸다
- **상태 검증은 객체 내부에서**: validation, 비교, 판단 로직은 해당 도메인 객체 안에 둔다
- **팩토리 메서드 활용**: 복잡한 객체 생성은 `companion object`의 팩토리 메서드로 의도를 드러낸다