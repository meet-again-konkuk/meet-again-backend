package com.konkuk.ma.domain.auth.domain

import com.konkuk.ma.domain.auth.domain.port.SmsRepository
import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.member.domain.NewMember
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import com.konkuk.ma.domain.member.exception.SmsNotVerifiedException
import com.konkuk.ma.exception.DuplicateException
import com.konkuk.ma.exception.EntityType
import org.springframework.stereotype.Component

@Component
class SignUpValidator(
    private val memberQueryRepository: MemberQueryRepository,
    private val smsRepository: SmsRepository,
) {
    fun validate(newMember: NewMember) {
        checkDuplicatedNickname(newMember.nickname)
        checkDuplicatedEmail(newMember.email)
        checkSmsVerification(newMember.phoneNumber.fullNumber)
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

    private fun checkSmsVerification(phoneNumber: String) {
        if (!smsRepository.getConfirmed(phoneNumber)) {
            throw SmsNotVerifiedException(phoneNumber)
        }
    }
}
