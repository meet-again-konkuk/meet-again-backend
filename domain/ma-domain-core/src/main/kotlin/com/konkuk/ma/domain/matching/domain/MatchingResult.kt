package com.konkuk.ma.domain.matching.domain

import com.konkuk.ma.domain.common.domain.date.remainingDays
import com.konkuk.ma.domain.matching.domain.NewMatchingResult.Companion.SHOWING_EXPIRY_DAYS
import com.konkuk.ma.exception.AccessDeniedException
import com.konkuk.ma.exception.EntityType
import com.konkuk.ma.exception.InvalidStateException
import java.time.LocalDate
import java.time.LocalDateTime



class MatchingResult(
    val id: Long,
    val registerId: Long,
    override val targetInfoId: Long,
    override val targetId: Long,

    val middleNumberMatched: Boolean,
    val lastNumberMatched: Boolean,
    val yearMatched: Boolean,
    val monthMatched: Boolean,
    val dayMatched: Boolean,
    val regionMatched: Boolean,

    val showingExpiryDate: LocalDateTime,
    val matchingExpiryDate: LocalDate,
    excluded: Boolean,
    claimed: Boolean = false,
) : HasMatchingKey {
    var excluded: Boolean = excluded
        private set
    var claimed: Boolean = claimed
        private set
    val matchRate: Int by lazy {
        MatchRateCalculator(
            groups = listOf(
                MatchingGroup.Phone(middleNumberMatched, lastNumberMatched),
                MatchingGroup.Birth(yearMatched, monthMatched, dayMatched),
            ),
            regionMatched = regionMatched,
        ).calculate()
    }


    fun isVisible(): Boolean {
        val showingStartDate = showingExpiryDate.minusDays(SHOWING_EXPIRY_DAYS)
        return LocalDateTime.now().isAfter(showingStartDate)
    }

    fun getRemainingDays(): Long {
        return showingExpiryDate.remainingDays()
    }

    fun validateOwnership(memberId: Long) {
        if (registerId != memberId) {
            throw AccessDeniedException(EntityType.MATCHING_RESULT, registerId.toString(), memberId.toString())
        }
    }

    fun exclude() {
        excluded = true
    }

    fun include() {
        excluded = false
    }

    fun claim() {
        validateNotClaimed()
        claimed = true
    }

    private fun validateNotClaimed() {
        if (claimed) {
            throw InvalidStateException(MatchingResult::class, id, "이미 claim된 매칭 결과입니다.")
        }
    }
}
