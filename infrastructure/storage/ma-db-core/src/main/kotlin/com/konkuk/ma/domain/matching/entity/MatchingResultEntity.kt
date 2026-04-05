package com.konkuk.ma.domain.matching.entity

import com.konkuk.ma.domain.matching.domain.MatchingResult
import com.konkuk.ma.domain.matching.entity.table.MatchingResultTable
import org.jetbrains.exposed.sql.ResultRow
import java.time.LocalDate
import java.time.LocalDateTime

class MatchingResultEntity(
    val id: Long,
    val registerEmail: String,
    val targetInfoId: Long,
    val targetEmail: String,
    val middleNumberMatched: Boolean,
    val lastNumberMatched: Boolean,
    val yearMatched: Boolean,
    val monthMatched: Boolean,
    val dayMatched: Boolean,
    val regionMatched: Boolean,
    val showingExpiryDate: LocalDateTime,
    val matchingExpiryDate: LocalDate,
) {
    fun toDomain(): MatchingResult {
        return MatchingResult(
            id = id,
            registerEmail = registerEmail,
            targetInfoId = targetInfoId,
            targetEmail = targetEmail,
            middleNumberMatched = middleNumberMatched,
            lastNumberMatched = lastNumberMatched,
            yearMatched = yearMatched,
            monthMatched = monthMatched,
            dayMatched = dayMatched,
            regionMatched = regionMatched,
            showingExpiryDate = showingExpiryDate,
            matchingExpiryDate = matchingExpiryDate,
        )
    }

    companion object {
        fun from(row: ResultRow): MatchingResultEntity {
            return MatchingResultEntity(
                id = row[MatchingResultTable.id].value,
                registerEmail = row[MatchingResultTable.registerEmail],
                targetInfoId = row[MatchingResultTable.targetInfoId],
                targetEmail = row[MatchingResultTable.targetEmail],
                middleNumberMatched = row[MatchingResultTable.middleNumberMatched],
                lastNumberMatched = row[MatchingResultTable.lastNumberMatched],
                yearMatched = row[MatchingResultTable.yearMatched],
                monthMatched = row[MatchingResultTable.monthMatched],
                dayMatched = row[MatchingResultTable.dayMatched],
                regionMatched = row[MatchingResultTable.regionMatched],
                showingExpiryDate = row[MatchingResultTable.showingExpiryDate],
                matchingExpiryDate = row[MatchingResultTable.matchingExpiryDate],
            )
        }
    }
}
