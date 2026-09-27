package com.konkuk.ma.domain.auth.application

import com.konkuk.ma.domain.auth.domain.SmsVerificationValidator
import com.konkuk.ma.domain.auth.domain.port.PasswordEncryptor
import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.member.domain.PhoneNumber
import com.konkuk.ma.domain.member.domain.port.MemberCommandRepository
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PasswordResetService(
    private val smsVerificationValidator: SmsVerificationValidator,
    private val memberQueryRepository: MemberQueryRepository,
    private val passwordEncryptor: PasswordEncryptor,
    private val memberCommandRepository: MemberCommandRepository
) {
    fun resetPassword(email: String, name: String, phone: String, newPassword: String) {
        val phoneNumber = PhoneNumber(phone)
        smsVerificationValidator.validate(phoneNumber)
        val member = memberQueryRepository.findOne(Email(email), name, phoneNumber)
        val encodedPassword = passwordEncryptor.encode(newPassword)
        memberCommandRepository.updatePassword(member.id, encodedPassword)
    }
}
