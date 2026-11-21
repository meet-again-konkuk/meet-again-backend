package com.konkuk.ma.domain.member.domain.photo

import com.konkuk.ma.domain.member.fixture.MemberPhotoFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class MemberPhotoTest : FunSpec({

    context("pickImageKey") {
        test("썸네일이 있으면 썸네일 키를 고른다") {
            val photo = MemberPhotoFixture.create(
                storageKey = "member/profile/1/photo.jpg",
                thumbnailKey = "member/thumbnail/1/thumb_photo.jpg"
            )

            val result = photo.pickImageKey()

            result shouldBe photo.thumbnailKey
        }

        test("썸네일이 없으면 원본 키를 고른다") {
            val photo = MemberPhotoFixture.create(
                storageKey = "member/profile/1/photo.jpg",
                thumbnailKey = null
            )

            val result = photo.pickImageKey()

            result shouldBe photo.storageKey
        }

        test("썸네일이 빈 문자열이면 null이 아니므로 썸네일 키를 그대로 고른다") {
            val photo = MemberPhotoFixture.create(thumbnailKey = "")

            val result = photo.pickImageKey()

            result shouldBe ""
        }
    }
})
