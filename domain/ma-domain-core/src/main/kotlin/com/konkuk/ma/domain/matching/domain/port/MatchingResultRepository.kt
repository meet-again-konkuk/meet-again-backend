package com.konkuk.ma.domain.matching.domain.port

import com.konkuk.ma.domain.matching.domain.MatchingResult
import com.konkuk.ma.domain.matching.domain.NewMatchingResult
import java.time.LocalDate

interface MatchingResultRepository {
    fun saveAll(matchingResults: List<NewMatchingResult>)
    fun findExistingMatchingResults(targetInfoIds: List<Long>): List<MatchingResult>
    fun deleteExpiredMatchingResults(baseDate: LocalDate): Int
    fun deleteExcludedExpiredMatchingResults(baseDate: LocalDate): Int
    fun find(memberId: Long, excluded: Boolean = false): List<MatchingResult>
    fun findOne(matchingResultId: Long): MatchingResult
    fun updateExcluded(matchingResult: MatchingResult)
    fun updateClaimStatus(matchingResult: MatchingResult)
    fun findClaimedByTarget(memberId: Long): List<MatchingResult>
    fun exists(targetInfoId: Long): Boolean
    fun delete(targetInfoId: Long, memberId: Long)
    fun deleteByRegister(memberId: Long)
}
