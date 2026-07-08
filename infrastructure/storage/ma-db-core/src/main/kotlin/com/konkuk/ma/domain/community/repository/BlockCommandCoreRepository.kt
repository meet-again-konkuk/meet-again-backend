package com.konkuk.ma.domain.community.repository

import com.konkuk.ma.domain.community.dao.BlockCommandDao
import com.konkuk.ma.domain.community.domain.block.NewBlock
import com.konkuk.ma.domain.community.domain.port.BlockCommandRepository
import org.springframework.stereotype.Repository

@Repository
class BlockCommandCoreRepository(
    private val blockCommandDao: BlockCommandDao,
) : BlockCommandRepository {
    override fun save(newBlock: NewBlock): Long {
        return blockCommandDao.save(newBlock)
    }

    override fun softDelete(blockId: Long, blockerId: Long) {
        blockCommandDao.softDelete(blockId, blockerId)
    }
}
