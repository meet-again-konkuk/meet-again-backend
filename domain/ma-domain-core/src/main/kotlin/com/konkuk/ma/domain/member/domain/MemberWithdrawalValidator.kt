package com.konkuk.ma.domain.member.domain

import com.konkuk.ma.domain.auth.domain.PasswordVerifier
import com.konkuk.ma.domain.member.exception.AlreadyWithdrawalRequestedException
import org.springframework.stereotype.Component

@Component
class MemberWithdrawalValidator(
    private val passwordVerifier: PasswordVerifier
) {
    fun validate(member: Member, password: String) {
        if (!member.isActive()) {
            throw AlreadyWithdrawalRequestedException(member.email)
        }
        passwordVerifier.verify(password, member)
    }
}
