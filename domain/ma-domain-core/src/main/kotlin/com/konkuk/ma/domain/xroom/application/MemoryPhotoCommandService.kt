package com.konkuk.ma.domain.xroom.application

import com.konkuk.ma.domain.common.domain.file.PhotoFile
import com.konkuk.ma.domain.xroom.domain.media.Media
import com.konkuk.ma.domain.xroom.domain.media.MediaProcessor
import com.konkuk.ma.domain.xroom.domain.media.NewMedia
import com.konkuk.ma.domain.xroom.domain.port.MediaCommandRepository
import com.konkuk.ma.domain.xroom.domain.port.MediaQueryRepository
import com.konkuk.ma.domain.xroom.domain.port.MemoryQueryRepository
import com.konkuk.ma.domain.xroom.domain.port.XroomQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class MemoryPhotoCommandService(
    private val xroomQueryRepository: XroomQueryRepository,
    private val memoryQueryRepository: MemoryQueryRepository,
    private val mediaQueryRepository: MediaQueryRepository,
    private val mediaCommandRepository: MediaCommandRepository,
    private val mediaProcessor: MediaProcessor,
) {
    fun uploadPhoto(xroomId: Long, memoryId: Long, memberId: Long, photoFile: PhotoFile): Media {
        validateOwnedMemory(xroomId, memoryId, memberId)

        replaceExistingPhoto(memoryId, memberId)
        val processed = mediaProcessor.process(memoryId, photoFile)
        val newMedia = NewMedia.create(
            memoryId = memoryId,
            storageKey = processed.storageKey,
            originalFilename = photoFile.originalFileName,
            mimeType = photoFile.mimeType,
            fileSize = photoFile.sizeInBytes,
            thumbnailKey = processed.thumbnailKey,
        )
        val mediaId = mediaCommandRepository.save(newMedia)
        return Media(
            id = mediaId,
            memoryId = memoryId,
            storageKey = processed.storageKey,
            originalFilename = photoFile.originalFileName,
            mimeType = photoFile.mimeType,
            fileSize = photoFile.sizeInBytes,
            thumbnailKey = processed.thumbnailKey,
        )
    }

    fun removePhoto(xroomId: Long, memoryId: Long, memberId: Long) {
        validateOwnedMemory(xroomId, memoryId, memberId)
        mediaCommandRepository.softDeleteByMemory(memoryId, memberId)
    }

    private fun validateOwnedMemory(xroomId: Long, memoryId: Long, memberId: Long) {
        val xroom = xroomQueryRepository.findOne(xroomId)
        xroom.validateOwnership(memberId)
        val memory = memoryQueryRepository.findOne(memoryId)
        memory.validateBelongsTo(xroomId)
    }

    private fun replaceExistingPhoto(memoryId: Long, memberId: Long) {
        mediaQueryRepository.findActiveByMemory(memoryId) ?: return
        mediaCommandRepository.softDeleteByMemory(memoryId, memberId)
    }
}
