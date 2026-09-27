package com.konkuk.ma.domain.community.entity

import com.konkuk.ma.domain.community.domain.block.Block
import com.konkuk.ma.domain.community.entity.table.BlockTable
import org.jetbrains.exposed.sql.ResultRow
import java.time.LocalDateTime

class BlockEntity(
    val id: Long,
    val blockerId: Long,
    val blockedId: Long,
    val createdDate: LocalDateTime,
) {
    fun toDomain(): Block {
        return Block(
            id = id,
            blockerId = blockerId,
            blockedId = blockedId,
            createdDate = createdDate,
        )
    }

    companion object {
        fun from(row: ResultRow): BlockEntity {
            return BlockEntity(
                id = row[BlockTable.id].value,
                blockerId = row[BlockTable.blockerId],
                blockedId = row[BlockTable.blockedId],
                createdDate = row[BlockTable.createdDate],
            )
        }
    }
}
