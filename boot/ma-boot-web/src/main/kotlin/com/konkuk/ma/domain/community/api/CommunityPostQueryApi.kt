package com.konkuk.ma.domain.community.api

import com.konkuk.ma.domain.common.domain.page.CursorRequest
import com.konkuk.ma.domain.community.api.response.PostResponse
import com.konkuk.ma.domain.community.application.PostQueryService
import com.konkuk.ma.domain.community.domain.PostCategory
import com.konkuk.ma.support.payload.response.CursorResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/community/posts")
class CommunityPostQueryApi(
    private val postQueryService: PostQueryService,
) {
    @GetMapping
    fun findPosts(
        @RequestParam(required = false) category: PostCategory?,
        @RequestParam(required = false) cursorId: Long?,
        @RequestParam(required = false) size: Int?,
    ): CursorResponse<List<PostResponse>> {
        val cursorResult = postQueryService.find(category, CursorRequest.of(cursorId, size))
        return CursorResponse(
            data = cursorResult.data.data.map { PostResponse.from(it) },
            hasNext = cursorResult.hasNext,
            nextCursorId = cursorResult.nextCursorId,
        )
    }
}
