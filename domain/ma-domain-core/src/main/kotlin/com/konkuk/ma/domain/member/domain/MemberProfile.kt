package com.konkuk.ma.domain.member.domain

class MemberProfile(
    val member: Member,
    val profileImageUrl: String?,
) {
    companion object {
        fun of(member: Member, profileImageUrl: String?): MemberProfile {
            return MemberProfile(
                member = member,
                profileImageUrl = profileImageUrl,
            )
        }
    }
}
