package com.konkuk.ma.domain.community.domain.block

import com.konkuk.ma.exception.InvalidValueException

class NewBlock(
    val blockerId: Long,
    val blockedId: Long,
) {
    init {
        if (blockerId == blockedId) {
            throw InvalidValueException(NewBlock::class, blockerId, "본인은 차단할 수 없습니다.")
        }
    }
}
