package com.konkuk.ma.domain.community.entity

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.community.domain.Post
import com.konkuk.ma.domain.community.domain.PostCategory
import com.konkuk.ma.domain.community.entity.table.PostTable
import org.jetbrains.exposed.sql.ResultRow
import java.time.LocalDateTime

class PostEntity(
    val id: Long,
    val authorEmail: String,
    val category: PostCategory,
    val title: String,
    val content: String,
    val likes: Int,
    val createdDate: LocalDateTime,
) {
    fun toDomain(): Post {
        return Post(
            id = id,
            authorEmail = Email(authorEmail),
            category = category,
            title = title,
            content = content,
            likes = likes,
            createdDate = createdDate,
        )
    }

    companion object {
        fun from(row: ResultRow): PostEntity {
            return PostEntity(
                id = row[PostTable.id].value,
                authorEmail = row[PostTable.authorEmail],
                category = PostCategory.valueOf(row[PostTable.category]),
                title = row[PostTable.title],
                content = row[PostTable.content],
                likes = row[PostTable.likes],
                createdDate = row[PostTable.createdDate],
            )
        }
    }
}
