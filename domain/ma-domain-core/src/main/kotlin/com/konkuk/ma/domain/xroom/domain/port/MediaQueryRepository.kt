package com.konkuk.ma.domain.xroom.domain.port

import com.konkuk.ma.domain.xroom.domain.media.Media
import java.time.LocalDateTime

interface MediaQueryRepository {
    fun findActiveByMemories(memoryIds: Set<Long>): List<Media>

    fun findDeletedBefore(cutoff: LocalDateTime, cursorId: Long?, pageSize: Int): List<Media>
}
