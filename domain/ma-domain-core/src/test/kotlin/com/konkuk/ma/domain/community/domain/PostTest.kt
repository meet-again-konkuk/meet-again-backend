package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.community.fixture.PostFixture
import com.konkuk.ma.exception.AccessDeniedException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec

/**
 * 게시글 도메인 모델 Post 의 행위 테스트.
 * 소유권 검증(validateOwnership)은 Comment.validateOwnership 와 동일 규약을 따른다.
 */
class PostTest : FunSpec({

    context("validateOwnership") {

        test("본인 memberId이면 예외 없이 통과한다") {
            val post = PostFixture.create()

            post.validateOwnership(post.authorId)
        }

        test("다른 memberId이면 AccessDeniedException이 발생한다") {
            val post = PostFixture.create()
            val otherMemberId = post.authorId + 1

            shouldThrow<AccessDeniedException> {
                post.validateOwnership(otherMemberId)
            }
        }
    }
})
