package com.konkuk.ma.domain.member.domain

import org.springframework.stereotype.Component

@Component
class MemberValidator(
    private val nicknameValidator: NicknameValidator,
) {
    fun validateNicknameAvailable(current: Member, nickname: String?) {
        nickname?.takeUnless { current.hasNickname(it) }
            ?.let { nicknameValidator.validate(it) }
    }
}
