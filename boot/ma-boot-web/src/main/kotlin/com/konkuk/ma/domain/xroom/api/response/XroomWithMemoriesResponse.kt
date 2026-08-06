package com.konkuk.ma.domain.xroom.api.response

import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.xroom.domain.CreatedXroom
import com.konkuk.ma.support.id.EncryptId

class XroomWithMemoriesResponse(
    @EncryptId(ObfuscationType.XROOM)
    val xroomId: Long,
    @EncryptId(ObfuscationType.MEMORY)
    val memoryIds: List<Long>,
) {
    companion object {
        fun from(createdXroom: CreatedXroom): XroomWithMemoriesResponse {
            return XroomWithMemoriesResponse(
                xroomId = createdXroom.xroomId,
                memoryIds = createdXroom.memoryIds,
            )
        }
    }
}
