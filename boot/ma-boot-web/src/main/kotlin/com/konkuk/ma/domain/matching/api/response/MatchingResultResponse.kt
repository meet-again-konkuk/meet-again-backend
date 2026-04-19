package com.konkuk.ma.domain.matching.api.response

import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.matching.domain.MatchingResultWithProfile
import com.konkuk.ma.support.id.EncryptId

class MatchingResultResponse(
    @EncryptId(ObfuscationType.MATCHING_RESULT)
    val matchingResultId: Long,
    @EncryptId(ObfuscationType.MEMBER)
    val targetMemberId: Long?,
    val targetName: String?,
    val targetNickname: String?,
    val profileImageUrl: String?,
    val remainingDays: Long,
    val matchRate: Int,
    val isWithdrawn: Boolean,
    val claimed: Boolean,
) {
    companion object {
        fun from(result: MatchingResultWithProfile): MatchingResultResponse {
            return MatchingResultResponse(
                matchingResultId = result.matchingResult.id,
                targetMemberId = result.targetMemberId,
                targetName = result.targetName,
                targetNickname = result.targetNickname,
                profileImageUrl = result.profileImageUrl,
                remainingDays = result.matchingResult.getRemainingDays(),
                matchRate = result.matchingResult.matchRate,
                isWithdrawn = result.isWithdrawn,
                claimed = result.matchingResult.claimed,
            )
        }
    }
}
