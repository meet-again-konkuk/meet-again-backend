package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.domain.community.entity.CommentEntity
import com.konkuk.ma.domain.community.entity.table.CommentTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Component

@Component
class CommentQueryDao {
    fun findOne(id: Long): CommentEntity? {
        return CommentTable
            .selectAll()
            .where { (CommentTable.id eq id) and (CommentTable.deleted eq false) }
            .map { row -> CommentEntity.from(row) }
            .singleOrNull()
    }
}
