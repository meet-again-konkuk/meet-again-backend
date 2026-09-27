package com.konkuk.ma.domain.matching.domain

import com.konkuk.ma.domain.common.domain.file.FileUrls
import com.konkuk.ma.domain.member.domain.Members

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

    fun combineWithProfiles(members: Members, imageUrls: FileUrls): MatchingResultsWithProfiles {
        val combined = data.map { result ->
            val member = members.findOne(result.targetId)
            MatchingResultWithProfile(
                matchingResult = result,
                targetMemberId = member?.id,
                targetName = member?.name,
                targetNickname = member?.nickname,
                profileImageUrl = member?.let { imageUrls.urlOf(it.id) },
            )
        }
        return MatchingResultsWithProfiles(combined)
    }

    fun toClaimerProfiles(
        members: Members,
        imageUrls: FileUrls,
        xroomExistTargetInfoIds: Set<Long>,
    ): ClaimerProfiles {
        val profiles = data.map { result ->
            val member = members.findOne(result.registerId)
            ClaimerProfile(
                memberId = member?.id,
                name = member?.name,
                nickname = member?.nickname,
                profileImageUrl = member?.let { imageUrls.urlOf(it.id) },
                hasXroom = xroomExistTargetInfoIds.contains(result.targetInfoId),
            )
        }
        return ClaimerProfiles(profiles)
    }
}
