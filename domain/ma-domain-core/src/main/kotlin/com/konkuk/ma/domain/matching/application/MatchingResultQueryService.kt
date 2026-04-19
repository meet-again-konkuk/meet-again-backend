package com.konkuk.ma.domain.matching.application

import com.konkuk.ma.domain.common.domain.Email
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
    fun find(email: String, excluded: Boolean = false): MatchingResultsWithProfiles {
        val domainEmail = Email(email)
        val matchingResults = MatchingResults(matchingResultRepository.find(domainEmail, excluded))
        val targetEmails = matchingResults.extractTargetEmails()

        val members = Members(memberQueryRepository.findByEmails(targetEmails))
        val photos = MemberPhotos(memberPhotoRepository.find(targetEmails))

        return matchingResults.combineWithProfiles(members, photos)
    }

    fun findDetail(matchingResultId: Long, email: String): MatchingResult {
        val matchingResult = matchingResultRepository.findOne(matchingResultId)
        matchingResult.validateOwnership(Email(email))
        return matchingResult
    }

    fun findClaimedBy(email: String): ClaimerProfiles {
        val memberEmail = Email(email)
        val matchingResults = MatchingResults(matchingResultRepository.findClaimedByTarget(memberEmail))
        val registerEmails = matchingResults.extractRegisterEmails()

        val members = Members(memberQueryRepository.findByEmails(registerEmails))
        val photos = MemberPhotos(memberPhotoRepository.find(registerEmails))

        val xroomExistTargetInfoIds = xroomQueryRepository.exists(matchingResults.extractTargetInfoIds())

        return matchingResults.toClaimerProfiles(members, photos, xroomExistTargetInfoIds)
    }
}
