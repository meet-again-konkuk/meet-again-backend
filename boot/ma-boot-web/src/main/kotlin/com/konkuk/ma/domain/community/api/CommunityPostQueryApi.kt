package com.konkuk.ma.domain.community.api

import com.konkuk.ma.domain.common.domain.page.CursorIdCondition
import com.konkuk.ma.domain.community.api.response.PostResponse
import com.konkuk.ma.domain.community.application.PostQueryService
import com.konkuk.ma.domain.community.domain.PostCategory
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import com.konkuk.ma.support.payload.response.CursorResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/community/posts")
class CommunityPostQueryApi(
    private val postQueryService: PostQueryService,
    private val memberQueryRepository: MemberQueryRepository,
) {
    @GetMapping
    fun findPosts(
        @RequestParam(required = false) category: PostCategory?,
        @RequestParam(required = false) cursorId: Long?,
        @RequestParam(required = false) size: Int?,
    ): CursorResponse<List<PostResponse>> {
        val cursorResult = postQueryService.find(category, CursorIdCondition.of(cursorId, size))
        val posts = cursorResult.data

        val nicknameByEmail = findNicknameByEmail(posts.map { it.authorEmail }.toSet())

        return CursorResponse(
            data = posts.map { post ->
                PostResponse.from(post, nicknameByEmail[post.authorEmail] ?: UNKNOWN_NICKNAME)
            },
            hasNext = cursorResult.hasNext,
            nextCursorId = cursorResult.nextCursorId,
        )
    }

    private fun findNicknameByEmail(emails: Set<String>): Map<String, String> {
        if (emails.isEmpty()) return emptyMap()
        return memberQueryRepository.findByEmails(emails).associate { it.email to it.nickname }
    }

    companion object {
        private const val UNKNOWN_NICKNAME = "알 수 없음"
    }
}
