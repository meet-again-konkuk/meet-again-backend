package com.konkuk.ma.domain.auth.application

import com.konkuk.ma.domain.auth.domain.SmsVerificationValidator
import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.member.domain.PhoneNumber
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class EmailRecoveryService(
    private val smsVerificationValidator: SmsVerificationValidator,
    private val memberQueryRepository: MemberQueryRepository
) {
    fun findEmail(name: String, phone: String): Email {
        val phoneNumber = PhoneNumber(phone)
        smsVerificationValidator.validate(phoneNumber)
        val member = memberQueryRepository.findOne(name, phoneNumber)
        return member.email
    }
}
