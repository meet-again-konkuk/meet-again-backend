package com.konkuk.ma.domain.xroom.domain.block

import com.konkuk.ma.exception.AccessDeniedException
import com.konkuk.ma.exception.EntityType

class Photo(
    val id: Long,
    val blockId: Long,
    val photoUrl: String,
    val orderIndex: Int,
) {
    fun validateBelongsTo(expectedBlockId: Long) {
        if (blockId != expectedBlockId) {
            throw AccessDeniedException(
                EntityType.XROOM_BLOCK_PHOTO,
                blockId.toString(),
                expectedBlockId.toString(),
            )
        }
    }
}
