package com.konkuk.ma.domain.xroom.dao

import com.konkuk.ma.config.DatabaseTest
import com.konkuk.ma.config.TestDatabaseConfig
import com.konkuk.ma.domain.xroom.domain.block.XroomBlockItem
import com.konkuk.ma.domain.xroom.entity.table.DdayBlockTable
import com.konkuk.ma.domain.xroom.entity.table.LongTextBlockTable
import com.konkuk.ma.domain.xroom.entity.table.MusicBlockTable
import com.konkuk.ma.domain.xroom.entity.table.PhotoBlockTable
import com.konkuk.ma.domain.xroom.entity.table.ShortTextBlockTable
import com.konkuk.ma.domain.xroom.entity.table.VideoBlockTable
import com.konkuk.ma.domain.xroom.entity.table.XroomBlockTable
import com.konkuk.ma.domain.xroom.fixture.XroomBlockFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate

@ContextConfiguration(classes = [TestDatabaseConfig::class, XroomBlockCommandDao::class])
@DatabaseTest
class XroomBlockCommandDaoTest(
    private val xroomBlockCommandDao: XroomBlockCommandDao,
) : FunSpec() {

    override fun extensions() = listOf(SpringExtension)

    init {
        beforeEach {
            SchemaUtils.create(
                XroomBlockTable,
                PhotoBlockTable,
                ShortTextBlockTable,
                LongTextBlockTable,
                MusicBlockTable,
                DdayBlockTable,
                VideoBlockTable,
            )
        }

        afterEach {
            SchemaUtils.drop(
                XroomBlockTable,
                PhotoBlockTable,
                ShortTextBlockTable,
                LongTextBlockTable,
                MusicBlockTable,
                DdayBlockTable,
                VideoBlockTable,
            )
        }

        context("save") {

            test("PHOTO 블록 저장 시 부모와 자식 테이블에 모두 INSERT된다") {
                val newBlock = XroomBlockFixture.newPhotoBlock(
                    item = XroomBlockItem.POLAROID_FRAME,
                    photoDate = LocalDate.of(2024, 3, 21),
                )

                val parentId = xroomBlockCommandDao.save(newBlock)

                parentId shouldNotBe null
                XroomBlockTable.selectAll().count() shouldBe 1L
                PhotoBlockTable.selectAll().count() shouldBe 1L
                val childRow = PhotoBlockTable.selectAll().single()
                childRow[PhotoBlockTable.id].value shouldBe parentId
                childRow[PhotoBlockTable.photoDate] shouldBe LocalDate.of(2024, 3, 21)
            }

            test("SHORT_TEXT 블록 저장 시 부모와 자식 테이블에 모두 INSERT된다") {
                val newBlock = XroomBlockFixture.newShortTextBlock(
                    item = XroomBlockItem.PLAIN_CARD,
                    content = "안녕",
                )

                val parentId = xroomBlockCommandDao.save(newBlock)

                ShortTextBlockTable.selectAll().count() shouldBe 1L
                val childRow = ShortTextBlockTable.selectAll().single()
                childRow[ShortTextBlockTable.id].value shouldBe parentId
                childRow[ShortTextBlockTable.content] shouldBe "안녕"
            }

            test("LONG_TEXT 블록 저장 시 부모와 자식 테이블에 모두 INSERT된다") {
                val newBlock = XroomBlockFixture.newLongTextBlock(
                    item = XroomBlockItem.LETTER_PAPER,
                    content = "긴 편지",
                )

                val parentId = xroomBlockCommandDao.save(newBlock)

                LongTextBlockTable.selectAll().count() shouldBe 1L
                val childRow = LongTextBlockTable.selectAll().single()
                childRow[LongTextBlockTable.id].value shouldBe parentId
                childRow[LongTextBlockTable.content] shouldBe "긴 편지"
            }

            test("MUSIC 블록 저장 시 부모와 자식 테이블에 모두 INSERT된다") {
                val newBlock = XroomBlockFixture.newMusicBlock(
                    musicUrl = "https://music.example.com/song.mp3",
                    title = "노래",
                    artist = "가수",
                )

                val parentId = xroomBlockCommandDao.save(newBlock)

                MusicBlockTable.selectAll().count() shouldBe 1L
                val childRow = MusicBlockTable.selectAll().single()
                childRow[MusicBlockTable.id].value shouldBe parentId
                childRow[MusicBlockTable.musicUrl] shouldBe "https://music.example.com/song.mp3"
                childRow[MusicBlockTable.title] shouldBe "노래"
                childRow[MusicBlockTable.artist] shouldBe "가수"
            }

            test("DDAY 블록 저장 시 부모와 자식 테이블에 모두 INSERT된다") {
                val newBlock = XroomBlockFixture.newDdayBlock(
                    anniversaryDate = LocalDate.of(2024, 1, 1),
                    label = "사귄지",
                )

                val parentId = xroomBlockCommandDao.save(newBlock)

                DdayBlockTable.selectAll().count() shouldBe 1L
                val childRow = DdayBlockTable.selectAll().single()
                childRow[DdayBlockTable.id].value shouldBe parentId
                childRow[DdayBlockTable.anniversaryDate] shouldBe LocalDate.of(2024, 1, 1)
                childRow[DdayBlockTable.label] shouldBe "사귄지"
            }

            test("VIDEO 블록 저장 시 부모와 자식 테이블에 모두 INSERT된다") {
                val newBlock = XroomBlockFixture.newVideoBlock(description = "영상 설명")

                val parentId = xroomBlockCommandDao.save(newBlock)

                VideoBlockTable.selectAll().count() shouldBe 1L
                val childRow = VideoBlockTable.selectAll().single()
                childRow[VideoBlockTable.id].value shouldBe parentId
                childRow[VideoBlockTable.description] shouldBe "영상 설명"
            }

            test("부모 테이블에 공통 속성이 정확히 저장된다") {
                val newBlock = XroomBlockFixture.newPhotoBlock(
                    xroomId = 42L,
                    item = XroomBlockItem.POLAROID_FRAME,
                    positionX = 100,
                    positionY = 200,
                    rotation = 45,
                )

                val parentId = xroomBlockCommandDao.save(newBlock)

                val parentRow = XroomBlockTable.selectAll().single()
                parentRow[XroomBlockTable.id].value shouldBe parentId
                parentRow[XroomBlockTable.xroomId] shouldBe 42L
                parentRow[XroomBlockTable.blockType] shouldBe "PHOTO"
                parentRow[XroomBlockTable.item] shouldBe "POLAROID_FRAME"
                parentRow[XroomBlockTable.positionX].toInt() shouldBe 100
                parentRow[XroomBlockTable.positionY].toInt() shouldBe 200
                parentRow[XroomBlockTable.rotation] shouldBe 45
            }
        }
    }
}
