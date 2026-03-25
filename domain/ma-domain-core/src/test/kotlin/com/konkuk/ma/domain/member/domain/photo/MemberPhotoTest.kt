package com.konkuk.ma.domain.member.domain.photo

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
            val result = photo.belongsTo("owner@example.com")

            // Then
            result.shouldBeTrue()
        }

        test("다른 이메일이면 false를 반환한다") {
            // Given
            val photo = MemberPhotoFixture.create(memberEmail = "owner@example.com")

            // When
            val result = photo.belongsTo("other@example.com")

            // Then
            result.shouldBeFalse()
        }

        test("빈 문자열 이메일이면 false를 반환한다") {
            // Given
            val photo = MemberPhotoFixture.create(memberEmail = "owner@example.com")

            // When
            val result = photo.belongsTo("")

            // Then
            result.shouldBeFalse()
        }
    }
})
