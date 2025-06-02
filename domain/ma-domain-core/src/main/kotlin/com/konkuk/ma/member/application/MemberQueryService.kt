package com.konkuk.ma.member.application

import com.konkuk.ma.member.domain.MemberQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MemberQueryService(
    private val memberQueryRepository: MemberQueryRepository
) {
    fun checkDuplicatedNickname(nickname: String): Boolean {
        return memberQueryRepository.existsByNickname(nickname)
    }
}
