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
    fun save(commentId: Long, memberId: Long): Long {
        return CommentLikeTable.insertAndGetId {
            it[CommentLikeTable.commentId] = commentId
            it[CommentLikeTable.memberId] = memberId
            it[createdBy] = memberId.toString()
            it[lastModifiedBy] = memberId.toString()
        }.value
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
