package com.konkuk.ma.domain.member.application

import com.konkuk.ma.domain.member.domain.MemberProfile
import com.konkuk.ma.domain.member.domain.MemberValidator
import com.konkuk.ma.domain.member.domain.ProfileChanges
import com.konkuk.ma.domain.member.domain.photo.MemberPhotoUrlResolver
import com.konkuk.ma.domain.member.domain.port.MemberCommandRepository
import com.konkuk.ma.domain.member.domain.port.MemberPhotoRepository
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class MemberProfileCommandService(
    private val memberQueryRepository: MemberQueryRepository,
    private val memberCommandRepository: MemberCommandRepository,
    private val memberPhotoRepository: MemberPhotoRepository,
    private val memberPhotoUrlResolver: MemberPhotoUrlResolver,
    private val memberValidator: MemberValidator,
) {
    fun update(memberId: Long, changes: ProfileChanges): MemberProfile {
        val member = memberQueryRepository.findOne(memberId)
        memberValidator.validateNicknameAvailable(member, changes.nickname)
        member.changeProfile(changes)
        memberCommandRepository.updateProfile(member)
        val photo = memberPhotoRepository.findOne(memberId)
        return MemberProfile.of(member, photo?.let { memberPhotoUrlResolver.resolve(it) })
    }
}
