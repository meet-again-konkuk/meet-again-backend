package com.konkuk.ma.domain.member.domain.photo

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

    fun process(memberId: Long, photoFile: PhotoFile): ProcessedPhoto {
        val filePath = storeOriginal(memberId, photoFile)
        val thumbnailPath = storeThumbnail(memberId, photoFile)
        return ProcessedPhoto(filePath, thumbnailPath)
    }

    fun deleteFiles(photo: MemberPhoto) {
        fileStorage.delete(photo.filePath)
        if (photo.hasThumbnail()) {
            fileStorage.delete(photo.thumbnailPath!!)
        }
    }

    private fun storeOriginal(memberId: Long, photoFile: PhotoFile): String {
        val directory = StoragePath.of(StorageDomainType.MEMBER, StorageUsageType.PROFILE, memberId)
        return fileStorage.store(directory.value, photoFile)
    }

    private fun storeThumbnail(memberId: Long, photoFile: PhotoFile): String? {
        return try {
            val thumbnailBytes = thumbnailGenerator.generate(photoFile.content, THUMBNAIL_WIDTH)
            val directory = StoragePath.of(StorageDomainType.MEMBER, StorageUsageType.THUMBNAIL, memberId)
            fileStorage.storeBytes(directory.value, "thumb_${photoFile.originalFileName}", thumbnailBytes)
        } catch (e: Exception) {
            logger.warn { "썸네일 생성 실패 (memberId=$memberId): ${e.message}" }
            null
        }
    }

    companion object {
        private const val THUMBNAIL_WIDTH = 400
    }
}
