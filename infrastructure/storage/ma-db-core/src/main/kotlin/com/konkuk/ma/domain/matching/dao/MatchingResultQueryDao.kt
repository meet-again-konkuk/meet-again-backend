package com.konkuk.ma.domain.matching.dao

import com.konkuk.ma.domain.matching.domain.MatchingResult
import com.konkuk.ma.domain.matching.entity.table.MatchingResultTable
import org.springframework.stereotype.Component

@Component
class MatchingResultQueryDao {
    fun findByTargetInfoIds(targetInfoIds: List<Long>): List<MatchingResult> {
        if (targetInfoIds.isEmpty()) return emptyList()
        return MatchingResultTable
            .select(
                MatchingResultTable.registerEmail,
                MatchingResultTable.targetInfoId,
                MatchingResultTable.targetEmail,
                MatchingResultTable.middleNumberMatched,
                MatchingResultTable.lastNumberMatched,
                MatchingResultTable.yearMatched,
                MatchingResultTable.monthMatched,
                MatchingResultTable.dayMatched,
                MatchingResultTable.regionMatched,
                MatchingResultTable.showingExpiryDate,
                MatchingResultTable.matchingExpiryDate
            )
            .where { MatchingResultTable.targetInfoId inList targetInfoIds }
            .map { row ->
                MatchingResult(
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
                    matchingExpiryDate = row[MatchingResultTable.matchingExpiryDate]
                )
            }
    }
}
