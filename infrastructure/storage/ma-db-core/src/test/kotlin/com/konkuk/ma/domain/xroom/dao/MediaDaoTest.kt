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
import org.jetbrains.exposed.sql.SchemaUtils
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
    }
}
