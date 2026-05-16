package com.konkuk.ma.job.domain.member

import com.konkuk.ma.domain.auth.domain.port.RefreshTokenRepository
import com.konkuk.ma.domain.member.domain.Member

class AuthCleanupItemWriter(
    private val refreshTokenRepository: RefreshTokenRepository
) : MemberCleanupItemWriter() {

    override fun cleanup(member: Member) {
        refreshTokenRepository.delete(member.email)
    }
}
