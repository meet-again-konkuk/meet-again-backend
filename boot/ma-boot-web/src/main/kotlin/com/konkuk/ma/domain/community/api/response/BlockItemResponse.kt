package com.konkuk.ma.domain.community.api.response

import com.konkuk.ma.domain.community.domain.block.BlockView
import java.time.LocalDateTime

class BlockItemResponse(
    val blockId: Long,
    val nickname: String,
    val blockedAt: LocalDateTime,
) {
    companion object {
        fun from(view: BlockView): BlockItemResponse {
            return BlockItemResponse(
                blockId = view.blockId,
                nickname = view.nickname,
                blockedAt = view.blockedAt,
            )
        }
    }
}
