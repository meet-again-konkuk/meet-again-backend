package com.konkuk.ma.domain.auth.domain

import com.konkuk.ma.domain.auth.domain.port.PasswordEncryptor
import com.konkuk.ma.domain.auth.exception.PasswordMismatchException
import com.konkuk.ma.domain.member.domain.Member
import org.springframework.stereotype.Component

@Component
class PasswordVerifier(
    private val passwordEncryptor: PasswordEncryptor
) {
    fun verify(inputPassword: String, member: Member) {
        if (!passwordEncryptor.matches(inputPassword, member.password)) {
            throw PasswordMismatchException(member.email)
        }
    }
}
