package com.konkuk.ma.domain.auth.domain

import com.konkuk.ma.domain.auth.domain.port.SmsRepository
import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.member.domain.NewMember
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import com.konkuk.ma.domain.member.exception.DuplicateEmailException
import com.konkuk.ma.domain.member.exception.DuplicateNicknameException
import com.konkuk.ma.domain.member.exception.SmsNotVerifiedException
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
            throw DuplicateNicknameException(nickname)
        }
    }

    private fun checkDuplicatedEmail(email: Email) {
        if (memberQueryRepository.existsByEmail(email)) {
            throw DuplicateEmailException(email)
        }
    }

    private fun checkSmsVerification(phoneNumber: String) {
        if (!smsRepository.getConfirmed(phoneNumber)) {
            throw SmsNotVerifiedException(phoneNumber)
        }
    }
}
