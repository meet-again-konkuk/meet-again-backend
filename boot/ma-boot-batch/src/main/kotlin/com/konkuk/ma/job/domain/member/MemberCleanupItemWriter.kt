package com.konkuk.ma.job.domain.member

import com.konkuk.ma.domain.member.domain.Member
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter

abstract class MemberCleanupItemWriter : ItemWriter<List<Member>> {

    final override fun write(chunk: Chunk<out List<Member>>) {
        chunk.items.flatten().forEach(::cleanup)
    }

    protected abstract fun cleanup(member: Member)
}
