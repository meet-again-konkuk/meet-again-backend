package com.konkuk.ma.domain.xroom.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.common.domain.id.port.IdObfuscator
import com.konkuk.ma.domain.xroom.api.request.CreateDdayBlockRequest
import com.konkuk.ma.domain.xroom.api.request.CreateLongTextBlockRequest
import com.konkuk.ma.domain.xroom.api.request.CreateMusicBlockRequest
import com.konkuk.ma.domain.xroom.api.request.CreatePhotoBlockRequest
import com.konkuk.ma.domain.xroom.api.request.CreateShortTextBlockRequest
import com.konkuk.ma.domain.xroom.api.request.CreateVideoBlockRequest
import com.konkuk.ma.domain.xroom.api.request.CreateXroomBlockRequest
import com.konkuk.ma.domain.xroom.application.XroomBlockCommandService
import com.konkuk.ma.domain.xroom.domain.block.XroomBlockItem
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.pathVariables
import com.konkuk.ma.extension.postJson
import com.konkuk.ma.extension.requestBody
import com.konkuk.ma.extension.requestParts
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.vocabulary.blockAnniversaryDate
import com.konkuk.ma.vocabulary.blockDescription
import com.konkuk.ma.vocabulary.blockId
import com.konkuk.ma.vocabulary.blockIdPath
import com.konkuk.ma.vocabulary.blockItem
import com.konkuk.ma.vocabulary.blockLabel
import com.konkuk.ma.vocabulary.blockMusicArtist
import com.konkuk.ma.vocabulary.blockMusicTitle
import com.konkuk.ma.vocabulary.blockMusicUrl
import com.konkuk.ma.vocabulary.blockPhotoDate
import com.konkuk.ma.vocabulary.blockPositionX
import com.konkuk.ma.vocabulary.blockPositionY
import com.konkuk.ma.vocabulary.blockRotation
import com.konkuk.ma.vocabulary.blockContent
import com.konkuk.ma.vocabulary.blockType
import com.konkuk.ma.vocabulary.photoId
import com.konkuk.ma.vocabulary.photoIdPath
import com.konkuk.ma.vocabulary.photoIds
import com.konkuk.ma.vocabulary.photoPart
import com.konkuk.ma.vocabulary.photosPart
import com.konkuk.ma.vocabulary.videoId
import com.konkuk.ma.vocabulary.videoIdPath
import com.konkuk.ma.vocabulary.videoIds
import com.konkuk.ma.vocabulary.videoPart
import com.konkuk.ma.vocabulary.videosPart
import com.konkuk.ma.vocabulary.xroomIdPath
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import java.time.LocalDate

@WebMvcTest(XroomBlockCommandApi::class)
@BaseApiTest
class XroomBlockCommandApiTest(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper,
    private val idObfuscator: IdObfuscator,
    @MockkBean private val xroomBlockCommandService: XroomBlockCommandService,
) : FunSpec({

    val authEmail = "holeman@naver.com"

    fun encryptedXroomId(rawId: Long = 1L): String =
        idObfuscator.encode(ObfuscationType.XROOM, rawId)

    fun encryptedBlockId(rawId: Long = 10L): String =
        idObfuscator.encode(ObfuscationType.XROOM_BLOCK, rawId)

    fun encryptedPhotoId(rawId: Long = 100L): String =
        idObfuscator.encode(ObfuscationType.XROOM_BLOCK_PHOTO, rawId)

    fun encryptedVideoId(rawId: Long = 200L): String =
        idObfuscator.encode(ObfuscationType.XROOM_BLOCK_VIDEO, rawId)

    fun postBlock(xroomId: String, request: CreateXroomBlockRequest): ResultActionsDsl =
        mockMvc.postJson("/api/xrooms/{xroomId}/blocks", xroomId) {
            content = mapper.writeValueAsString(request)
        }

    test("PHOTO 블록 생성 API 문서화") {
        // Given
        val xroomId = encryptedXroomId()
        val request = CreatePhotoBlockRequest(
            item = XroomBlockItem.POLAROID_FRAME,
            positionX = 120,
            positionY = 80,
            rotation = 0,
            photoDate = LocalDate.of(2024, 3, 21),
        )
        every { xroomBlockCommandService.create(1L, authEmail, any()) } returns 10L

        // When & Then
        postBlock(xroomId, request)
            .andExpect { status { isCreated() } }
            .andDocument(
                "xroom-block/create-photo-block",
                pathVariables(xroomIdPath()),
                requestBody(
                    blockType(),
                    blockItem(),
                    blockPositionX(),
                    blockPositionY(),
                    blockRotation(),
                    blockPhotoDate() isOptional true,
                ),
                responseBody(blockId()),
            )
    }

    test("SHORT_TEXT 블록 생성 API 문서화") {
        // Given
        val xroomId = encryptedXroomId()
        val request = CreateShortTextBlockRequest(
            item = XroomBlockItem.PLAIN_CARD,
            positionX = 50,
            positionY = 60,
            rotation = 0,
            content = "안녕하세요",
        )
        every { xroomBlockCommandService.create(1L, authEmail, any()) } returns 10L

        // When & Then
        postBlock(xroomId, request)
            .andExpect { status { isCreated() } }
            .andDocument(
                "xroom-block/create-short-text-block",
                pathVariables(xroomIdPath()),
                requestBody(
                    blockType(),
                    blockItem(),
                    blockPositionX(),
                    blockPositionY(),
                    blockRotation(),
                    blockContent(),
                ),
                responseBody(blockId()),
            )
    }

    test("LONG_TEXT 블록 생성 API 문서화") {
        // Given
        val xroomId = encryptedXroomId()
        val request = CreateLongTextBlockRequest(
            item = XroomBlockItem.LETTER_PAPER,
            positionX = 30,
            positionY = 40,
            rotation = 0,
            content = "오랜만에 편지를 씁니다. 잘 지내고 있나요?",
        )
        every { xroomBlockCommandService.create(1L, authEmail, any()) } returns 10L

        // When & Then
        postBlock(xroomId, request)
            .andExpect { status { isCreated() } }
            .andDocument(
                "xroom-block/create-long-text-block",
                pathVariables(xroomIdPath()),
                requestBody(
                    blockType(),
                    blockItem(),
                    blockPositionX(),
                    blockPositionY(),
                    blockRotation(),
                    blockContent(),
                ),
                responseBody(blockId()),
            )
    }

    test("MUSIC 블록 생성 API 문서화") {
        // Given
        val xroomId = encryptedXroomId()
        val request = CreateMusicBlockRequest(
            item = XroomBlockItem.MUSIC_CARD,
            positionX = 70,
            positionY = 90,
            rotation = 0,
            musicUrl = "https://example.com/song.mp3",
            title = "Spring Day",
            artist = "BTS",
        )
        every { xroomBlockCommandService.create(1L, authEmail, any()) } returns 10L

        // When & Then
        postBlock(xroomId, request)
            .andExpect { status { isCreated() } }
            .andDocument(
                "xroom-block/create-music-block",
                pathVariables(xroomIdPath()),
                requestBody(
                    blockType(),
                    blockItem(),
                    blockPositionX(),
                    blockPositionY(),
                    blockRotation(),
                    blockMusicUrl(),
                    blockMusicTitle(),
                    blockMusicArtist() isOptional true,
                ),
                responseBody(blockId()),
            )
    }

    test("DDAY 블록 생성 API 문서화") {
        // Given
        val xroomId = encryptedXroomId()
        val request = CreateDdayBlockRequest(
            item = XroomBlockItem.DDAY_BADGE,
            positionX = 100,
            positionY = 100,
            rotation = 0,
            anniversaryDate = LocalDate.of(2023, 5, 1),
            label = "처음 만난 날",
        )
        every { xroomBlockCommandService.create(1L, authEmail, any()) } returns 10L

        // When & Then
        postBlock(xroomId, request)
            .andExpect { status { isCreated() } }
            .andDocument(
                "xroom-block/create-dday-block",
                pathVariables(xroomIdPath()),
                requestBody(
                    blockType(),
                    blockItem(),
                    blockPositionX(),
                    blockPositionY(),
                    blockRotation(),
                    blockAnniversaryDate(),
                    blockLabel(),
                ),
                responseBody(blockId()),
            )
    }

    test("VIDEO 블록 생성 API 문서화") {
        // Given
        val xroomId = encryptedXroomId()
        val request = CreateVideoBlockRequest(
            item = XroomBlockItem.VIDEO_FRAME,
            positionX = 110,
            positionY = 120,
            rotation = 0,
            description = "여름 휴가 영상",
        )
        every { xroomBlockCommandService.create(1L, authEmail, any()) } returns 10L

        // When & Then
        postBlock(xroomId, request)
            .andExpect { status { isCreated() } }
            .andDocument(
                "xroom-block/create-video-block",
                pathVariables(xroomIdPath()),
                requestBody(
                    blockType(),
                    blockItem(),
                    blockPositionX(),
                    blockPositionY(),
                    blockRotation(),
                    blockDescription() isOptional true,
                ),
                responseBody(blockId()),
            )
    }

    test("X룸 블록 사진 업로드 API 문서화") {
        // Given
        val blockId = encryptedBlockId()
        val photoFile1 = MockMultipartFile(
            "photos",
            "photo1.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "fake-image-1".toByteArray(),
        )
        val photoFile2 = MockMultipartFile(
            "photos",
            "photo2.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "fake-image-2".toByteArray(),
        )
        every {
            xroomBlockCommandService.uploadPhotos(10L, authEmail, any())
        } returns listOf(101L, 102L)

        // When & Then
        @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
        ResultActionsDsl(
            mockMvc.perform(
                RestDocumentationRequestBuilders.multipart("/api/xrooms/blocks/{blockId}/photos", blockId)
                    .file(photoFile1)
                    .file(photoFile2)
            ),
            mockMvc,
        )
            .andExpect { status { isCreated() } }
            .andDocument(
                "xroom-block/upload-photos",
                pathVariables(blockIdPath()),
                requestParts(photosPart()),
                responseBody(photoIds()),
            )
    }

    test("X룸 블록 영상 업로드 API 문서화") {
        // Given
        val blockId = encryptedBlockId()
        val videoFile = MockMultipartFile(
            "videos",
            "video1.mp4",
            "video/mp4",
            "fake-video-1".toByteArray(),
        )
        every {
            xroomBlockCommandService.uploadVideos(10L, authEmail, any())
        } returns listOf(201L)

        // When & Then
        @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
        ResultActionsDsl(
            mockMvc.perform(
                RestDocumentationRequestBuilders.multipart("/api/xrooms/blocks/{blockId}/videos", blockId)
                    .file(videoFile)
            ),
            mockMvc,
        )
            .andExpect { status { isCreated() } }
            .andDocument(
                "xroom-block/upload-videos",
                pathVariables(blockIdPath()),
                requestParts(videosPart()),
                responseBody(videoIds()),
            )
    }

    test("X룸 블록 사진 교체 API 문서화") {
        // Given
        val blockId = encryptedBlockId()
        val photoId = encryptedPhotoId()
        val newPhotoFile = MockMultipartFile(
            "photo",
            "new-photo.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "new-fake-image".toByteArray(),
        )
        every {
            xroomBlockCommandService.replacePhoto(any())
        } returns 100L

        // When & Then
        @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
        ResultActionsDsl(
            mockMvc.perform(
                RestDocumentationRequestBuilders.multipart(
                    "/api/xrooms/blocks/{blockId}/photos/{photoId}",
                    blockId,
                    photoId,
                )
                    .file(newPhotoFile)
                    .with { request ->
                        request.method = "PUT"
                        request
                    }
            ),
            mockMvc,
        )
            .andExpect { status { isOk() } }
            .andDocument(
                "xroom-block/replace-photo",
                pathVariables(blockIdPath(), photoIdPath()),
                requestParts(photoPart()),
                responseBody(photoId()),
            )
    }

    test("X룸 블록 영상 교체 API 문서화") {
        // Given
        val blockId = encryptedBlockId()
        val videoId = encryptedVideoId()
        val newVideoFile = MockMultipartFile(
            "video",
            "new-video.mp4",
            "video/mp4",
            "new-fake-video".toByteArray(),
        )
        every {
            xroomBlockCommandService.replaceVideo(any())
        } returns 200L

        // When & Then
        @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
        ResultActionsDsl(
            mockMvc.perform(
                RestDocumentationRequestBuilders.multipart(
                    "/api/xrooms/blocks/{blockId}/videos/{videoId}",
                    blockId,
                    videoId,
                )
                    .file(newVideoFile)
                    .with { request ->
                        request.method = "PUT"
                        request
                    }
            ),
            mockMvc,
        )
            .andExpect { status { isOk() } }
            .andDocument(
                "xroom-block/replace-video",
                pathVariables(blockIdPath(), videoIdPath()),
                requestParts(videoPart()),
                responseBody(videoId()),
            )
    }
})
