package com.konkuk.ma.domain.matching.api.response

import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.matching.domain.MatchingResultWithProfile
import com.konkuk.ma.support.id.EncryptId

class MatchingResultDetailResponse(
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
    val middleNumberMatched: Boolean,
    val lastNumberMatched: Boolean,
    val yearMatched: Boolean,
    val monthMatched: Boolean,
    val dayMatched: Boolean,
    val regionMatched: Boolean,
) {
    companion object {
        fun from(result: MatchingResultWithProfile): MatchingResultDetailResponse {
            return MatchingResultDetailResponse(
                matchingResultId = result.matchingResult.id,
                targetMemberId = result.targetMemberId,
                targetName = result.targetName,
                targetNickname = result.targetNickname,
                profileImageUrl = result.profileImageUrl,
                remainingDays = result.matchingResult.getRemainingDays(),
                matchRate = result.matchingResult.matchRate,
                isWithdrawn = result.isWithdrawn,
                middleNumberMatched = result.matchingResult.middleNumberMatched,
                lastNumberMatched = result.matchingResult.lastNumberMatched,
                yearMatched = result.matchingResult.yearMatched,
                monthMatched = result.matchingResult.monthMatched,
                dayMatched = result.matchingResult.dayMatched,
                regionMatched = result.matchingResult.regionMatched,
            )
        }
    }
}
