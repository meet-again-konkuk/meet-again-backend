package com.konkuk.ma.domain.xroom.domain.memory

import com.konkuk.ma.domain.xroom.fixture.MemoryDetailsFixture
import com.konkuk.ma.exception.InvalidValueException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NewMemoriesTest : FunSpec({

    val maxSize = 50

    fun details(size: Int): List<MemoryDetails> =
        (1..size).map { MemoryDetailsFixture.create(title = "기억 $it") }

    context("NewMemories 객체 생성") {

        test("기억이 하나도 없으면 InvalidValueException이 발생한다") {
            shouldThrow<InvalidValueException> {
                NewMemories(emptyList())
            }
        }

        test("기억이 상한(50개)을 초과하면 InvalidValueException이 발생한다") {
            shouldThrow<InvalidValueException> {
                NewMemories(details(maxSize + 1))
            }
        }

        test("기억이 1개면 정상 생성된다") {
            val data = details(1)

            NewMemories(data).data shouldBe data
        }

        test("기억이 상한(50개)과 같으면 정상 생성된다") {
            val data = details(maxSize)

            NewMemories(data).data shouldBe data
        }
    }

    context("bindTo") {

        test("모든 기억에 주어진 xroomId를 주입한다") {
            val xroomId = 10L
            val newMemories = NewMemories(details(3))

            val bound = newMemories.bindTo(xroomId)

            bound.map { it.xroomId } shouldBe List(newMemories.data.size) { xroomId }
        }

        test("입력한 기억의 순서를 그대로 보존한다") {
            val data = details(3)
            val newMemories = NewMemories(data)

            val bound = newMemories.bindTo(10L)

            bound.map { it.title.value } shouldBe data.map { it.title.value }
        }

        test("기억의 상세 값을 그대로 옮긴다") {
            val details = MemoryDetailsFixture.create()
            val newMemories = NewMemories(listOf(details))

            val bound = newMemories.bindTo(10L)

            val newMemory = bound.single()
            newMemory.title.value shouldBe details.title.value
            newMemory.eventDate.toWire() shouldBe details.eventDate.toWire()
            newMemory.location shouldBe details.location
            newMemory.emotionTags.data shouldBe details.emotionTags.data
            newMemory.content.text shouldBe details.content.text
            newMemory.content.letter shouldBe details.content.letter
        }
    }
})
