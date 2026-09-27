package com.konkuk.ma.domain.member.domain

import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import com.konkuk.ma.exception.DuplicateException
import com.konkuk.ma.exception.EntityType
import org.springframework.stereotype.Component

@Component
class NicknameValidator(
    private val memberQueryRepository: MemberQueryRepository,
) {
    fun validate(nickname: String) {
        if (memberQueryRepository.existsByNickname(nickname)) {
            throw DuplicateException(EntityType.MEMBER, "nickname", nickname)
        }
    }
}
