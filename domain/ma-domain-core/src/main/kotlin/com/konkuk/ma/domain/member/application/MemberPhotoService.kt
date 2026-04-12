package com.konkuk.ma.domain.member.application

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.common.domain.file.PhotoFile
import com.konkuk.ma.domain.member.domain.photo.MemberPhotoProcessor
import com.konkuk.ma.domain.member.domain.photo.NewPhoto
import com.konkuk.ma.domain.member.domain.port.MemberPhotoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class MemberPhotoService(
    private val memberPhotoProcessor: MemberPhotoProcessor,
    private val memberPhotoRepository: MemberPhotoRepository
) {
    fun upload(email: String, photoFile: PhotoFile) {
        val domainEmail = Email(email)
        delete(email)
        val processed = memberPhotoProcessor.process(domainEmail, photoFile)
        val newPhoto = NewPhoto.create(domainEmail, processed.filePath, photoFile.originalFileName, processed.thumbnailPath)
        memberPhotoRepository.save(newPhoto)
    }

    fun delete(email: String) {
        val domainEmail = Email(email)
        val existing = memberPhotoRepository.findOne(domainEmail) ?: return
        memberPhotoProcessor.deleteFiles(existing)
        memberPhotoRepository.delete(domainEmail)
    }
}
