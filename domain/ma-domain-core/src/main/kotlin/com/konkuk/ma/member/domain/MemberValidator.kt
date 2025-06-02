package com.konkuk.ma.member.domain

import org.springframework.stereotype.Component

@Component
class MemberValidator(
    private val memberQueryRepository: MemberQueryRepository
) {
    fun checkDuplicatedNickname(nickname: String) {
        if (memberQueryRepository.existsByNickname(nickname)) {
            throw IllegalArgumentException("이미 사용중인 닉네임입니다.")
        }
    }
}
