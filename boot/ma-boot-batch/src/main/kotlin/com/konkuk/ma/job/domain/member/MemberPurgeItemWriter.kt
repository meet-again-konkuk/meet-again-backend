package com.konkuk.ma.job.domain.member

import com.konkuk.ma.domain.member.domain.Member
import com.konkuk.ma.domain.withdrawal.domain.MemberDataCleaner
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter

class MemberPurgeItemWriter(
    private val dataCleaner: MemberDataCleaner
) : ItemWriter<Member> {

    override fun write(chunk: Chunk<out Member>) {
        chunk.items.forEach(dataCleaner::clean)
    }
}
