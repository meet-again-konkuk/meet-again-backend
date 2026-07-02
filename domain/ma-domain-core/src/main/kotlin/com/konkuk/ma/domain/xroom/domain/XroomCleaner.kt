package com.konkuk.ma.domain.xroom.domain

import com.konkuk.ma.domain.xroom.domain.memory.Memories
import com.konkuk.ma.domain.xroom.domain.port.MediaCommandRepository
import com.konkuk.ma.domain.xroom.domain.port.MemoryCommandRepository
import com.konkuk.ma.domain.xroom.domain.port.MemoryQueryRepository
import com.konkuk.ma.domain.xroom.domain.port.XroomCommandRepository
import com.konkuk.ma.domain.xroom.domain.port.XroomQueryRepository
import org.springframework.stereotype.Component

@Component
class XroomCleaner(
    private val xroomQueryRepository: XroomQueryRepository,
    private val memoryQueryRepository: MemoryQueryRepository,
    private val memoryCommandRepository: MemoryCommandRepository,
    private val mediaCommandRepository: MediaCommandRepository,
    private val xroomCommandRepository: XroomCommandRepository,
) {
    fun clean(ownerId: Long) {
        val xroomIds = Xrooms(xroomQueryRepository.find(ownerId)).extractIds()
        val memoryIds = Memories(memoryQueryRepository.find(xroomIds)).extractIds()
        mediaCommandRepository.softDeleteByMemories(memoryIds, ownerId)
        memoryCommandRepository.deleteByXrooms(xroomIds, ownerId)
        xroomCommandRepository.delete(ownerId)
    }
}
