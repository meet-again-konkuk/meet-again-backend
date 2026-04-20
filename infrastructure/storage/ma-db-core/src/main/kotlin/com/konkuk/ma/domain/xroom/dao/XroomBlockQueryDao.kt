package com.konkuk.ma.domain.xroom.dao

import com.konkuk.ma.domain.xroom.domain.block.XroomBlockType
import com.konkuk.ma.domain.xroom.entity.XroomBlockEntity
import com.konkuk.ma.domain.xroom.entity.table.XroomBlockTable
import org.jetbrains.exposed.sql.ColumnSet
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Component

@Component
class XroomBlockQueryDao(
    private val factories: List<XroomBlockEntityFactory>,
) {
    fun findOne(blockId: Long): XroomBlockEntity? {
        return buildJoinedSource()
            .selectAll()
            .where { (XroomBlockTable.id eq blockId) and (XroomBlockTable.deleted eq false) }
            .limit(1)
            .firstOrNull()
            ?.let { row ->
                val type = XroomBlockType.valueOf(row[XroomBlockTable.blockType])
                val factory = factories.find { it.type == type }
                    ?: error("등록되지 않은 X룸 블록 타입: $type")
                factory.createFrom(row)
            }
    }

    private fun buildJoinedSource(): ColumnSet {
        return factories.fold(XroomBlockTable as ColumnSet) { acc, factory ->
            acc.join(factory.childTable, JoinType.LEFT, XroomBlockTable.id, factory.childTable.id)
        }
    }
}
