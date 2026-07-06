package com.konkuk.ma.domain.community.api

import com.konkuk.ma.domain.community.api.response.BlockResponse
import com.konkuk.ma.domain.community.api.response.BlocksResponse
import com.konkuk.ma.domain.community.application.BlockCommandService
import com.konkuk.ma.domain.community.application.BlockQueryService
import com.konkuk.ma.domain.community.domain.block.BlockResult
import com.konkuk.ma.support.security.LoginMember
import com.konkuk.ma.support.security.MemberInfo
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
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
    fun blockPostAuthor(
        @LoginMember memberInfo: MemberInfo,
        @PathVariable postId: Long,
    ): ResponseEntity<BlockResponse> {
        val result = blockCommandService.blockPostAuthor(postId, memberInfo.id)
        return toResponseEntity(result)
    }

    @PostMapping("/comments/{commentId}/author/block")
    fun blockCommentAuthor(
        @LoginMember memberInfo: MemberInfo,
        @PathVariable commentId: Long,
    ): ResponseEntity<BlockResponse> {
        val result = blockCommandService.blockCommentAuthor(commentId, memberInfo.id)
        return toResponseEntity(result)
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

    private fun toResponseEntity(result: BlockResult): ResponseEntity<BlockResponse> {
        val status = if (result.newlyBlocked) HttpStatus.CREATED else HttpStatus.OK
        return ResponseEntity.status(status).body(BlockResponse.from(result))
    }
}
