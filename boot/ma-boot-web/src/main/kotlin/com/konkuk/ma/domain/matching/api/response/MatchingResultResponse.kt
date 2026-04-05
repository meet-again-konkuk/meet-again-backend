package com.konkuk.ma.domain.matching.api.response

import com.konkuk.ma.domain.matching.domain.MatchingResultWithProfile

class MatchingResultResponse(
    val matchingResultId: Long,
    val targetName: String,
    val targetNickname: String,
    val profileImageUrl: String?,
    val remainingDays: Long,
    val matchRate: Int,
) {
    companion object {
        fun from(result: MatchingResultWithProfile): MatchingResultResponse {
            return MatchingResultResponse(
                matchingResultId = result.matchingResult.id,
                targetName = result.targetName,
                targetNickname = result.targetNickname,
                profileImageUrl = result.profileImageUrl,
                remainingDays = result.matchingResult.getRemainingDays(),
                matchRate = result.matchingResult.matchRate,
            )
        }
    }
}
