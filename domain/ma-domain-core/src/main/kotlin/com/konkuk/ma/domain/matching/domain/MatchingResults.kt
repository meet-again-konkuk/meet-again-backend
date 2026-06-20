package com.konkuk.ma.domain.matching.domain

import com.konkuk.ma.domain.member.domain.Members
import com.konkuk.ma.domain.member.domain.photo.MemberPhotos

class MatchingResults(
    data: List<MatchingResult>
) {
    val data: List<MatchingResult> = data.filter { it.isVisible() }

    fun extractTargetIds(): Set<Long> {
        return data.map { it.targetId }.toSet()
    }

    fun extractRegisterIds(): Set<Long> {
        return data.map { it.registerId }.toSet()
    }

    fun extractTargetInfoIds(): Set<Long> {
        return data.map { it.targetInfoId }.toSet()
    }

    fun combineWithProfiles(members: Members, photos: MemberPhotos): MatchingResultsWithProfiles {
        val combined = data.map { result ->
            val member = members.findOne(result.targetId)
            val photo = member?.let { photos.findOne(it.id) }
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

    fun toClaimerProfiles(
        members: Members,
        photos: MemberPhotos,
        xroomExistTargetInfoIds: Set<Long>,
    ): ClaimerProfiles {
        val profiles = data.map { result ->
            val member = members.findOne(result.registerId)
            val photo = member?.let { photos.findOne(it.id) }
            ClaimerProfile(
                memberId = member?.id,
                name = member?.name,
                nickname = member?.nickname,
                profileImageUrl = photo?.thumbnailPath,
                hasXroom = xroomExistTargetInfoIds.contains(result.targetInfoId),
            )
        }
        return ClaimerProfiles(profiles)
    }
}
