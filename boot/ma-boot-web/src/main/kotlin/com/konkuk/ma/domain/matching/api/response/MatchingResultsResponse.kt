package com.konkuk.ma.domain.matching.api.response

import com.konkuk.ma.domain.matching.domain.MatchingResultsWithProfiles

class MatchingResultsResponse(
    val matchingResults: List<MatchingResultResponse>,
) {
    companion object {
        fun from(results: MatchingResultsWithProfiles): MatchingResultsResponse {
            return MatchingResultsResponse(
                matchingResults = results.data.map { MatchingResultResponse.from(it) }
            )
        }
    }
}
