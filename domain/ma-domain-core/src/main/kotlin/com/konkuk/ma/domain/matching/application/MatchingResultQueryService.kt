package com.konkuk.ma.domain.matching.application

import com.konkuk.ma.domain.matching.domain.ClaimerProfiles
import com.konkuk.ma.domain.matching.domain.MatchingResult
import com.konkuk.ma.domain.matching.domain.MatchingResults
import com.konkuk.ma.domain.matching.domain.MatchingResultsWithProfiles
import com.konkuk.ma.domain.matching.domain.port.MatchingResultRepository
import com.konkuk.ma.domain.member.domain.Members
import com.konkuk.ma.domain.member.domain.photo.MemberPhotos
import com.konkuk.ma.domain.member.domain.port.MemberPhotoRepository
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import com.konkuk.ma.domain.xroom.domain.port.XroomQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MatchingResultQueryService(
    private val matchingResultRepository: MatchingResultRepository,
    private val memberQueryRepository: MemberQueryRepository,
    private val memberPhotoRepository: MemberPhotoRepository,
    private val xroomQueryRepository: XroomQueryRepository,
) {
    fun find(memberId: Long, excluded: Boolean = false): MatchingResultsWithProfiles {
        val matchingResults = MatchingResults(matchingResultRepository.find(memberId, excluded))
        val targetIds = matchingResults.extractTargetIds()

        val members = Members(memberQueryRepository.findByIds(targetIds))
        val photos = MemberPhotos(memberPhotoRepository.find(members.extractEmails()))

        return matchingResults.combineWithProfiles(members, photos)
    }

    fun findDetail(matchingResultId: Long, memberId: Long): MatchingResult {
        val matchingResult = matchingResultRepository.findOne(matchingResultId)
        matchingResult.validateOwnership(memberId)
        return matchingResult
    }

    fun findClaimedBy(memberId: Long): ClaimerProfiles {
        val matchingResults = MatchingResults(matchingResultRepository.findClaimedByTarget(memberId))
        val registerIds = matchingResults.extractRegisterIds()

        val members = Members(memberQueryRepository.findByIds(registerIds))
        val photos = MemberPhotos(memberPhotoRepository.find(members.extractEmails()))

        val xroomExistTargetInfoIds = xroomQueryRepository.exists(matchingResults.extractTargetInfoIds())

        return matchingResults.toClaimerProfiles(members, photos, xroomExistTargetInfoIds)
    }
}
