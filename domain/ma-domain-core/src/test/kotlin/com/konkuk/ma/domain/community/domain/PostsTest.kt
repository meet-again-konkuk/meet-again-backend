package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.community.fixture.PostFixture
import com.konkuk.ma.domain.matching.fixture.MemberFixture
import com.konkuk.ma.domain.member.domain.Members
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class PostsTest : FunSpec({

    context("extractAuthorEmails") {

        test("게시글 작성자들의 이메일을 추출한다") {
            // Given
            val post1 = PostFixture.create(id = 1L, authorEmail = "author1@example.com")
            val post2 = PostFixture.create(id = 2L, authorEmail = "author2@example.com")
            val posts = Posts(listOf(post1, post2))

            // When
            val result = posts.extractAuthorEmails()

            // Then
            result shouldContainExactlyInAnyOrder setOf(post1.authorEmail, post2.authorEmail)
        }

        test("동일한 작성자가 여러 게시글을 작성해도 이메일이 중복되지 않는다") {
            // Given
            val sameEmail = "same@example.com"
            val post1 = PostFixture.create(id = 1L, authorEmail = sameEmail)
            val post2 = PostFixture.create(id = 2L, authorEmail = sameEmail)
            val posts = Posts(listOf(post1, post2))

            // When
            val result = posts.extractAuthorEmails()

            // Then
            result shouldHaveSize 1
            result.first() shouldBe Email(sameEmail)
        }

        test("게시글이 없으면 빈 Set을 반환한다") {
            // Given
            val posts = Posts(emptyList())

            // When
            val result = posts.extractAuthorEmails()

            // Then
            result shouldHaveSize 0
        }
    }

    context("combineWithAuthors") {

        test("게시글에 작성자 닉네임이 매핑된 PostWithAuthor를 반환한다") {
            // Given
            val post1 = PostFixture.create(id = 1L, authorEmail = "author1@example.com")
            val post2 = PostFixture.create(id = 2L, authorEmail = "author2@example.com")
            val posts = Posts(listOf(post1, post2))
            val members = Members(
                listOf(
                    MemberFixture.create(email = post1.authorEmail.value, nickname = "작성자1"),
                    MemberFixture.create(email = post2.authorEmail.value, nickname = "작성자2"),
                )
            )

            // When
            val result = posts.combineWithAuthors(members, LikeCounts.from(emptyMap()))

            // Then
            result shouldHaveSize 2
            result[0].post shouldBe post1
            result[0].nickname shouldBe "작성자1"
            result[1].post shouldBe post2
            result[1].nickname shouldBe "작성자2"
        }

        test("탈퇴한 회원의 닉네임은 '알 수 없음'으로 표시된다") {
            // Given
            val post = PostFixture.create(id = 1L, authorEmail = "deleted@example.com")
            val posts = Posts(listOf(post))
            val members = Members(emptyList())

            // When
            val result = posts.combineWithAuthors(members, LikeCounts.from(emptyMap()))

            // Then
            result shouldHaveSize 1
            result[0].nickname shouldBe "알 수 없음"
        }

        test("게시글이 없으면 빈 목록을 반환한다") {
            // Given
            val posts = Posts(emptyList())
            val members = Members(emptyList())

            // When
            val result = posts.combineWithAuthors(members, LikeCounts.from(emptyMap()))

            // Then
            result shouldHaveSize 0
        }
    }
})
