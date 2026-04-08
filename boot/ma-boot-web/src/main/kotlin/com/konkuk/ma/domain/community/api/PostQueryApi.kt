package com.konkuk.ma.domain.community.api

import com.konkuk.ma.domain.common.domain.page.CursorIdCondition
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
class PostQueryApi(
    private val postQueryService: PostQueryService,
) {
    @GetMapping
    fun findPosts(
        @RequestParam(required = false) category: PostCategory?,
        @RequestParam(required = false) cursorId: Long?,
        @RequestParam(required = false) size: Int?,
    ): CursorResponse<List<PostResponse>> {
        val cursorResult = postQueryService.find(category, CursorIdCondition.of(cursorId, size))
        return CursorResponse(
            data = cursorResult.data.map { PostResponse.from(it) },
            hasNext = cursorResult.hasNext,
            nextCursorId = cursorResult.nextCursorId,
        )
    }
}
