package com.konkuk.ma.domain.community.entity

import com.konkuk.ma.domain.community.domain.PostLike
import com.konkuk.ma.domain.community.entity.table.PostLikeTable
import org.jetbrains.exposed.sql.ResultRow

class PostLikeEntity(
    val id: Long,
    val postId: Long,
    val memberId: Long,
) {
    fun toDomain(): PostLike {
        return PostLike(
            id = id,
            postId = postId,
            memberId = memberId,
        )
    }

    companion object {
        fun from(row: ResultRow): PostLikeEntity {
            return PostLikeEntity(
                id = row[PostLikeTable.id].value,
                postId = row[PostLikeTable.postId],
                memberId = row[PostLikeTable.memberId],
            )
        }
    }
}
