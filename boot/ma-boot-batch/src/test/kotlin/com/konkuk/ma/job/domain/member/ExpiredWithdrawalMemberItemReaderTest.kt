package com.konkuk.ma.job.domain.member

import com.konkuk.ma.domain.member.domain.Member
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime

class ExpiredWithdrawalMemberItemReaderTest : FunSpec({

    val expiredBefore = LocalDateTime.of(2026, 6, 1, 0, 0)

    fun memberWithId(value: Long): Member = mockk { every { id } returns value }

    test("첫 read는 첫 페이지를 반환하고 cursor를 마지막 id로 이동시킨 뒤 소진되면 null을 반환한다") {
        val repository = mockk<MemberQueryRepository>()
        val reader = ExpiredWithdrawalMemberItemReader(repository, expiredBefore, 2)
        every { repository.findExpiredWithdrawalRequests(expiredBefore, null, 2) } returns
            listOf(memberWithId(1L), memberWithId(2L))
        every { repository.findExpiredWithdrawalRequests(expiredBefore, 2L, 2) } returns emptyList()

        reader.read()!!.map { it.id } shouldBe listOf(1L, 2L)
        reader.read().shouldBeNull()
    }

    test("빈 결과를 받으면 소진 처리하고 이후 read는 repository를 다시 호출하지 않는다") {
        val repository = mockk<MemberQueryRepository>()
        val reader = ExpiredWithdrawalMemberItemReader(repository, expiredBefore, 2)
        every { repository.findExpiredWithdrawalRequests(expiredBefore, null, 2) } returns emptyList()

        reader.read().shouldBeNull()
        reader.read().shouldBeNull()

        verify(exactly = 1) { repository.findExpiredWithdrawalRequests(expiredBefore, null, 2) }
    }
})
