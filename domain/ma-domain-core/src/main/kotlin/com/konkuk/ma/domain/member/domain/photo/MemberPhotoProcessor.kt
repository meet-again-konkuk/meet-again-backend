package com.konkuk.ma.domain.member.domain.photo

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
class MemberPhotoProcessor(
    private val fileStorage: FileStorage,
    private val thumbnailGenerator: ThumbnailGenerator
) {

    fun process(memberId: Long, photoFile: PhotoFile): ProcessedPhoto {
        val storageKey = storeOriginal(memberId, photoFile)
        val thumbnailKey = storeThumbnail(memberId, photoFile)
        return ProcessedPhoto(storageKey, thumbnailKey)
    }

    fun deleteFiles(photo: MemberPhoto) {
        fileStorage.deleteByKey(photo.storageKey)
        photo.thumbnailKey?.let { fileStorage.deleteByKey(it) }
    }

    private fun storeOriginal(memberId: Long, photoFile: PhotoFile): String {
        val directory = StoragePath.of(StorageDomainType.MEMBER, StorageUsageType.PROFILE, memberId).value
        val storedPath = fileStorage.store(directory, photoFile)
        return toRelativeKey(directory, storedPath)
    }

    private fun storeThumbnail(memberId: Long, photoFile: PhotoFile): String? {
        return try {
            val thumbnailBytes = thumbnailGenerator.generate(photoFile.content, THUMBNAIL_WIDTH)
            val directory = StoragePath.of(StorageDomainType.MEMBER, StorageUsageType.THUMBNAIL, memberId).value
            val fileName = "thumb_${photoFile.originalFileName}"
            fileStorage.storeBytes(directory, fileName, thumbnailBytes)
            "$directory/$fileName"
        } catch (e: Exception) {
            logger.warn { "썸네일 생성 실패 (memberId=$memberId): ${e.message}" }
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
