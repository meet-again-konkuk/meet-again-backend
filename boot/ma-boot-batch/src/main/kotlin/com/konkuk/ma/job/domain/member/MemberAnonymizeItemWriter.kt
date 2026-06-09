package com.konkuk.ma.job.domain.member

import com.konkuk.ma.domain.member.domain.Member
import com.konkuk.ma.domain.member.domain.port.MemberCommandRepository

class MemberAnonymizeItemWriter(
    private val memberCommandRepository: MemberCommandRepository
) : MemberCleanupItemWriter() {

    override fun cleanup(member: Member) {
        memberCommandRepository.anonymizeAndSoftDelete(member)
    }
}
