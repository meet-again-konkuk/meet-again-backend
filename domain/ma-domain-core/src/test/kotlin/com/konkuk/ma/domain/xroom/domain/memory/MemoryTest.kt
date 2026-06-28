package com.konkuk.ma.domain.xroom.domain.memory

import com.konkuk.ma.domain.xroom.fixture.MemoryFixture
import com.konkuk.ma.domain.xroom.fixture.NewMemoryFixture
import com.konkuk.ma.exception.AccessDeniedException
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class MemoryTest : FunSpec({

    context("update") {

        test("변경 가능한 필드는 새 값으로 교체된다") {
            // Given
            val memory = MemoryFixture.create()
            val newMemory = NewMemoryFixture.create(
                title = "두 번째 만남",
                eventDate = "2020-08-15",
                eventDatePrecision = "DAY",
                location = "부산",
                emotionTags = listOf("그리움"),
                text = "바뀐 기억",
            )

            // When
            val updated = memory.update(newMemory)

            // Then
            updated.titleValue shouldBe newMemory.title.value
            updated.eventDateWire shouldBe newMemory.eventDate.toWire()
            updated.location shouldBe newMemory.location
            updated.emotionTagValues shouldContainExactly newMemory.emotionTags.data
            updated.text shouldBe newMemory.content.text
        }

        test("id·xroomId·createdDate는 기존 값이 보존된다") {
            // Given
            val memory = MemoryFixture.create()
            val newMemory = NewMemoryFixture.create(title = "두 번째 만남")

            // When
            val updated = memory.update(newMemory)

            // Then
            updated.id shouldBe memory.id
            updated.xroomId shouldBe memory.xroomId
            updated.createdDate shouldBe memory.createdDate
        }

        test("text 기억을 letter로 교체하면 text는 null이 되고 letter가 채워진다") {
            // Given
            val memory = MemoryFixture.create(
                content = MemoryContent.of(text = "그날의 기억", letter = null),
            )
            val newMemory = NewMemoryFixture.create(text = null, letter = "보고 싶었어")

            // When
            val updated = memory.update(newMemory)

            // Then
            updated.text shouldBe null
            updated.letter shouldBe newMemory.content.letter
        }
    }

    context("validateBelongsTo") {

        test("같은 방의 기억이면 예외가 발생하지 않는다") {
            // Given
            val memory = MemoryFixture.create(xroomId = 1L)

            // When & Then
            shouldNotThrowAny {
                memory.validateBelongsTo(memory.xroomId)
            }
        }

        test("다른 방의 기억이면 접근 권한 예외가 발생한다") {
            // Given
            val memory = MemoryFixture.create(xroomId = 1L)
            val otherXroomId = 2L

            // When & Then
            shouldThrow<AccessDeniedException> {
                memory.validateBelongsTo(otherXroomId)
            }
        }
    }
})
