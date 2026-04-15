package com.konkuk.ma.domain.matching.domain.port

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.matching.domain.MatchingResult
import com.konkuk.ma.domain.matching.domain.NewMatchingResult
import java.time.LocalDate

interface MatchingResultRepository {
    fun saveAll(matchingResults: List<NewMatchingResult>)
    fun findExistingMatchingResults(targetInfoIds: List<Long>): List<MatchingResult>
    fun deleteExpiredMatchingResults(baseDate: LocalDate): Int
    fun deleteExcludedExpiredMatchingResults(baseDate: LocalDate): Int
    fun find(email: Email, excluded: Boolean = false): List<MatchingResult>
    fun findOne(matchingResultId: Long): MatchingResult
    fun updateExcluded(matchingResult: MatchingResult)
    fun deleteByTargetInfoId(targetInfoId: Long)
}
