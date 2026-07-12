package com.konkuk.ma.domain.auth.application

import com.konkuk.ma.domain.auth.domain.FindEmailValidator
import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.member.domain.PhoneNumber
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class FindEmailService(
    private val findEmailValidator: FindEmailValidator,
    private val memberQueryRepository: MemberQueryRepository
) {
    fun findEmail(name: String, phone: String): Email {
        val phoneNumber = PhoneNumber(phone)
        findEmailValidator.validate(phoneNumber)
        val member = memberQueryRepository.findOne(name, phoneNumber)
        return member.email
    }
}
