package com.konkuk.ma.job.domain.member

import com.konkuk.ma.domain.member.domain.Member
import com.konkuk.ma.domain.withdrawal.domain.MemberBackupArchiver
import org.springframework.batch.item.ItemProcessor

/**
 * 정리 직전 단계: 회원 데이터를 백업(스냅샷 저장)하고 그대로 통과시킨다.
 * 청크 내에서 process 가 write(정리)보다 먼저 수행되므로 "백업 후 삭제" 순서가 보장된다.
 */
class MemberBackupItemProcessor(
    private val backupArchiver: MemberBackupArchiver
) : ItemProcessor<Member, Member> {

    override fun process(item: Member): Member {
        backupArchiver.archive(item)
        return item
    }
}
