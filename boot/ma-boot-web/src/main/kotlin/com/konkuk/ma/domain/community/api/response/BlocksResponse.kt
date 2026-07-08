package com.konkuk.ma.domain.community.api.response

import com.konkuk.ma.domain.community.domain.block.BlockView

class BlocksResponse(
    val blocks: List<BlockItemResponse>,
) {
    companion object {
        fun from(views: List<BlockView>): BlocksResponse {
            return BlocksResponse(
                blocks = views.map { BlockItemResponse.from(it) },
            )
        }
    }
}
