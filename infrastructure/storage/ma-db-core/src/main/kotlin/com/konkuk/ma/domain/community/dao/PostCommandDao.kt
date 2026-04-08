package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.domain.community.domain.NewPost
import com.konkuk.ma.domain.community.entity.table.PostTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.update
import org.springframework.stereotype.Component

@Component
class PostCommandDao {
    fun save(newPost: NewPost): Long {
        return PostTable.insertAndGetId {
            it[authorEmail] = newPost.authorEmail
            it[category] = newPost.category.name
            it[title] = newPost.title
            it[content] = newPost.content
            it[createdBy] = newPost.authorEmail
            it[lastModifiedBy] = newPost.authorEmail
        }.value
    }

    fun incrementComments(id: Long) {
        PostTable.update({ PostTable.id eq id }) {
            it[comments] = comments + 1
        }
    }
}
