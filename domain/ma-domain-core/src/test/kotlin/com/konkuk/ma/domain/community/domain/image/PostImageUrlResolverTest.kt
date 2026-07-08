package com.konkuk.ma.domain.community.domain.image

import com.konkuk.ma.domain.common.domain.file.port.FileUrlResolver
import com.konkuk.ma.domain.community.fixture.PostImageFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk

/**
 * 게시글 이미지 URL 변환기(PostImageUrlResolver) 계약 테스트.
 *
 * storageKey/thumbnailKey 를 FileUrlResolver 로 URL 로 변환한다.
 * MediaUrlResolver 선례 미러 — 썸네일 키가 없으면 thumbnailUrl 은 null, 목록 변환은 postId 별로 담는다.
 */
class PostImageUrlResolverTest : FunSpec({

    val fileUrlResolver = mockk<FileUrlResolver>()
    val resolver = PostImageUrlResolver(fileUrlResolver)

    beforeEach {
        clearAllMocks()
        every { fileUrlResolver.resolve(any()) } answers { "/files/${firstArg<String>()}" }
    }

    context("resolve") {

        test("원본과 썸네일 키를 각각 URL 로 변환한다") {
            // Given
            val image = PostImageFixture.create(
                storageKey = "community/post-image/1/photo.jpg",
                thumbnailKey = "community/thumbnail/1/thumb_photo.jpg",
            )

            // When
            val urls = resolver.resolve(image)

            // Then
            urls.imageUrl shouldBe "/files/${image.storageKey}"
            urls.thumbnailUrl shouldBe "/files/${image.thumbnailKey}"
        }

        test("썸네일 키가 없으면 thumbnailUrl 은 null 이다") {
            // Given
            val image = PostImageFixture.create(thumbnailKey = null)

            // When
            val urls = resolver.resolve(image)

            // Then
            urls.imageUrl shouldBe "/files/${image.storageKey}"
            urls.thumbnailUrl.shouldBeNull()
        }
    }

    context("resolveByPosts") {

        test("게시글별로 imageUrl·thumbnailUrl 을 담는다") {
            // Given
            val first = PostImageFixture.create(postId = 1L, storageKey = "community/post-image/1/a.jpg")
            val second = PostImageFixture.create(postId = 2L, storageKey = "community/post-image/2/b.jpg")

            // When
            val urlsByPost = resolver.resolveByPosts(listOf(first, second))

            // Then
            urlsByPost.imageUrlOf(first.postId) shouldBe "/files/${first.storageKey}"
            urlsByPost.imageUrlOf(second.postId) shouldBe "/files/${second.storageKey}"
        }

        test("빈 목록이면 어떤 게시글의 URL 도 없다") {
            // When
            val urlsByPost = resolver.resolveByPosts(emptyList())

            // Then
            urlsByPost.imageUrlOf(1L).shouldBeNull()
            urlsByPost.thumbnailUrlOf(1L).shouldBeNull()
        }
    }
})
