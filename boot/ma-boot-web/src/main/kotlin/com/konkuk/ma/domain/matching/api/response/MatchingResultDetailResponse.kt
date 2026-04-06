package com.konkuk.ma.domain.matching.api.response

import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.matching.domain.MatchingResult
import com.konkuk.ma.support.id.EncryptId

class MatchingResultDetailResponse(
    @EncryptId(ObfuscationType.MATCHING_RESULT)
    val matchingResultId: Long,
    val remainingDays: Long,
    val matchRate: Int,
    val middleNumberMatched: Boolean,
    val lastNumberMatched: Boolean,
    val yearMatched: Boolean,
    val monthMatched: Boolean,
    val dayMatched: Boolean,
    val regionMatched: Boolean,
) {
    companion object {
        fun from(matchingResult: MatchingResult): MatchingResultDetailResponse {
            return MatchingResultDetailResponse(
                matchingResultId = matchingResult.id,
                remainingDays = matchingResult.getRemainingDays(),
                matchRate = matchingResult.matchRate,
                middleNumberMatched = matchingResult.middleNumberMatched,
                lastNumberMatched = matchingResult.lastNumberMatched,
                yearMatched = matchingResult.yearMatched,
                monthMatched = matchingResult.monthMatched,
                dayMatched = matchingResult.dayMatched,
                regionMatched = matchingResult.regionMatched,
            )
        }
    }
}
