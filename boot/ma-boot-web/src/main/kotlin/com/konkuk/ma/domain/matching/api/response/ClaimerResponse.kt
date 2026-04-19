package com.konkuk.ma.domain.matching.api.response

import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.matching.domain.ClaimerProfile
import com.konkuk.ma.support.id.EncryptId

class ClaimerResponse(
    @EncryptId(ObfuscationType.MEMBER)
    val memberId: Long?,
    val name: String?,
    val nickname: String?,
    val profileImageUrl: String?,
    val isWithdrawn: Boolean,
    val hasXroom: Boolean,
) {
    companion object {
        fun from(profile: ClaimerProfile): ClaimerResponse {
            return ClaimerResponse(
                memberId = profile.memberId,
                name = profile.name,
                nickname = profile.nickname,
                profileImageUrl = profile.profileImageUrl,
                isWithdrawn = profile.isWithdrawn,
                hasXroom = profile.hasXroom,
            )
        }
    }
}
