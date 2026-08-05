package com.konkuk.ma.domain.xroom.api.request

import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.xroom.domain.memory.NewMemories
import com.konkuk.ma.support.id.EncryptId

class CreateXroomWithMemoriesRequest(
    @field:EncryptId(ObfuscationType.TARGET_INFO)
    val targetInfoId: Long,
    val finalMessage: String? = null,
    val memories: List<AddMemoryRequest> = emptyList(),
) {
    fun toNewMemories(): NewMemories {
        return NewMemories(memories.map { it.toMemoryDetails() })
    }
}
