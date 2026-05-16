package com.konkuk.ma.domain.member.domain

import com.konkuk.ma.domain.member.domain.policy.WithdrawnSentinel

object WithdrawnNicknameMasker {
    fun mask(member: Member): String {
        return if (member.isActive()) member.nickname else WithdrawnSentinel.nickname(member.id)
    }
}
