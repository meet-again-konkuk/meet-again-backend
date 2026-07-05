package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.domain.community.entity.CommentEntity
import com.konkuk.ma.domain.community.entity.table.CommentTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Component

@Component
class CommentQueryDao {
    fun findOne(id: Long): CommentEntity? {
        return CommentTable
            .activeRows { CommentTable.id eq id }
            .limit(1)
            .firstOrNull()
            ?.let { CommentEntity.from(it) }
    }

    fun find(postId: Long): List<CommentEntity> {
        return CommentTable
            .selectAll()
            .where { CommentTable.postId eq postId }
            .orderBy(CommentTable.id to SortOrder.ASC)
            .map { row -> CommentEntity.from(row) }
    }

    fun findByAuthor(authorId: Long): List<CommentEntity> {
        return CommentTable
            .activeRows { CommentTable.authorId eq authorId }
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

    fun count(postIds: List<Long>): Map<Long, Int> {
        if (postIds.isEmpty()) {
            return emptyMap()
        }
        val commentCount = CommentTable.id.count()
        return CommentTable
            .select(CommentTable.postId, commentCount)
            .where { (CommentTable.deleted eq false) and (CommentTable.postId inList postIds) }
            .groupBy(CommentTable.postId)
            .associate { row -> row[CommentTable.postId] to row[commentCount].toInt() }
    }
}
