package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.domain.community.entity.CommentLikeEntity
import com.konkuk.ma.domain.community.entity.table.CommentLikeTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.springframework.stereotype.Component

@Component
class CommentLikeDao {
    fun save(commentId: Long, memberEmail: String): Long {
        return CommentLikeTable.insertAndGetId {
            it[CommentLikeTable.commentId] = commentId
            it[CommentLikeTable.memberEmail] = memberEmail
            it[createdBy] = memberEmail
            it[lastModifiedBy] = memberEmail
        }.value
    }

    fun find(memberEmail: String): List<CommentLikeEntity> {
        return CommentLikeTable
            .activeRows { CommentLikeTable.memberEmail eq memberEmail }
            .map { CommentLikeEntity.from(it) }
    }

    fun count(commentId: Long): Int {
        val likeCount = CommentLikeTable.id.count()
        return CommentLikeTable
            .select(likeCount)
            .where { (CommentLikeTable.deleted eq false) and (CommentLikeTable.commentId eq commentId) }
            .single()[likeCount].toInt()
    }

    fun count(commentIds: List<Long>): Map<Long, Int> {
        if (commentIds.isEmpty()) {
            return emptyMap()
        }
        val likeCount = CommentLikeTable.id.count()
        return CommentLikeTable
            .select(CommentLikeTable.commentId, likeCount)
            .where { (CommentLikeTable.deleted eq false) and (CommentLikeTable.commentId inList commentIds) }
            .groupBy(CommentLikeTable.commentId)
            .associate { row -> row[CommentLikeTable.commentId] to row[likeCount].toInt() }
    }

    fun delete(commentId: Long, memberEmail: String) {
        CommentLikeTable.deleteWhere {
            (CommentLikeTable.commentId eq commentId) and
                (CommentLikeTable.memberEmail eq memberEmail)
        }
    }

    fun deleteByMember(memberEmail: String) {
        CommentLikeTable.deleteWhere { CommentLikeTable.memberEmail eq memberEmail }
    }
}
