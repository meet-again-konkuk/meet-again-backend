package com.konkuk.ma.domain.community.domain.block

import com.konkuk.ma.domain.member.domain.Members
import com.konkuk.ma.exception.AccessDeniedException
import com.konkuk.ma.exception.EntityType
import java.time.LocalDateTime

class Block(
    val id: Long = 0L,
    val blockerId: Long,
    val blockedId: Long,
    val createdDate: LocalDateTime = LocalDateTime.now(),
) {
    fun validateOwnership(memberId: Long) {
        if (blockerId != memberId) {
            throw AccessDeniedException(EntityType.COMMUNITY_BLOCK, blockerId.toString(), memberId.toString())
        }
    }

    fun combineWithView(members: Members): BlockView {
        return BlockView(
            blockId = id,
            nickname = members.findNickname(blockedId),
            blockedAt = createdDate,
        )
    }
}
