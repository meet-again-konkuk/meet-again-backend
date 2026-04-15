package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.domain.community.entity.CommentEntity
import com.konkuk.ma.domain.community.entity.table.CommentTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Component

@Component
class CommentQueryDao {
    fun findOne(id: Long): CommentEntity? {
        return CommentTable
            .activeRows { CommentTable.id eq id }
            .map { row -> CommentEntity.from(row) }
            .singleOrNull()
    }

    fun find(postId: Long): List<CommentEntity> {
        return CommentTable
            .selectAll()
            .where { CommentTable.postId eq postId }
            .orderBy(CommentTable.id to SortOrder.ASC)
            .map { row -> CommentEntity.from(row) }
    }

    fun findReplies(parentCommentId: Long): List<CommentEntity> {
        return CommentTable
            .selectAll()
            .where { CommentTable.parentCommentId eq parentCommentId }
            .orderBy(CommentTable.id to SortOrder.DESC)
            .map { row -> CommentEntity.from(row) }
    }
}
