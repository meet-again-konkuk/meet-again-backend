package com.konkuk.ma.job.domain.member

import com.konkuk.ma.domain.member.domain.Member
import com.konkuk.ma.domain.withdrawal.domain.MemberBackupArchiver
import org.springframework.batch.item.ItemProcessor

class MemberBackupItemProcessor(
    private val backupArchiver: MemberBackupArchiver
) : ItemProcessor<Member, Member> {

    override fun process(item: Member): Member {
        backupArchiver.archive(item)
        return item
    }
}
