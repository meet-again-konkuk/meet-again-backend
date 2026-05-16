package com.konkuk.ma.domain.member.application

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.common.domain.file.PhotoFile
import com.konkuk.ma.domain.member.domain.photo.MemberPhotoCleaner
import com.konkuk.ma.domain.member.domain.photo.MemberPhotoProcessor
import com.konkuk.ma.domain.member.domain.photo.NewPhoto
import com.konkuk.ma.domain.member.domain.port.MemberPhotoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class MemberPhotoService(
    private val memberPhotoProcessor: MemberPhotoProcessor,
    private val memberPhotoRepository: MemberPhotoRepository,
    private val memberPhotoCleaner: MemberPhotoCleaner
) {
    fun upload(email: String, photoFile: PhotoFile) {
        val memberEmail = Email(email)
        memberPhotoCleaner.clean(memberEmail)
        val processed = memberPhotoProcessor.process(memberEmail, photoFile)
        val newPhoto = NewPhoto.create(memberEmail, processed.filePath, photoFile.originalFileName, processed.thumbnailPath)
        memberPhotoRepository.save(newPhoto)
    }

    fun delete(email: String) {
        memberPhotoCleaner.clean(Email(email))
    }
}
