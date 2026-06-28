package com.konkuk.ma.domain.xroom.domain

import com.konkuk.ma.domain.matching.domain.MatchingResults
import com.konkuk.ma.domain.matching.domain.port.MatchingResultRepository
import org.springframework.stereotype.Component

@Component
class XroomAccessValidator(
    private val matchingResultRepository: MatchingResultRepository,
) {
    fun validate(xroom: Xroom, memberId: Long) {
        if (xroom.isOwnedBy(memberId)) return

        val receivedTargetInfoIds =
            MatchingResults(matchingResultRepository.findClaimedByTarget(memberId)).extractTargetInfoIds()
        xroom.validateRecipient(receivedTargetInfoIds)
    }
}
