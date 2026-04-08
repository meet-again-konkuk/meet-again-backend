package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.member.domain.Members

class Comments(val data: List<Comment>) {

    fun extractAuthorEmails(): Set<String> {
        return data.map { it.authorEmail }.toSet()
    }

    fun groupByParent(members: Members): List<CommentWithAuthor> {
        val rootComments = RootComments(data.filter { !it.hasParent() })
        val allReplies = data.filter { it.hasParent() }
        val groupedComments = GroupedComments(rootComments.groupWith(allReplies))
        return groupedComments.combineWithAuthors(members)
    }
}
