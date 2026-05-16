package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.domain.community.entity.table.PostLikeTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
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
