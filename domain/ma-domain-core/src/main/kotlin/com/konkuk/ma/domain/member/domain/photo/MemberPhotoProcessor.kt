package com.konkuk.ma.domain.member.domain.photo

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.common.domain.file.PhotoFile
import com.konkuk.ma.domain.common.domain.file.StorageDomainType
import com.konkuk.ma.domain.common.domain.file.StoragePath
import com.konkuk.ma.domain.common.domain.file.StorageUsageType
import com.konkuk.ma.domain.common.domain.file.port.FileStorage
import com.konkuk.ma.domain.common.domain.file.port.ThumbnailGenerator
import com.konkuk.ma.logger
import org.springframework.stereotype.Component

@Component
class MemberPhotoProcessor(
    private val fileStorage: FileStorage,
    private val thumbnailGenerator: ThumbnailGenerator
) {

    fun process(email: Email, photoFile: PhotoFile): ProcessedPhoto {
        val filePath = storeOriginal(email, photoFile)
        val thumbnailPath = storeThumbnail(email, photoFile)
        return ProcessedPhoto(filePath, thumbnailPath)
    }

    fun deleteFiles(photo: MemberPhoto) {
        fileStorage.delete(photo.filePath)
        if (photo.hasThumbnail()) {
            fileStorage.delete(photo.thumbnailPath!!)
        }
    }

    private fun storeOriginal(email: Email, photoFile: PhotoFile): String {
        val directory = StoragePath.of(StorageDomainType.MEMBER, StorageUsageType.PROFILE, email.value)
        return fileStorage.store(directory.value, photoFile)
    }

    private fun storeThumbnail(email: Email, photoFile: PhotoFile): String? {
        return try {
            val thumbnailBytes = thumbnailGenerator.generate(photoFile.content, THUMBNAIL_WIDTH)
            val directory = StoragePath.of(StorageDomainType.MEMBER, StorageUsageType.THUMBNAIL, email.value)
            fileStorage.storeBytes(directory.value, "thumb_${photoFile.originalFileName}", thumbnailBytes)
        } catch (e: Exception) {
            logger.warn { "썸네일 생성 실패 (email=${email.value}): ${e.message}" }
            null
        }
    }

    companion object {
        private const val THUMBNAIL_WIDTH = 400
    }
}
