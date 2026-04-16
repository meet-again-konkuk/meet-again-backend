package com.konkuk.ma.domain.matching.domain

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.member.domain.Members
import com.konkuk.ma.domain.member.domain.photo.MemberPhotos

class MatchingResults(
    val data: List<MatchingResult>
) {
    fun filterVisible(): MatchingResults {
        return MatchingResults(data.filter { it.isVisible() })
    }

    fun extractTargetEmails(): Set<Email> {
        return data.map { it.targetEmail }.toSet()
    }

    fun combineWithProfiles(members: Members, photos: MemberPhotos): MatchingResultsWithProfiles {
        val combined = data.map { result ->
            val member = members.findOne(result.targetEmail)
            val photo = photos.findOne(result.targetEmail)
            MatchingResultWithProfile(
                matchingResult = result,
                targetMemberId = member?.id,
                targetName = member?.name,
                targetNickname = member?.nickname,
                profileImageUrl = photo?.thumbnailPath,
            )
        }
        return MatchingResultsWithProfiles(combined)
    }
}
