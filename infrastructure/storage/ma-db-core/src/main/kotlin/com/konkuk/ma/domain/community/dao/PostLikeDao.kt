package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.domain.community.entity.PostLikeEntity
import com.konkuk.ma.domain.community.entity.table.PostLikeTable
import java.sql.SQLIntegrityConstraintViolationException
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.springframework.stereotype.Component

@Component
class PostLikeDao {
    fun save(postId: Long, memberId: Long) {
        try {
            PostLikeTable.insert {
                it[PostLikeTable.postId] = postId
                it[PostLikeTable.memberId] = memberId
                it[createdBy] = memberId.toString()
                it[lastModifiedBy] = memberId.toString()
            }
        } catch (e: ExposedSQLException) {
            // 복합 유니크(POST_ID, MEMBER_ID) 충돌 = 이미 좋아요한 상태 → 멱등 처리
            if (e.cause !is SQLIntegrityConstraintViolationException) throw e
        }
    }

    fun find(memberId: Long): List<PostLikeEntity> {
        return PostLikeTable
            .activeRows { PostLikeTable.memberId eq memberId }
            .map { PostLikeEntity.from(it) }
    }

    fun count(postId: Long): Int {
        val likeCount = PostLikeTable.id.count()
        return PostLikeTable
            .select(likeCount)
            .where { (PostLikeTable.deleted eq false) and (PostLikeTable.postId eq postId) }
            .single()[likeCount].toInt()
    }

    fun count(postIds: List<Long>): Map<Long, Int> {
        if (postIds.isEmpty()) {
            return emptyMap()
        }
        val likeCount = PostLikeTable.id.count()
        return PostLikeTable
            .select(PostLikeTable.postId, likeCount)
            .where { (PostLikeTable.deleted eq false) and (PostLikeTable.postId inList postIds) }
            .groupBy(PostLikeTable.postId)
            .associate { row -> row[PostLikeTable.postId] to row[likeCount].toInt() }
    }

    fun delete(postId: Long, memberId: Long) {
        PostLikeTable.deleteWhere {
            (PostLikeTable.postId eq postId) and
                (PostLikeTable.memberId eq memberId)
        }
    }

    fun deleteByMember(memberId: Long) {
        PostLikeTable.deleteWhere { PostLikeTable.memberId eq memberId }
    }
}
