package com.konkuk.ma.domain.xroom.application

import com.konkuk.ma.domain.common.domain.file.PhotoFile
import com.konkuk.ma.domain.common.domain.file.port.FileUrlResolver
import com.konkuk.ma.domain.xroom.application.result.MediaUploadResult
import com.konkuk.ma.domain.xroom.domain.XroomValidator
import com.konkuk.ma.domain.xroom.domain.media.Media
import com.konkuk.ma.domain.xroom.domain.media.MediaProcessor
import com.konkuk.ma.domain.xroom.domain.port.MediaCommandRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class MemoryPhotoCommandService(
    private val mediaCommandRepository: MediaCommandRepository,
    private val mediaProcessor: MediaProcessor,
    private val fileUrlResolver: FileUrlResolver,
    private val xroomValidator: XroomValidator,
) {
    fun uploadPhoto(xroomId: Long, memoryId: Long, memberId: Long, photoFile: PhotoFile): MediaUploadResult {
        xroomValidator.validateOwnedMemory(xroomId, memoryId, memberId)
        mediaCommandRepository.softDeleteByMemory(memoryId, memberId)

        val newMedia = mediaProcessor.process(memoryId, photoFile)
        val mediaId = mediaCommandRepository.save(newMedia)
        return toUploadResult(newMedia.toMedia(mediaId))
    }

    fun removePhoto(xroomId: Long, memoryId: Long, memberId: Long) {
        xroomValidator.validateOwnedMemory(xroomId, memoryId, memberId)
        mediaCommandRepository.softDeleteByMemory(memoryId, memberId)
    }

    private fun toUploadResult(media: Media): MediaUploadResult {
        return MediaUploadResult(
            mediaId = media.id,
            photoUrl = fileUrlResolver.resolve(media.storageKey),
            thumbnailUrl = media.thumbnailKey?.let { fileUrlResolver.resolve(it) },
        )
    }
}
