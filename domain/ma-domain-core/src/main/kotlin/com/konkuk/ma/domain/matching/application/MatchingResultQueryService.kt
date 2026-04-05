package com.konkuk.ma.domain.matching.application

import com.konkuk.ma.domain.matching.domain.MatchingResultWithProfile
import com.konkuk.ma.domain.matching.domain.MatchingResultsWithProfiles
import com.konkuk.ma.domain.matching.domain.port.MatchingResultRepository
import com.konkuk.ma.domain.member.domain.port.MemberPhotoRepository
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import com.konkuk.ma.exception.EntityNotFoundException
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

        val members = memberQueryRepository.findByEmails(targetEmails)
        val photos = memberPhotoRepository.findByEmails(targetEmails)

        return matchingResults.combineWithProfiles(members, photos)
    }

    fun findDetailById(matchingResultId: Long, email: String): MatchingResultWithProfile {
        val matchingResult = matchingResultRepository.findById(matchingResultId)
            ?: throw EntityNotFoundException("MatchingResult", "id", matchingResultId.toString())

        matchingResult.validateOwnership(email)

        val targetEmail = matchingResult.targetEmail
        val members = memberQueryRepository.findByEmails(setOf(targetEmail))
        val photos = memberPhotoRepository.findByEmails(setOf(targetEmail))

        val member = members.findByEmail(targetEmail)
        val photo = photos.findByEmail(targetEmail)

        return MatchingResultWithProfile(
            matchingResult = matchingResult,
            targetMemberId = member?.id,
            targetName = member?.name,
            targetNickname = member?.nickname,
            profileImageUrl = photo?.thumbnailPath,
        )
    }
}
