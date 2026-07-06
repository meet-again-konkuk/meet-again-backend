package com.konkuk.ma.domain.community.entity.table

import com.konkuk.ma.domain.common.entity.table.BaseTable

object BlockTable : BaseTable("COMMUNITY_BLOCKS", "COMMUNITY_BLOCK_ID") {
    val blockerId = long("BLOCKER_ID")
    val blockedId = long("BLOCKED_ID")

    init {
        index("IDX_COMMUNITY_BLOCKS_BLOCKER_BLOCKED", false, blockerId, blockedId)
        index("IDX_COMMUNITY_BLOCKS_BLOCKER", false, blockerId)
    }
}
