package com.konkuk.ma.domain.auth.domain

import com.konkuk.ma.domain.auth.domain.port.SmsRepository
import com.konkuk.ma.domain.member.domain.PhoneNumber
import com.konkuk.ma.domain.member.exception.SmsNotVerifiedException
import org.springframework.stereotype.Component

@Component
class FindEmailValidator(
    private val smsRepository: SmsRepository,
) {
    fun validate(phoneNumber: PhoneNumber) {
        if (!smsRepository.getConfirmed(phoneNumber.fullNumber)) {
            throw SmsNotVerifiedException(phoneNumber.masked())
        }
    }
}
