package com.konkuk.ma.domain.member.application

import com.konkuk.ma.domain.member.application.command.WithdrawalRequestCommand
import com.konkuk.ma.domain.member.domain.MemberWithdrawalValidator
import com.konkuk.ma.domain.member.domain.port.MemberCommandRepository
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class MemberWithdrawalService(
    private val memberQueryRepository: MemberQueryRepository,
    private val memberCommandRepository: MemberCommandRepository,
    private val memberWithdrawalValidator: MemberWithdrawalValidator
) {
    fun requestWithdrawal(command: WithdrawalRequestCommand) {
        val member = memberQueryRepository.findOne(command.email)
        memberWithdrawalValidator.validate(member, command.password)

        val requestedAt = member.requestWithdrawal()
        memberCommandRepository.requestWithdrawal(member.email, requestedAt)
    }
}
