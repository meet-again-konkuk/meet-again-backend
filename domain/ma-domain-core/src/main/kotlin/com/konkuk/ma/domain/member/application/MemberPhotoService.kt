package com.konkuk.ma.domain.member.application

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
    fun upload(memberId: Long, photoFile: PhotoFile) {
        delete(memberId)
        val processed = memberPhotoProcessor.process(memberId, photoFile)
        val newPhoto = NewPhoto.create(memberId, processed.filePath, photoFile.originalFileName, processed.thumbnailPath)
        memberPhotoRepository.save(newPhoto)
    }

    fun delete(memberId: Long) {
        val existing = memberPhotoRepository.findOne(memberId) ?: return
        memberPhotoProcessor.deleteFiles(existing)
        memberPhotoRepository.delete(memberId)
    }
}
