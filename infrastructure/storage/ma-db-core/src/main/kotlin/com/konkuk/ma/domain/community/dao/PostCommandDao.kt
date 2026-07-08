package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.domain.community.domain.NewPost
import com.konkuk.ma.domain.community.domain.PostDetails
import com.konkuk.ma.domain.community.entity.table.PostTable
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.update
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class PostCommandDao {
    fun save(newPost: NewPost): Long {
        return PostTable.insertAndGetId {
            it[authorId] = newPost.authorId
            it[category] = newPost.category.name
            it[title] = newPost.title
            it[content] = newPost.content
            it[createdBy] = newPost.authorId.toString()
            it[lastModifiedBy] = newPost.authorId.toString()
        }.value
    }

    fun update(postId: Long, details: PostDetails) {
        PostTable.update({ PostTable.id eq postId }) {
            it[category] = details.category.name
            it[title] = details.title
            it[content] = details.content
            it[lastModifiedDate] = LocalDateTime.now()
        }
    }

    fun softDelete(postId: Long, memberId: Long) {
        PostTable.softDelete({ PostTable.id eq postId }, memberId.toString())
    }
}
