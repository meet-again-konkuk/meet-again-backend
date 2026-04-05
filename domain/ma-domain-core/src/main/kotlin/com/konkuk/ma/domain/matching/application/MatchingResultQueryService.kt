package com.konkuk.ma.domain.matching.application

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
    fun findByRegisterEmail(email: String): MatchingResultsWithProfiles {
        val matchingResults = matchingResultRepository.findByRegisterEmail(email)
        val targetEmails = matchingResults.extractTargetEmails()
        if (targetEmails.isEmpty()) return MatchingResultsWithProfiles(emptyList())

        val members = memberQueryRepository.findByEmails(targetEmails)
        val membersByEmail = members.associateBy { it.email }
        val photosByEmail = memberPhotoRepository.findByEmails(targetEmails)

        return MatchingResultsWithProfiles.combine(matchingResults, membersByEmail, photosByEmail)
    }
}
