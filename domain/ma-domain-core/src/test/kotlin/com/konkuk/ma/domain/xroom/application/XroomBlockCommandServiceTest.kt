package com.konkuk.ma.domain.xroom.application

import com.konkuk.ma.domain.common.fixture.PhotoFileFixture
import com.konkuk.ma.domain.common.fixture.VideoFileFixture
import com.konkuk.ma.domain.xroom.domain.block.NewPhoto
import com.konkuk.ma.domain.xroom.domain.block.NewVideo
import com.konkuk.ma.domain.xroom.domain.block.XroomBlockItem
import com.konkuk.ma.domain.xroom.domain.block.XroomBlockValidator
import com.konkuk.ma.domain.xroom.domain.port.PhotoUploader
import com.konkuk.ma.domain.xroom.domain.port.VideoUploader
import com.konkuk.ma.domain.xroom.domain.port.XroomBlockCommandRepository
import com.konkuk.ma.domain.xroom.domain.port.XroomBlockPhotoCommandRepository
import com.konkuk.ma.domain.xroom.domain.port.XroomBlockPhotoQueryRepository
import com.konkuk.ma.domain.xroom.domain.port.XroomBlockQueryRepository
import com.konkuk.ma.domain.xroom.domain.port.XroomBlockVideoCommandRepository
import com.konkuk.ma.domain.xroom.domain.port.XroomBlockVideoQueryRepository
import com.konkuk.ma.domain.xroom.domain.port.XroomQueryRepository
import com.konkuk.ma.domain.xroom.fixture.XroomBlockDomainFixture
import com.konkuk.ma.domain.xroom.fixture.XroomBlockFixture
import com.konkuk.ma.domain.xroom.fixture.XroomFixture
import com.konkuk.ma.exception.AccessDeniedException
import com.konkuk.ma.exception.DuplicateException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs

class XroomBlockCommandServiceTest : FunSpec({

    val xroomQueryRepository = mockk<XroomQueryRepository>()
    val xroomBlockCommandRepository = mockk<XroomBlockCommandRepository>()
    val xroomBlockQueryRepository = mockk<XroomBlockQueryRepository>()
    val xroomBlockPhotoCommandRepository = mockk<XroomBlockPhotoCommandRepository>()
    val xroomBlockPhotoQueryRepository = mockk<XroomBlockPhotoQueryRepository>()
    val xroomBlockVideoCommandRepository = mockk<XroomBlockVideoCommandRepository>()
    val xroomBlockVideoQueryRepository = mockk<XroomBlockVideoQueryRepository>()
    val xroomBlockValidator = mockk<XroomBlockValidator>()
    val photoUploader = mockk<PhotoUploader>()
    val videoUploader = mockk<VideoUploader>()

    val xroomBlockCommandService = XroomBlockCommandService(
        xroomQueryRepository = xroomQueryRepository,
        xroomBlockCommandRepository = xroomBlockCommandRepository,
        xroomBlockQueryRepository = xroomBlockQueryRepository,
        xroomBlockPhotoCommandRepository = xroomBlockPhotoCommandRepository,
        xroomBlockPhotoQueryRepository = xroomBlockPhotoQueryRepository,
        xroomBlockVideoCommandRepository = xroomBlockVideoCommandRepository,
        xroomBlockVideoQueryRepository = xroomBlockVideoQueryRepository,
        xroomBlockValidator = xroomBlockValidator,
        photoUploader = photoUploader,
        videoUploader = videoUploader,
    )

    context("create") {

        test("소유자 검증 통과 후 블록을 저장하고 ID를 반환한다") {
            val xroom = XroomFixture.create(ownerEmail = "owner@example.com")
            val newBlock = XroomBlockFixture.newPhotoBlock(xroomId = xroom.id)
            val expectedBlockId = 100L
            every { xroomQueryRepository.findOne(xroom.id) } returns xroom
            every { xroomBlockValidator.validate(newBlock) } just runs
            every { xroomBlockCommandRepository.save(newBlock) } returns expectedBlockId

            val result = xroomBlockCommandService.create(xroom.id, xroom.ownerEmail.value, newBlock)

            result shouldBe expectedBlockId
        }

        test("X룸 소유자가 아니면 AccessDeniedException이 발생한다") {
            val xroom = XroomFixture.create(ownerEmail = "owner@example.com")
            val newBlock = XroomBlockFixture.newPhotoBlock(xroomId = xroom.id)
            every { xroomQueryRepository.findOne(xroom.id) } returns xroom

            shouldThrow<AccessDeniedException> {
                xroomBlockCommandService.create(xroom.id, "other@example.com", newBlock)
            }
        }
    }

    context("uploadPhotos") {

        test("PHOTO 블록에 정확한 사진 수를 업로드하면 photoIds를 반환한다") {
            val block = XroomBlockDomainFixture.photoBlock(item = XroomBlockItem.POLAROID_FRAME)
            val email = "owner@example.com"
            val photoFile = PhotoFileFixture.create()
            val files = listOf(photoFile)
            val urls = listOf("https://cdn.example.com/photo.jpg")
            val expectedNewPhotos = listOf(NewPhoto("https://cdn.example.com/photo.jpg", 0))
            val expectedPhotoIds = listOf(1L)
            every { xroomBlockQueryRepository.findOne(block.id) } returns block
            every { xroomBlockValidator.validateBlockOwnership(block, email) } just runs
            every { xroomBlockValidator.validatePhotosNotUploaded(block.id) } just runs
            every { photoUploader.upload(files) } returns urls
            every {
                xroomBlockPhotoCommandRepository.saveAll(block.id, match { matchesNewPhotos(it, expectedNewPhotos) })
            } returns expectedPhotoIds

            val result = xroomBlockCommandService.uploadPhotos(block.id, email, files)

            result shouldBe expectedPhotoIds
        }

        test("PHOTO 블록이 아니면 InvalidValueException이 발생한다") {
            val block = XroomBlockDomainFixture.shortTextBlock()
            val email = "owner@example.com"
            val files = listOf(PhotoFileFixture.create())
            every { xroomBlockQueryRepository.findOne(block.id) } returns block
            every { xroomBlockValidator.validateBlockOwnership(block, email) } just runs

            shouldThrow<com.konkuk.ma.exception.InvalidValueException> {
                xroomBlockCommandService.uploadPhotos(block.id, email, files)
            }
        }

        test("item.maxPhotoCount와 일치하지 않으면 InvalidValueException이 발생한다") {
            val block = XroomBlockDomainFixture.photoBlock(item = XroomBlockItem.POLAROID_FRAME)
            val email = "owner@example.com"
            val files = listOf(PhotoFileFixture.create(), PhotoFileFixture.create())
            every { xroomBlockQueryRepository.findOne(block.id) } returns block
            every { xroomBlockValidator.validateBlockOwnership(block, email) } just runs

            shouldThrow<com.konkuk.ma.exception.InvalidValueException> {
                xroomBlockCommandService.uploadPhotos(block.id, email, files)
            }
        }

        test("이미 사진이 업로드된 블록이면 DuplicateException이 발생한다") {
            val block = XroomBlockDomainFixture.photoBlock(item = XroomBlockItem.POLAROID_FRAME)
            val email = "owner@example.com"
            val files = listOf(PhotoFileFixture.create())
            every { xroomBlockQueryRepository.findOne(block.id) } returns block
            every { xroomBlockValidator.validateBlockOwnership(block, email) } just runs
            every {
                xroomBlockValidator.validatePhotosNotUploaded(block.id)
            } throws DuplicateException(com.konkuk.ma.exception.EntityType.XROOM_BLOCK_PHOTO, "blockId", block.id.toString())

            shouldThrow<DuplicateException> {
                xroomBlockCommandService.uploadPhotos(block.id, email, files)
            }
        }

        test("X룸 소유자가 아니면 AccessDeniedException이 발생한다") {
            val block = XroomBlockDomainFixture.photoBlock()
            val files = listOf(PhotoFileFixture.create())
            every { xroomBlockQueryRepository.findOne(block.id) } returns block
            every {
                xroomBlockValidator.validateBlockOwnership(block, "other@example.com")
            } throws AccessDeniedException(
                com.konkuk.ma.exception.EntityType.XROOM,
                "owner@example.com",
                "other@example.com",
            )

            shouldThrow<AccessDeniedException> {
                xroomBlockCommandService.uploadPhotos(block.id, "other@example.com", files)
            }
        }
    }

    context("uploadVideos") {

        test("VIDEO 블록에 정확한 영상 수를 업로드하면 videoIds를 반환한다") {
            val block = XroomBlockDomainFixture.videoBlock(item = XroomBlockItem.VIDEO_FRAME)
            val email = "owner@example.com"
            val videoFile = VideoFileFixture.create()
            val files = listOf(videoFile)
            val urls = listOf("https://cdn.example.com/video.mp4")
            val expectedNewVideos = listOf(NewVideo("https://cdn.example.com/video.mp4", 0))
            val expectedVideoIds = listOf(2L)
            every { xroomBlockQueryRepository.findOne(block.id) } returns block
            every { xroomBlockValidator.validateBlockOwnership(block, email) } just runs
            every { xroomBlockValidator.validateVideosNotUploaded(block.id) } just runs
            every { videoUploader.upload(files) } returns urls
            every {
                xroomBlockVideoCommandRepository.saveAll(block.id, match { matchesNewVideos(it, expectedNewVideos) })
            } returns expectedVideoIds

            val result = xroomBlockCommandService.uploadVideos(block.id, email, files)

            result shouldBe expectedVideoIds
        }

        test("VIDEO 블록이 아니면 InvalidValueException이 발생한다") {
            val block = XroomBlockDomainFixture.shortTextBlock()
            val email = "owner@example.com"
            val files = listOf(VideoFileFixture.create())
            every { xroomBlockQueryRepository.findOne(block.id) } returns block
            every { xroomBlockValidator.validateBlockOwnership(block, email) } just runs

            shouldThrow<com.konkuk.ma.exception.InvalidValueException> {
                xroomBlockCommandService.uploadVideos(block.id, email, files)
            }
        }

        test("item.maxVideoCount와 일치하지 않으면 InvalidValueException이 발생한다") {
            val block = XroomBlockDomainFixture.videoBlock(item = XroomBlockItem.VIDEO_FRAME)
            val email = "owner@example.com"
            val files = listOf(VideoFileFixture.create(), VideoFileFixture.create())
            every { xroomBlockQueryRepository.findOne(block.id) } returns block
            every { xroomBlockValidator.validateBlockOwnership(block, email) } just runs

            shouldThrow<com.konkuk.ma.exception.InvalidValueException> {
                xroomBlockCommandService.uploadVideos(block.id, email, files)
            }
        }

        test("이미 영상이 업로드된 블록이면 DuplicateException이 발생한다") {
            val block = XroomBlockDomainFixture.videoBlock(item = XroomBlockItem.VIDEO_FRAME)
            val email = "owner@example.com"
            val files = listOf(VideoFileFixture.create())
            every { xroomBlockQueryRepository.findOne(block.id) } returns block
            every { xroomBlockValidator.validateBlockOwnership(block, email) } just runs
            every {
                xroomBlockValidator.validateVideosNotUploaded(block.id)
            } throws DuplicateException(com.konkuk.ma.exception.EntityType.XROOM_BLOCK_VIDEO, "blockId", block.id.toString())

            shouldThrow<DuplicateException> {
                xroomBlockCommandService.uploadVideos(block.id, email, files)
            }
        }
    }

    context("replacePhoto") {

        test("정상 흐름이면 photoId를 반환한다") {
            val block = XroomBlockDomainFixture.photoBlock()
            val photo = XroomBlockDomainFixture.photo(blockId = block.id)
            val photoFile = PhotoFileFixture.create()
            val email = "owner@example.com"
            val newUrl = "https://cdn.example.com/new.jpg"
            val command = ReplacePhotoCommand(block.id, photo.id, email, photoFile)
            every { xroomBlockQueryRepository.findOne(block.id) } returns block
            every { xroomBlockValidator.validateBlockOwnership(block, email) } just runs
            every { xroomBlockPhotoQueryRepository.findOne(photo.id) } returns photo
            every { photoUploader.upload(photoFile) } returns newUrl
            every {
                xroomBlockPhotoCommandRepository.replace(photo.id, match { it.photoUrl == newUrl && it.orderIndex == photo.orderIndex })
            } just runs

            val result = xroomBlockCommandService.replacePhoto(command)

            result shouldBe photo.id
        }

        test("photo가 다른 블록 소속이면 AccessDeniedException이 발생한다") {
            val block = XroomBlockDomainFixture.photoBlock(id = 100L)
            val photo = XroomBlockDomainFixture.photo(blockId = 999L)
            val photoFile = PhotoFileFixture.create()
            val email = "owner@example.com"
            val command = ReplacePhotoCommand(block.id, photo.id, email, photoFile)
            every { xroomBlockQueryRepository.findOne(block.id) } returns block
            every { xroomBlockValidator.validateBlockOwnership(block, email) } just runs
            every { xroomBlockPhotoQueryRepository.findOne(photo.id) } returns photo

            shouldThrow<AccessDeniedException> {
                xroomBlockCommandService.replacePhoto(command)
            }
        }

        test("X룸 소유자가 아니면 AccessDeniedException이 발생한다") {
            val block = XroomBlockDomainFixture.photoBlock()
            val photo = XroomBlockDomainFixture.photo(blockId = block.id)
            val photoFile = PhotoFileFixture.create()
            val command = ReplacePhotoCommand(block.id, photo.id, "other@example.com", photoFile)
            every { xroomBlockQueryRepository.findOne(block.id) } returns block
            every {
                xroomBlockValidator.validateBlockOwnership(block, "other@example.com")
            } throws AccessDeniedException(
                com.konkuk.ma.exception.EntityType.XROOM,
                "owner@example.com",
                "other@example.com",
            )

            shouldThrow<AccessDeniedException> {
                xroomBlockCommandService.replacePhoto(command)
            }
        }
    }

    context("replaceVideo") {

        test("정상 흐름이면 videoId를 반환한다") {
            val block = XroomBlockDomainFixture.videoBlock()
            val video = XroomBlockDomainFixture.video(blockId = block.id)
            val videoFile = VideoFileFixture.create()
            val email = "owner@example.com"
            val newUrl = "https://cdn.example.com/new.mp4"
            val command = ReplaceVideoCommand(block.id, video.id, email, videoFile)
            every { xroomBlockQueryRepository.findOne(block.id) } returns block
            every { xroomBlockValidator.validateBlockOwnership(block, email) } just runs
            every { xroomBlockVideoQueryRepository.findOne(video.id) } returns video
            every { videoUploader.upload(videoFile) } returns newUrl
            every {
                xroomBlockVideoCommandRepository.replace(video.id, match { it.videoUrl == newUrl && it.orderIndex == video.orderIndex })
            } just runs

            val result = xroomBlockCommandService.replaceVideo(command)

            result shouldBe video.id
        }

        test("video가 다른 블록 소속이면 AccessDeniedException이 발생한다") {
            val block = XroomBlockDomainFixture.videoBlock(id = 100L)
            val video = XroomBlockDomainFixture.video(blockId = 999L)
            val videoFile = VideoFileFixture.create()
            val email = "owner@example.com"
            val command = ReplaceVideoCommand(block.id, video.id, email, videoFile)
            every { xroomBlockQueryRepository.findOne(block.id) } returns block
            every { xroomBlockValidator.validateBlockOwnership(block, email) } just runs
            every { xroomBlockVideoQueryRepository.findOne(video.id) } returns video

            shouldThrow<AccessDeniedException> {
                xroomBlockCommandService.replaceVideo(command)
            }
        }
    }
})

private fun matchesNewPhotos(actual: List<NewPhoto>, expected: List<NewPhoto>): Boolean {
    if (actual.size != expected.size) return false
    return actual.zip(expected).all { (a, e) -> a.photoUrl == e.photoUrl && a.orderIndex == e.orderIndex }
}

private fun matchesNewVideos(actual: List<NewVideo>, expected: List<NewVideo>): Boolean {
    if (actual.size != expected.size) return false
    return actual.zip(expected).all { (a, e) -> a.videoUrl == e.videoUrl && a.orderIndex == e.orderIndex }
}
