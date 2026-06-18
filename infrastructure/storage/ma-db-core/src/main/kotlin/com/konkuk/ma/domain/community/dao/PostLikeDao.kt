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
    fun save(postId: Long, memberEmail: String): Long {
        return PostLikeTable.insertAndGetId {
            it[PostLikeTable.postId] = postId
            it[PostLikeTable.memberEmail] = memberEmail
            it[createdBy] = memberEmail
            it[lastModifiedBy] = memberEmail
        }.value
    }

    fun find(memberEmail: String): List<PostLikeEntity> {
        return PostLikeTable
            .activeRows { PostLikeTable.memberEmail eq memberEmail }
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

    fun delete(postId: Long, memberEmail: String) {
        PostLikeTable.deleteWhere {
            (PostLikeTable.postId eq postId) and
                (PostLikeTable.memberEmail eq memberEmail)
        }
    }

    fun deleteByMember(memberEmail: String) {
        PostLikeTable.deleteWhere { PostLikeTable.memberEmail eq memberEmail }
    }
}
