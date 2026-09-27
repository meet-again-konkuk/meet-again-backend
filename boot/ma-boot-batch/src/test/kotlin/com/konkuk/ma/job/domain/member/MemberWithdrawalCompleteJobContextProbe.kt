package com.konkuk.ma.job.domain.member

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldNotBe
import org.springframework.batch.core.Job
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(properties = ["spring.batch.job.enabled=false"])
@ActiveProfiles("test")
class MemberWithdrawalCompleteJobContextProbe(
    private val memberWithdrawalCompleteJob: Job,
) : FunSpec({
    test("배치 컨텍스트가 부팅되고 회원 탈퇴 완료 잡 빈이 등록된다") {
        memberWithdrawalCompleteJob shouldNotBe null
    }
})
