package com.konkuk.ma.domain.matching.domain

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.common.domain.date.remainingDays
import com.konkuk.ma.exception.AccessDeniedException
import com.konkuk.ma.exception.EntityType
import java.time.LocalDate
import java.time.LocalDateTime



class MatchingResult(
    val id: Long,
    val registerEmail: Email,
    override val targetInfoId: Long,
    override val targetEmail: Email,

    val middleNumberMatched: Boolean,
    val lastNumberMatched: Boolean,
    val yearMatched: Boolean,
    val monthMatched: Boolean,
    val dayMatched: Boolean,
    val regionMatched: Boolean,

    val showingExpiryDate: LocalDateTime,
    val matchingExpiryDate: LocalDate,
    excluded: Boolean,
) : HasMatchingKey {
    var excluded: Boolean = excluded
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


    fun getRemainingDays(): Long {
        return showingExpiryDate.remainingDays()
    }

    fun validateOwnership(email: Email) {
        if (registerEmail != email) {
            throw AccessDeniedException(EntityType.MATCHING_RESULT, registerEmail.value, email.value)
        }
    }

    fun exclude() {
        excluded = true
    }

    fun include() {
        excluded = false
    }
}
