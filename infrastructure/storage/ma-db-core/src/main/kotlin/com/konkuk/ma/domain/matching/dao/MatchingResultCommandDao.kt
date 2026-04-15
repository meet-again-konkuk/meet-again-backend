package com.konkuk.ma.domain.matching.dao

import com.konkuk.ma.domain.matching.domain.MatchingResult
import com.konkuk.ma.domain.matching.domain.NewMatchingResult
import com.konkuk.ma.domain.matching.entity.table.MatchingResultTable
import java.time.LocalDate
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.update
import org.springframework.stereotype.Component

@Component
class MatchingResultCommandDao {
    fun deleteExpired(baseDate: LocalDate): Int {
        return MatchingResultTable.deleteWhere {
            (matchingExpiryDate less baseDate) and (excluded eq false)
        }
    }

    fun deleteExcludedExpired(baseDate: LocalDate): Int {
        return MatchingResultTable.deleteWhere {
            (excluded eq true) and (matchingExpiryDate less baseDate)
        }
    }

    fun saveAll(matchingResults: List<NewMatchingResult>) {
        MatchingResultTable.batchInsert(matchingResults) {
            this[MatchingResultTable.registerEmail] = it.registerEmail.value
            this[MatchingResultTable.targetInfoId] = it.targetInfoId
            this[MatchingResultTable.targetEmail] = it.targetEmail.value
            this[MatchingResultTable.middleNumberMatched] = it.middleNumberMatched
            this[MatchingResultTable.lastNumberMatched] = it.lastNumberMatched
            this[MatchingResultTable.yearMatched] = it.yearMatched
            this[MatchingResultTable.monthMatched] = it.monthMatched
            this[MatchingResultTable.dayMatched] = it.dayMatched
            this[MatchingResultTable.regionMatched] = it.regionMatched
            this[MatchingResultTable.showingExpiryDate] = it.showingExpiryDate
            this[MatchingResultTable.matchingExpiryDate] = it.matchingExpiryDate
            this[MatchingResultTable.createdBy] = it.registerEmail.value
            this[MatchingResultTable.lastModifiedBy] = it.registerEmail.value
            this[MatchingResultTable.excluded] = false
        }
    }

    fun updateExcluded(matchingResult: MatchingResult) {
        MatchingResultTable.update({ MatchingResultTable.id eq matchingResult.id }) {
            it[excluded] = matchingResult.excluded
        }
    }

    fun delete(targetInfoId: Long, email: String) {
        MatchingResultTable.softDelete({ MatchingResultTable.targetInfoId eq targetInfoId }, email)
    }
}
