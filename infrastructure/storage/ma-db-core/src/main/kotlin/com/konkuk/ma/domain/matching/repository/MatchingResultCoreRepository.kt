package com.konkuk.ma.domain.matching.repository

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.matching.dao.MatchingResultCommandDao
import com.konkuk.ma.domain.matching.dao.MatchingResultQueryDao
import com.konkuk.ma.domain.matching.domain.MatchingResult
import com.konkuk.ma.domain.matching.domain.NewMatchingResult
import com.konkuk.ma.domain.matching.domain.port.MatchingResultRepository
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.exception.EntityType
import java.time.LocalDate
import org.springframework.stereotype.Repository

@Repository
class MatchingResultCoreRepository(
    private val matchingResultCommandDao: MatchingResultCommandDao,
    private val matchingResultQueryDao: MatchingResultQueryDao
) : MatchingResultRepository {
    override fun saveAll(matchingResults: List<NewMatchingResult>) {
        matchingResultCommandDao.saveAll(matchingResults)
    }

    override fun findExistingMatchingResults(targetInfoIds: List<Long>): List<MatchingResult> {
        return matchingResultQueryDao.find(targetInfoIds)
            .map { it.toDomain() }
    }

    override fun deleteExpired(baseDate: LocalDate): Int {
        return matchingResultCommandDao.deleteExpired(baseDate)
    }

    override fun deleteExcludedExpired(baseDate: LocalDate): Int {
        return matchingResultCommandDao.deleteExcludedExpired(baseDate)
    }

    override fun find(email: Email, excluded: Boolean): List<MatchingResult> {
        return matchingResultQueryDao.find(email.value, excluded)
            .map { it.toDomain() }
    }

    override fun findOne(matchingResultId: Long): MatchingResult {
        return matchingResultQueryDao.findOne(matchingResultId)
            ?.toDomain()
            ?: throw EntityNotFoundException(EntityType.MATCHING_RESULT, matchingResultId.toString())
    }

    override fun exists(targetInfoId: Long): Boolean {
        return matchingResultQueryDao.exists(targetInfoId)
    }

    override fun updateExcluded(matchingResult: MatchingResult) {
        matchingResultCommandDao.updateExcluded(matchingResult)
    }

    override fun updateClaimed(matchingResult: MatchingResult) {
        matchingResultCommandDao.updateClaimed(matchingResult)
    }

    override fun findClaimedByTarget(email: Email): List<MatchingResult> {
        return matchingResultQueryDao.findClaimedByTarget(email.value)
            .map { it.toDomain() }
    }

    override fun delete(targetInfoId: Long, email: Email) {
        matchingResultCommandDao.delete(targetInfoId, email.value)
    }

    override fun deleteByMember(email: Email) {
        matchingResultCommandDao.deleteByMember(email.value)
    }
}
