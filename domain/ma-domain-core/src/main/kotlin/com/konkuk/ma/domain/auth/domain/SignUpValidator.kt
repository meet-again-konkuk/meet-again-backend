package com.konkuk.ma.domain.auth.domain

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.member.domain.NewMember
import com.konkuk.ma.domain.member.domain.PhoneNumber
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import com.konkuk.ma.exception.DuplicateException
import com.konkuk.ma.exception.EntityType
import org.springframework.stereotype.Component

@Component
class SignUpValidator(
    private val memberQueryRepository: MemberQueryRepository,
    private val smsVerificationValidator: SmsVerificationValidator,
) {
    fun validate(newMember: NewMember) {
        checkDuplicatedNickname(newMember.nickname)
        checkDuplicatedEmail(newMember.email)
        checkDuplicatedPhoneNumber(newMember.phoneNumber)
        smsVerificationValidator.validate(newMember.phoneNumber)
    }

    private fun checkDuplicatedNickname(nickname: String) {
        if (memberQueryRepository.existsByNickname(nickname)) {
            throw DuplicateException(EntityType.MEMBER, "nickname", nickname)
        }
    }

    private fun checkDuplicatedEmail(email: Email) {
        if (memberQueryRepository.exists(email)) {
            throw DuplicateException(EntityType.MEMBER, "email", email.value)
        }
    }

    private fun checkDuplicatedPhoneNumber(phoneNumber: PhoneNumber) {
        if (memberQueryRepository.exists(phoneNumber)) {
            throw DuplicateException(EntityType.MEMBER, "phoneNumber", phoneNumber.fullNumber)
        }
    }
}
