package com.konkuk.ma.domain.matching.entity

import com.konkuk.ma.domain.matching.domain.ClaimStatus
import com.konkuk.ma.domain.matching.domain.MatchingResult
import com.konkuk.ma.domain.matching.entity.table.MatchingResultTable
import org.jetbrains.exposed.sql.ResultRow
import java.time.LocalDate
import java.time.LocalDateTime

class MatchingResultEntity(
    val id: Long,
    val registerId: Long,
    val targetInfoId: Long,
    val targetId: Long,
    val middleNumberMatched: Boolean,
    val lastNumberMatched: Boolean,
    val yearMatched: Boolean,
    val monthMatched: Boolean,
    val dayMatched: Boolean,
    val regionMatched: Boolean,
    val showingExpiryDate: LocalDateTime,
    val matchingExpiryDate: LocalDate,
    val excluded: Boolean,
    val claimStatus: ClaimStatus,
) {
    fun toDomain(): MatchingResult {
        return MatchingResult(
            id = id,
            registerId = registerId,
            targetInfoId = targetInfoId,
            targetId = targetId,
            middleNumberMatched = middleNumberMatched,
            lastNumberMatched = lastNumberMatched,
            yearMatched = yearMatched,
            monthMatched = monthMatched,
            dayMatched = dayMatched,
            regionMatched = regionMatched,
            showingExpiryDate = showingExpiryDate,
            matchingExpiryDate = matchingExpiryDate,
            excluded = excluded,
            claimStatus = claimStatus,
        )
    }

    companion object {
        fun from(row: ResultRow): MatchingResultEntity {
            return MatchingResultEntity(
                id = row[MatchingResultTable.id].value,
                registerId = row[MatchingResultTable.registerId],
                targetInfoId = row[MatchingResultTable.targetInfoId],
                targetId = row[MatchingResultTable.targetId],
                middleNumberMatched = row[MatchingResultTable.middleNumberMatched],
                lastNumberMatched = row[MatchingResultTable.lastNumberMatched],
                yearMatched = row[MatchingResultTable.yearMatched],
                monthMatched = row[MatchingResultTable.monthMatched],
                dayMatched = row[MatchingResultTable.dayMatched],
                regionMatched = row[MatchingResultTable.regionMatched],
                showingExpiryDate = row[MatchingResultTable.showingExpiryDate],
                matchingExpiryDate = row[MatchingResultTable.matchingExpiryDate],
                excluded = row[MatchingResultTable.excluded],
                claimStatus = row[MatchingResultTable.claimStatus],
            )
        }
    }
}
