package com.konkuk.ma.domain.community.domain.image

import com.konkuk.ma.domain.common.domain.file.ImageSignature
import com.konkuk.ma.domain.common.domain.file.PhotoFile
import com.konkuk.ma.domain.common.domain.file.StorageDomainType
import com.konkuk.ma.domain.common.domain.file.StoragePath
import com.konkuk.ma.domain.common.domain.file.StorageUsageType
import com.konkuk.ma.domain.common.domain.file.port.FileStorage
import com.konkuk.ma.domain.common.domain.file.port.ThumbnailGenerator
import com.konkuk.ma.exception.InvalidValueException
import com.konkuk.ma.logger
import java.nio.file.Paths
import org.springframework.stereotype.Component

@Component
class PostImageProcessor(
    private val fileStorage: FileStorage,
    private val thumbnailGenerator: ThumbnailGenerator,
) {

    fun process(postId: Long, photoFile: PhotoFile): NewPostImage {
        validateSupportedImage(photoFile)
        val storageKey = storeOriginal(postId, photoFile)
        val thumbnailKey = storeThumbnail(postId, photoFile)
        return NewPostImage.of(postId, photoFile, storageKey, thumbnailKey)
    }

    private fun validateSupportedImage(photoFile: PhotoFile) {
        if (!ImageSignature.matches(photoFile.content, photoFile.extension.normalized)) {
            throw InvalidValueException(
                PostImageProcessor::class,
                photoFile.originalFileName,
                "지원하지 않는 이미지 형식이거나 확장자와 실제 내용이 일치하지 않습니다.",
            )
        }
    }

    private fun storeOriginal(postId: Long, photoFile: PhotoFile): String {
        val directory = StoragePath.of(StorageDomainType.COMMUNITY, StorageUsageType.POST_IMAGE, postId).value
        val storedPath = fileStorage.store(directory, photoFile)
        return toRelativeKey(directory, storedPath)
    }

    private fun storeThumbnail(postId: Long, photoFile: PhotoFile): String? {
        return try {
            val thumbnailBytes = thumbnailGenerator.generate(photoFile.content, THUMBNAIL_WIDTH)
            val directory = StoragePath.of(StorageDomainType.COMMUNITY, StorageUsageType.THUMBNAIL, postId).value
            val fileName = "thumb_${photoFile.originalFileName}"
            fileStorage.storeBytes(directory, fileName, thumbnailBytes)
            "$directory/$fileName"
        } catch (e: Exception) {
            logger.warn { "썸네일 생성 실패 (postId=$postId): ${e.message}" }
            null
        }
    }

    private fun toRelativeKey(directory: String, storedPath: String): String {
        val fileName = Paths.get(storedPath).fileName.toString()
        return "$directory/$fileName"
    }

    companion object {
        private const val THUMBNAIL_WIDTH = 400
    }
}
