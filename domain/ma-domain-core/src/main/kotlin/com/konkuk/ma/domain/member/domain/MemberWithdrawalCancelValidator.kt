package com.konkuk.ma.domain.member.domain

import com.konkuk.ma.domain.member.exception.NotWithdrawalRequestedException
import com.konkuk.ma.domain.member.exception.WithdrawalExpiredException
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class MemberWithdrawalCancelValidator {
    fun validate(member: Member, now: LocalDateTime = LocalDateTime.now()) {
        if (member.isActive()) {
            throw NotWithdrawalRequestedException(member.email)
        }
        if (member.isWithdrawalExpired(now)) {
            throw WithdrawalExpiredException(member.email)
        }
    }
}
