package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.member.domain.Members

class Comments(val data: List<Comment>) {

    fun extractAuthorEmails(): Set<String> {
        return data.map { it.authorEmail }.toSet()
    }

    fun combineWithAuthors(members: Members): List<CommentWithAuthor> {
        val parentComments = data.filter { !it.hasParent() }
        val repliesByParentId = data.filter { it.hasParent() }
            .groupBy { it.parentCommentId!! }

        return parentComments.map { parent ->
            val allReplies = repliesByParentId[parent.id].orEmpty()
                .sortedByDescending { it.createdDate }
            val previewReplies = allReplies.take(REPLY_PREVIEW_COUNT)
            val remainingCount = (allReplies.size - REPLY_PREVIEW_COUNT).coerceAtLeast(0)

            CommentWithAuthor(
                comment = parent,
                nickname = members.findNickname(parent.authorEmail),
                replies = previewReplies.map { reply ->
                    ReplyWithAuthor(
                        comment = reply,
                        nickname = members.findNickname(reply.authorEmail),
                    )
                },
                remainingReplyCount = remainingCount,
            )
        }
    }

    companion object {
        private const val REPLY_PREVIEW_COUNT = 3
    }
}
