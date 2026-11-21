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
                it[nickname] = "테스터-$email"
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
            val email = "xroom-tx-rollback@example.com"
            val password = "password123"
            val memberId = insertMember(email = email, rawPassword = password)
            val targetInfoId = insertTargetInfo(registerId = memberId)
            val accessToken = login(email, password)

            every { memoryCommandRepository.saveAll(any()) } answers {
                memoryCommandDao.saveAll(firstArg<List<NewMemory>>())
                throw IllegalStateException("기억 저장 중 장애가 발생했습니다.")
            }

            mockMvc.postJson("/api/xrooms/with-memories") {
                content = mapper.writeValueAsString(withMemoriesRequest(targetInfoId))
                authorization("Bearer $accessToken")
            }
                .andExpect { status { is5xxServerError() } }

            xroomCount() shouldBe 0L
            memoryCount() shouldBe 0L
            emotionTagCount() shouldBe 0L
        }

        test("대조군 - 기억 저장이 정상 동작하면 같은 요청으로 XROOMS row가 1건 남는다") {
            val email = "xroom-tx-commit@example.com"
            val password = "password123"
            val memberId = insertMember(email = email, rawPassword = password)
            val targetInfoId = insertTargetInfo(registerId = memberId)
            val accessToken = login(email, password)

            every { memoryCommandRepository.saveAll(any()) } answers {
                memoryCommandDao.saveAll(firstArg<List<NewMemory>>())
            }

            val result = mockMvc.postJson("/api/xrooms/with-memories") {
                content = mapper.writeValueAsString(withMemoriesRequest(targetInfoId))
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isCreated() } }
                .andReturn()

            xroomCount() shouldBe 1L
            memoryCount() shouldBe 2L
            emotionTagCount() shouldBe 3L

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
