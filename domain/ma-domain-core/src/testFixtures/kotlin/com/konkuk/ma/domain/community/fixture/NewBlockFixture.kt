package com.konkuk.ma.domain.community.fixture

import com.konkuk.ma.domain.community.domain.block.NewBlock

object NewBlockFixture {
    fun create(
        blockerId: Long = 1L,
        blockedId: Long = 2L,
    ): NewBlock {
        return NewBlock(
            blockerId = blockerId,
            blockedId = blockedId,
        )
    }
}
