package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.domain.community.domain.image.NewPostImage
import com.konkuk.ma.domain.community.entity.table.PostImageTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.springframework.stereotype.Component

@Component
class PostImageCommandDao {
    fun save(newPostImage: NewPostImage): Long {
        return PostImageTable.insertAndGetId {
            it[postId] = newPostImage.postId
            it[storageKey] = newPostImage.storageKey
            it[originalFilename] = newPostImage.originalFilename
            it[mimeType] = newPostImage.mimeType
            it[fileSize] = newPostImage.fileSize
            it[thumbnailKey] = newPostImage.thumbnailKey
        }.value
    }

    fun softDeleteByPost(postId: Long, memberId: Long) {
        PostImageTable.softDelete(
            { (PostImageTable.postId eq postId) and (PostImageTable.deleted eq false) },
            memberId.toString(),
        )
    }

    fun delete(imageId: Long) {
        PostImageTable.deleteWhere { PostImageTable.id eq imageId }
    }
}
