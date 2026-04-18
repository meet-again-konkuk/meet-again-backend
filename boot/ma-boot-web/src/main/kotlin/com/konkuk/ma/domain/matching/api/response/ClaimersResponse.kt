package com.konkuk.ma.domain.matching.api.response

import com.konkuk.ma.domain.matching.domain.ClaimerProfiles

class ClaimersResponse(
    val claimers: List<ClaimerResponse>,
) {
    companion object {
        fun from(profiles: ClaimerProfiles): ClaimersResponse {
            return ClaimersResponse(
                claimers = profiles.data.map { ClaimerResponse.from(it) }
            )
        }
    }
}
