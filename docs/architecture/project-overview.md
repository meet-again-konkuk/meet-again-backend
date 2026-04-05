# Meet-Again 프로젝트 아키텍처

## 개요

건국대학교 졸업 프로젝트 "다시 만남" 서비스의 백엔드 시스템.
잃어버린 사람을 찾기 위한 매칭 서비스로, 사용자가 찾는 상대의 정보를 등록하면 자동 매칭 알고리즘으로 후보를 찾아줌.

## 기술 스택

| 항목 | 기술 | 버전 |
|------|------|------|
| 언어 | Kotlin | 1.9.25 |
| JDK | Java | 21 |
| 프레임워크 | Spring Boot | 3.3.4 |
| 보안 | Spring Security (JWT) | - |
| 배치 | Spring Batch | 5.1.0 |
| DB | MariaDB | - |
| ORM | Jetbrains Exposed (DSL) | 0.57.0 |
| 캐시 | Redis | Spring Data Redis |
| JWT | JJWT | 0.11.5 |
| SMS | Nurigo SDK | 4.3.0 |
| ID 난독화 | Hashids | 1.0.3 |
| 테스트 | KoTest + Mockk | 5.5.5 |
| API 문서 | Spring REST Docs | AsciiDoc |

## 모듈 구조

```
meet-again/
├── boot/                           # 실행 가능 모듈
│   ├── ma-boot-web                 # REST API 서버
│   └── ma-boot-batch              # 배치 작업
│
├── domain/                         # 비즈니스 로직 (프레임워크 독립)
│   └── ma-domain-core
│
├── infrastructure/                 # 포트 구현 (어댑터)
│   ├── storage/
│   │   ├── ma-db-core             # MariaDB + Exposed ORM
│   │   └── ma-redis-core          # Redis 캐싱
│   └── support/
│       ├── ma-jwt-core            # JWT 토큰
│       ├── ma-crypto-core         # BCrypt 암호화
│       ├── ma-sms-sender          # SMS 전송 (Nurigo)
│       ├── ma-file-storage        # 로컬 파일 저장소
│       └── ma-id-obfuscator       # ID 난독화 (Hashids)
│
└── config/                         # 공통 설정
    ├── ma-config-yaml-importer    # YAML 설정 로드
    └── ma-config-logging          # 로깅 (Logback + JSON)
```

## 아키텍처 패턴

헥사고날(포트-어댑터) 아키텍처:

```
┌─────────────────────────────────────────────┐
│ Boot Layer (Controllers, Security, Config)  │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│ Domain Layer (Models, Services, Ports)       │
│  - Spring 의존성 없음                         │
│  - 비즈니스 로직만 포함                        │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│ Infrastructure Layer (Port 구현체)            │
│  - DB, Redis, JWT, SMS, FileStorage 등       │
└─────────────────────────────────────────────┘
```

**의존 방향:**
- Boot → Domain (컴파일 타임)
- Boot → Infrastructure (런타임 only)
- Infrastructure → Domain (포트 구현)
- Domain은 어디에도 의존하지 않음

## 도메인

### Member (회원)
- 회원가입 (프로필 사진 포함, multipart)
- 닉네임/이메일 중복 확인
- 비밀번호 암호화 (BCrypt)

주요 클래스: `Member`, `PhoneNumber`, `FourDigit`, `Gender`, `Region`

### Auth (인증)
- JWT 기반 로그인 (Access + Refresh 토큰)
- 토큰 갱신
- SMS 인증 (전송, 확인)

주요 클래스: `LoginInfo`, `RefreshToken`, `SignUpValidator`, `SmsVerification`

### Matching (매칭)
- 찾는 사람 정보 등록
- 자동 매칭 (Spring Batch)
- 매칭률 계산 알고리즘
- 만료 매칭 결과 삭제

주요 클래스: `TargetInfo`, `MatchingResult`, `MatchRateCalculator`, `MatchingGroup`

### Common (공통)
- Value Object: `Year`, `Month`, `Day`
- 파일: `PhotoFile`, `AllowedExtension`, `StoragePath`
- ID 난독화: `ObfuscationType`, `IdObfuscator`

## API 엔드포인트

### 인증 (`/api/auth`)
| 메서드 | 경로 | 설명 | 인증 |
|--------|------|------|------|
| POST | `/api/auth/sign-up` | 회원가입 (multipart) | X |
| POST | `/api/auth/login` | 로그인 | X |
| POST | `/api/auth/refresh-token` | 토큰 갱신 | X |

### SMS (`/api/sms`)
| 메서드 | 경로 | 설명 | 인증 |
|--------|------|------|------|
| POST | `/api/sms/verification-code` | 인증코드 전송 | X |
| POST | `/api/sms/verification-code/confirm` | 인증코드 확인 | X |

### 회원 (`/api/members`)
| 메서드 | 경로 | 설명 | 인증 |
|--------|------|------|------|
| POST | `/api/members/duplicated-nickname` | 닉네임 중복 확인 | X |
| POST | `/api/members/duplicated-email` | 이메일 중복 확인 | X |

### 매칭 (`/api/target-infos`)
| 메서드 | 경로 | 설명 | 인증 |
|--------|------|------|------|
| POST | `/api/target-infos` | 찾는 사람 정보 등록 | O |

## 배치 작업

### Matching Job
- 목적: 등록된 찾는 사람 정보를 기반으로 자동 매칭
- 방식: Chunk 처리 (100개씩 읽기 → 매칭 → 저장)
- 리더: NoOffset 페이징 (커서 기반)
- 프로세서: 매칭 알고리즘 수행 + 중복 제거
- 라이터: 새 매칭 결과 저장

### Expired Matching Result Delete Job
- 목적: 210일 경과한 매칭 결과 삭제
- 방식: Tasklet (단일 작업)

## 매칭률 알고리즘

조합 우선 판정 + 부분 가산:

| 조건 | 매칭률 |
|------|--------|
| 전화번호 완전 + 생년월일 완전 + 지역 | 99% |
| 전화번호 완전 + 생년월일 완전 | 97% |
| 전화번호 완전 + 지역 | 90% |
| 전화번호 완전 | 85% |
| 생년월일 완전 + 지역 | 83% |
| 생년월일 완전 | 80% |
| 부분 일치 시 | 전화번호 부분 30% + 생년월일 항목당 10% + 지역 10% |

`MatchingGroup` (sealed class)으로 그룹별 점수 계산을 캡슐화하여 OCP 적용.

## 보안

### JWT 인증 흐름
```
요청 → JwtAuthenticationFilter → 토큰 검증 → SecurityContext 설정 → Controller
```

- 세션리스 (STATELESS)
- Bearer 토큰 방식
- Access Token: 1시간, Refresh Token: 7일

### ID 난독화
- `@EncryptId(ObfuscationType.MEMBER)` — Response에서 Long → 인코딩된 String
- `@DecryptId(ObfuscationType.MEMBER)` — PathVariable에서 인코딩된 String → Long
- Entity별 다른 salt로 cross-entity 공격 방지

## 설계 원칙

1. **도메인 객체에 행위 부여** — getter로 꺼내서 판단하지 않고 객체에게 메시지를 보냄
2. **원시값 포장 (Value Object)** — `FourDigit`, `Year`, `Month`, `Day`, `AllowedExtension`
3. **일급 컬렉션** — `TargetInfos`, `MatchingResults`, `Targets` (멤버 변수명 `val data`)
4. **디미터 법칙** — 직접 협력하는 객체에게만 메시지
5. **포트 인터페이스는 도메인 타입 사용** — 원시 타입이 아닌 도메인 객체/일급 컬렉션
6. **Service는 Service를 참조하지 않음** — 포트(인터페이스)만 의존
7. **DDL에서 FK 사용 금지** — PK와 INDEX만 사용
8. **DAO는 Entity를 반환, Entity에서 도메인으로 변환** — `toDomain()` 메서드

## 테스트

- **단위 테스트**: KoTest FunSpec/BehaviorSpec + Mockk
- **API 테스트**: `@WebMvcTest` + `@BaseApiTest` + Spring REST Docs
- **통합 테스트**: `@SpringBootTest` + H2 + embedded-redis
- **Vocabulary 패턴**: REST Docs 필드 정의를 재사용 함수로 분리

## 문서 구조

```
docs/
├── architecture/          # 기술 아키텍처 문서
│   ├── project-overview.md
│   └── entity-id-obfuscation.md
└── requirement/           # 구현 계획/요구사항
    └── 202603/
        ├── entity-id-obfuscation.requirement.md
        ├── matching-result-cleanup-job.requirement.md
        └── member-photo-upload.requirement.md
```
