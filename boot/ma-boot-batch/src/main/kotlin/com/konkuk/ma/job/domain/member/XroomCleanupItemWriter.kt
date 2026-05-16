package com.konkuk.ma.job.domain.member

import com.konkuk.ma.domain.member.domain.Member
import com.konkuk.ma.domain.xroom.domain.port.XroomCommandRepository

class XroomCleanupItemWriter(
    private val xroomCommandRepository: XroomCommandRepository
) : MemberCleanupItemWriter() {

    override fun cleanup(member: Member) {
        xroomCommandRepository.delete(member.email)
    }
}
