package com.konkuk.ma.domain.matching.domain

import com.konkuk.ma.domain.member.domain.Member
import com.konkuk.ma.domain.member.domain.photo.MemberPhoto

class MatchingResultsWithProfiles(
    val data: List<MatchingResultWithProfile>,
) {
    companion object {
        fun combine(
            matchingResults: MatchingResults,
            membersByEmail: Map<String, Member>,
            photosByEmail: Map<String, MemberPhoto>,
        ): MatchingResultsWithProfiles {
            val combined = matchingResults.data.mapNotNull { result ->
                val member = membersByEmail[result.targetEmail] ?: return@mapNotNull null
                val photo = photosByEmail[result.targetEmail]
                MatchingResultWithProfile(
                    matchingResult = result,
                    targetName = member.name,
                    targetNickname = member.nickname,
                    profileImageUrl = photo?.thumbnailPath,
                )
            }
            return MatchingResultsWithProfiles(combined)
        }
    }
}
