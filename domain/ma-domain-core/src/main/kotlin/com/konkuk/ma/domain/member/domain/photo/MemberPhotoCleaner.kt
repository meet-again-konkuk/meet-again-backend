package com.konkuk.ma.domain.member.domain.photo

import com.konkuk.ma.domain.member.domain.port.MemberPhotoRepository
import org.springframework.stereotype.Component

@Component
class MemberPhotoCleaner(
    private val memberPhotoProcessor: MemberPhotoProcessor,
    private val memberPhotoRepository: MemberPhotoRepository
) {
    fun clean(memberId: Long) {
        val existing = memberPhotoRepository.findOne(memberId) ?: return
        memberPhotoProcessor.deleteFiles(existing)
        memberPhotoRepository.delete(memberId)
    }
}
