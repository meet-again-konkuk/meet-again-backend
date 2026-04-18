package com.konkuk.ma.domain.matching.domain

class ClaimerProfile(
    val memberId: Long?,
    val name: String?,
    val nickname: String?,
    val profileImageUrl: String?,
    val hasXroom: Boolean,
) {
    val isWithdrawn: Boolean get() = memberId == null
}
