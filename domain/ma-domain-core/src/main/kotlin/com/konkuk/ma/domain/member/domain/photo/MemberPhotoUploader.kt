package com.konkuk.ma.domain.member.domain.photo

import com.konkuk.ma.domain.common.domain.file.PhotoFile
import com.konkuk.ma.domain.common.domain.file.StorageDomainType
import com.konkuk.ma.domain.common.domain.file.StoragePath
import com.konkuk.ma.domain.common.domain.file.StorageUsageType
import com.konkuk.ma.domain.common.domain.file.port.FileStorage
import com.konkuk.ma.domain.member.domain.port.MemberPhotoRepository
import org.springframework.stereotype.Component

@Component
class MemberPhotoUploader(
    private val fileStorage: FileStorage,
    private val memberPhotoRepository: MemberPhotoRepository
) {
    fun upload(email: String, photoFile: PhotoFile?) {
        photoFile ?: return
        deleteExisting(email)

        val directory = StoragePath.of(StorageDomainType.MEMBER, StorageUsageType.PROFILE, email)
        val filePath = fileStorage.store(directory.value, photoFile)
        val newPhoto = NewPhoto.create(email, filePath, photoFile.originalFileName)
        memberPhotoRepository.save(newPhoto)
    }

    private fun deleteExisting(email: String) {
        val existing = memberPhotoRepository.findByMemberEmail(email) ?: return
        fileStorage.delete(existing.filePath)
        memberPhotoRepository.deleteByMemberEmail(email)
    }
}
