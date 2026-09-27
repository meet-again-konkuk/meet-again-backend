package com.konkuk.ma.support.security

import com.konkuk.ma.domain.member.domain.Member

class MemberInfo(
    val id: Long,
    val email: String,
    val nickname: String,
) {
    companion object {
        fun from(member: Member): MemberInfo {
            return MemberInfo(
                id = member.id,
                email = member.email.value,
                nickname = member.nickname,
            )
        }
    }
}
