package com.konkuk.ma.domain.auth.application

import com.konkuk.ma.domain.auth.application.command.WithdrawalRequestCommand
import com.konkuk.ma.domain.auth.domain.PasswordVerifier
import com.konkuk.ma.domain.member.domain.port.MemberCommandRepository
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class WithdrawalService(
    private val memberQueryRepository: MemberQueryRepository,
    private val memberCommandRepository: MemberCommandRepository,
    private val passwordVerifier: PasswordVerifier
) {
    fun requestWithdrawal(command: WithdrawalRequestCommand) {
        val member = memberQueryRepository.findOne(command.email)
        passwordVerifier.verify(command.password, member)

        val requestedAt = member.requestWithdrawal()
        memberCommandRepository.requestWithdrawal(member.email, requestedAt)
    }
}
