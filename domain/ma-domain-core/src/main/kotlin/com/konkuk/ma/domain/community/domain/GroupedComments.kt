package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.member.domain.Members

class GroupedComments(val data: List<GroupedComment>) {

    fun combineWithAuthors(members: Members): List<CommentWithAuthor> {
        return data.map { it.combineWithAuthor(members) }
    }
}
