package com.konkuk.ma.config

import com.konkuk.ma.domain.matching.fixture.MemberFixture
import com.konkuk.ma.domain.member.application.MemberQueryService
import io.mockk.every
import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class MemberQueryServiceTestConfig {

    @Bean
    fun memberQueryService(): MemberQueryService {
        val memberQueryService = mockk<MemberQueryService>()
        every { memberQueryService.findOne(AUTHENTICATED_MEMBER_ID) } returns
            MemberFixture.create(
                id = AUTHENTICATED_MEMBER_ID,
                email = AUTHENTICATED_MEMBER_EMAIL,
                nickname = AUTHENTICATED_MEMBER_NICKNAME,
            )
        return memberQueryService
    }

    companion object {
        private const val AUTHENTICATED_MEMBER_ID = 1L
        private const val AUTHENTICATED_MEMBER_EMAIL = "holeman@naver.com"
        private const val AUTHENTICATED_MEMBER_NICKNAME = "holeman"
    }
}
