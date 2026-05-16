package com.konkuk.ma.domain.member.application

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MemberQueryService(
    private val memberQueryRepository: MemberQueryRepository
) {
    fun isDuplicatedNickname(nickname: String): Boolean {
        return memberQueryRepository.existsByNickname(nickname)
    }

    fun isDuplicatedEmail(email: String): Boolean {
        return memberQueryRepository.exists(Email(email))
    }

    fun isWithdrawalRequested(email: String): Boolean {
        val member = memberQueryRepository.findOneOrNull(Email(email)) ?: return false
        return member.isWithdrawalRequested()
    }
}
