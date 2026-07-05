package com.konkuk.ma.domain.auth.application

import com.konkuk.ma.domain.auth.domain.PasswordVerifier
import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.member.domain.port.MemberCommandRepository
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class WithdrawalCancelService(
    private val memberQueryRepository: MemberQueryRepository,
    private val memberCommandRepository: MemberCommandRepository,
    private val passwordVerifier: PasswordVerifier,
) {
    fun cancel(email: String, password: String) {
        val member = memberQueryRepository.findOne(Email(email))
        passwordVerifier.verify(password, member)
        member.cancelWithdrawal()
        memberCommandRepository.cancelWithdrawal(member.id)
    }
}
