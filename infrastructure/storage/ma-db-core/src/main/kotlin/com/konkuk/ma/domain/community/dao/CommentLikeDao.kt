package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.domain.community.entity.CommentLikeEntity
import com.konkuk.ma.domain.community.entity.table.CommentLikeTable
import java.sql.SQLIntegrityConstraintViolationException
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.springframework.stereotype.Component

@Component
class CommentLikeDao {
    fun save(commentId: Long, memberId: Long) {
        try {
            CommentLikeTable.insert {
                it[CommentLikeTable.commentId] = commentId
                it[CommentLikeTable.memberId] = memberId
                it[createdBy] = memberId.toString()
                it[lastModifiedBy] = memberId.toString()
            }
        } catch (e: ExposedSQLException) {
            // 복합 유니크(COMMENT_ID, MEMBER_ID) 충돌 = 이미 좋아요한 상태 → 멱등 처리
            if (e.cause !is SQLIntegrityConstraintViolationException) throw e
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
