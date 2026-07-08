package com.konkuk.ma.domain.community.api.response

import com.konkuk.ma.domain.community.domain.block.BlockResult

class BlockResponse(
    val blockId: Long,
    val blocked: Boolean,
    val blockedNickname: String,
) {
    companion object {
        fun from(result: BlockResult): BlockResponse {
            return BlockResponse(
                blockId = result.blockId,
                blocked = true,
                blockedNickname = result.blockedNickname,
            )
        }
    }
}
