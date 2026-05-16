package com.konkuk.ma.job.domain.member

import com.konkuk.ma.domain.member.domain.Member
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import org.springframework.batch.item.ItemReader
import java.time.LocalDateTime

class ExpiredWithdrawalMemberItemReader(
    private val memberQueryRepository: MemberQueryRepository,
    private val expiredBefore: LocalDateTime,
    private val pageSize: Int
) : ItemReader<List<Member>> {

    private var cursorId: Long? = null
    private var exhausted: Boolean = false

    override fun read(): List<Member>? {
        if (exhausted) return null
        val members = memberQueryRepository.findExpiredWithdrawalRequests(expiredBefore, cursorId, pageSize)
        if (members.isEmpty()) {
            exhausted = true
            return null
        }
        cursorId = members.last().id
        return members
    }
}
