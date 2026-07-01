package com.konkuk.ma.domain.xroom.domain.port

import com.konkuk.ma.domain.xroom.domain.media.NewMedia

interface MediaCommandRepository {
    fun save(newMedia: NewMedia): Long

    fun softDeleteByMemory(memoryId: Long, memberId: Long)
}
