package com.konkuk.ma.job.domain.member

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.member.domain.Member
import com.konkuk.ma.domain.point.domain.port.MemberPointRepository
import com.konkuk.ma.domain.point.domain.port.PointHistoryRepository

class PointCleanupItemWriter(
    private val memberPointRepository: MemberPointRepository,
    private val pointHistoryRepository: PointHistoryRepository
) : MemberCleanupItemWriter() {

    override fun cleanup(member: Member) {
        memberPointRepository.delete(member.email)
        pointHistoryRepository.anonymizeOwner(member.email, Email.withdrawn(member.id))
    }
}
