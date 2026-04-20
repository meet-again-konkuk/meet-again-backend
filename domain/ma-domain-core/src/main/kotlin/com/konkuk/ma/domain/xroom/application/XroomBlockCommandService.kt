package com.konkuk.ma.domain.xroom.application

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.common.domain.file.PhotoFile
import com.konkuk.ma.domain.common.domain.file.VideoFile
import com.konkuk.ma.domain.xroom.domain.block.NewPhoto
import com.konkuk.ma.domain.xroom.domain.block.NewVideo
import com.konkuk.ma.domain.xroom.domain.block.NewXroomBlock
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
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class XroomBlockCommandService(
    private val xroomQueryRepository: XroomQueryRepository,
    private val xroomBlockCommandRepository: XroomBlockCommandRepository,
    private val xroomBlockQueryRepository: XroomBlockQueryRepository,
    private val xroomBlockPhotoCommandRepository: XroomBlockPhotoCommandRepository,
    private val xroomBlockPhotoQueryRepository: XroomBlockPhotoQueryRepository,
    private val xroomBlockVideoCommandRepository: XroomBlockVideoCommandRepository,
    private val xroomBlockVideoQueryRepository: XroomBlockVideoQueryRepository,
    private val xroomBlockValidator: XroomBlockValidator,
    private val photoUploader: PhotoUploader,
    private val videoUploader: VideoUploader,
) {
    fun create(xroomId: Long, email: String, newBlock: NewXroomBlock): Long {
        val xroom = xroomQueryRepository.findOne(xroomId)
        xroom.validateOwnership(Email(email))
        xroomBlockValidator.validate(newBlock)
        return xroomBlockCommandRepository.save(newBlock)
    }

    fun uploadPhotos(blockId: Long, email: String, photoFiles: List<PhotoFile>): List<Long> {
        val block = xroomBlockQueryRepository.findOne(blockId)
        xroomBlockValidator.validateBlockOwnership(block, email)
        block.validatePhotoCount(photoFiles.size)
        xroomBlockValidator.validatePhotosNotUploaded(blockId)
        val photoUrls = photoUploader.upload(photoFiles)
        val newPhotos = photoUrls.mapIndexed { index, url -> NewPhoto(url, index) }
        return xroomBlockPhotoCommandRepository.saveAll(blockId, newPhotos)
    }

    fun uploadVideos(blockId: Long, email: String, videoFiles: List<VideoFile>): List<Long> {
        val block = xroomBlockQueryRepository.findOne(blockId)
        xroomBlockValidator.validateBlockOwnership(block, email)
        block.validateVideoCount(videoFiles.size)
        xroomBlockValidator.validateVideosNotUploaded(blockId)
        val videoUrls = videoUploader.upload(videoFiles)
        val newVideos = videoUrls.mapIndexed { index, url -> NewVideo(url, index) }
        return xroomBlockVideoCommandRepository.saveAll(blockId, newVideos)
    }

    fun replacePhoto(command: ReplacePhotoCommand): Long {
        val block = xroomBlockQueryRepository.findOne(command.blockId)
        xroomBlockValidator.validateBlockOwnership(block, command.email)
        val photo = xroomBlockPhotoQueryRepository.findOne(command.photoId)
        photo.validateBelongsTo(command.blockId)
        val newUrl = photoUploader.upload(command.photoFile)
        xroomBlockPhotoCommandRepository.replace(command.photoId, NewPhoto(newUrl, photo.orderIndex))
        return command.photoId
    }

    fun replaceVideo(command: ReplaceVideoCommand): Long {
        val block = xroomBlockQueryRepository.findOne(command.blockId)
        xroomBlockValidator.validateBlockOwnership(block, command.email)
        val video = xroomBlockVideoQueryRepository.findOne(command.videoId)
        video.validateBelongsTo(command.blockId)
        val newUrl = videoUploader.upload(command.videoFile)
        xroomBlockVideoCommandRepository.replace(command.videoId, NewVideo(newUrl, video.orderIndex))
        return command.videoId
    }
}
