package com.konkuk.ma.domain.xroom.dao

import com.konkuk.ma.config.DatabaseTest
import com.konkuk.ma.config.TestDatabaseConfig
import com.konkuk.ma.domain.xroom.domain.memory.EmotionTags
import com.konkuk.ma.domain.xroom.domain.memory.EventDate
import com.konkuk.ma.domain.xroom.domain.memory.EventDatePrecision
import com.konkuk.ma.domain.xroom.domain.memory.Memory
import com.konkuk.ma.domain.xroom.domain.memory.MemoryContent
import com.konkuk.ma.domain.xroom.domain.memory.MemoryTitle
import com.konkuk.ma.domain.xroom.domain.memory.NewMemory
import com.konkuk.ma.domain.xroom.entity.table.MemoryEmotionTagTable
import com.konkuk.ma.domain.xroom.entity.table.MemoryTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.LocalDate
import java.time.LocalDateTime
import org.jetbrains.exposed.sql.SchemaUtils
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [TestDatabaseConfig::class, MemoryCommandDao::class, MemoryQueryDao::class])
@DatabaseTest
class MemoryDaoTest(
    private val memoryCommandDao: MemoryCommandDao,
    private val memoryQueryDao: MemoryQueryDao,
) : FunSpec() {

    override fun extensions() = listOf(SpringExtension)

    init {
        beforeEach {
            SchemaUtils.create(MemoryTable, MemoryEmotionTagTable)
        }

        afterEach {
            SchemaUtils.drop(MemoryTable, MemoryEmotionTagTable)
        }

        fun newMemory(
            xroomId: Long = 1L,
            title: String = "첫 만남",
            eventDate: String = "2019-05-10",
            eventDatePrecision: String = "DAY",
            location: String? = "서울",
            emotionTags: List<String> = listOf("설렘", "행복"),
            text: String? = "그날의 기억",
            letter: String? = null,
        ) = NewMemory(
            xroomId = xroomId,
            title = title,
            eventDate = eventDate,
            eventDatePrecision = eventDatePrecision,
            location = location,
            emotionTags = emotionTags,
            text = text,
            letter = letter,
        )

        fun memory(
            id: Long,
            xroomId: Long = 1L,
            title: String = "수정된 만남",
            eventDate: LocalDate = LocalDate.of(2020, 8, 15),
            eventDatePrecision: EventDatePrecision = EventDatePrecision.DAY,
            location: String? = "부산",
            emotionTags: List<String> = listOf("그리움"),
            text: String? = "수정된 기억",
            letter: String? = null,
        ) = Memory(
            id = id,
            xroomId = xroomId,
            title = MemoryTitle(title),
            eventDate = EventDate.of(eventDatePrecision, eventDate),
            location = location,
            emotionTags = EmotionTags.of(emotionTags),
            content = MemoryContent.of(text, letter),
            createdDate = LocalDateTime.now(),
        )

        context("save") {

            test("기억을 저장하고 ID를 반환한다") {
                val id = memoryCommandDao.save(newMemory())

                id shouldNotBe null
                id shouldBe 1L
            }

            test("감정 태그가 함께 저장되어 조회 시 반영된다") {
                val tags = listOf("설렘", "행복", "그리움")

                val id = memoryCommandDao.save(newMemory(emotionTags = tags))

                val found = memoryQueryDao.find(1L).first { it.id == id }
                found.emotionTags shouldContainExactlyInAnyOrder tags
            }
        }

        context("find") {

            test("EVENT_DATE 오름차순으로 정렬되어 반환된다") {
                memoryCommandDao.save(newMemory(eventDate = "2020-01-01"))
                memoryCommandDao.save(newMemory(eventDate = "2018-01-01"))
                memoryCommandDao.save(newMemory(eventDate = "2019-01-01"))

                val found = memoryQueryDao.find(1L)

                found.map { it.eventDate.year } shouldContainExactly listOf(2018, 2019, 2020)
            }

            test("EVENT_DATE가 같으면 id 오름차순으로 정렬되어 반환된다") {
                val firstId = memoryCommandDao.save(newMemory(eventDate = "2019-05-10"))
                val secondId = memoryCommandDao.save(newMemory(eventDate = "2019-05-10"))

                val found = memoryQueryDao.find(1L)

                found.map { it.id } shouldContainExactly listOf(firstId, secondId)
            }

            test("감정 태그가 합성되어 함께 반환된다") {
                val tags = listOf("설렘", "행복")
                val id = memoryCommandDao.save(newMemory(emotionTags = tags))

                val found = memoryQueryDao.find(1L).first { it.id == id }

                found.emotionTags shouldContainExactlyInAnyOrder tags
            }

            test("감정 태그가 없는 기억은 빈 리스트로 반환된다") {
                val id = memoryCommandDao.save(newMemory(emotionTags = emptyList()))

                val found = memoryQueryDao.find(1L).first { it.id == id }

                found.emotionTags.shouldBeEmpty()
            }

            test("soft-deleted 기억은 제외된다") {
                memoryCommandDao.save(newMemory(eventDate = "2019-01-01"))
                val deletedId = memoryCommandDao.save(newMemory(eventDate = "2020-01-01"))
                MemoryTable.softDelete({ MemoryTable.id eq deletedId }, "1")

                val found = memoryQueryDao.find(1L)

                found.map { it.id } shouldContainExactly listOf(1L)
            }

            test("해당 방의 기억이 없으면 빈 리스트를 반환한다") {
                memoryCommandDao.save(newMemory(xroomId = 1L))

                memoryQueryDao.find(999L).shouldBeEmpty()
            }
        }

        context("count") {

            test("방별 기억 수를 Map으로 반환한다") {
                memoryCommandDao.save(newMemory(xroomId = 1L))
                memoryCommandDao.save(newMemory(xroomId = 1L))
                memoryCommandDao.save(newMemory(xroomId = 2L))

                val counts = memoryQueryDao.count(setOf(1L, 2L))

                counts[1L] shouldBe 2
                counts[2L] shouldBe 1
            }

            test("soft-deleted 기억은 집계에서 제외된다") {
                memoryCommandDao.save(newMemory(xroomId = 1L))
                val deletedId = memoryCommandDao.save(newMemory(xroomId = 1L))
                MemoryTable.softDelete({ MemoryTable.id eq deletedId }, "1")

                memoryQueryDao.count(setOf(1L))[1L] shouldBe 1
            }

            test("기억이 있는 방만 키로 포함한다") {
                memoryCommandDao.save(newMemory(xroomId = 1L))

                val counts = memoryQueryDao.count(setOf(1L, 2L))

                counts[1L] shouldBe 1
                counts.containsKey(2L) shouldBe false
            }

            test("빈 Set이 입력되면 빈 Map을 반환한다") {
                memoryCommandDao.save(newMemory(xroomId = 1L))

                memoryQueryDao.count(emptySet()).shouldBeEmpty()
            }
        }

        context("findOne") {

            test("저장된 기억을 단건 조회하면 감정 태그가 합성되어 반환된다") {
                val tags = listOf("설렘", "행복")
                val id = memoryCommandDao.save(newMemory(emotionTags = tags))

                val found = memoryQueryDao.findOne(id)

                found shouldNotBe null
                found!!.id shouldBe id
                found.emotionTags shouldContainExactlyInAnyOrder tags
            }

            test("soft-deleted 기억은 null을 반환한다") {
                val id = memoryCommandDao.save(newMemory())
                MemoryTable.softDelete({ MemoryTable.id eq id }, "1")

                memoryQueryDao.findOne(id) shouldBe null
            }

            test("존재하지 않는 id로 조회하면 null을 반환한다") {
                memoryCommandDao.save(newMemory())

                memoryQueryDao.findOne(999L) shouldBe null
            }
        }

        context("update") {

            test("기억 본문이 새 값으로 갱신된다") {
                val id = memoryCommandDao.save(
                    newMemory(
                        title = "첫 만남",
                        eventDate = "2019-05-10",
                        eventDatePrecision = "DAY",
                        location = "서울",
                        text = "그날의 기억",
                    ),
                )
                val updated = memory(
                    id = id,
                    title = "두 번째 만남",
                    eventDate = LocalDate.of(2020, 8, 1),
                    eventDatePrecision = EventDatePrecision.MONTH,
                    location = "부산",
                    text = "바뀐 기억",
                )

                memoryCommandDao.update(updated, memberId = 1L)

                val found = memoryQueryDao.findOne(id)!!
                found.title shouldBe updated.titleValue
                found.eventDate shouldBe updated.eventDate.normalizedDate
                found.eventDatePrecision shouldBe updated.eventDatePrecisionValue
                found.location shouldBe updated.location
                found.text shouldBe updated.text
            }

            test("감정 태그를 다른 태그로 바꾸면 옛 태그는 사라지고 새 태그만 남는다") {
                val oldTags = listOf("설렘", "행복")
                val newTags = listOf("그리움")
                val id = memoryCommandDao.save(newMemory(emotionTags = oldTags))

                memoryCommandDao.update(memory(id = id, emotionTags = newTags), memberId = 1L)

                val found = memoryQueryDao.findOne(id)!!
                found.emotionTags shouldContainExactlyInAnyOrder newTags
            }

            test("text 기억을 letter로 전환하면 text는 null이 되고 letter가 채워진다") {
                val id = memoryCommandDao.save(newMemory(text = "그날의 기억", letter = null))
                val updated = memory(id = id, text = null, letter = "보고 싶었어")

                memoryCommandDao.update(updated, memberId = 1L)

                val found = memoryQueryDao.findOne(id)!!
                found.text shouldBe null
                found.letter shouldBe updated.letter
            }
        }

        context("softDelete") {

            test("삭제하면 단건 조회·목록 조회·집계에서 모두 제외된다") {
                val xroomId = 1L
                val id = memoryCommandDao.save(newMemory(xroomId = xroomId))

                memoryCommandDao.delete(id, memberId = 1L)

                memoryQueryDao.findOne(id) shouldBe null
                memoryQueryDao.find(xroomId).shouldBeEmpty()
                memoryQueryDao.count(setOf(xroomId)).containsKey(xroomId) shouldBe false
            }
        }
    }
}
