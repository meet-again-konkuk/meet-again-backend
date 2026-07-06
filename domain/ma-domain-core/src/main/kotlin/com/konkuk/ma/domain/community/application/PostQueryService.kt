package com.konkuk.ma.domain.community.application

import com.konkuk.ma.domain.common.domain.page.CursorIdCondition
import com.konkuk.ma.domain.common.domain.page.CursorResult
import com.konkuk.ma.domain.community.domain.CommentCounts
import com.konkuk.ma.domain.community.domain.Comments
import com.konkuk.ma.domain.community.domain.LikeCounts
import com.konkuk.ma.domain.community.domain.LikedIds
import com.konkuk.ma.domain.community.domain.PostCategory
import com.konkuk.ma.domain.community.domain.PostDetail
import com.konkuk.ma.domain.community.domain.PostWithAuthor
import com.konkuk.ma.domain.community.domain.Posts
import com.konkuk.ma.domain.community.domain.Viewer
import com.konkuk.ma.domain.community.domain.block.BlockedMemberIds
import com.konkuk.ma.domain.community.domain.port.BlockQueryRepository
import com.konkuk.ma.domain.community.domain.port.CommentLikeRepository
import com.konkuk.ma.domain.community.domain.port.CommentQueryRepository
import com.konkuk.ma.domain.community.domain.port.PostLikeRepository
import com.konkuk.ma.domain.community.domain.port.PostQueryRepository
import com.konkuk.ma.domain.member.domain.Members
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.exception.EntityType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PostQueryService(
    private val postQueryRepository: PostQueryRepository,
    private val commentQueryRepository: CommentQueryRepository,
    private val memberQueryRepository: MemberQueryRepository,
    private val postLikeRepository: PostLikeRepository,
    private val commentLikeRepository: CommentLikeRepository,
    private val blockQueryRepository: BlockQueryRepository,
) {
    fun find(
        category: PostCategory?,
        cursorCondition: CursorIdCondition,
        viewerId: Long,
    ): CursorResult<List<PostWithAuthor>> {
        val blockedMemberIds = blockQueryRepository.findBlockedMemberIds(viewerId)
        val cursorResult = postQueryRepository.find(category, cursorCondition, blockedMemberIds)
        val posts = Posts(cursorResult.data)
        val postIds = posts.extractIds()
        val members = Members(memberQueryRepository.findByIds(posts.extractAuthorIds()))
        val likeCounts = LikeCounts.from(postLikeRepository.count(postIds))
        val commentCounts = CommentCounts.from(commentQueryRepository.count(postIds))
        val viewer = Viewer(
            viewerId,
            LikedIds(postLikeRepository.findLikedPostIds(viewerId, postIds)),
            BlockedMemberIds(blockedMemberIds),
        )

        return CursorResult(
            data = posts.combineWithAuthors(members, likeCounts, commentCounts, viewer),
            hasNext = cursorResult.hasNext,
            nextCursorId = cursorResult.nextCursorId,
        )
    }

    fun findDetail(id: Long, viewerId: Long): PostDetail {
        val post = postQueryRepository.findOne(id)
        val blockedMemberIds = BlockedMemberIds(blockQueryRepository.findBlockedMemberIds(viewerId))
        if (blockedMemberIds.contains(post.authorId)) {
            throw EntityNotFoundException(EntityType.COMMUNITY_POST, id.toString())
        }
        val comments = Comments(commentQueryRepository.find(id))
        val commentIds = comments.extractIds()
        val authorIds = comments.extractAuthorIds() + post.authorId
        val members = Members(memberQueryRepository.findByIds(authorIds))
        val postLikeCount = postLikeRepository.count(listOf(post.id))[post.id] ?: 0
        val commentLikeCounts = LikeCounts.from(commentLikeRepository.count(commentIds))
        val postLikeViewer = Viewer(viewerId, LikedIds(postLikeRepository.findLikedPostIds(viewerId, listOf(post.id))))
        val commentLikeViewer = Viewer(
            viewerId,
            LikedIds(commentLikeRepository.findLikedCommentIds(viewerId, commentIds)),
            blockedMemberIds,
        )

        return PostDetail(
            post = post,
            nickname = members.findNickname(post.authorId),
            likeCount = postLikeCount,
            comments = comments.groupByRootComment(members, commentLikeCounts, commentLikeViewer),
            likedByMe = postLikeViewer.isLikedByMe(post.id),
            isMine = postLikeViewer.isMine(post.authorId),
        )
    }
}
