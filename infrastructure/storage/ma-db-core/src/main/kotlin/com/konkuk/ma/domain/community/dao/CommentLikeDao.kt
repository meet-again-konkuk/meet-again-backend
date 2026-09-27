package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.domain.community.entity.CommentLikeEntity
import com.konkuk.ma.domain.community.entity.table.CommentLikeTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.springframework.stereotype.Component

@Component
class CommentLikeDao {
    fun exists(commentId: Long, memberId: Long): Boolean {
        return CommentLikeTable
            .activeRows { (CommentLikeTable.commentId eq commentId) and (CommentLikeTable.memberId eq memberId) }
            .limit(1)
            .any()
    }

    fun save(commentId: Long, memberId: Long) {
        CommentLikeTable.insert {
            it[CommentLikeTable.commentId] = commentId
            it[CommentLikeTable.memberId] = memberId
            it[createdBy] = memberId.toString()
            it[lastModifiedBy] = memberId.toString()
        }
    }

    fun find(memberId: Long): List<CommentLikeEntity> {
        return CommentLikeTable
            .activeRows { CommentLikeTable.memberId eq memberId }
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

    fun findLikedCommentIds(memberId: Long, commentIds: List<Long>): Set<Long> {
        if (commentIds.isEmpty()) {
            return emptySet()
        }
        return CommentLikeTable
            .select(CommentLikeTable.commentId)
            .where {
                (CommentLikeTable.deleted eq false) and
                    (CommentLikeTable.memberId eq memberId) and
                    (CommentLikeTable.commentId inList commentIds)
            }
            .map { row -> row[CommentLikeTable.commentId] }
            .toSet()
    }

    fun delete(commentId: Long, memberId: Long) {
        CommentLikeTable.deleteWhere {
            (CommentLikeTable.commentId eq commentId) and
                (CommentLikeTable.memberId eq memberId)
        }
    }

    fun deleteByMember(memberId: Long) {
        CommentLikeTable.deleteWhere { CommentLikeTable.memberId eq memberId }
    }
}
