package com.konkuk.ma.domain.community.application

import com.konkuk.ma.domain.common.domain.page.CursorIdCondition
import com.konkuk.ma.domain.common.domain.page.CursorResult
import com.konkuk.ma.domain.community.domain.Comments
import com.konkuk.ma.domain.community.domain.PostCategory
import com.konkuk.ma.domain.community.domain.PostDetail
import com.konkuk.ma.domain.community.domain.PostWithAuthor
import com.konkuk.ma.domain.community.domain.Posts
import com.konkuk.ma.domain.community.domain.port.CommentQueryRepository
import com.konkuk.ma.domain.community.domain.port.PostQueryRepository
import com.konkuk.ma.domain.member.domain.Members
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PostQueryService(
    private val postQueryRepository: PostQueryRepository,
    private val commentQueryRepository: CommentQueryRepository,
    private val memberQueryRepository: MemberQueryRepository,
) {
    fun find(category: PostCategory?, cursorCondition: CursorIdCondition): CursorResult<List<PostWithAuthor>> {
        val cursorResult = postQueryRepository.find(category, cursorCondition)
        val posts = Posts(cursorResult.data)
        val members = Members(memberQueryRepository.findByEmails(posts.extractAuthorEmails()))

        return CursorResult(
            data = posts.combineWithAuthors(members),
            hasNext = cursorResult.hasNext,
            nextCursorId = cursorResult.nextCursorId,
        )
    }

    fun findDetail(id: Long): PostDetail {
        val post = postQueryRepository.findOne(id)
        val comments = Comments(commentQueryRepository.find(id))

        val authorEmails = comments.extractAuthorEmails() + post.authorEmail
        val members = Members(memberQueryRepository.findByEmails(authorEmails))

        return PostDetail(
            post = post,
            nickname = members.findNickname(post.authorEmail),
            comments = comments.groupByParent().combineWithAuthors(members),
        )
    }
}
