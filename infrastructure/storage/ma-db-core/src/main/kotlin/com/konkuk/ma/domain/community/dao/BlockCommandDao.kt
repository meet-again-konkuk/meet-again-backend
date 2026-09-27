package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.domain.community.domain.block.NewBlock
import com.konkuk.ma.domain.community.entity.table.BlockTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insertAndGetId
import org.springframework.stereotype.Component

@Component
class BlockCommandDao {
    fun save(newBlock: NewBlock): Long {
        return BlockTable.insertAndGetId {
            it[blockerId] = newBlock.blockerId
            it[blockedId] = newBlock.blockedId
            it[createdBy] = newBlock.blockerId.toString()
            it[lastModifiedBy] = newBlock.blockerId.toString()
        }.value
    }

    fun softDelete(blockId: Long, blockerId: Long) {
        BlockTable.softDelete({ BlockTable.id eq blockId }, blockerId.toString())
    }
}
