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