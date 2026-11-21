package com.konkuk.ma.domain.member.domain.photo

import com.konkuk.ma.domain.common.domain.file.port.FileUrlResolver
import com.konkuk.ma.domain.member.fixture.MemberPhotoFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk

class MemberPhotoUrlResolverTest : FunSpec({

    val fileUrlResolver = mockk<FileUrlResolver>()
    val resolver = MemberPhotoUrlResolver(fileUrlResolver)

    beforeEach {
        clearAllMocks()
        every { fileUrlResolver.resolve(any()) } answers { "/files/${firstArg<String>()}" }
    }

    context("resolve") {

        test("썸네일이 있으면 썸네일 키를 URL 로 변환한다") {
            val photo = MemberPhotoFixture.create(
                storageKey = "member/profile/1/photo.jpg",
                thumbnailKey = "member/thumbnail/1/thumb_photo.jpg"
            )

            val url = resolver.resolve(photo)

            url shouldBe "/files/${photo.thumbnailKey}"
        }

        test("썸네일이 없으면 원본 키를 URL 로 변환한다") {
            val photo = MemberPhotoFixture.create(
                storageKey = "member/profile/1/photo.jpg",
                thumbnailKey = null
            )

            val url = resolver.resolve(photo)

            url shouldBe "/files/${photo.storageKey}"
        }

    }

    context("resolveByMember") {

        test("회원별로 프로필 이미지 URL 을 담는다") {
            val first = MemberPhotoFixture.create(id = 1L, memberId = 10L, storageKey = "member/profile/10/a.jpg")
            val second = MemberPhotoFixture.create(id = 2L, memberId = 20L, storageKey = "member/profile/20/b.jpg")

            val imageUrls = resolver.resolveByMember(listOf(first, second))

            imageUrls.urlOf(first.memberId) shouldBe "/files/${first.storageKey}"
            imageUrls.urlOf(second.memberId) shouldBe "/files/${second.storageKey}"
        }

        test("썸네일이 있는 회원은 썸네일 URL 을 담는다") {
            val photo = MemberPhotoFixture.create(
                memberId = 10L,
                storageKey = "member/profile/10/photo.jpg",
                thumbnailKey = "member/thumbnail/10/thumb_photo.jpg"
            )

            val imageUrls = resolver.resolveByMember(listOf(photo))

            imageUrls.urlOf(photo.memberId) shouldBe "/files/${photo.thumbnailKey}"
        }

        test("빈 사진 목록이면 어떤 회원의 URL 도 없다") {
            val imageUrls = resolver.resolveByMember(emptyList())

            imageUrls.data.isEmpty() shouldBe true
            imageUrls.urlOf(1L).shouldBeNull()
        }

        test("사진이 없는 회원을 물어보면 null 을 반환한다") {
            val photo = MemberPhotoFixture.create(memberId = 10L)

            val imageUrls = resolver.resolveByMember(listOf(photo))

            imageUrls.urlOf(99L).shouldBeNull()
        }
    }
})
