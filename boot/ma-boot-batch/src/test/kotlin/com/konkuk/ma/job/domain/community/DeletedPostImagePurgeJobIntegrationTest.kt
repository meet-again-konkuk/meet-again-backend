package com.konkuk.ma.job.domain.community

import com.konkuk.ma.domain.community.entity.table.PostImageTable
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
 * soft-deleted 게시글 이미지 물리 파일 purge 배치(deletedPostImagePurgeJob) E2E 통합 테스트.
 * DeletedMediaPurgeJobIntegrationTest 미러 — 실제 Job 을 H2 DB + 실제 파일시스템(테스트 임시 디렉토리) 위에서 실행하여,
 * "유예가 만료된 soft-deleted 이미지의 물리 파일과 행을 삭제하고, 그 외(만료 전·active)는 보존한다"를
 * 관심사별로 한 테스트씩 검증한다.
 */
@SpringBootTest(properties = ["spring.batch.job.enabled=false"])
@ActiveProfiles("test")
class DeletedPostImagePurgeJobIntegrationTest(
    private val jobLauncher: JobLauncher,
    private val deletedPostImagePurgeJob: Job,
    @Value("\${file.upload.base-path}") private val uploadBasePath: String,
) : FunSpec({

    // inputDate(2026-06-17) - 유예 7일 → cutoff 2026-06-10 00:00. 그 이전 삭제분이 purge 대상.
    val inputDate = "2026-06-17"
    val beforeCutoff = LocalDateTime.of(2026, 6, 1, 0, 0)
    val afterCutoff = LocalDateTime.of(2026, 6, 15, 0, 0)

    fun insertPostImage(
        storageKey: String,
        thumbnailKey: String?,
        deleted: Boolean,
        deletedDate: LocalDateTime?,
    ): Long = transaction {
        PostImageTable.insertAndGetId {
            it[postId] = 1L
            it[PostImageTable.storageKey] = storageKey
            it[originalFilename] = "photo.jpg"
            it[mimeType] = "image/jpeg"
            it[fileSize] = 2048L
            it[PostImageTable.thumbnailKey] = thumbnailKey
            it[PostImageTable.deleted] = deleted
            it[PostImageTable.deletedDate] = deletedDate
        }.value
    }

    fun writePhysicalFile(storageKey: String) {
        val path = Paths.get(uploadBasePath).resolve(storageKey)
        Files.createDirectories(path.parent)
        Files.write(path, "fake-image".toByteArray())
    }

    fun fileExists(storageKey: String): Boolean =
        Files.exists(Paths.get(uploadBasePath).resolve(storageKey))

    fun rowCount(imageId: Long): Long = transaction {
        PostImageTable.selectAll().where { PostImageTable.id eq imageId }.count()
    }

    fun runPurgeJob(runId: Long): BatchStatus {
        val params = JobParametersBuilder()
            .addString("inputDate", inputDate)
            .addLong("run.id", runId)
            .toJobParameters()
        return jobLauncher.run(deletedPostImagePurgeJob, params).status
    }

    beforeSpec {
        transaction { SchemaUtils.create(PostImageTable) }
    }

    afterSpec {
        transaction { SchemaUtils.drop(PostImageTable) }
    }

    afterEach {
        transaction { PostImageTable.deleteAll() }
        File(uploadBasePath).deleteRecursively()
    }

    test("cutoff 이전에 soft delete 된 이미지의 물리 파일과 행을 삭제한다") {
        val storageKey = "community/post-image/1/expired.jpg"
        val thumbnailKey = "community/thumbnail/1/thumb_expired.jpg"
        val imageId = insertPostImage(storageKey, thumbnailKey, deleted = true, deletedDate = beforeCutoff)
        writePhysicalFile(storageKey)
        writePhysicalFile(thumbnailKey)

        runPurgeJob(1L) shouldBe BatchStatus.COMPLETED

        fileExists(storageKey) shouldBe false
        fileExists(thumbnailKey) shouldBe false
        rowCount(imageId) shouldBe 0L
    }

    test("cutoff 이후 soft delete 된 이미지와 active 이미지는 파일·행을 보존한다") {
        val recentKey = "community/post-image/1/recent.jpg"
        val activeKey = "community/post-image/1/active.jpg"
        val recentId = insertPostImage(recentKey, thumbnailKey = null, deleted = true, deletedDate = afterCutoff)
        val activeId = insertPostImage(activeKey, thumbnailKey = null, deleted = false, deletedDate = null)
        writePhysicalFile(recentKey)
        writePhysicalFile(activeKey)

        runPurgeJob(2L) shouldBe BatchStatus.COMPLETED

        fileExists(recentKey) shouldBe true
        fileExists(activeKey) shouldBe true
        rowCount(recentId) shouldBe 1L
        rowCount(activeId) shouldBe 1L
    }

    test("물리 파일이 이미 없는 이미지도 실패 없이 행을 정리한다") {
        val storageKey = "community/post-image/1/missing.jpg"
        val imageId = insertPostImage(storageKey, thumbnailKey = null, deleted = true, deletedDate = beforeCutoff)
        // 물리 파일을 생성하지 않는다 (이미 없는 상태)

        runPurgeJob(3L) shouldBe BatchStatus.COMPLETED

        rowCount(imageId) shouldBe 0L
    }
})
