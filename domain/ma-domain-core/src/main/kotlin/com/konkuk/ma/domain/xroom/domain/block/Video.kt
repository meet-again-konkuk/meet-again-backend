package com.konkuk.ma.domain.xroom.domain.block

import com.konkuk.ma.exception.AccessDeniedException
import com.konkuk.ma.exception.EntityType

class Video(
    val id: Long,
    val blockId: Long,
    val videoUrl: String,
    val orderIndex: Int,
) {
    fun validateBelongsTo(expectedBlockId: Long) {
        if (blockId != expectedBlockId) {
            throw AccessDeniedException(
                EntityType.XROOM_BLOCK_VIDEO,
                blockId.toString(),
                expectedBlockId.toString(),
            )
        }
    }
}
