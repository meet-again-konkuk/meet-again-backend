package com.konkuk.ma.domain.member.application

import com.konkuk.ma.domain.member.domain.MemberProfile
import com.konkuk.ma.domain.member.domain.photo.MemberPhotoUrlResolver
import com.konkuk.ma.domain.member.domain.port.MemberPhotoRepository
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MemberProfileQueryService(
    private val memberQueryRepository: MemberQueryRepository,
    private val memberPhotoRepository: MemberPhotoRepository,
    private val memberPhotoUrlResolver: MemberPhotoUrlResolver,
) {
    fun findOne(memberId: Long): MemberProfile {
        val member = memberQueryRepository.findOne(memberId)
        val photo = memberPhotoRepository.findOne(memberId)
        return MemberProfile.of(member, photo?.let { memberPhotoUrlResolver.resolve(it) })
    }
}
