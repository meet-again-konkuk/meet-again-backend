package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.member.domain.Members

class Replies(val data: List<Comment>) {

    private val byParentId: Map<Long, List<Comment>> = data.groupBy { it.parentCommentId!! }

    fun previewFor(parent: Comment): CommentWithPreviewReplies {
        val sorted = findReplies(parent.id)
        return CommentWithPreviewReplies(
            parent = parent,
            previewReplies = Replies(sorted.take(PREVIEW_COUNT)),
            remainingReplyCount = (sorted.size - PREVIEW_COUNT).coerceAtLeast(0),
        )
    }

    private fun findReplies(parentId: Long): List<Comment> = byParentId[parentId].orEmpty()
        .sortedByDescending { it.createdDate }

    fun extractAuthorEmails(): Set<Email> {
        return data.map { it.authorEmail }.toSet()
    }

    fun extractIds(): List<Long> {
        return data.map { it.id }
    }

    fun combineWithAuthors(members: Members, likeCounts: LikeCounts): List<ReplyWithAuthor> {
        return data.map { reply ->
            ReplyWithAuthor(
                comment = reply,
                nickname = members.findNickname(reply.authorEmail),
                likeCount = likeCounts.countOf(reply.id),
            )
        }
    }

    companion object {
        private const val PREVIEW_COUNT = 3
    }
}
