package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.domain.community.entity.PostEntity
import com.konkuk.ma.domain.community.entity.table.PostTable
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Component

@Component
class PostQueryDao {
    fun find(category: String?, cursorId: Long?, size: Int): List<PostEntity> {
        return PostTable
            .selectAll()
            .where {
                var condition: Op<Boolean> = PostTable.deleted eq false
                if (category != null) {
                    condition = condition and (PostTable.category eq category)
                }
                if (cursorId != null) {
                    condition = condition and (PostTable.id less cursorId)
                }
                condition
            }
            .orderBy(PostTable.id to SortOrder.DESC)
            .limit(size)
            .map { row -> PostEntity.from(row) }
    }

    fun findOne(id: Long): PostEntity? {
        return PostTable
            .activeRows { PostTable.id eq id }
            .limit(1)
            .firstOrNull()
            ?.let { PostEntity.from(it) }
    }

    fun exists(id: Long): Boolean {
        return PostTable
            .activeRows { PostTable.id eq id }
            .limit(1)
            .any()
    }
}
