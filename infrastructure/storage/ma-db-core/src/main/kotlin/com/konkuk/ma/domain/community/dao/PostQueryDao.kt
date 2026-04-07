package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.domain.community.entity.PostEntity
import com.konkuk.ma.domain.community.entity.table.PostTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Component

@Component
class PostQueryDao {
    fun find(category: String, cursorId: Long?, fetchSize: Int): List<PostEntity> {
        return PostTable
            .selectAll()
            .where {
                val baseCondition = (PostTable.category eq category) and (PostTable.deleted eq false)
                if (cursorId != null) {
                    baseCondition and (PostTable.id less cursorId)
                } else {
                    baseCondition
                }
            }
            .orderBy(PostTable.id to SortOrder.DESC)
            .limit(fetchSize)
            .map { row -> PostEntity.from(row) }
    }
}
