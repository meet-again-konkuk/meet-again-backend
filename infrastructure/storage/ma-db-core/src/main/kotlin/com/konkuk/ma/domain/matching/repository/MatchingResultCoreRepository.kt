package com.konkuk.ma.domain.matching.repository

import com.konkuk.ma.domain.matching.dao.MatchingResultCommandDao
import com.konkuk.ma.domain.matching.dao.MatchingResultQueryDao
import com.konkuk.ma.domain.matching.domain.MatchingResult
import com.konkuk.ma.domain.matching.domain.MatchingResults
import com.konkuk.ma.domain.matching.domain.port.MatchingResultRepository
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.exception.EntityType
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class MatchingResultCoreRepository(
    private val matchingResultCommandDao: MatchingResultCommandDao,
    private val matchingResultQueryDao: MatchingResultQueryDao
) : MatchingResultRepository {
    override fun saveAll(matchingResults: MatchingResults) {
        matchingResultCommandDao.saveAll(matchingResults.data)
    }

    override fun findExistingMatchingResults(targetInfoIds: List<Long>): MatchingResults {
        return MatchingResults(
            matchingResultQueryDao.find(targetInfoIds)
                .map { it.toDomain() }
        )
    }

    override fun deleteExpiredMatchingResults(baseDate: LocalDate): Int {
        return matchingResultCommandDao.deleteExpired(baseDate)
    }

    override fun deleteExcludedExpiredMatchingResults(baseDate: LocalDate): Int {
        return matchingResultCommandDao.deleteExcludedExpired(baseDate)
    }

    override fun find(email: String): MatchingResults {
        return MatchingResults(
            matchingResultQueryDao.find(email)
                .map { it.toDomain() }
        )
    }

    override fun findOne(matchingResultId: Long): MatchingResult {
        return matchingResultQueryDao.findOne(matchingResultId)
            ?.toDomain()
            ?: throw EntityNotFoundException(EntityType.MATCHING_RESULT, matchingResultId.toString())
    }

    override fun updateExcluded(matchingResult: MatchingResult) {
        matchingResultCommandDao.updateExcluded(matchingResult)
    }
}
