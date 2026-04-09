package com.konkuk.ma.domain.member.domain.photo

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.member.fixture.MemberPhotoFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue

class MemberPhotoTest : FunSpec({

    context("belongsTo") {
        test("동일한 이메일이면 true를 반환한다") {
            // Given
            val photo = MemberPhotoFixture.create(memberEmail = "owner@example.com")

            // When
            val result = photo.belongsTo(Email("owner@example.com"))

            // Then
            result.shouldBeTrue()
        }

        test("다른 이메일이면 false를 반환한다") {
            // Given
            val photo = MemberPhotoFixture.create(memberEmail = "owner@example.com")

            // When
            val result = photo.belongsTo(Email("other@example.com"))

            // Then
            result.shouldBeFalse()
        }

        test("다른 이메일이면 false를 반환한다 - 다른 도메인") {
            // Given
            val photo = MemberPhotoFixture.create(memberEmail = "owner@example.com")

            // When
            val result = photo.belongsTo(Email("owner@other.com"))

            // Then
            result.shouldBeFalse()
        }
    }

    context("hasThumbnail") {
        test("thumbnailPath가 존재하면 true를 반환한다") {
            // Given
            val photo = MemberPhotoFixture.create(thumbnailPath = "member/thumbnail/thumb_photo.jpg")

            // When
            val result = photo.hasThumbnail()

            // Then
            result.shouldBeTrue()
        }

        test("thumbnailPath가 null이면 false를 반환한다") {
            // Given
            val photo = MemberPhotoFixture.create(thumbnailPath = null)

            // When
            val result = photo.hasThumbnail()

            // Then
            result.shouldBeFalse()
        }
    }
})
