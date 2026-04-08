package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.member.domain.Members

class Comments(val data: List<Comment>) {

    fun extractAuthorEmails(): Set<String> {
        return data.map { it.authorEmail }.toSet()
    }

    fun groupByParent(): List<GroupedComment> {
        val roots = RootComments(data.filter { !it.hasParent() })
        val replies = Replies(data.filter { it.hasParent() })
        return roots.groupWith(replies)
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
}
