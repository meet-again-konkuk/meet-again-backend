package com.konkuk.ma.domain.community.domain

class Comments(val data: List<Comment>) {

    fun extractAuthorEmails(): Set<String> {
        return data.map { it.authorEmail }.toSet()
    }

    fun groupByParent(): GroupedComments {
        val roots = RootComments(data.filter { !it.hasParent() })
        val replies = Replies(data.filter { it.hasParent() })
        return GroupedComments(roots.groupWith(replies))
    }
}
