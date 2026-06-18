package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.domain.community.domain.NewPost
import com.konkuk.ma.domain.community.entity.table.PostTable
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

    fun anonymizeAuthor(oldEmail: String, newEmail: String) {
        PostTable.update({ PostTable.authorEmail eq oldEmail }) {
            it[authorEmail] = newEmail
            it[lastModifiedBy] = newEmail
        }
    }
}
