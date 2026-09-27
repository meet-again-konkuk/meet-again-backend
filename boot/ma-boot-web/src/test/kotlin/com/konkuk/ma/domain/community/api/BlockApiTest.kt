package com.konkuk.ma.domain.community.api

import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.community.application.BlockCommandService
import com.konkuk.ma.domain.community.application.BlockQueryService
import com.konkuk.ma.domain.community.domain.block.BlockResult
import com.konkuk.ma.domain.community.domain.block.BlockView
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.deleteJson
import com.konkuk.ma.extension.getJson
import com.konkuk.ma.extension.pathVariables
import com.konkuk.ma.extension.postJson
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.vocabulary.blockId
import com.konkuk.ma.vocabulary.blockIdPath
import com.konkuk.ma.vocabulary.blocked
import com.konkuk.ma.vocabulary.blockedNickname
import com.konkuk.ma.vocabulary.blocksArray
import com.konkuk.ma.vocabulary.blocksBlockId
import com.konkuk.ma.vocabulary.blocksBlockedAt
import com.konkuk.ma.vocabulary.blocksNickname
import com.konkuk.ma.vocabulary.commentIdPath
import com.konkuk.ma.vocabulary.postIdPath
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import java.time.LocalDateTime

@WebMvcTest(BlockApi::class)
@BaseApiTest
class BlockApiTest(
    private val mockMvc: MockMvc,
    @MockkBean private val blockCommandService: BlockCommandService,
    @MockkBean private val blockQueryService: BlockQueryService,
) : FunSpec({

    test("게시글 작성자 차단 API 문서화") {
        // Given
        every { blockCommandService.blockPostAuthor(1L, 1L) } returns
            BlockResult(blockId = 1L, blockedNickname = "차단된작성자")

        // When & Then
        mockMvc.postJson("/api/community/posts/{postId}/author/block", 1L)
            .andExpect { status { isCreated() } }
            .andDocument(
                "community/block-post-author",
                pathVariables(
                    postIdPath(),
                ),
                responseBody(
                    blockId(),
                    blocked(),
                    blockedNickname(),
                ),
            )
    }

    test("댓글 작성자 차단 API 문서화") {
        // Given
        every { blockCommandService.blockCommentAuthor(1L, 1L) } returns
            BlockResult(blockId = 2L, blockedNickname = "차단된작성자")

        // When & Then
        mockMvc.postJson("/api/community/comments/{commentId}/author/block", 1L)
            .andExpect { status { isCreated() } }
            .andDocument(
                "community/block-comment-author",
                pathVariables(
                    commentIdPath(),
                ),
                responseBody(
                    blockId(),
                    blocked(),
                    blockedNickname(),
                ),
            )
    }

    test("차단 목록 조회 API 문서화") {
        // Given
        every { blockQueryService.findBlocks(1L) } returns listOf(
            BlockView(
                blockId = 1L,
                nickname = "차단된작성자",
                blockedAt = LocalDateTime.of(2026, 7, 7, 10, 30, 0),
            ),
        )

        // When & Then
        mockMvc.getJson("/api/community/blocks")
            .andExpect { status { isOk() } }
            .andDocument(
                "community/find-blocks",
                responseBody(
                    blocksArray(),
                    blocksBlockId(),
                    blocksNickname(),
                    blocksBlockedAt(),
                ),
            )
    }

    test("차단 해제 API 문서화") {
        // Given
        every { blockCommandService.unblock(1L, 1L) } just runs

        // When & Then
        mockMvc.deleteJson("/api/community/blocks/{blockId}", 1L)
            .andExpect { status { isNoContent() } }
            .andDocument(
                "community/unblock",
                pathVariables(
                    blockIdPath(),
                ),
            )
    }
})
