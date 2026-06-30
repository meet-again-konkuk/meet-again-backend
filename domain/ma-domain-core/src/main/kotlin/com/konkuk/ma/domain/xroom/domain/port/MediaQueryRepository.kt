package com.konkuk.ma.domain.xroom.domain.port

import com.konkuk.ma.domain.xroom.domain.media.Media

interface MediaQueryRepository {
    fun findActiveByMemory(memoryId: Long): Media?

    fun findActiveByMemories(memoryIds: Set<Long>): List<Media>
}
