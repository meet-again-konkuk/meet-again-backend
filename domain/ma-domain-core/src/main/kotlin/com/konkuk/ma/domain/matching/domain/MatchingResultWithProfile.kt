package com.konkuk.ma.domain.matching.domain

class MatchingResultWithProfile(
    val matchingResult: MatchingResult,
    val targetMemberId: Long?,
    val targetName: String?,
    val targetNickname: String?,
    val profileImageUrl: String?,
) {
    val isWithdrawn: Boolean get() = targetMemberId == null
}
