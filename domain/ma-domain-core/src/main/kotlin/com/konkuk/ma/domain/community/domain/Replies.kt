package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.member.domain.Members

class Replies(val data: List<Comment>) {

    fun combineWithAuthors(members: Members): List<ReplyWithAuthor> {
        return data.map { reply ->
            ReplyWithAuthor(
                comment = reply,
                nickname = members.findNickname(reply.authorEmail),
            )
        }
    }
}
