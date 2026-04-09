package com.konkuk.ma.domain.community.entity

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.community.domain.PostLike
import com.konkuk.ma.domain.community.entity.table.PostLikeTable
import org.jetbrains.exposed.sql.ResultRow

class PostLikeEntity(
    val id: Long,
    val postId: Long,
    val memberEmail: String,
) {
    fun toDomain(): PostLike {
        return PostLike(
            id = id,
            postId = postId,
            memberEmail = Email(memberEmail),
        )
    }

    companion object {
        fun from(row: ResultRow): PostLikeEntity {
            return PostLikeEntity(
                id = row[PostLikeTable.id].value,
                postId = row[PostLikeTable.postId],
                memberEmail = row[PostLikeTable.memberEmail],
            )
        }
    }
}
