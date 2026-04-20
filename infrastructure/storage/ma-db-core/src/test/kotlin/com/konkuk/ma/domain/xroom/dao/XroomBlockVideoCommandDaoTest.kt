package com.konkuk.ma.domain.xroom.dao

import com.konkuk.ma.config.DatabaseTest
import com.konkuk.ma.config.TestDatabaseConfig
import com.konkuk.ma.domain.xroom.domain.block.NewVideo
import com.konkuk.ma.domain.xroom.entity.table.XroomBlockVideoTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [TestDatabaseConfig::class, XroomBlockVideoCommandDao::class])
@DatabaseTest
class XroomBlockVideoCommandDaoTest(
    private val xroomBlockVideoCommandDao: XroomBlockVideoCommandDao,
) : FunSpec() {

    override fun extensions() = listOf(SpringExtension)

    init {
        beforeEach {
            SchemaUtils.create(XroomBlockVideoTable)
        }

        afterEach {
            SchemaUtils.drop(XroomBlockVideoTable)
        }

        context("saveAll") {

            test("주어진 NewVideo 리스트의 orderIndex 그대로 저장한다") {
                val blockId = 200L
                val newVideos = listOf(
                    NewVideo("https://cdn.example.com/v1.mp4", 0),
                    NewVideo("https://cdn.example.com/v2.mp4", 1),
                )

                val savedIds = xroomBlockVideoCommandDao.saveAll(blockId, newVideos)

                savedIds shouldHaveSize 2
                val rows = XroomBlockVideoTable.selectAll().orderBy(XroomBlockVideoTable.orderIndex).toList()
                rows shouldHaveSize 2
                rows[0][XroomBlockVideoTable.orderIndex] shouldBe 0
                rows[0][XroomBlockVideoTable.videoUrl] shouldBe newVideos[0].videoUrl
                rows[0][XroomBlockVideoTable.blockId] shouldBe blockId
                rows[1][XroomBlockVideoTable.orderIndex] shouldBe 1
                rows[1][XroomBlockVideoTable.videoUrl] shouldBe newVideos[1].videoUrl
            }

            test("빈 리스트면 빈 ID 리스트를 반환하고 INSERT하지 않는다") {
                val savedIds = xroomBlockVideoCommandDao.saveAll(200L, emptyList())

                savedIds shouldHaveSize 0
                XroomBlockVideoTable.selectAll().count() shouldBe 0L
            }
        }

        context("replace") {

            test("videoId의 VIDEO_URL을 새로운 URL로 갱신한다") {
                val blockId = 200L
                val originalVideos = listOf(NewVideo("https://cdn.example.com/old.mp4", 0))
                val videoIds = xroomBlockVideoCommandDao.saveAll(blockId, originalVideos)
                val videoId = videoIds.first()
                val newVideo = NewVideo("https://cdn.example.com/new.mp4", 0)

                xroomBlockVideoCommandDao.replace(videoId, newVideo)

                val row = XroomBlockVideoTable.selectAll().single()
                row[XroomBlockVideoTable.videoUrl] shouldBe newVideo.videoUrl
                row[XroomBlockVideoTable.orderIndex] shouldBe 0
            }
        }
    }
}
