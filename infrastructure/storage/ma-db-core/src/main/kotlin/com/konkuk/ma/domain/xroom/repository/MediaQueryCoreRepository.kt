package com.konkuk.ma.domain.xroom.repository

import com.konkuk.ma.domain.xroom.dao.MediaQueryDao
import com.konkuk.ma.domain.xroom.domain.media.Media
import com.konkuk.ma.domain.xroom.domain.port.MediaQueryRepository
import java.time.LocalDateTime
import org.springframework.stereotype.Repository

@Repository
class MediaQueryCoreRepository(
    private val mediaQueryDao: MediaQueryDao,
) : MediaQueryRepository {
    override fun findActiveByMemories(memoryIds: Set<Long>): List<Media> {
        return mediaQueryDao.findActiveByMemories(memoryIds).map { it.toDomain() }
    }

    override fun findDeletedBefore(cutoff: LocalDateTime, cursorId: Long?, pageSize: Int): List<Media> {
        return mediaQueryDao.findDeletedBefore(cutoff, cursorId, pageSize).map { it.toDomain() }
    }
}
