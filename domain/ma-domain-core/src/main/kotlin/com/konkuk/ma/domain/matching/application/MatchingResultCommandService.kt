package com.konkuk.ma.domain.matching.application

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.matching.domain.port.MatchingResultRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class MatchingResultCommandService(
    private val matchingResultRepository: MatchingResultRepository,
) {
    fun exclude(matchingResultId: Long, email: String) {
        val matchingResult = matchingResultRepository.findOne(matchingResultId)
        matchingResult.validateOwnership(Email(email))
        matchingResult.exclude()
        matchingResultRepository.updateExcluded(matchingResult)
    }

    fun include(matchingResultId: Long, email: String) {
        val matchingResult = matchingResultRepository.findOne(matchingResultId)
        matchingResult.validateOwnership(Email(email))
        matchingResult.include()
        matchingResultRepository.updateExcluded(matchingResult)
    }
}
