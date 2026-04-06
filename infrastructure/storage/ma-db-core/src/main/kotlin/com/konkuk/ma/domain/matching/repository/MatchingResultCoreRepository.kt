package com.konkuk.ma.domain.matching.repository

import com.konkuk.ma.domain.matching.dao.MatchingResultCommandDao
import com.konkuk.ma.domain.matching.dao.MatchingResultQueryDao
import com.konkuk.ma.domain.matching.domain.MatchingResult
import com.konkuk.ma.domain.matching.domain.MatchingResults
import com.konkuk.ma.domain.matching.domain.port.MatchingResultRepository
import com.konkuk.ma.exception.EntityNotFoundException
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
            matchingResultQueryDao.findByTargetInfoIds(targetInfoIds)
                .map { it.toDomain() }
        )
    }

    override fun deleteExpiredMatchingResults(baseDate: LocalDate): Int {
        return matchingResultCommandDao.deleteExpired(baseDate)
    }

    override fun findByRegisterEmail(email: String): MatchingResults {
        return MatchingResults(
            matchingResultQueryDao.findByRegisterEmail(email)
                .map { it.toDomain() }
        )
    }

    override fun findById(matchingResultId: Long): MatchingResult {
        return matchingResultQueryDao.findById(matchingResultId)
            ?.toDomain()
            ?: throw EntityNotFoundException("MatchingResult", "id", matchingResultId.toString())
    }
}
