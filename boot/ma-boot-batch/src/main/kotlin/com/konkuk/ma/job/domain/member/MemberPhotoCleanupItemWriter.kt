package com.konkuk.ma.job.domain.member

import com.konkuk.ma.domain.member.domain.Member
import com.konkuk.ma.domain.member.domain.photo.MemberPhotoCleaner

class MemberPhotoCleanupItemWriter(
    private val memberPhotoCleaner: MemberPhotoCleaner
) : MemberCleanupItemWriter() {

    override fun cleanup(member: Member) {
        memberPhotoCleaner.clean(member.email)
    }
}
