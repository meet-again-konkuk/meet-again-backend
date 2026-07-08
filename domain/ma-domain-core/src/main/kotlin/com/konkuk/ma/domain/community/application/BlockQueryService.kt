package com.konkuk.ma.domain.community.application

import com.konkuk.ma.domain.community.domain.block.BlockView
import com.konkuk.ma.domain.community.domain.block.Blocks
import com.konkuk.ma.domain.community.domain.port.BlockQueryRepository
import com.konkuk.ma.domain.member.domain.Members
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class BlockQueryService(
    private val blockQueryRepository: BlockQueryRepository,
    private val memberQueryRepository: MemberQueryRepository,
) {
    fun findBlocks(blockerId: Long): List<BlockView> {
        val blocks = Blocks(blockQueryRepository.findByBlocker(blockerId))
        val members = Members(memberQueryRepository.findByIds(blocks.extractBlockedIds()))
        return blocks.combineWithViews(members)
    }
}
