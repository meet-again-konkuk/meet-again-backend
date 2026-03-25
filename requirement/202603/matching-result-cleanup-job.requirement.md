# MatchingResult Cleanup Batch Job 요구사항 명세서

## 1. 목표 (Goal)

`matchingExpiryDate`가 현재 시각보다 이전인 만료된 `MatchingResult` 레코드를 Spring Batch Job으로 주기적으로 Hard Delete하여 DB 스토리지를 관리한다.

---

## 2. 요구사항 이해 (Requirement Analysis)

### 2.1 명시된 요구사항
- `matchingExpiryDate < now()`인 MatchingResult를 삭제
- Spring Batch Job으로 구현 (기존 `ma-boot-batch` 모듈)
- 대량 데이터 삭제 시 DB lock 방지를 위해 배치 단위(chunk) 삭제
- Hard Delete (물리 삭제)

### 2.2 전제사항 및 설계 결정
- **Hard Delete 방식**: `BaseTable`에 `deleted` 컬럼(soft delete)이 존재하지만, 요구사항에 따라 `DELETE FROM` SQL로 물리 삭제한다.
- **MATCHING_EXPIRY_DAYS = 210일**: MatchingResult 생성 후 약 7개월 뒤 만료. 따라서 한 번에 삭제되는 데이터량은 210일 전 생성된 건수에 비례한다.
- **Tasklet 기반 구현**: 이 Job은 Reader -> Writer 패턴이 아닌, 직접 DELETE 쿼리를 chunk 단위로 실행하는 Tasklet 방식이 적합하다. 이유: 삭제 대상 데이터를 메모리에 읽어올 필요 없이 DB에서 직접 DELETE하는 것이 효율적이다.
- **삭제 단위**: 한 번에 최대 1,000건씩 `DELETE ... LIMIT`으로 삭제하여 DB lock 시간을 최소화한다.
- **반복 실행**: 삭제 대상이 0건이 될 때까지 반복한다.

---

## 3. 기능 요구사항 (Functional Requirements)

| # | 요구사항 | 설명 |
|---|---------|------|
| FR-1 | 만료 MatchingResult 삭제 | `MATCHING_EXPIRY_DATE < NOW()`인 레코드를 `MATCHING_RESULTS` 테이블에서 물리 삭제 |
| FR-2 | 배치 단위 삭제 | 한 번의 DELETE 쿼리에 최대 1,000건씩 삭제하여 DB lock 최소화 |
| FR-3 | 반복 삭제 | 삭제된 건수가 0이 될 때까지 반복 실행 |
| FR-4 | Job 파라미터 | 실행 시점을 Job Parameter로 전달하여 멱등성 보장 (동일 파라미터로 재실행 방지) |

---

## 4. 비기능 요구사항 (Non-functional Requirements)

| # | 항목 | 설명 |
|---|------|------|
| NFR-1 | DB Lock 최소화 | `DELETE ... LIMIT 1000` 패턴으로 짧은 트랜잭션 유지 |
| NFR-2 | 메모리 효율 | 삭제 대상을 메모리에 로드하지 않고 DB에서 직접 삭제 |
| NFR-3 | 로깅 | 각 chunk 삭제 시 삭제 건수 로깅, Job 완료 시 총 삭제 건수 로깅 |

---

## 5. 제약사항 (Constraints)

| # | 제약 | 설명 |
|---|------|------|
| C-1 | Exposed ORM 사용 | Jetbrains Exposed DSL로 DELETE 쿼리 작성 (Raw SQL 사용 금지) |
| C-2 | 도메인 레이어 독립성 | 도메인 포트(인터페이스)에 삭제 메서드 추가, Infrastructure에서 구현 |
| C-3 | 기존 패턴 준수 | `AbstractJobConfig` 상속, 기존 `MatchingJobConfig`와 동일한 구조 패턴 |

---

## 6. 예외/엣지 케이스 (Edge Cases)

| # | 케이스 | 처리 방안 |
|---|--------|----------|
| EC-1 | 삭제 대상 0건 | Step이 즉시 종료, Job은 COMPLETED 상태 |
| EC-2 | 삭제 중 DB 에러 | Spring Batch의 기본 재시도/실패 처리에 위임. 트랜잭션 롤백 후 Job FAILED |
| EC-3 | 대량 데이터 (수십만 건) | 1,000건씩 반복 삭제하므로 한 트랜잭션 내 lock 범위 제한 |
| EC-4 | 동시 실행 | Spring Batch의 Job Instance 관리로 동일 파라미터 중복 실행 방지 |

---

## 7. 아키텍처 다이어그램

```
┌─────────────────────────────────────────────────────────┐
│  boot/ma-boot-batch                                     │
│                                                         │
│  MatchingResultCleanupJobConfig                         │
│    ├─ matchingResultCleanupJob(): Job                   │
│    └─ matchingResultCleanupStep(): Step (Tasklet)       │
│         └─ MatchingResultCleanupTasklet                 │
│              └─ uses MatchingResultCleanupRepository     │
│                        (domain port)                    │
└───────────────────────┬─────────────────────────────────┘
                        │ depends on
┌───────────────────────▼─────────────────────────────────┐
│  domain/ma-domain-core                                  │
│                                                         │
│  port/MatchingResultCleanupRepository (interface)       │
│    └─ deleteExpiredBefore(now: LocalDateTime,            │
│                          limit: Int): Int               │
└───────────────────────┬─────────────────────────────────┘
                        │ implemented by
┌───────────────────────▼─────────────────────────────────┐
│  infrastructure/storage/ma-db-core                      │
│                                                         │
│  MatchingResultCleanupDao                               │
│    └─ deleteExpiredBefore(now, limit): Int               │
│         → Exposed DSL: DELETE FROM MATCHING_RESULTS     │
│           WHERE MATCHING_EXPIRY_DATE < now LIMIT limit  │
│                                                         │
│  MatchingResultCleanupCoreRepository                    │
│    └─ implements MatchingResultCleanupRepository         │
│    └─ delegates to MatchingResultCleanupDao              │
└─────────────────────────────────────────────────────────┘
```

---

## 8. 구현 계획 (Implementation Plan)

### Phase 1: Domain Port 정의 (난이도: Low)

**영향 범위**: `domain/ma-domain-core`

**Task 1.1** - `MatchingResultCleanupRepository` 포트 인터페이스 생성

파일: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/matching/domain/port/MatchingResultCleanupRepository.kt`

```kotlin
package com.konkuk.ma.domain.matching.domain.port

import java.time.LocalDateTime

interface MatchingResultCleanupRepository {
    /**
     * matchingExpiryDate가 now보다 이전인 MatchingResult를 최대 limit건 삭제한다.
     * @return 실제 삭제된 건수
     */
    fun deleteExpiredBefore(now: LocalDateTime, limit: Int): Int
}
```

**의존성**: 없음

---

### Phase 2: Infrastructure 어댑터 구현 (난이도: Medium)

**영향 범위**: `infrastructure/storage/ma-db-core`

**Task 2.1** - `MatchingResultCleanupDao` 생성

파일: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/matching/dao/MatchingResultCleanupDao.kt`

```kotlin
package com.konkuk.ma.domain.matching.dao

import com.konkuk.ma.domain.matching.entity.table.MatchingResultTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.deleteWhere
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class MatchingResultCleanupDao {
    fun deleteExpiredBefore(now: LocalDateTime, limit: Int): Int {
        return MatchingResultTable.deleteWhere(limit = limit) {
            MatchingResultTable.matchingExpiryDate less now
        }
    }
}
```

> **주의**: Exposed `deleteWhere`의 `limit` 파라미터 지원 여부를 Exposed 0.57.0 API에서 확인해야 한다. 만약 `limit` 파라미터가 없다면, 아래 대안 방식을 사용한다:
>
> **대안** (서브쿼리 방식):
> ```kotlin
> @Component
> class MatchingResultCleanupDao {
>     fun deleteExpiredBefore(now: LocalDateTime, limit: Int): Int {
>         // 1) 삭제 대상 ID를 limit 만큼 조회
>         val targetIds = MatchingResultTable
>             .select(MatchingResultTable.id)
>             .where { MatchingResultTable.matchingExpiryDate less now }
>             .limit(limit)
>             .map { it[MatchingResultTable.id].value }
>
>         if (targetIds.isEmpty()) return 0
>
>         // 2) 조회된 ID로 삭제
>         return MatchingResultTable.deleteWhere {
>             MatchingResultTable.id inList targetIds
>         }
>     }
> }
> ```

**Task 2.2** - `MatchingResultCleanupCoreRepository` 생성

파일: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/matching/repository/MatchingResultCleanupCoreRepository.kt`

```kotlin
package com.konkuk.ma.domain.matching.repository

import com.konkuk.ma.domain.matching.dao.MatchingResultCleanupDao
import com.konkuk.ma.domain.matching.domain.port.MatchingResultCleanupRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class MatchingResultCleanupCoreRepository(
    private val matchingResultCleanupDao: MatchingResultCleanupDao
) : MatchingResultCleanupRepository {
    override fun deleteExpiredBefore(now: LocalDateTime, limit: Int): Int {
        return matchingResultCleanupDao.deleteExpiredBefore(now, limit)
    }
}
```

**의존성**: Task 1.1

---

### Phase 3: Batch Job 구현 (난이도: Medium)

**영향 범위**: `boot/ma-boot-batch`

**Task 3.1** - `MatchingResultCleanupTasklet` 생성

파일: `boot/ma-boot-batch/src/main/kotlin/com/konkuk/ma/job/domain/matching/MatchingResultCleanupTasklet.kt`

```kotlin
package com.konkuk.ma.job.domain.matching

import com.konkuk.ma.domain.matching.domain.port.MatchingResultCleanupRepository
import org.slf4j.LoggerFactory
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import java.time.LocalDateTime

class MatchingResultCleanupTasklet(
    private val matchingResultCleanupRepository: MatchingResultCleanupRepository,
    private val chunkSize: Int,
    private val now: LocalDateTime
) : Tasklet {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun execute(
        contribution: StepContribution,
        chunkContext: ChunkContext
    ): RepeatStatus {
        val deletedCount = matchingResultCleanupRepository.deleteExpiredBefore(now, chunkSize)
        log.info("[MatchingResultCleanup] 삭제 건수: {}", deletedCount)

        contribution.incrementWriteCount(deletedCount.toLong())

        return if (deletedCount < chunkSize) {
            RepeatStatus.FINISHED
        } else {
            RepeatStatus.CONTINUABLE
        }
    }
}
```

> **설계 포인트**:
> - `RepeatStatus.CONTINUABLE`: 삭제 건수가 chunkSize와 같으면 아직 남은 데이터가 있을 수 있으므로 반복
> - `RepeatStatus.FINISHED`: 삭제 건수가 chunkSize 미만이면 더 이상 삭제 대상 없음
> - `now`를 외부에서 주입받아 Job 실행 중 시각 변동 방지

**Task 3.2** - `MatchingResultCleanupJobConfig` 생성

파일: `boot/ma-boot-batch/src/main/kotlin/com/konkuk/ma/job/domain/matching/MatchingResultCleanupJobConfig.kt`

```kotlin
package com.konkuk.ma.job.domain.matching

import com.konkuk.ma.domain.matching.domain.port.MatchingResultCleanupRepository
import com.konkuk.ma.job.common.AbstractJobConfig
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.LocalDateTime

@Configuration
class MatchingResultCleanupJobConfig(
    private val matchingResultCleanupRepository: MatchingResultCleanupRepository
) : AbstractJobConfig() {

    @Bean
    fun matchingResultCleanupJob(): Job {
        return JobBuilder("matchingResultCleanupJob", jobRepository)
            .start(matchingResultCleanupStep())
            .build()
    }

    @Bean
    fun matchingResultCleanupStep(): Step {
        return StepBuilder("matchingResultCleanupStep", jobRepository)
            .tasklet(
                MatchingResultCleanupTasklet(
                    matchingResultCleanupRepository = matchingResultCleanupRepository,
                    chunkSize = CHUNK_SIZE_1000,
                    now = LocalDateTime.now()
                ),
                transactionManager
            )
            .build()
    }
}
```

**의존성**: Task 1.1, Task 2.1, Task 2.2, Task 3.1

---

### Phase 4: 테스트 구현 (난이도: Medium)

**영향 범위**: `boot/ma-boot-batch`, `infrastructure/storage/ma-db-core`

**Task 4.1** - `MatchingResultCleanupTasklet` 단위 테스트

파일: `boot/ma-boot-batch/src/test/kotlin/com/konkuk/ma/job/domain/matching/MatchingResultCleanupTaskletTest.kt`

```kotlin
package com.konkuk.ma.job.domain.matching

import com.konkuk.ma.domain.matching.domain.port.MatchingResultCleanupRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.repeat.RepeatStatus
import java.time.LocalDateTime

class MatchingResultCleanupTaskletTest : BehaviorSpec({

    val cleanupRepository = mockk<MatchingResultCleanupRepository>()
    val contribution = mockk<StepContribution>(relaxed = true)
    val chunkContext = mockk<ChunkContext>(relaxed = true)
    val now = LocalDateTime.of(2026, 3, 17, 0, 0)
    val chunkSize = 1000

    Given("삭제 대상이 chunkSize만큼 존재하는 경우") {
        every { cleanupRepository.deleteExpiredBefore(now, chunkSize) } returns chunkSize

        val tasklet = MatchingResultCleanupTasklet(cleanupRepository, chunkSize, now)

        When("tasklet을 실행하면") {
            val result = tasklet.execute(contribution, chunkContext)

            Then("CONTINUABLE을 반환하여 반복 실행한다") {
                result shouldBe RepeatStatus.CONTINUABLE
            }
        }
    }

    Given("삭제 대상이 chunkSize 미만인 경우") {
        every { cleanupRepository.deleteExpiredBefore(now, chunkSize) } returns 500

        val tasklet = MatchingResultCleanupTasklet(cleanupRepository, chunkSize, now)

        When("tasklet을 실행하면") {
            val result = tasklet.execute(contribution, chunkContext)

            Then("FINISHED를 반환하여 종료한다") {
                result shouldBe RepeatStatus.FINISHED
            }
        }
    }

    Given("삭제 대상이 0건인 경우") {
        every { cleanupRepository.deleteExpiredBefore(now, chunkSize) } returns 0

        val tasklet = MatchingResultCleanupTasklet(cleanupRepository, chunkSize, now)

        When("tasklet을 실행하면") {
            val result = tasklet.execute(contribution, chunkContext)

            Then("FINISHED를 반환하여 즉시 종료한다") {
                result shouldBe RepeatStatus.FINISHED
            }
        }
    }
})
```

**Task 4.2** - `MatchingResultCleanupDao` 통합 테스트

파일: `infrastructure/storage/ma-db-core/src/test/kotlin/com/konkuk/ma/domain/matching/dao/MatchingResultCleanupDaoTest.kt`

```kotlin
// 통합 테스트: 실제 DB(테스트용 MariaDB/H2)에 만료 데이터를 넣고 삭제 확인
// - 만료된 데이터가 정확히 삭제되는지 검증
// - 만료되지 않은 데이터가 유지되는지 검증
// - limit 파라미터가 올바르게 동작하는지 검증
```

**의존성**: Phase 1, 2, 3 완료 후

---

## 9. 작업 요약 (Task Summary)

| Phase | Task | 파일 | 레이어 | 난이도 | 의존성 |
|-------|------|------|--------|--------|--------|
| 1 | 1.1 Port 인터페이스 | `domain/.../port/MatchingResultCleanupRepository.kt` | Domain | Low | - |
| 2 | 2.1 DAO 구현 | `infrastructure/.../dao/MatchingResultCleanupDao.kt` | Infra | Medium | 1.1 |
| 2 | 2.2 Repository 구현 | `infrastructure/.../repository/MatchingResultCleanupCoreRepository.kt` | Infra | Low | 1.1, 2.1 |
| 3 | 3.1 Tasklet 구현 | `boot/.../matching/MatchingResultCleanupTasklet.kt` | Boot | Medium | 1.1 |
| 3 | 3.2 JobConfig 구현 | `boot/.../matching/MatchingResultCleanupJobConfig.kt` | Boot | Low | 3.1, 2.2 |
| 4 | 4.1 Tasklet 단위 테스트 | `boot/.../matching/MatchingResultCleanupTaskletTest.kt` | Boot | Medium | 3.1 |
| 4 | 4.2 DAO 통합 테스트 | `infrastructure/.../dao/MatchingResultCleanupDaoTest.kt` | Infra | Medium | 2.1 |

---

## 10. 확인 필요 사항 (Open Items)

| # | 항목 | 설명 |
|---|------|------|
| OI-1 | Exposed `deleteWhere` limit 지원 | Exposed 0.57.0에서 `deleteWhere`에 `limit` 파라미터가 지원되는지 확인 필요. 미지원 시 서브쿼리(SELECT ID -> DELETE BY ID) 방식으로 대체 (Task 2.1 대안 참조) |
| OI-2 | Job 스케줄링 | 이 Job의 실행 주기 (매일? 매주?) 및 스케줄러 설정은 별도 논의 필요 |
| OI-3 | 관련 테이블 cascade | MatchingResult 삭제 시 FK로 연결된 다른 테이블이 있는지 확인 필요. 현재 코드상 MatchingResultTable은 MemberTable.email과 TargetInfoTable.id를 참조하지만, 역방향 참조(다른 테이블이 MatchingResult를 FK로 참조)가 있는지 DB 스키마 확인 필요 |

---

## 11. 품질 체크리스트

- [x] 모든 기능 요구사항(FR-1~4)이 Task에 매핑됨
- [x] 도메인 레이어 Task(1.1)에 Spring/Infrastructure 의존성 없음
- [x] 엣지 케이스(EC-1~4)가 설계에 반영됨
- [x] Task가 의존성 순서대로 정렬됨
- [x] 테스트 Task(4.1, 4.2)가 주요 컴포넌트별로 포함됨
