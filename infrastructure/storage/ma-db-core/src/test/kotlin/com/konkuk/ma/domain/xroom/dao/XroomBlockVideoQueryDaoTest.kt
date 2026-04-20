package com.konkuk.ma.domain.xroom.dao

import com.konkuk.ma.config.DatabaseTest
import com.konkuk.ma.config.TestDatabaseConfig
import com.konkuk.ma.domain.xroom.entity.table.XroomBlockVideoTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insertAndGetId
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [TestDatabaseConfig::class, XroomBlockVideoQueryDao::class])
@DatabaseTest
class XroomBlockVideoQueryDaoTest(
    private val xroomBlockVideoQueryDao: XroomBlockVideoQueryDao,
) : FunSpec() {

    override fun extensions() = listOf(SpringExtension)

    private fun insertVideo(
        blockId: Long = 200L,
        videoUrl: String = "https://cdn.example.com/video.mp4",
        orderIndex: Int = 0,
    ): Long {
        return XroomBlockVideoTable.insertAndGetId {
            it[XroomBlockVideoTable.blockId] = blockId
            it[XroomBlockVideoTable.videoUrl] = videoUrl
            it[XroomBlockVideoTable.orderIndex] = orderIndex
        }.value
    }

    init {
        beforeEach {
            SchemaUtils.create(XroomBlockVideoTable)
        }

        afterEach {
            SchemaUtils.drop(XroomBlockVideoTable)
        }

        context("exists") {

            test("blockId의 영상이 존재하면 true를 반환한다") {
                val blockId = 200L
                insertVideo(blockId = blockId)

                xroomBlockVideoQueryDao.exists(blockId).shouldBeTrue()
            }

            test("blockId의 영상이 없으면 false를 반환한다") {
                xroomBlockVideoQueryDao.exists(999L).shouldBeFalse()
            }

            test("soft-deleted 영상은 존재하지 않는 것으로 판단한다") {
                val blockId = 200L
                insertVideo(blockId = blockId)
                XroomBlockVideoTable.softDelete({ XroomBlockVideoTable.blockId eq blockId }, "owner@example.com")

                xroomBlockVideoQueryDao.exists(blockId).shouldBeFalse()
            }
        }

        context("findOne") {

            test("videoId가 존재하면 해당 엔티티를 반환한다") {
                val videoId = insertVideo(
                    blockId = 200L,
                    videoUrl = "https://cdn.example.com/v.mp4",
                    orderIndex = 1,
                )

                val result = xroomBlockVideoQueryDao.findOne(videoId)

                result shouldNotBe null
                result!!.id shouldBe videoId
                result.blockId shouldBe 200L
                result.videoUrl shouldBe "https://cdn.example.com/v.mp4"
                result.orderIndex shouldBe 1
            }

            test("videoId가 존재하지 않으면 null을 반환한다") {
                val result = xroomBlockVideoQueryDao.findOne(999L)

                result shouldBe null
            }

            test("soft-deleted 영상은 null을 반환한다") {
                val videoId = insertVideo()
                XroomBlockVideoTable.softDelete({ XroomBlockVideoTable.id eq videoId }, "owner@example.com")

                val result = xroomBlockVideoQueryDao.findOne(videoId)

                result shouldBe null
            }
        }
    }
}
