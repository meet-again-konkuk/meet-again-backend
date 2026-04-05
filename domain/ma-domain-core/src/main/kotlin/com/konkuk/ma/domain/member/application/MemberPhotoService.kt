package com.konkuk.ma.domain.member.application

import com.konkuk.ma.domain.common.domain.file.PhotoFile
import com.konkuk.ma.domain.common.domain.file.StorageDomainType
import com.konkuk.ma.domain.common.domain.file.StoragePath
import com.konkuk.ma.domain.common.domain.file.StorageUsageType
import com.konkuk.ma.domain.common.domain.file.port.FileStorage
import com.konkuk.ma.domain.common.domain.file.port.ThumbnailGenerator
import com.konkuk.ma.domain.member.domain.photo.NewPhoto
import com.konkuk.ma.domain.member.domain.port.MemberPhotoRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class MemberPhotoService(
    private val fileStorage: FileStorage,
    private val thumbnailGenerator: ThumbnailGenerator,
    private val memberPhotoRepository: MemberPhotoRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun upload(email: String, photoFile: PhotoFile) {
        delete(email)
        val directory = StoragePath.of(StorageDomainType.MEMBER, StorageUsageType.PROFILE, email)
        val filePath = fileStorage.store(directory.value, photoFile)
        val thumbnailPath = generateThumbnail(email, photoFile)
        val newPhoto = NewPhoto.create(email, filePath, photoFile.originalFileName, thumbnailPath)
        memberPhotoRepository.save(newPhoto)
    }

    fun delete(email: String) {
        val existing = memberPhotoRepository.findByMemberEmail(email) ?: return
        fileStorage.delete(existing.filePath)
        if (existing.hasThumbnail()) {
            fileStorage.delete(existing.thumbnailPath!!)
        }
        memberPhotoRepository.deleteByMemberEmail(email)
    }

    private fun generateThumbnail(email: String, photoFile: PhotoFile): String? {
        return try {
            val thumbnailBytes = thumbnailGenerator.generate(photoFile.content, THUMBNAIL_WIDTH)
            val thumbnailDirectory = StoragePath.of(StorageDomainType.MEMBER, StorageUsageType.THUMBNAIL, email)
            val thumbnailFileName = "thumb_${photoFile.originalFileName}"
            fileStorage.storeBytes(thumbnailDirectory.value, thumbnailFileName, thumbnailBytes)
        } catch (e: Exception) {
            log.warn("썸네일 생성 실패 (email={}): {}", email, e.message)
            null
        }
    }

    companion object {
        private const val THUMBNAIL_WIDTH = 400
    }
}
