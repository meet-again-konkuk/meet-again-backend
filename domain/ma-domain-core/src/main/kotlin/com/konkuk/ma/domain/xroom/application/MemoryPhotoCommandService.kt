package com.konkuk.ma.domain.xroom.application

import com.konkuk.ma.domain.common.domain.file.PhotoFile
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
    private val xroomValidator: XroomValidator,
) {
    fun uploadPhoto(xroomId: Long, memoryId: Long, memberId: Long, photoFile: PhotoFile): Media {
        xroomValidator.validateOwnedMemory(xroomId, memoryId, memberId)
        mediaCommandRepository.softDeleteByMemory(memoryId, memberId)

        val newMedia = mediaProcessor.process(memoryId, photoFile)
        val mediaId = mediaCommandRepository.save(newMedia)
        return newMedia.toMedia(mediaId)
    }

    fun removePhoto(xroomId: Long, memoryId: Long, memberId: Long) {
        xroomValidator.validateOwnedMemory(xroomId, memoryId, memberId)
        mediaCommandRepository.softDeleteByMemory(memoryId, memberId)
    }
}
