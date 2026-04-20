package com.konkuk.ma.domain.xroom.dao

import com.konkuk.ma.config.DatabaseTest
import com.konkuk.ma.config.TestDatabaseConfig
import com.konkuk.ma.domain.xroom.domain.block.NewPhoto
import com.konkuk.ma.domain.xroom.entity.table.XroomBlockPhotoTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [TestDatabaseConfig::class, XroomBlockPhotoCommandDao::class])
@DatabaseTest
class XroomBlockPhotoCommandDaoTest(
    private val xroomBlockPhotoCommandDao: XroomBlockPhotoCommandDao,
) : FunSpec() {

    override fun extensions() = listOf(SpringExtension)

    init {
        beforeEach {
            SchemaUtils.create(XroomBlockPhotoTable)
        }

        afterEach {
            SchemaUtils.drop(XroomBlockPhotoTable)
        }

        context("saveAll") {

            test("주어진 NewPhoto 리스트의 orderIndex 그대로 저장한다") {
                val blockId = 100L
                val newPhotos = listOf(
                    NewPhoto("https://cdn.example.com/p1.jpg", 0),
                    NewPhoto("https://cdn.example.com/p2.jpg", 1),
                    NewPhoto("https://cdn.example.com/p3.jpg", 2),
                )

                val savedIds = xroomBlockPhotoCommandDao.saveAll(blockId, newPhotos)

                savedIds shouldHaveSize 3
                val rows = XroomBlockPhotoTable.selectAll().orderBy(XroomBlockPhotoTable.orderIndex).toList()
                rows shouldHaveSize 3
                rows[0][XroomBlockPhotoTable.orderIndex] shouldBe 0
                rows[0][XroomBlockPhotoTable.photoUrl] shouldBe newPhotos[0].photoUrl
                rows[0][XroomBlockPhotoTable.blockId] shouldBe blockId
                rows[1][XroomBlockPhotoTable.orderIndex] shouldBe 1
                rows[1][XroomBlockPhotoTable.photoUrl] shouldBe newPhotos[1].photoUrl
                rows[2][XroomBlockPhotoTable.orderIndex] shouldBe 2
                rows[2][XroomBlockPhotoTable.photoUrl] shouldBe newPhotos[2].photoUrl
            }

            test("빈 리스트면 빈 ID 리스트를 반환하고 INSERT하지 않는다") {
                val savedIds = xroomBlockPhotoCommandDao.saveAll(100L, emptyList())

                savedIds shouldHaveSize 0
                XroomBlockPhotoTable.selectAll().count() shouldBe 0L
            }

            test("단일 NewPhoto도 정상 저장된다") {
                val blockId = 100L
                val newPhotos = listOf(NewPhoto("https://cdn.example.com/single.jpg", 0))

                xroomBlockPhotoCommandDao.saveAll(blockId, newPhotos)

                val row = XroomBlockPhotoTable.selectAll().single()
                row[XroomBlockPhotoTable.orderIndex] shouldBe 0
            }
        }

        context("replace") {

            test("photoId의 PHOTO_URL을 새로운 URL로 갱신한다") {
                val blockId = 100L
                val originalPhotos = listOf(NewPhoto("https://cdn.example.com/old.jpg", 0))
                val photoIds = xroomBlockPhotoCommandDao.saveAll(blockId, originalPhotos)
                val photoId = photoIds.first()
                val newPhoto = NewPhoto("https://cdn.example.com/new.jpg", 0)

                xroomBlockPhotoCommandDao.replace(photoId, newPhoto)

                val row = XroomBlockPhotoTable.selectAll().single()
                row[XroomBlockPhotoTable.photoUrl] shouldBe newPhoto.photoUrl
                row[XroomBlockPhotoTable.orderIndex] shouldBe 0
            }
        }
    }
}
