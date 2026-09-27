package com.konkuk.ma.domain.community.domain.port

import com.konkuk.ma.domain.community.domain.block.NewBlock

interface BlockCommandRepository {
    fun save(newBlock: NewBlock): Long
    fun softDelete(blockId: Long, blockerId: Long)
}
