package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.member.domain.Members

class GroupedComments(val data: List<GroupedComment>) {

    fun combineWithAuthors(members: Members): List<CommentWithAuthor> {
        return data.map { grouped ->
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
