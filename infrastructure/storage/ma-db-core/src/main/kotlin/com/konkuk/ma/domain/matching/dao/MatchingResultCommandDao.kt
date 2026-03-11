package com.konkuk.ma.domain.matching.dao

import com.konkuk.ma.domain.matching.domain.MatchingResult
import com.konkuk.ma.domain.matching.entity.table.MatchingResultTable
import org.jetbrains.exposed.sql.batchInsert
import org.springframework.stereotype.Component

@Component
class MatchingResultCommandDao {
    fun saveAll(matchingResults: List<MatchingResult>) {
        MatchingResultTable.batchInsert(matchingResults) {
            this[MatchingResultTable.registerEmail] = it.registerEmail
            this[MatchingResultTable.targetInfoId] = it.targetInfoId
            this[MatchingResultTable.targetEmail] = it.targetEmail
            this[MatchingResultTable.middleNumberMatched] = it.middleNumberMatched
            this[MatchingResultTable.lastNumberMatched] = it.lastNumberMatched
            this[MatchingResultTable.yearMatched] = it.yearMatched
            this[MatchingResultTable.monthMatched] = it.monthMatched
            this[MatchingResultTable.dayMatched] = it.dayMatched
            this[MatchingResultTable.regionMatched] = it.regionMatched
            this[MatchingResultTable.showingExpiryDate] = it.showingExpiryDate
            this[MatchingResultTable.matchingExpiryDate] = it.matchingExpiryDate
            this[MatchingResultTable.createdBy] = it.registerEmail
            this[MatchingResultTable.lastModifiedBy] = it.registerEmail
        }
    }
}
