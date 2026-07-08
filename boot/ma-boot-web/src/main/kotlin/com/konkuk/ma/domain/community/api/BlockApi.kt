package com.konkuk.ma.domain.community.api

import com.konkuk.ma.domain.community.api.response.BlockResponse
import com.konkuk.ma.domain.community.api.response.BlocksResponse
import com.konkuk.ma.domain.community.application.BlockCommandService
import com.konkuk.ma.domain.community.application.BlockQueryService
import com.konkuk.ma.support.security.LoginMember
import com.konkuk.ma.support.security.MemberInfo
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/community")
class BlockApi(
    private val blockCommandService: BlockCommandService,
    private val blockQueryService: BlockQueryService,
) {
    @PostMapping("/posts/{postId}/author/block")
    @ResponseStatus(HttpStatus.CREATED)
    fun blockPostAuthor(
        @LoginMember memberInfo: MemberInfo,
        @PathVariable postId: Long,
    ): BlockResponse {
        val result = blockCommandService.blockPostAuthor(postId, memberInfo.id)
        return BlockResponse.from(result)
    }

    @PostMapping("/comments/{commentId}/author/block")
    @ResponseStatus(HttpStatus.CREATED)
    fun blockCommentAuthor(
        @LoginMember memberInfo: MemberInfo,
        @PathVariable commentId: Long,
    ): BlockResponse {
        val result = blockCommandService.blockCommentAuthor(commentId, memberInfo.id)
        return BlockResponse.from(result)
    }

    @GetMapping("/blocks")
    fun findBlocks(
        @LoginMember memberInfo: MemberInfo,
    ): BlocksResponse {
        val blockViews = blockQueryService.findBlocks(memberInfo.id)
        return BlocksResponse.from(blockViews)
    }

    @DeleteMapping("/blocks/{blockId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun unblock(
        @LoginMember memberInfo: MemberInfo,
        @PathVariable blockId: Long,
    ) {
        blockCommandService.unblock(blockId, memberInfo.id)
    }
}
