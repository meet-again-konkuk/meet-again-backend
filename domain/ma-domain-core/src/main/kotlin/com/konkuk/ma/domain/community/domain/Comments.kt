package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.member.domain.Members

class Comments(val data: List<Comment>) {

    fun extractAuthorEmails(): Set<String> {
        return data.map { it.authorEmail }.toSet()
    }

    fun groupByParent(): List<GroupedComment> {
        val parentComments = data.filter { !it.hasParent() }
        val repliesByParentId = data.filter { it.hasParent() }
            .groupBy { it.parentCommentId!! }

        return parentComments.map { parent ->
            val allReplies = repliesByParentId[parent.id].orEmpty()
                .sortedByDescending { it.createdDate }

            GroupedComment(
                parent = parent,
                previewReplies = allReplies.take(REPLY_PREVIEW_COUNT),
                remainingReplyCount = (allReplies.size - REPLY_PREVIEW_COUNT).coerceAtLeast(0),
            )
        }
    }

    fun combineWithAuthors(groupedComments: List<GroupedComment>, members: Members): List<CommentWithAuthor> {
        return groupedComments.map { grouped ->
            CommentWithAuthor(
                comment = grouped.parent,
                nickname = members.findNickname(grouped.parent.authorEmail),
                replies = grouped.previewReplies.map { reply ->
                    ReplyWithAuthor(
                        comment = reply,
                        nickname = members.findNickname(reply.authorEmail),
                    )
                },
                remainingReplyCount = grouped.remainingReplyCount,
            )
        }
    }

    companion object {
        private const val REPLY_PREVIEW_COUNT = 3
    }
}
