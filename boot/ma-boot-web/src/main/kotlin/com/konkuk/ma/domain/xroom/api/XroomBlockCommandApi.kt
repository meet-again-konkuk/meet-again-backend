package com.konkuk.ma.domain.xroom.api

import com.konkuk.ma.domain.common.domain.file.PhotoFile
import com.konkuk.ma.domain.common.domain.file.VideoFile
import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.xroom.api.request.CreateXroomBlockRequest
import com.konkuk.ma.domain.xroom.api.response.CreateXroomBlockResponse
import com.konkuk.ma.domain.xroom.api.response.ReplaceXroomBlockPhotoResponse
import com.konkuk.ma.domain.xroom.api.response.ReplaceXroomBlockVideoResponse
import com.konkuk.ma.domain.xroom.api.response.UploadXroomBlockPhotosResponse
import com.konkuk.ma.domain.xroom.api.response.UploadXroomBlockVideosResponse
import com.konkuk.ma.domain.xroom.application.ReplacePhotoCommand
import com.konkuk.ma.domain.xroom.application.ReplaceVideoCommand
import com.konkuk.ma.domain.xroom.application.XroomBlockCommandService
import com.konkuk.ma.support.id.DecryptId
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/xrooms")
class XroomBlockCommandApi(
    private val xroomBlockCommandService: XroomBlockCommandService,
) {
    @PostMapping("/{xroomId}/blocks")
    @ResponseStatus(HttpStatus.CREATED)
    fun createBlock(
        @AuthenticationPrincipal email: String,
        @PathVariable @DecryptId(ObfuscationType.XROOM) xroomId: Long,
        @RequestBody request: CreateXroomBlockRequest,
    ): CreateXroomBlockResponse {
        val newBlock = request.toCommand(xroomId)
        val blockId = xroomBlockCommandService.create(xroomId, email, newBlock)
        return CreateXroomBlockResponse(blockId = blockId)
    }

    @PostMapping("/blocks/{blockId}/photos", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun uploadPhotos(
        @AuthenticationPrincipal email: String,
        @PathVariable @DecryptId(ObfuscationType.XROOM_BLOCK) blockId: Long,
        @RequestPart("photos") photos: List<MultipartFile>,
    ): UploadXroomBlockPhotosResponse {
        val photoFiles = photos.map { PhotoFile.create(it.originalFilename, it.size, it.bytes) }
        val photoIds = xroomBlockCommandService.uploadPhotos(blockId, email, photoFiles)
        return UploadXroomBlockPhotosResponse(photoIds = photoIds)
    }

    @PostMapping("/blocks/{blockId}/videos", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun uploadVideos(
        @AuthenticationPrincipal email: String,
        @PathVariable @DecryptId(ObfuscationType.XROOM_BLOCK) blockId: Long,
        @RequestPart("videos") videos: List<MultipartFile>,
    ): UploadXroomBlockVideosResponse {
        val videoFiles = videos.map { VideoFile.create(it.originalFilename, it.size, it.bytes) }
        val videoIds = xroomBlockCommandService.uploadVideos(blockId, email, videoFiles)
        return UploadXroomBlockVideosResponse(videoIds = videoIds)
    }

    @PutMapping("/blocks/{blockId}/photos/{photoId}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun replacePhoto(
        @AuthenticationPrincipal email: String,
        @PathVariable @DecryptId(ObfuscationType.XROOM_BLOCK) blockId: Long,
        @PathVariable @DecryptId(ObfuscationType.XROOM_BLOCK_PHOTO) photoId: Long,
        @RequestPart("photo") photo: MultipartFile,
    ): ReplaceXroomBlockPhotoResponse {
        val photoFile = PhotoFile.create(photo.originalFilename, photo.size, photo.bytes)
        val command = ReplacePhotoCommand(blockId, photoId, email, photoFile)
        val replacedPhotoId = xroomBlockCommandService.replacePhoto(command)
        return ReplaceXroomBlockPhotoResponse(photoId = replacedPhotoId)
    }

    @PutMapping("/blocks/{blockId}/videos/{videoId}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun replaceVideo(
        @AuthenticationPrincipal email: String,
        @PathVariable @DecryptId(ObfuscationType.XROOM_BLOCK) blockId: Long,
        @PathVariable @DecryptId(ObfuscationType.XROOM_BLOCK_VIDEO) videoId: Long,
        @RequestPart("video") video: MultipartFile,
    ): ReplaceXroomBlockVideoResponse {
        val videoFile = VideoFile.create(video.originalFilename, video.size, video.bytes)
        val command = ReplaceVideoCommand(blockId, videoId, email, videoFile)
        val replacedVideoId = xroomBlockCommandService.replaceVideo(command)
        return ReplaceXroomBlockVideoResponse(videoId = replacedVideoId)
    }
}
