package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.domain.community.domain.NewPost
import com.konkuk.ma.domain.community.entity.table.PostTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.update
import org.springframework.stereotype.Component

@Component
class PostCommandDao {
    fun save(newPost: NewPost): Long {
        return PostTable.insertAndGetId {
            it[authorEmail] = newPost.authorEmail.value
            it[category] = newPost.category.name
            it[title] = newPost.title
            it[content] = newPost.content
            it[createdBy] = newPost.authorEmail.value
            it[lastModifiedBy] = newPost.authorEmail.value
        }.value
    }

    fun increaseLikes(postId: Long): Int {
        PostTable.update({ PostTable.id eq postId }) {
            with(SqlExpressionBuilder) {
                it[likes] = likes + 1
            }
        }
        return findLikeCount(postId)
    }

    fun decreaseLikes(postId: Long): Int {
        PostTable.update({ PostTable.id eq postId }) {
            with(SqlExpressionBuilder) {
                it[likes] = likes - 1
            }
        }
        return findLikeCount(postId)
    }

    private fun findLikeCount(postId: Long): Int {
        return PostTable
            .select(PostTable.likes)
            .where { PostTable.id eq postId }
            .limit(1)
            .first()[PostTable.likes]
    }
}
