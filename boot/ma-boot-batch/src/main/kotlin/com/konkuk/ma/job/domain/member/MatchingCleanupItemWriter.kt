package com.konkuk.ma.job.domain.member

import com.konkuk.ma.domain.matching.domain.port.MatchingResultRepository
import com.konkuk.ma.domain.matching.domain.port.TargetInfoCommandRepository
import com.konkuk.ma.domain.member.domain.Member

class MatchingCleanupItemWriter(
    private val targetInfoCommandRepository: TargetInfoCommandRepository,
    private val matchingResultRepository: MatchingResultRepository
) : MemberCleanupItemWriter() {

    override fun cleanup(member: Member) {
        targetInfoCommandRepository.delete(member.email)
        matchingResultRepository.deleteByMember(member.email)
    }
}
