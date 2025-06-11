package com.konkuk.ma.member.domain

import com.konkuk.ma.auth.domain.port.SmsRepository
import org.springframework.stereotype.Component

@Component
class MemberValidator(
    private val memberQueryRepository: MemberQueryRepository,
    private val smsRepository: SmsRepository
) {
    fun checkDuplicatedNickname(nickname: String) {
        if (memberQueryRepository.existsByNickname(nickname)) {
            throw IllegalArgumentException("이미 사용중인 닉네임입니다.")
        }
    }
    
    fun checkDuplicatedEmail(email: String) {
        if (memberQueryRepository.existsByEmail(email)) {
            throw IllegalArgumentException("이미 사용중인 이메일입니다.")
        }
    }
    
    fun checkSmsVerification(phoneNumber: String) {
        if (!smsRepository.getConfirmed(phoneNumber)) {
            throw IllegalArgumentException("휴대폰 번호 인증이 완료되지 않았습니다.")
        }
    }
}
