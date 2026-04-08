package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.member.domain.Members

class Comments(val data: List<Comment>) {

    fun extractAuthorEmails(): Set<String> {
        return data.map { it.authorEmail }.toSet()
    }

    fun groupByParent(members: Members): List<CommentWithAuthor> {
        val roots = RootComments(data.filter { !it.hasParent() })
        val replies = Replies(data.filter { it.hasParent() })
        val groupedComments = GroupedComments(roots.groupWith(replies))
        return groupedComments.combineWithAuthors(members)
    }
}
