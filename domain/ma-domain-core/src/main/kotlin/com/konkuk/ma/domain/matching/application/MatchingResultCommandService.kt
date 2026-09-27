package com.konkuk.ma.domain.matching.application

import com.konkuk.ma.domain.matching.domain.port.MatchingResultRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class MatchingResultCommandService(
    private val matchingResultRepository: MatchingResultRepository,
) {
    fun exclude(matchingResultId: Long, memberId: Long) {
        val matchingResult = matchingResultRepository.findOne(matchingResultId)
        matchingResult.validateOwnership(memberId)
        matchingResult.exclude()
        matchingResultRepository.updateExcluded(matchingResult)
    }

    fun include(matchingResultId: Long, memberId: Long) {
        val matchingResult = matchingResultRepository.findOne(matchingResultId)
        matchingResult.validateOwnership(memberId)
        matchingResult.include()
        matchingResultRepository.updateExcluded(matchingResult)
    }

    fun claim(matchingResultId: Long, memberId: Long) {
        val matchingResult = matchingResultRepository.findOne(matchingResultId)
        matchingResult.validateOwnership(memberId)
        matchingResult.claim()
        matchingResultRepository.updateClaimStatus(matchingResult)
    }

    fun reject(matchingResultId: Long, memberId: Long) {
        val matchingResult = matchingResultRepository.findOne(matchingResultId)
        matchingResult.validateTargetOwnership(memberId)
        matchingResult.reject()
        matchingResultRepository.updateClaimStatus(matchingResult)
    }
}
