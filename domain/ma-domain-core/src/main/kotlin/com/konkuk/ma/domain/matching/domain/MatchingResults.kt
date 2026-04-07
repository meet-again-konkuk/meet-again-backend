package com.konkuk.ma.domain.matching.domain

import com.konkuk.ma.domain.member.domain.Members
import com.konkuk.ma.domain.member.domain.photo.MemberPhotos

class MatchingResults(
    val data: List<MatchingResult>
) {
    fun targetInfoIds(): List<Long> {
        return data.map { it.targetInfoId }.distinct()
    }

    fun extractTargetEmails(): Set<String> {
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

    private fun createUniqueKeys(): Set<Pair<Long, String>> {
        return data.map { it.createUniqueKey() }.toSet()
    }

    fun filterNew(existing: MatchingResults): MatchingResults {
        val existingKeys = existing.createUniqueKeys()
        return MatchingResults(data.filter { it.createUniqueKey() !in existingKeys })
    }

    companion object {
        fun merge(dataList: List<MatchingResults>): MatchingResults {
            return MatchingResults(dataList.flatMap { it.data })
        }
    }
}
