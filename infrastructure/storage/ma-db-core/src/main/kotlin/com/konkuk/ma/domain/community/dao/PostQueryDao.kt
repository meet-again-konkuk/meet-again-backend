package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.domain.community.entity.PostEntity
import com.konkuk.ma.domain.community.entity.table.PostTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Component

@Component
class PostQueryDao {
    fun find(category: String, pageSize: Int, offset: Long): List<PostEntity> {
        return PostTable
            .selectAll()
            .where {
                (PostTable.category eq category) and
                    (PostTable.deleted eq false)
            }
            .orderBy(PostTable.id to SortOrder.DESC)
            .limit(pageSize)
            .offset(offset)
            .map { row -> PostEntity.from(row) }
    }

    fun count(category: String): Long {
        return PostTable
            .selectAll()
            .where {
                (PostTable.category eq category) and
                    (PostTable.deleted eq false)
            }
            .count()
    }
}
