package com.konkuk.ma.domain.member.domain.photo

import com.konkuk.ma.domain.member.fixture.MemberPhotoFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue

class MemberPhotoTest : FunSpec({

    context("belongsTo") {
        test("동일한 memberId면 true를 반환한다") {
            // Given
            val photo = MemberPhotoFixture.create(memberId = 1L)

            // When
            val result = photo.belongsTo(1L)

            // Then
            result.shouldBeTrue()
        }

        test("다른 memberId면 false를 반환한다") {
            // Given
            val photo = MemberPhotoFixture.create(memberId = 1L)

            // When
            val result = photo.belongsTo(2L)

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
