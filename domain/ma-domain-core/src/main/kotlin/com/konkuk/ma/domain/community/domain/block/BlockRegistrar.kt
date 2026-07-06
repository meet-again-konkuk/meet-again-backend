package com.konkuk.ma.domain.community.domain.block

import com.konkuk.ma.domain.community.domain.port.BlockCommandRepository
import com.konkuk.ma.domain.community.domain.port.BlockQueryRepository
import com.konkuk.ma.domain.member.domain.Members
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import org.springframework.stereotype.Component

@Component
class BlockRegistrar(
    private val blockCommandRepository: BlockCommandRepository,
    private val blockQueryRepository: BlockQueryRepository,
    private val memberQueryRepository: MemberQueryRepository,
) {
    fun register(newBlock: NewBlock): BlockResult {
        val nickname = Members(memberQueryRepository.findByIds(setOf(newBlock.blockedId)))
            .findNickname(newBlock.blockedId)
        val existingBlock = blockQueryRepository.findExistingActiveOrNull(newBlock.blockerId, newBlock.blockedId)
        if (existingBlock != null) {
            return BlockResult(existingBlock.id, nickname, newlyBlocked = false)
        }
        return BlockResult(blockCommandRepository.save(newBlock), nickname, newlyBlocked = true)
    }
}
