package com.konkuk.ma.domain.community.entity

import com.konkuk.ma.domain.community.domain.Post
import com.konkuk.ma.domain.community.domain.PostCategory
import com.konkuk.ma.domain.community.entity.table.PostTable
import org.jetbrains.exposed.sql.ResultRow
import java.time.LocalDateTime

class PostEntity(
    val id: Long,
    val authorId: Long,
    val category: PostCategory,
    val title: String,
    val content: String,
    val createdDate: LocalDateTime,
) {
    fun toDomain(): Post {
        return Post(
            id = id,
            authorId = authorId,
            category = category,
            title = title,
            content = content,
            createdDate = createdDate,
        )
    }

    companion object {
        fun from(row: ResultRow): PostEntity {
            return PostEntity(
                id = row[PostTable.id].value,
                authorId = row[PostTable.authorId],
                category = PostCategory.valueOf(row[PostTable.category]),
                title = row[PostTable.title],
                content = row[PostTable.content],
                createdDate = row[PostTable.createdDate],
            )
        }
    }
}
