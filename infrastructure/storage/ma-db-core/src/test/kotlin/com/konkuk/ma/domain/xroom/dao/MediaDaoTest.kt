package com.konkuk.ma.domain.xroom.dao

import com.konkuk.ma.config.DatabaseTest
import com.konkuk.ma.config.TestDatabaseConfig
import com.konkuk.ma.domain.xroom.domain.media.NewMedia
import com.konkuk.ma.domain.xroom.entity.table.MemoryMediaTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.LocalDateTime
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [TestDatabaseConfig::class, MediaCommandDao::class, MediaQueryDao::class])
@DatabaseTest
class MediaDaoTest(
    private val mediaCommandDao: MediaCommandDao,
    private val mediaQueryDao: MediaQueryDao,
) : FunSpec() {

    override fun extensions() = listOf(SpringExtension)

    init {
        beforeEach {
            SchemaUtils.create(MemoryMediaTable)
        }

        afterEach {
            SchemaUtils.drop(MemoryMediaTable)
        }

        fun newMedia(
            memoryId: Long = 1L,
            storageKey: String = "memory/memory-photo/1/photo.jpg",
            originalFilename: String = "photo.jpg",
            mimeType: String = "image/jpeg",
            fileSize: Long = 2048L,
            thumbnailKey: String? = "memory/thumbnail/1/thumb_photo.jpg",
        ) = NewMedia(
            memoryId = memoryId,
            storageKey = storageKey,
            originalFilename = originalFilename,
            mimeType = mimeType,
            fileSize = fileSize,
            thumbnailKey = thumbnailKey,
        )

        context("save") {

            test("미디어를 저장하고 ID를 반환한다") {
                val id = mediaCommandDao.save(newMedia())

                id shouldNotBe null
                id shouldBe 1L
            }

            test("저장한 값이 그대로 적재되어 조회 시 반영된다") {
                val media = newMedia()

                val id = mediaCommandDao.save(media)

                val found = mediaQueryDao.findActiveByMemories(setOf(media.memoryId)).first()
                found.id shouldBe id
                found.storageKey shouldBe media.storageKey
                found.originalFilename shouldBe media.originalFilename
                found.mimeType shouldBe media.mimeType
                found.fileSize shouldBe media.fileSize
                found.thumbnailKey shouldBe media.thumbnailKey
            }
        }

        context("findActiveByMemories") {

            test("빈 Set이면 빈 리스트를 반환한다") {
                mediaCommandDao.save(newMedia(memoryId = 1L))

                mediaQueryDao.findActiveByMemories(emptySet()).shouldBeEmpty()
            }

            test("여러 memoryId의 미디어를 벌크 조회한다") {
                mediaCommandDao.save(newMedia(memoryId = 1L))
                mediaCommandDao.save(newMedia(memoryId = 2L))
                mediaCommandDao.save(newMedia(memoryId = 3L))

                val found = mediaQueryDao.findActiveByMemories(setOf(1L, 2L))

                found shouldHaveSize 2
                found.map { it.memoryId }.toSet() shouldBe setOf(1L, 2L)
            }

            test("soft-deleted 미디어는 벌크 조회에서 제외된다") {
                mediaCommandDao.save(newMedia(memoryId = 1L))
                mediaCommandDao.save(newMedia(memoryId = 2L))
                mediaCommandDao.softDeleteByMemory(memoryId = 2L, memberId = 1L)

                val found = mediaQueryDao.findActiveByMemories(setOf(1L, 2L))

                found shouldHaveSize 1
                found.first().memoryId shouldBe 1L
            }
        }

        context("softDeleteByMemory") {

            test("deleted=true·deletedBy=memberId·deletedDate가 채워지고 이후 조회에서 제외된다") {
                val memberId = 7L
                val media = newMedia()
                mediaCommandDao.save(media)

                mediaCommandDao.softDeleteByMemory(media.memoryId, memberId)

                val row = MemoryMediaTable.selectAll()
                    .where { MemoryMediaTable.memoryId eq media.memoryId }
                    .single()
                row[MemoryMediaTable.deleted] shouldBe true
                row[MemoryMediaTable.deletedBy] shouldBe memberId.toString()
                row[MemoryMediaTable.deletedDate] shouldNotBe null

                mediaQueryDao.findActiveByMemories(setOf(media.memoryId)).shouldBeEmpty()
            }
        }

        context("softDeleteByMemories") {

            test("대상 기억들의 미디어만 soft delete 하고 다른 기억 미디어는 남긴다") {
                val memberId = 7L
                mediaCommandDao.save(newMedia(memoryId = 1L))
                mediaCommandDao.save(newMedia(memoryId = 2L))
                mediaCommandDao.save(newMedia(memoryId = 3L)) // 대상 외 기억

                mediaCommandDao.softDeleteByMemories(setOf(1L, 2L), memberId)

                mediaQueryDao.findActiveByMemories(setOf(1L, 2L)).shouldBeEmpty()
                val row = MemoryMediaTable.selectAll()
                    .where { MemoryMediaTable.memoryId eq 1L }
                    .single()
                row[MemoryMediaTable.deleted] shouldBe true
                row[MemoryMediaTable.deletedBy] shouldBe memberId.toString()
                mediaQueryDao.findActiveByMemories(setOf(3L)) shouldHaveSize 1
            }

            test("빈 Set이 입력되면 아무 미디어도 삭제하지 않는다") {
                mediaCommandDao.save(newMedia(memoryId = 1L))

                mediaCommandDao.softDeleteByMemories(emptySet(), memberId = 1L)

                mediaQueryDao.findActiveByMemories(setOf(1L)) shouldHaveSize 1
            }
        }

        // deletedDate를 특정 시각으로 제어해야 하므로 softDelete 헬퍼(now 사용) 대신 직접 삽입한다.
        fun insertDeletedMedia(
            storageKey: String = "memory/memory-photo/1/photo.jpg",
            thumbnailKey: String? = "memory/thumbnail/1/thumb_photo.jpg",
            deletedDate: LocalDateTime,
        ): Long = MemoryMediaTable.insertAndGetId {
            it[memoryId] = 1L
            it[MemoryMediaTable.storageKey] = storageKey
            it[originalFilename] = "photo.jpg"
            it[mimeType] = "image/jpeg"
            it[fileSize] = 2048L
            it[MemoryMediaTable.thumbnailKey] = thumbnailKey
            it[deleted] = true
            it[MemoryMediaTable.deletedDate] = deletedDate
        }.value

        context("findDeletedBefore") {

            val cutoff = LocalDateTime.of(2026, 6, 10, 0, 0)
            val beforeCutoff = LocalDateTime.of(2026, 6, 1, 0, 0)
            val afterCutoff = LocalDateTime.of(2026, 6, 15, 0, 0)

            test("cutoff 이전에 soft delete된 미디어만 반환한다 (active·cutoff 이후 제외)") {
                mediaCommandDao.save(newMedia()) // active → 제외
                val expiredId = insertDeletedMedia(deletedDate = beforeCutoff)
                insertDeletedMedia(deletedDate = afterCutoff) // cutoff 이후 → 제외

                val found = mediaQueryDao.findDeletedBefore(cutoff, null, 100)

                found shouldHaveSize 1
                found.first().id shouldBe expiredId
            }

            test("id ASC 순으로 정렬해 반환한다") {
                val firstId = insertDeletedMedia(deletedDate = beforeCutoff)
                val secondId = insertDeletedMedia(deletedDate = beforeCutoff)
                val thirdId = insertDeletedMedia(deletedDate = beforeCutoff)

                val found = mediaQueryDao.findDeletedBefore(cutoff, null, 100)

                found.map { it.id } shouldBe listOf(firstId, secondId, thirdId)
            }

            test("cursorId 이후의 미디어만 반환한다") {
                val firstId = insertDeletedMedia(deletedDate = beforeCutoff)
                val secondId = insertDeletedMedia(deletedDate = beforeCutoff)
                val thirdId = insertDeletedMedia(deletedDate = beforeCutoff)

                val found = mediaQueryDao.findDeletedBefore(cutoff, firstId, 100)

                found.map { it.id } shouldBe listOf(secondId, thirdId)
            }

            test("pageSize만큼만 반환한다") {
                val firstId = insertDeletedMedia(deletedDate = beforeCutoff)
                val secondId = insertDeletedMedia(deletedDate = beforeCutoff)
                insertDeletedMedia(deletedDate = beforeCutoff)

                val found = mediaQueryDao.findDeletedBefore(cutoff, null, 2)

                found.map { it.id } shouldBe listOf(firstId, secondId)
            }

            test("반환된 Entity에 storageKey와 thumbnailKey가 매핑된다") {
                val storageKey = "memory/memory-photo/9/keepsake.jpg"
                val thumbnailKey = "memory/thumbnail/9/thumb_keepsake.jpg"
                insertDeletedMedia(storageKey = storageKey, thumbnailKey = thumbnailKey, deletedDate = beforeCutoff)

                val found = mediaQueryDao.findDeletedBefore(cutoff, null, 100).first()

                found.storageKey shouldBe storageKey
                found.thumbnailKey shouldBe thumbnailKey
            }
        }

        context("delete") {

            test("해당 미디어 행을 물리 삭제한다") {
                val id = mediaCommandDao.save(newMedia())

                mediaCommandDao.delete(id)

                MemoryMediaTable.selectAll()
                    .where { MemoryMediaTable.id eq id }
                    .count() shouldBe 0L
            }

            test("지정한 미디어만 삭제하고 다른 미디어는 유지한다") {
                val targetId = mediaCommandDao.save(newMedia(memoryId = 1L))
                val survivorId = mediaCommandDao.save(newMedia(memoryId = 2L))

                mediaCommandDao.delete(targetId)

                MemoryMediaTable.selectAll()
                    .where { MemoryMediaTable.id eq targetId }
                    .count() shouldBe 0L
                MemoryMediaTable.selectAll()
                    .where { MemoryMediaTable.id eq survivorId }
                    .count() shouldBe 1L
            }
        }
    }
}
