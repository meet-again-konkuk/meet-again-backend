package com.konkuk.ma.domain.matching.application

import com.konkuk.ma.domain.matching.domain.MatchingResult
import com.konkuk.ma.domain.matching.domain.MatchingResultsWithProfiles
import com.konkuk.ma.domain.matching.domain.port.MatchingResultRepository
import com.konkuk.ma.domain.member.domain.port.MemberPhotoRepository
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MatchingResultQueryService(
    private val matchingResultRepository: MatchingResultRepository,
    private val memberQueryRepository: MemberQueryRepository,
    private val memberPhotoRepository: MemberPhotoRepository,
) {
    fun find(email: String): MatchingResultsWithProfiles {
        val matchingResults = matchingResultRepository.find(email)
        val targetEmails = matchingResults.extractTargetEmails()

        val members = memberQueryRepository.findByEmails(targetEmails)
        val photos = memberPhotoRepository.find(targetEmails)

        return matchingResults.combineWithProfiles(members, photos)
    }

    fun findDetailById(matchingResultId: Long, email: String): MatchingResult {
        val matchingResult = matchingResultRepository.findOne(matchingResultId)
        matchingResult.validateOwnership(email)
        return matchingResult
    }
}
