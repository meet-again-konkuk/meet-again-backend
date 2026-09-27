package com.konkuk.ma.domain.community.domain.block

import java.time.LocalDateTime

class BlockView(
    val blockId: Long,
    val nickname: String,
    val blockedAt: LocalDateTime,
)
