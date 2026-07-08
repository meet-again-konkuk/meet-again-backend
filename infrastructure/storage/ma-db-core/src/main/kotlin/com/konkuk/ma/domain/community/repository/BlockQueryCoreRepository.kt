package com.konkuk.ma.domain.community.repository

import com.konkuk.ma.domain.community.dao.BlockQueryDao
import com.konkuk.ma.domain.community.domain.block.Block
import com.konkuk.ma.domain.community.domain.port.BlockQueryRepository
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.exception.EntityType
import org.springframework.stereotype.Repository

@Repository
class BlockQueryCoreRepository(
    private val blockQueryDao: BlockQueryDao,
) : BlockQueryRepository {
    override fun findByBlocker(blockerId: Long): List<Block> {
        return blockQueryDao.findByBlocker(blockerId).map { it.toDomain() }
    }

    override fun findBlockedMemberIds(blockerId: Long): Set<Long> {
        return blockQueryDao.findBlockedMemberIds(blockerId)
    }

    override fun findExistingActiveOrNull(blockerId: Long, blockedId: Long): Block? {
        return blockQueryDao.findExistingActiveOrNull(blockerId, blockedId)?.toDomain()
    }

    override fun findOne(blockId: Long): Block {
        return blockQueryDao.findOne(blockId)?.toDomain()
            ?: throw EntityNotFoundException(EntityType.COMMUNITY_BLOCK, blockId.toString())
    }
}
