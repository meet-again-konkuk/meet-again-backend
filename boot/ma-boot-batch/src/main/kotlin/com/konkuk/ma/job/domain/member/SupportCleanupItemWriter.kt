package com.konkuk.ma.job.domain.member

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.member.domain.Member
import com.konkuk.ma.domain.support.domain.port.InquiryCommandRepository

class SupportCleanupItemWriter(
    private val inquiryCommandRepository: InquiryCommandRepository
) : MemberCleanupItemWriter() {

    override fun cleanup(member: Member) {
        inquiryCommandRepository.anonymizeAuthor(member.email, Email.withdrawn(member.id))
    }
}
