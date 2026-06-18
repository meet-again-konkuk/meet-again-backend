package com.konkuk.ma.job.domain.member

import com.konkuk.ma.domain.member.domain.Member
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import org.springframework.batch.item.ItemReader
import java.time.LocalDateTime

open class ExpiredWithdrawalMemberItemReader(
    private val memberQueryRepository: MemberQueryRepository,
    private val expiredBefore: LocalDateTime,
    private val pageSize: Int
) : ItemReader<Member> {

    private val buffer: ArrayDeque<Member> = ArrayDeque()
    private var cursorId: Long? = null
    private var exhausted: Boolean = false

    override fun read(): Member? {
        if (buffer.isEmpty() && !fetchNextPage()) {
            return null
        }
        return buffer.removeFirst()
    }

    private fun fetchNextPage(): Boolean {
        if (exhausted) return false
        val members = memberQueryRepository.findExpiredWithdrawalRequests(expiredBefore, cursorId, pageSize)
        if (members.isEmpty()) {
            exhausted = true
            return false
        }
        cursorId = members.last().id
        buffer.addAll(members)
        return true
    }
}
