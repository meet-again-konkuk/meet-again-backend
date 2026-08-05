package com.konkuk.ma.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.domain.auth.entity.table.RefreshTokenTable
import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.common.domain.id.port.IdObfuscator
import com.konkuk.ma.domain.matching.entity.table.TargetInfoTable
import com.konkuk.ma.domain.member.entity.table.MemberTable
import com.konkuk.ma.domain.xroom.dao.MemoryCommandDao
import com.konkuk.ma.domain.xroom.domain.memory.NewMemory
import com.konkuk.ma.domain.xroom.domain.port.MemoryCommandRepository
import com.konkuk.ma.domain.xroom.entity.table.MemoryEmotionTagTable
import com.konkuk.ma.domain.xroom.entity.table.MemoryTable
import com.konkuk.ma.domain.xroom.entity.table.XroomTable
import com.konkuk.ma.extension.postJson
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import java.time.LocalDate
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc

/**
 * POST /api/xrooms/with-memories 의 존재 이유인 **원자성**을 관측한다.
 *
 * 도메인 검증 한도와 DB 컬럼 폭이 같아(title 200/200, tag 50/50) API 입력만으로는
 * 트랜잭션 **내부** 실패를 만들 수 없다. 그래서 MemoryCommandRepository 포트를 @MockkBean 으로 대체하고,
 * 실제 DAO로 MEMORIES·MEMORY_EMOTION_TAGS 를 insert 한 직후 예외를 던지게 해
 * "방 insert 가 이미 커밋 대상이 된 뒤 기억 저장이 실패하는" 상황을 재현한다.
 *
 * 포트를 모킹하므로 실제 저장이 필요한 XroomIntegrationTest 와는 스펙을 분리한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class XroomTransactionIntegrationTest(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper,
    private val idObfuscator: IdObfuscator,
    private val memoryCommandDao: MemoryCommandDao,
    @MockkBean private val memoryCommandRepository: MemoryCommandRepository,
) : FunSpec({

    val passwordEncoder = BCryptPasswordEncoder()

    beforeSpec {
        transaction {
            SchemaUtils.create(
                MemberTable,
                RefreshTokenTable,
                TargetInfoTable,
                XroomTable,
                MemoryTable,
                MemoryEmotionTagTable,
            )
        }
    }

    afterEach {
        transaction {
            MemoryEmotionTagTable.deleteAll()
            MemoryTable.deleteAll()
            XroomTable.deleteAll()
            TargetInfoTable.deleteAll()
            RefreshTokenTable.deleteAll()
            MemberTable.deleteAll()
        }
    }

    afterSpec {
        transaction {
            SchemaUtils.drop(
                MemoryEmotionTagTable,
                MemoryTable,
                XroomTable,
                TargetInfoTable,
                RefreshTokenTable,
                MemberTable,
            )
        }
    }

    fun insertMember(
        email: String = "xroom-tx@example.com",
        rawPassword: String = "password123",
    ): Long {
        return transaction {
            MemberTable.insertAndGetId {
                it[MemberTable.email] = email
                it[password] = passwordEncoder.encode(rawPassword)
                it[nickname] = "테스터"
                it[gender] = "MALE"
                it[phoneNumber] = "01012345678"
                it[name] = "김테스트"
                it[birthDate] = LocalDate.of(1990, 1, 1)
                it[region] = "SEOUL"
            }.value
        }
    }

    fun insertTargetInfo(registerId: Long): Long {
        return transaction {
            TargetInfoTable.insertAndGetId {
                it[TargetInfoTable.registerId] = registerId
                it[name] = "홍길동"
                it[targetGender] = "FEMALE"
                it[region] = "SEOUL"
            }.value
        }
    }

    fun login(email: String, password: String): String {
        val result = mockMvc.postJson("/api/auth/login") {
            content = mapper.writeValueAsString(mapOf("email" to email, "password" to password))
        }
            .andExpect { status { isOk() } }
            .andReturn()

        return mapper.readTree(result.response.contentAsString).get("accessToken").asText()
    }

    val firstTitle = "첫 만남"
    val secondTitle = "마지막 인사"

    fun withMemoriesRequest(targetInfoId: Long) = mapOf(
        "targetInfoId" to idObfuscator.encode(ObfuscationType.TARGET_INFO, targetInfoId),
        "finalMessage" to "고마웠어",
        "memories" to listOf(
            mapOf(
                "title" to firstTitle,
                "eventDate" to "2019-05-10",
                "eventDatePrecision" to "DAY",
                "emotionTags" to listOf("설렘", "행복"),
                "text" to "그날의 기억",
            ),
            mapOf(
                "title" to secondTitle,
                "eventDate" to "2020-08-15",
                "eventDatePrecision" to "DAY",
                "emotionTags" to listOf("그리움"),
                "letter" to "보고 싶었어",
            ),
        ),
    )

    fun xroomCount() = transaction { XroomTable.selectAll().count() }

    fun memoryCount() = transaction { MemoryTable.selectAll().count() }

    fun emotionTagCount() = transaction { MemoryEmotionTagTable.selectAll().count() }

    context("POST /api/xrooms/with-memories 트랜잭션 원자성") {

        test("기억 저장이 트랜잭션 안에서 실패하면 방까지 롤백되어 XROOMS row가 남지 않는다") {
            // Given - 방 소유 조건을 갖춘 회원
            val email = "xroom-tx-rollback@example.com"
            val password = "password123"
            val memberId = insertMember(email = email, rawPassword = password)
            val targetInfoId = insertTargetInfo(registerId = memberId)
            val accessToken = login(email, password)

            // Given - 기억을 실제로 insert 한 뒤 실패시킨다 (방 insert 가 이미 커밋 대상이 된 시점)
            every { memoryCommandRepository.saveAll(any()) } answers {
                memoryCommandDao.saveAll(firstArg<List<NewMemory>>())
                throw IllegalStateException("기억 저장 중 장애가 발생했습니다.")
            }

            // When
            mockMvc.postJson("/api/xrooms/with-memories") {
                content = mapper.writeValueAsString(withMemoriesRequest(targetInfoId))
                authorization("Bearer $accessToken")
            }
                .andExpect { status { is5xxServerError() } }

            // Then - 방·기억·감정태그가 전부 롤백되어 한 건도 남지 않는다
            xroomCount() shouldBe 0L
            memoryCount() shouldBe 0L
            emotionTagCount() shouldBe 0L
        }

        test("대조군 - 기억 저장이 정상 동작하면 같은 요청으로 XROOMS row가 1건 남는다") {
            // Given - 롤백 케이스와 동일한 사전 조건·요청
            val email = "xroom-tx-commit@example.com"
            val password = "password123"
            val memberId = insertMember(email = email, rawPassword = password)
            val targetInfoId = insertTargetInfo(registerId = memberId)
            val accessToken = login(email, password)

            // Given - 예외 없이 실제 저장만 수행한다
            every { memoryCommandRepository.saveAll(any()) } answers {
                memoryCommandDao.saveAll(firstArg<List<NewMemory>>())
            }

            // When
            val result = mockMvc.postJson("/api/xrooms/with-memories") {
                content = mapper.writeValueAsString(withMemoriesRequest(targetInfoId))
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isCreated() } }
                .andReturn()

            // Then - 롤백 케이스의 0건이 "원래 안 만들어져서"가 아님을 드러낸다
            xroomCount() shouldBe 1L
            memoryCount() shouldBe 2L
            emotionTagCount() shouldBe 3L

            // Then - 커밋된 방·기억이 응답 식별자와 일치한다
            val body = mapper.readTree(result.response.contentAsString)
            val xroomId = transaction { XroomTable.selectAll().single()[XroomTable.id].value }
            idObfuscator.decode(ObfuscationType.XROOM, body.get("xroomId").asText()) shouldBe xroomId

            val memoryIds = body.get("memoryIds").map { idObfuscator.decode(ObfuscationType.MEMORY, it.asText()) }
            val titleById = transaction {
                MemoryTable.selectAll().associate { it[MemoryTable.id].value to it[MemoryTable.title] }
            }
            memoryIds.map { titleById[it] } shouldContainExactly listOf(firstTitle, secondTitle)
        }
    }
})
