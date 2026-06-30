package com.konkuk.ma.domain.xroom.domain.media

import com.konkuk.ma.domain.common.domain.file.PhotoFile
import com.konkuk.ma.domain.common.domain.file.StorageDomainType
import com.konkuk.ma.domain.common.domain.file.StoragePath
import com.konkuk.ma.domain.common.domain.file.StorageUsageType
import com.konkuk.ma.domain.common.domain.file.port.FileStorage
import com.konkuk.ma.domain.common.domain.file.port.ThumbnailGenerator
import com.konkuk.ma.logger
import java.nio.file.Paths
import org.springframework.stereotype.Component

@Component
class MediaProcessor(
    private val fileStorage: FileStorage,
    private val thumbnailGenerator: ThumbnailGenerator,
) {

    fun process(memoryId: Long, photoFile: PhotoFile): ProcessedMedia {
        val storageKey = storeOriginal(memoryId, photoFile)
        val thumbnailKey = storeThumbnail(memoryId, photoFile)
        return ProcessedMedia(storageKey, thumbnailKey)
    }

    private fun storeOriginal(memoryId: Long, photoFile: PhotoFile): String {
        val directory = StoragePath.of(StorageDomainType.MEMORY, StorageUsageType.MEMORY_PHOTO, memoryId).value
        val storedPath = fileStorage.store(directory, photoFile)
        return toRelativeKey(directory, storedPath)
    }

    private fun storeThumbnail(memoryId: Long, photoFile: PhotoFile): String? {
        return try {
            val thumbnailBytes = thumbnailGenerator.generate(photoFile.content, THUMBNAIL_WIDTH)
            val directory = StoragePath.of(StorageDomainType.MEMORY, StorageUsageType.THUMBNAIL, memoryId).value
            val fileName = "thumb_${photoFile.originalFileName}"
            fileStorage.storeBytes(directory, fileName, thumbnailBytes)
            "$directory/$fileName"
        } catch (e: Exception) {
            logger.warn { "썸네일 생성 실패 (memoryId=$memoryId): ${e.message}" }
            null
        }
    }

    // FileStorage.store 가 반환한 절대경로에서 파일명만 추출해 DB에 저장할 상대 storageKey 로 만든다.
    private fun toRelativeKey(directory: String, storedPath: String): String {
        val fileName = Paths.get(storedPath).fileName.toString()
        return "$directory/$fileName"
    }

    companion object {
        private const val THUMBNAIL_WIDTH = 400
    }
}
