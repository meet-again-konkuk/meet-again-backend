package com.konkuk.ma.domain.community.application

import com.konkuk.ma.domain.community.domain.CommentDetail
import com.konkuk.ma.domain.community.domain.CommentWithAuthor
import com.konkuk.ma.domain.community.domain.LikeCounts
import com.konkuk.ma.domain.community.domain.LikedIds
import com.konkuk.ma.domain.community.domain.Replies
import com.konkuk.ma.domain.community.domain.Viewer
import com.konkuk.ma.domain.community.domain.port.CommentLikeRepository
import com.konkuk.ma.domain.community.domain.port.CommentQueryRepository
import com.konkuk.ma.domain.member.domain.Members
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CommentQueryService(
    private val commentQueryRepository: CommentQueryRepository,
    private val memberQueryRepository: MemberQueryRepository,
    private val commentLikeRepository: CommentLikeRepository,
) {
    fun findDetail(commentId: Long, viewerId: Long): CommentWithAuthor {
        val rootComment = commentQueryRepository.findOne(commentId)
        rootComment.validateIsRootComment()
        val replies = Replies(commentQueryRepository.findReplies(commentId))
        val commentDetail = CommentDetail(rootComment, replies)
        val commentIds = commentDetail.extractIds()
        val members = Members(memberQueryRepository.findByIds(commentDetail.extractAuthorIds()))
        val likeCounts = LikeCounts.from(commentLikeRepository.count(commentIds))
        val viewer = Viewer(viewerId, LikedIds(commentLikeRepository.findLikedCommentIds(viewerId, commentIds)))

        return commentDetail.combineWithAuthor(members, likeCounts, viewer)
    }
}
