package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.community.fixture.NewPostFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NewPostTest : FunSpec({

    context("NewPost 객체 생성 테스트") {

        test("authorId 와 details 로 게시글 작성 정보를 조립한다") {
            val newPost = NewPostFixture.create(
                authorId = 42L,
                category = PostCategory.CHEER,
                title = "테스트 게시글",
                content = "테스트 내용입니다.",
            )

            newPost.authorId shouldBe 42L
            newPost.category shouldBe PostCategory.CHEER
            newPost.title shouldBe "테스트 게시글"
            newPost.content shouldBe "테스트 내용입니다."
        }
    }
})
