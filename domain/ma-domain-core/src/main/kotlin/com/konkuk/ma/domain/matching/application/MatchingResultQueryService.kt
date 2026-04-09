package com.konkuk.ma.domain.matching.application

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.matching.domain.MatchingResult
import com.konkuk.ma.domain.matching.domain.MatchingResults
import com.konkuk.ma.domain.matching.domain.MatchingResultsWithProfiles
import com.konkuk.ma.domain.matching.domain.port.MatchingResultRepository
import com.konkuk.ma.domain.member.domain.Members
import com.konkuk.ma.domain.member.domain.photo.MemberPhotos
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
    fun find(email: Email, excluded: Boolean = false): MatchingResultsWithProfiles {
        val matchingResults = MatchingResults(matchingResultRepository.find(email, excluded))
        val targetEmails = matchingResults.extractTargetEmails()

        val members = Members(memberQueryRepository.findByEmails(targetEmails))
        val photos = MemberPhotos(memberPhotoRepository.find(targetEmails))

        return matchingResults.combineWithProfiles(members, photos)
    }

    fun findDetail(matchingResultId: Long, email: Email): MatchingResult {
        val matchingResult = matchingResultRepository.findOne(matchingResultId)
        matchingResult.validateOwnership(email)
        return matchingResult
    }
}
