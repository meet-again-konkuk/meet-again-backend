package com.konkuk.ma.domain.community.domain.port

import com.konkuk.ma.domain.community.domain.block.Block

interface BlockQueryRepository {
    fun findByBlocker(blockerId: Long): List<Block>
    fun findBlockedMemberIds(blockerId: Long): Set<Long>
    fun findExistingActiveOrNull(blockerId: Long, blockedId: Long): Block?
    fun findOne(blockId: Long): Block
}
