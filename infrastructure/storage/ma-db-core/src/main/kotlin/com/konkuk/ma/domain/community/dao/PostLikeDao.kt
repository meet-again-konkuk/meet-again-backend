package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.domain.community.entity.PostLikeEntity
import com.konkuk.ma.domain.community.entity.table.PostLikeTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.springframework.stereotype.Component

@Component
class PostLikeDao {
    fun save(postId: Long, memberId: Long): Long {
        return PostLikeTable.insertAndGetId {
            it[PostLikeTable.postId] = postId
            it[PostLikeTable.memberId] = memberId
            it[createdBy] = memberId.toString()
            it[lastModifiedBy] = memberId.toString()
        }.value
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

    fun findLikedPostIds(memberId: Long, postIds: List<Long>): Set<Long> {
        if (postIds.isEmpty()) {
            return emptySet()
        }
        return PostLikeTable
            .select(PostLikeTable.postId)
            .where {
                (PostLikeTable.deleted eq false) and
                    (PostLikeTable.memberId eq memberId) and
                    (PostLikeTable.postId inList postIds)
            }
            .map { row -> row[PostLikeTable.postId] }
            .toSet()
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
