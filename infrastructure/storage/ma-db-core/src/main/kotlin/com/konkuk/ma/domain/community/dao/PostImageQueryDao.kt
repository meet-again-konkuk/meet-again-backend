package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.domain.community.entity.PostImageEntity
import com.konkuk.ma.domain.community.entity.table.PostImageTable
import java.time.LocalDateTime
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Component

@Component
class PostImageQueryDao {
    fun findOneActiveOrNull(postId: Long): PostImageEntity? {
        return PostImageTable
            .activeRows { PostImageTable.postId eq postId }
            .limit(1)
            .firstOrNull()
            ?.let { PostImageEntity.from(it) }
    }

    fun findActiveByPosts(postIds: List<Long>): List<PostImageEntity> {
        if (postIds.isEmpty()) return emptyList()
        return PostImageTable
            .activeRows { PostImageTable.postId inList postIds }
            .map { PostImageEntity.from(it) }
    }

    fun findDeletedBefore(cutoff: LocalDateTime, cursorId: Long?, pageSize: Int): List<PostImageEntity> {
        return PostImageTable
            .selectAll()
            .where {
                val baseCondition = (PostImageTable.deleted eq true) and (PostImageTable.deletedDate less cutoff)
                if (cursorId == null) baseCondition else baseCondition and (PostImageTable.id greater cursorId)
            }
            .orderBy(PostImageTable.id to SortOrder.ASC)
            .limit(pageSize)
            .map { PostImageEntity.from(it) }
    }
}
