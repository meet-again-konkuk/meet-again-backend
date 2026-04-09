package com.konkuk.ma.domain.community.domain.port

import com.konkuk.ma.domain.community.domain.NewComment

interface CommentCommandRepository {
    fun save(newComment: NewComment): Long
    fun increaseLikes(commentId: Long): Int
    fun decreaseLikes(commentId: Long): Int
    fun delete(commentId: Long)
    fun deleteReplies(parentCommentId: Long)
}
