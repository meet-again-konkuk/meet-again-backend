package com.konkuk.ma.domain.xroom.dao

import com.konkuk.ma.config.DatabaseTest
import com.konkuk.ma.config.TestDatabaseConfig
import com.konkuk.ma.domain.xroom.entity.table.XroomBlockPhotoTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insertAndGetId
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [TestDatabaseConfig::class, XroomBlockPhotoQueryDao::class])
@DatabaseTest
class XroomBlockPhotoQueryDaoTest(
    private val xroomBlockPhotoQueryDao: XroomBlockPhotoQueryDao,
) : FunSpec() {

    override fun extensions() = listOf(SpringExtension)

    private fun insertPhoto(
        blockId: Long = 100L,
        photoUrl: String = "https://cdn.example.com/photo.jpg",
        orderIndex: Int = 0,
    ): Long {
        return XroomBlockPhotoTable.insertAndGetId {
            it[XroomBlockPhotoTable.blockId] = blockId
            it[XroomBlockPhotoTable.photoUrl] = photoUrl
            it[XroomBlockPhotoTable.orderIndex] = orderIndex
        }.value
    }

    init {
        beforeEach {
            SchemaUtils.create(XroomBlockPhotoTable)
        }

        afterEach {
            SchemaUtils.drop(XroomBlockPhotoTable)
        }

        context("exists") {

            test("blockId의 사진이 존재하면 true를 반환한다") {
                val blockId = 100L
                insertPhoto(blockId = blockId)

                xroomBlockPhotoQueryDao.exists(blockId).shouldBeTrue()
            }

            test("blockId의 사진이 없으면 false를 반환한다") {
                xroomBlockPhotoQueryDao.exists(999L).shouldBeFalse()
            }

            test("soft-deleted 사진은 존재하지 않는 것으로 판단한다") {
                val blockId = 100L
                insertPhoto(blockId = blockId)
                XroomBlockPhotoTable.softDelete({ XroomBlockPhotoTable.blockId eq blockId }, "owner@example.com")

                xroomBlockPhotoQueryDao.exists(blockId).shouldBeFalse()
            }
        }

        context("findOne") {

            test("photoId가 존재하면 해당 엔티티를 반환한다") {
                val photoId = insertPhoto(
                    blockId = 100L,
                    photoUrl = "https://cdn.example.com/p.jpg",
                    orderIndex = 2,
                )

                val result = xroomBlockPhotoQueryDao.findOne(photoId)

                result shouldNotBe null
                result!!.id shouldBe photoId
                result.blockId shouldBe 100L
                result.photoUrl shouldBe "https://cdn.example.com/p.jpg"
                result.orderIndex shouldBe 2
            }

            test("photoId가 존재하지 않으면 null을 반환한다") {
                val result = xroomBlockPhotoQueryDao.findOne(999L)

                result shouldBe null
            }

            test("soft-deleted 사진은 null을 반환한다") {
                val photoId = insertPhoto()
                XroomBlockPhotoTable.softDelete({ XroomBlockPhotoTable.id eq photoId }, "owner@example.com")

                val result = xroomBlockPhotoQueryDao.findOne(photoId)

                result shouldBe null
            }
        }
    }
}
