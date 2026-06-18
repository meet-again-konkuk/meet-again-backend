package com.konkuk.ma.domain.member.domain.photo

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.member.domain.port.MemberPhotoRepository
import org.springframework.stereotype.Component

@Component
class MemberPhotoCleaner(
    private val memberPhotoProcessor: MemberPhotoProcessor,
    private val memberPhotoRepository: MemberPhotoRepository
) {
    fun clean(email: Email) {
        val existing = memberPhotoRepository.findOne(email) ?: return
        memberPhotoProcessor.deleteFiles(existing)
        memberPhotoRepository.delete(email)
    }
}
