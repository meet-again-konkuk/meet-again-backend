package com.konkuk.ma.domain.member.domain.photo

import com.konkuk.ma.domain.common.domain.file.PhotoFile
import com.konkuk.ma.domain.common.domain.file.StorageDomainType
import com.konkuk.ma.domain.common.domain.file.StoragePath
import com.konkuk.ma.domain.common.domain.file.StorageUsageType
import com.konkuk.ma.domain.common.domain.file.port.FileStorage
import com.konkuk.ma.domain.common.domain.file.port.ThumbnailGenerator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class MemberPhotoProcessor(
    private val fileStorage: FileStorage,
    private val thumbnailGenerator: ThumbnailGenerator
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun process(email: String, photoFile: PhotoFile): ProcessedPhoto {
        val filePath = storeOriginal(email, photoFile)
        val thumbnailPath = storeThumbnail(email, photoFile)
        return ProcessedPhoto(filePath, thumbnailPath)
    }

    fun deleteFile(filePath: String) {
        fileStorage.delete(filePath)
    }

    private fun storeOriginal(email: String, photoFile: PhotoFile): String {
        val directory = StoragePath.of(StorageDomainType.MEMBER, StorageUsageType.PROFILE, email)
        return fileStorage.store(directory.value, photoFile)
    }

    private fun storeThumbnail(email: String, photoFile: PhotoFile): String? {
        return try {
            val thumbnailBytes = thumbnailGenerator.generate(photoFile.content, THUMBNAIL_WIDTH)
            val directory = StoragePath.of(StorageDomainType.MEMBER, StorageUsageType.THUMBNAIL, email)
            fileStorage.storeBytes(directory.value, "thumb_${photoFile.originalFileName}", thumbnailBytes)
        } catch (e: Exception) {
            log.warn("썸네일 생성 실패 (email={}): {}", email, e.message)
            null
        }
    }

    companion object {
        private const val THUMBNAIL_WIDTH = 400
    }
}
