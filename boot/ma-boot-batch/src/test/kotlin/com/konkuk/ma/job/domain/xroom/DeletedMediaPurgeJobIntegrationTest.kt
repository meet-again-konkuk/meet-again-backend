package com.konkuk.ma.job.domain.xroom

import com.konkuk.ma.domain.xroom.entity.table.MemoryMediaTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * soft-deleted 미디어 물리 파일 purge 배치(deletedMediaPurgeJob) E2E 통합 테스트.
 * 실제 Job 을 H2 DB + 실제 파일시스템(테스트 임시 디렉토리) 위에서 실행하여,
 * "유예가 만료된 soft-deleted 미디어의 물리 파일과 행을 삭제하고, 그 외(만료 전·active)는 보존한다"를
 * 관심사별로 한 테스트씩 검증한다.
 */
@SpringBootTest(properties = ["spring.batch.job.enabled=false"])
@ActiveProfiles("test")
class DeletedMediaPurgeJobIntegrationTest(
    private val jobLauncher: JobLauncher,
    private val deletedMediaPurgeJob: Job,
    @Value("\${file.upload.base-path}") private val uploadBasePath: String,
) : FunSpec({

    // inputDate(2026-06-17) - 유예 7일 → cutoff 2026-06-10 00:00. 그 이전 삭제분이 purge 대상.
    val inputDate = "2026-06-17"
    val beforeCutoff = LocalDateTime.of(2026, 6, 1, 0, 0)
    val afterCutoff = LocalDateTime.of(2026, 6, 15, 0, 0)

    fun insertMedia(
        storageKey: String,
        thumbnailKey: String?,
        deleted: Boolean,
        deletedDate: LocalDateTime?,
    ): Long = transaction {
        MemoryMediaTable.insertAndGetId {
            it[memoryId] = 1L
            it[MemoryMediaTable.storageKey] = storageKey
            it[originalFilename] = "photo.jpg"
            it[mimeType] = "image/jpeg"
            it[fileSize] = 2048L
            it[MemoryMediaTable.thumbnailKey] = thumbnailKey
            it[MemoryMediaTable.deleted] = deleted
            it[MemoryMediaTable.deletedDate] = deletedDate
        }.value
    }

    fun writePhysicalFile(storageKey: String) {
        val path = Paths.get(uploadBasePath).resolve(storageKey)
        Files.createDirectories(path.parent)
        Files.write(path, "fake-image".toByteArray())
    }

    fun fileExists(storageKey: String): Boolean =
        Files.exists(Paths.get(uploadBasePath).resolve(storageKey))

    fun rowCount(mediaId: Long): Long = transaction {
        MemoryMediaTable.selectAll().where { MemoryMediaTable.id eq mediaId }.count()
    }

    fun runPurgeJob(runId: Long): BatchStatus {
        val params = JobParametersBuilder()
            .addString("inputDate", inputDate)
            .addLong("run.id", runId)
            .toJobParameters()
        return jobLauncher.run(deletedMediaPurgeJob, params).status
    }

    beforeSpec {
        transaction { SchemaUtils.create(MemoryMediaTable) }
    }

    afterSpec {
        transaction { SchemaUtils.drop(MemoryMediaTable) }
    }

    afterEach {
        transaction { MemoryMediaTable.deleteAll() }
        File(uploadBasePath).deleteRecursively()
    }

    test("cutoff 이전에 soft delete된 미디어의 물리 파일과 행을 삭제한다") {
        val storageKey = "memory/memory-photo/1/expired.jpg"
        val thumbnailKey = "memory/thumbnail/1/thumb_expired.jpg"
        val mediaId = insertMedia(storageKey, thumbnailKey, deleted = true, deletedDate = beforeCutoff)
        writePhysicalFile(storageKey)
        writePhysicalFile(thumbnailKey)

        runPurgeJob(1L) shouldBe BatchStatus.COMPLETED

        fileExists(storageKey) shouldBe false
        fileExists(thumbnailKey) shouldBe false
        rowCount(mediaId) shouldBe 0L
    }

    test("cutoff 이후 soft delete된 미디어와 active 미디어는 파일·행을 보존한다") {
        val recentKey = "memory/memory-photo/1/recent.jpg"
        val activeKey = "memory/memory-photo/1/active.jpg"
        val recentId = insertMedia(recentKey, thumbnailKey = null, deleted = true, deletedDate = afterCutoff)
        val activeId = insertMedia(activeKey, thumbnailKey = null, deleted = false, deletedDate = null)
        writePhysicalFile(recentKey)
        writePhysicalFile(activeKey)

        runPurgeJob(2L) shouldBe BatchStatus.COMPLETED

        fileExists(recentKey) shouldBe true
        fileExists(activeKey) shouldBe true
        rowCount(recentId) shouldBe 1L
        rowCount(activeId) shouldBe 1L
    }

    test("물리 파일이 이미 없는 미디어도 실패 없이 행을 정리한다") {
        val storageKey = "memory/memory-photo/1/missing.jpg"
        val mediaId = insertMedia(storageKey, thumbnailKey = null, deleted = true, deletedDate = beforeCutoff)
        // 물리 파일을 생성하지 않는다 (이미 없는 상태)

        runPurgeJob(3L) shouldBe BatchStatus.COMPLETED

        rowCount(mediaId) shouldBe 0L
    }
})
