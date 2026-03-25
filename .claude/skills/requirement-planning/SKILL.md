---
name: requirement-planning
description: "requirement-planner 에이전트가 구현 계획을 세울 때 따라야 하는 상세 설계 가이드. Design 문서 수준의 코드 스니펫, 아키텍처 다이어그램, import 목록, 메서드 시그니처를 포함한 구체적인 구현 계획을 작성한다. 요구사항 분석, 구현 계획 수립, 태스크 분해 시 항상 참조해야 한다."
---

# 구현 계획 작성 규칙 (Requirement Planning Rules)

requirement-planner 에이전트가 구현 계획을 작성할 때 반드시 따라야 하는 규칙을 정의한다.
추상적인 설명이 아닌, **개발자가 바로 코드를 작성할 수 있는 수준의 상세한 설계**를 제공해야 한다.

---

## 1. 문서 구조

구현 계획은 반드시 다음 구조를 따른다:

```markdown
# Design: {기능명}

## 1. 설계 개요
한 문장으로 무엇을 어떻게 구현하는지 요약

## 2. 아키텍처
ASCII 다이어그램으로 레이어 간 호출 흐름 시각화

## 3. 상세 설계
파일별 코드 스니펫과 설명

## 4. 구현 순서
의존성을 고려한 순서 테이블

## 5. 고려사항
기술적 제약, 성능, 안전성 검토
```

---

## 2. 아키텍처 다이어그램 (필수)

레이어 간 의존 관계를 ASCII 박스 다이어그램으로 표현한다. 헥사고날 아키텍처의 boot → domain(port) → infrastructure 흐름이 드러나야 한다.

```
┌─────────────────────────────────────────────┐
│ boot/ma-boot-batch (또는 ma-boot-web)       │
│                                             │
│  JobConfig / Controller                     │
│    └── 호출 흐름 설명                        │
└──────────────────────┬──────────────────────┘
                       │ (port)
┌──────────────────────▼──────────────────────┐
│ domain/ma-domain-core                       │
│                                             │
│  Port Interface                             │
│    + 메서드시그니처(파라미터): 반환타입       │
└──────────────────────┬──────────────────────┘
                       │ (implements)
┌──────────────────────▼──────────────────────┐
│ infrastructure/storage/ma-db-core           │
│                                             │
│  Repository → DAO                           │
│    └── 실제 SQL/Exposed DSL 설명            │
└─────────────────────────────────────────────┘
```

---

## 3. 상세 설계 작성 규칙

### 3.1 파일별 섹션 구성

각 변경 파일마다 다음을 반드시 포함한다:

1. **파일 경로** (전체 모듈 경로 포함)
2. **변경 후 전체 코드 스니펫** (기존 코드 + 추가 코드가 합쳐진 형태)
3. **파라미터/반환값 설명** (각각의 의미와 선택 이유)
4. **기술적 포인트** (사용하는 API, 주의할 연산자 등)

### 3.2 코드 스니펫 작성 기준

```markdown
### 3.X 레이어명 - 클래스명

**파일**: `모듈/src/main/.../ClassName.kt`

​```kotlin
// 변경 후 전체 코드 (기존 메서드 포함, 추가분은 주석으로 표시)
@Component
class ExampleDao {
    // 기존 메서드 유지

    fun newMethod(param: Type): ReturnType {  // 추가
        return Table.operation {
            // 구현 내용
        }
    }
}
​```

- 첫 번째 포인트: API/연산자 설명
- 두 번째 포인트: 파라미터 선택 이유
```

### 3.3 반드시 포함할 내용

| 항목 | 필수 | 설명 |
|------|:----:|------|
| 파일 전체 경로 | O | 모듈 루트부터의 상대 경로 |
| 변경 후 코드 스니펫 | O | 컴파일 가능한 수준의 코드 |
| 메서드 시그니처 | O | 파라미터명, 타입, 반환타입 |
| 파라미터 설계 이유 | O | 왜 이 파라미터가 필요한지 |
| 사용하는 API/라이브러리 | O | Exposed DSL, Spring Batch 등 |
| 기존 코드와의 관계 | O | 기존 메서드 유지/수정 여부 |
| import 변경 | △ | 새로운 import가 필요한 경우 |

---

## 4. 구현 순서 테이블 (필수)

의존성 순서를 고려한 테이블을 반드시 포함한다:

```markdown
| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 1 | `domain/.../port/SomeRepository.kt` | 수정 | 메서드 추가 |
| 2 | `infrastructure/.../dao/SomeDao.kt` | 수정 | 구현 |
| 3 | `infrastructure/.../repository/SomeCoreRepository.kt` | 수정 | 위임 |
| 4 | `boot/.../SomeConfig.kt` | 신규 | Job/Controller 정의 |
```

- **변경 유형**: 수정 / 신규 / 삭제 명시
- **의존성 순서**: 컴파일 의존성을 고려 (port → dao → repository → boot)

---

## 5. 고려사항 작성 규칙

단순 나열이 아닌, **판단 근거와 대안**을 함께 제시한다:

```markdown
## 고려사항

- **라이브러리 호환성**: Exposed 0.57.0에서 `deleteWhere(limit)` 지원 여부 확인 필요.
  미지원 시 서브쿼리 또는 raw SQL로 대체
- **FK 안전성**: MatchingResult는 참조하는 쪽이므로 삭제해도 FK 위반 없음
- **성능**: 인덱스 없으면 full table scan → 인덱스 추가 권장
```

---

## 6. 프로젝트 컨텍스트 반영

### 기존 패턴 참조 (필수)

이 프로젝트에서 이미 사용 중인 패턴을 반드시 확인하고 일관성을 유지한다:

- **배치 Job**: `AbstractJobConfig` 상속, `@Bean`, `@StepScope` 어노테이션
- **포트 인터페이스**: 도메인 타입 사용 (일급 컬렉션 포함)
- **DAO 패턴**: `CommandDao` (쓰기) / `QueryDao` (읽기) 분리
- **Repository**: 포트 구현체가 DAO에 위임하는 구조
- **ORM**: Jetbrains Exposed DSL (NOT JPA)

### DDL 스크립트 관리 (필수)

새로운 테이블이 생성되거나 기존 테이블 스키마가 변경되면, 반드시 DDL 쿼리를 `infrastructure/storage/ma-db-core/src/main/resources/script/ddl.sql`에 추가한다.

- 기존 `ddl.sql` 파일을 먼저 읽고, 동일한 스타일(컬럼 정렬, 주석 패턴, BaseTable 공통 컬럼 순서)로 작성
- BaseTable 공통 컬럼: `CREATED_DATE`, `CREATED_BY`, `LAST_MODIFIED_DATE`, `LAST_MODIFIED_BY`, `DELETED`
- **FK(FOREIGN KEY) 사용 금지** — PK와 INDEX만 사용. 참조 관계는 애플리케이션 레벨에서 관리
- 인덱스는 테이블 하단에 명시
- 구현 순서 테이블에 DDL 변경 항목도 포함

### 기존 코드 읽기 (필수)

구현 계획 작성 전에 반드시 다음을 확인한다:

1. 변경 대상 파일의 현재 코드 전체 읽기
2. 동일 패턴을 사용하는 기존 구현체 참조 (예: 비슷한 Job이 있으면 해당 코드 확인)
3. 사용하려는 라이브러리 API의 시그니처 확인
4. `ddl.sql`을 읽고 기존 DDL 스타일 확인

---

## 7. 안티패턴 (하지 말아야 할 것)

```markdown
# BAD - 추상적인 설명만 있음
1. Repository에 삭제 메서드를 추가합니다.
2. DAO에서 구현합니다.
3. Job을 만듭니다.

# GOOD - 코드와 함께 구체적으로
### 3.1 Domain Port 확장

**파일**: `domain/.../port/MatchingResultRepository.kt`

​```kotlin
interface MatchingResultRepository {
    fun saveAll(matchingResults: MatchingResults)
    fun findExistingMatchingResults(targetInfoIds: List<Long>): MatchingResults
    fun deleteExpired(now: LocalDateTime, limit: Int): Long  // 추가
}
​```

- `now`: 비교 기준 시각 (테스트 용이성을 위해 외부에서 주입)
- `limit`: 한 번에 삭제할 최대 건수
- 반환값: 실제 삭제된 행 수
```

| BAD | GOOD |
|-----|------|
| "Repository에 메서드 추가" | 메서드 시그니처 + 파라미터 설명 포함 |
| "Exposed로 삭제 구현" | `deleteWhere(limit)` 코드 스니펫 + 연산자 설명 |
| "Job을 만듭니다" | `JobBuilder`, `StepBuilder`, `Tasklet` 전체 코드 |
| "적절히 처리" | 구체적인 조건문과 반환값 명시 |
| import 목록 누락 | 새로 필요한 import 명시 |
| 결과를 대화로만 반환 | 반드시 파일로 저장 |

---

## 8. 파일 저장 규칙 (필수)

구현 계획 작성이 완료되면 **반드시 파일로 저장**한다. 대화 내 반환만으로는 부족하다.

**저장 경로**: `requirement/{YYYYMM}/{feature-name}.requirement.md`

년월(`YYYYMM`) 디렉토리로 구분하여 저장한다. 현재 날짜 기준으로 년월을 결정한다.

```
requirement/
  └── 202603/
  │   └── matching-result-cleanup-job.requirement.md
  │   └── matching-result-query-api.requirement.md
  └── 202604/
      └── user-authentication.requirement.md
```

**규칙**:
- `requirement/{YYYYMM}/` 디렉토리가 없으면 생성
- 파일명은 feature 이름을 kebab-case로 변환
- 작성 완료 후 저장 경로를 사용자에게 반드시 안내
- 파일 최상단에 작성일과 상태를 기록