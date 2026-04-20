package com.konkuk.ma.domain.xroom.dao

import com.konkuk.ma.config.DatabaseTest
import com.konkuk.ma.config.TestDatabaseConfig
import com.konkuk.ma.domain.xroom.domain.block.XroomBlockItem
import com.konkuk.ma.domain.xroom.domain.block.XroomBlockType
import com.konkuk.ma.domain.xroom.entity.DdayBlockEntity
import com.konkuk.ma.domain.xroom.entity.LongTextBlockEntity
import com.konkuk.ma.domain.xroom.entity.MusicBlockEntity
import com.konkuk.ma.domain.xroom.entity.PhotoBlockEntity
import com.konkuk.ma.domain.xroom.entity.ShortTextBlockEntity
import com.konkuk.ma.domain.xroom.entity.VideoBlockEntity
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
import io.kotest.matchers.types.shouldBeInstanceOf
import org.jetbrains.exposed.sql.SchemaUtils
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate

@ContextConfiguration(
    classes = [
        TestDatabaseConfig::class,
        XroomBlockCommandDao::class,
        XroomBlockQueryDao::class,
        PhotoBlockEntityFactory::class,
        ShortTextBlockEntityFactory::class,
        LongTextBlockEntityFactory::class,
        MusicBlockEntityFactory::class,
        DdayBlockEntityFactory::class,
        VideoBlockEntityFactory::class,
    ]
)
@DatabaseTest
class XroomBlockQueryDaoTest(
    private val xroomBlockCommandDao: XroomBlockCommandDao,
    private val xroomBlockQueryDao: XroomBlockQueryDao,
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

        context("findOne") {

            test("PHOTO 블록을 정확히 매핑하여 반환한다") {
                val newBlock = XroomBlockFixture.newPhotoBlock(
                    xroomId = 1L,
                    item = XroomBlockItem.POLAROID_FRAME,
                    positionX = 50,
                    positionY = 80,
                    rotation = 30,
                    photoDate = LocalDate.of(2024, 5, 1),
                )
                val blockId = xroomBlockCommandDao.save(newBlock)

                val result = xroomBlockQueryDao.findOne(blockId)

                result.shouldBeInstanceOf<PhotoBlockEntity>()
                result.id shouldBe blockId
                result.xroomId shouldBe 1L
                result.item shouldBe XroomBlockItem.POLAROID_FRAME
                result.positionX shouldBe 50
                result.positionY shouldBe 80
                result.rotation shouldBe 30
                result.photoDate shouldBe LocalDate.of(2024, 5, 1)
            }

            test("SHORT_TEXT 블록을 정확히 매핑하여 반환한다") {
                val newBlock = XroomBlockFixture.newShortTextBlock(content = "짧은 글")
                val blockId = xroomBlockCommandDao.save(newBlock)

                val result = xroomBlockQueryDao.findOne(blockId)

                result.shouldBeInstanceOf<ShortTextBlockEntity>()
                result.content shouldBe "짧은 글"
            }

            test("LONG_TEXT 블록을 정확히 매핑하여 반환한다") {
                val newBlock = XroomBlockFixture.newLongTextBlock(content = "긴 편지 내용")
                val blockId = xroomBlockCommandDao.save(newBlock)

                val result = xroomBlockQueryDao.findOne(blockId)

                result.shouldBeInstanceOf<LongTextBlockEntity>()
                result.content shouldBe "긴 편지 내용"
            }

            test("MUSIC 블록을 정확히 매핑하여 반환한다") {
                val newBlock = XroomBlockFixture.newMusicBlock(
                    musicUrl = "https://music.example.com/song.mp3",
                    title = "노래제목",
                    artist = "가수명",
                )
                val blockId = xroomBlockCommandDao.save(newBlock)

                val result = xroomBlockQueryDao.findOne(blockId)

                result.shouldBeInstanceOf<MusicBlockEntity>()
                result.musicUrl shouldBe "https://music.example.com/song.mp3"
                result.title shouldBe "노래제목"
                result.artist shouldBe "가수명"
            }

            test("DDAY 블록을 정확히 매핑하여 반환한다") {
                val newBlock = XroomBlockFixture.newDdayBlock(
                    anniversaryDate = LocalDate.of(2023, 12, 25),
                    label = "크리스마스",
                )
                val blockId = xroomBlockCommandDao.save(newBlock)

                val result = xroomBlockQueryDao.findOne(blockId)

                result.shouldBeInstanceOf<DdayBlockEntity>()
                result.anniversaryDate shouldBe LocalDate.of(2023, 12, 25)
                result.label shouldBe "크리스마스"
            }

            test("VIDEO 블록을 정확히 매핑하여 반환한다") {
                val newBlock = XroomBlockFixture.newVideoBlock(description = "영상 설명")
                val blockId = xroomBlockCommandDao.save(newBlock)

                val result = xroomBlockQueryDao.findOne(blockId)

                result.shouldBeInstanceOf<VideoBlockEntity>()
                result.description shouldBe "영상 설명"
            }

            test("존재하지 않는 blockId면 null을 반환한다") {
                val result = xroomBlockQueryDao.findOne(999L)

                result shouldBe null
            }

            test("soft-deleted 블록은 null을 반환한다") {
                val newBlock = XroomBlockFixture.newPhotoBlock()
                val blockId = xroomBlockCommandDao.save(newBlock)
                XroomBlockTable.softDelete({ XroomBlockTable.id eq blockId }, "owner@example.com")

                val result = xroomBlockQueryDao.findOne(blockId)

                result shouldBe null
            }

            test("blockType 분기에 따라 올바른 sealed 자식 타입을 반환한다") {
                val photoId = xroomBlockCommandDao.save(XroomBlockFixture.newPhotoBlock())
                val shortTextId = xroomBlockCommandDao.save(XroomBlockFixture.newShortTextBlock())
                val musicId = xroomBlockCommandDao.save(XroomBlockFixture.newMusicBlock())

                xroomBlockQueryDao.findOne(photoId)?.shouldBeInstanceOf<PhotoBlockEntity>()
                xroomBlockQueryDao.findOne(shortTextId)?.shouldBeInstanceOf<ShortTextBlockEntity>()
                xroomBlockQueryDao.findOne(musicId)?.shouldBeInstanceOf<MusicBlockEntity>()
            }
        }
    }
}
