package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.domain.community.entity.BlockEntity
import com.konkuk.ma.domain.community.entity.table.BlockTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.springframework.stereotype.Component

@Component
class BlockQueryDao {
    fun findByBlocker(blockerId: Long): List<BlockEntity> {
        return BlockTable
            .activeRows { BlockTable.blockerId eq blockerId }
            .map { BlockEntity.from(it) }
    }

    fun findBlockedMemberIds(blockerId: Long): Set<Long> {
        return BlockTable
            .select(BlockTable.blockedId)
            .where { (BlockTable.deleted eq false) and (BlockTable.blockerId eq blockerId) }
            .map { it[BlockTable.blockedId] }
            .toSet()
    }

    fun findExistingActiveOrNull(blockerId: Long, blockedId: Long): BlockEntity? {
        return BlockTable
            .activeRows { (BlockTable.blockerId eq blockerId) and (BlockTable.blockedId eq blockedId) }
            .limit(1)
            .firstOrNull()
            ?.let { BlockEntity.from(it) }
    }

    fun findOne(blockId: Long): BlockEntity? {
        return BlockTable
            .activeRows { BlockTable.id eq blockId }
            .limit(1)
            .firstOrNull()
            ?.let { BlockEntity.from(it) }
    }
}
