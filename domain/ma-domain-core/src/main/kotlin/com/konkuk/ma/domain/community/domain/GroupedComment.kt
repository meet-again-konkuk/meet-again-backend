package com.konkuk.ma.domain.community.domain

class GroupedComment(
    val parent: Comment,
    val previewReplies: List<Comment>,
    val remainingReplyCount: Int,
)
