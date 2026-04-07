package com.konkuk.ma.domain.matching.domain.port

import com.konkuk.ma.domain.matching.domain.MatchingResult
import com.konkuk.ma.domain.matching.domain.MatchingResults
import java.time.LocalDate

interface MatchingResultRepository {
    fun saveAll(matchingResults: MatchingResults)
    fun findExistingMatchingResults(targetInfoIds: List<Long>): MatchingResults
    fun deleteExpiredMatchingResults(baseDate: LocalDate): Int
    fun deleteExcludedExpiredMatchingResults(baseDate: LocalDate): Int
    fun find(email: String): MatchingResults
    fun findOne(matchingResultId: Long): MatchingResult
    fun updateExcluded(matchingResult: MatchingResult)
}
