package com.konkuk.ma.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.domain.auth.entity.table.RefreshTokenTable
import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.common.domain.id.port.IdObfuscator
import com.konkuk.ma.domain.matching.domain.ClaimStatus
import com.konkuk.ma.domain.matching.entity.table.MatchingResultTable
import com.konkuk.ma.domain.matching.entity.table.TargetInfoTable
import com.konkuk.ma.domain.member.entity.table.MemberTable
import com.konkuk.ma.domain.xroom.entity.table.MemoryEmotionTagTable
import com.konkuk.ma.domain.xroom.entity.table.MemoryMediaTable
import com.konkuk.ma.domain.xroom.entity.table.MemoryTable
import com.konkuk.ma.domain.xroom.entity.table.XroomTable
import com.konkuk.ma.extension.deleteJson
import com.konkuk.ma.extension.getJson
import com.konkuk.ma.extension.patchJson
import com.konkuk.ma.extension.postJson
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.time.LocalDateTime
import javax.imageio.ImageIO
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.multipart

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class XroomIntegrationTest(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper,
    private val idObfuscator: IdObfuscator,
) : FunSpec({

    val passwordEncoder = BCryptPasswordEncoder()
    val testUploadDir = Paths.get(System.getProperty("java.io.tmpdir"), "meet-again-test", "uploads")

    fun cleanUploadDir() {
        if (Files.exists(testUploadDir)) {
            Files.walk(testUploadDir)
                .sorted(Comparator.reverseOrder())
                .filter { it != testUploadDir }
                .forEach { Files.deleteIfExists(it) }
        }
    }

    beforeSpec {
        transaction {
            SchemaUtils.create(
                MemberTable,
                RefreshTokenTable,
                TargetInfoTable,
                XroomTable,
                MatchingResultTable,
                MemoryTable,
                MemoryEmotionTagTable,
                MemoryMediaTable,
            )
        }
    }

    afterEach {
        transaction {
            MemoryMediaTable.deleteAll()
            MemoryEmotionTagTable.deleteAll()
            MemoryTable.deleteAll()
            XroomTable.deleteAll()
            MatchingResultTable.deleteAll()
            TargetInfoTable.deleteAll()
            RefreshTokenTable.deleteAll()
            MemberTable.deleteAll()
        }
        cleanUploadDir()
    }

    afterSpec {
        transaction {
            SchemaUtils.drop(
                MemoryMediaTable,
                MemoryEmotionTagTable,
                MemoryTable,
                MatchingResultTable,
                XroomTable,
                TargetInfoTable,
                RefreshTokenTable,
                MemberTable,
            )
        }
        cleanUploadDir()
    }

    fun insertMember(
        email: String = "xroom-test@example.com",
        rawPassword: String = "password123",
        name: String = "김테스트",
    ): Long {
        return transaction {
            MemberTable.insertAndGetId {
                it[MemberTable.email] = email
                it[password] = passwordEncoder.encode(rawPassword)
                it[nickname] = "테스터"
                it[gender] = "MALE"
                it[phoneNumber] = "01012345678"
                it[MemberTable.name] = name
                it[birthDate] = LocalDate.of(1990, 1, 1)
                it[region] = "SEOUL"
            }.value
        }
    }

    fun insertMatchingResult(
        registerId: Long,
        targetInfoId: Long,
        targetId: Long,
        claimStatus: ClaimStatus = ClaimStatus.CLAIMED,
    ) {
        transaction {
            MatchingResultTable.insert {
                it[MatchingResultTable.registerId] = registerId
                it[MatchingResultTable.targetInfoId] = targetInfoId
                it[MatchingResultTable.targetId] = targetId
                it[middleNumberMatched] = false
                it[lastNumberMatched] = false
                it[yearMatched] = false
                it[monthMatched] = false
                it[dayMatched] = false
                it[regionMatched] = false
                // isVisible()=true 보장: now > showingExpiryDate - 30일
                it[showingExpiryDate] = LocalDateTime.now().plusDays(7)
                it[matchingExpiryDate] = LocalDate.now().plusDays(30)
                it[excluded] = false
                it[MatchingResultTable.claimStatus] = claimStatus
            }
        }
    }

    fun insertTargetInfo(
        registerId: Long,
        targetName: String = "홍길동",
    ): Long {
        return transaction {
            TargetInfoTable.insertAndGetId {
                it[TargetInfoTable.registerId] = registerId
                it[name] = targetName
                it[targetGender] = "FEMALE"
                it[region] = "SEOUL"
            }.value
        }
    }

    fun insertXroom(
        ownerId: Long,
        targetInfoId: Long,
        title: String = "기억의 방",
        finalMessage: String? = "고마웠어",
    ): Long {
        return transaction {
            XroomTable.insertAndGetId {
                it[XroomTable.ownerId] = ownerId
                it[XroomTable.targetInfoId] = targetInfoId
                it[template] = "chat_memory"
                it[XroomTable.title] = title
                it[XroomTable.finalMessage] = finalMessage
            }.value
        }
    }

    fun login(email: String, password: String): String {
        val request = mapOf("email" to email, "password" to password)
        val result = mockMvc.postJson("/api/auth/login") {
            content = mapper.writeValueAsString(request)
        }
            .andExpect { status { isOk() } }
            .andReturn()

        return mapper.readTree(result.response.contentAsString).get("accessToken").asText()
    }

    fun insertMemory(
        xroomId: Long,
        title: String = "첫 만남",
        eventDate: LocalDate = LocalDate.of(2019, 5, 10),
        eventDatePrecision: String = "DAY",
        text: String = "그날의 기억",
    ): Long {
        return transaction {
            MemoryTable.insertAndGetId {
                it[MemoryTable.xroomId] = xroomId
                it[MemoryTable.title] = title
                it[MemoryTable.eventDate] = eventDate
                it[MemoryTable.eventDatePrecision] = eventDatePrecision
                it[MemoryTable.text] = text
            }.value
        }
    }

    fun createTestPngBytes(): ByteArray {
        val image = BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        graphics.fillRect(0, 0, 800, 600)
        graphics.dispose()

        val outputStream = ByteArrayOutputStream()
        ImageIO.write(image, "png", outputStream)
        return outputStream.toByteArray()
    }

    fun pngPhotoFile(fileName: String = "memory-photo.png") =
        MockMultipartFile("photo", fileName, MediaType.IMAGE_PNG_VALUE, createTestPngBytes())

    fun uploadPhoto(encodedXroomId: String, encodedMemoryId: String, accessToken: String, fileName: String = "memory-photo.png") =
        mockMvc.multipart("/api/xrooms/{xroomId}/memories/{memoryId}/photo", encodedXroomId, encodedMemoryId) {
            file(pngPhotoFile(fileName))
            header("Authorization", "Bearer $accessToken")
        }

    fun activeMedias(memoryId: Long) = transaction {
        MemoryMediaTable.selectAll()
            .where { (MemoryMediaTable.memoryId eq memoryId) and (MemoryMediaTable.deleted eq false) }
            .toList()
    }

    context("GET /api/xrooms/me") {

        test("로그인 후 내가 만든 방 목록을 조회하면 수신자 이름과 함께 반환한다") {
            // Given
            val email = "xroom-me@example.com"
            val password = "password123"
            val memberId = insertMember(email = email, rawPassword = password)
            val targetName = "홍길동"
            val targetInfoId = insertTargetInfo(registerId = memberId, targetName = targetName)
            insertXroom(ownerId = memberId, targetInfoId = targetInfoId)
            val accessToken = login(email, password)

            // When
            val result = mockMvc.getJson("/api/xrooms/me") {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then
            val rooms = mapper.readTree(result.response.contentAsByteArray).get("rooms")
            rooms.size() shouldBe 1
            val room = rooms.get(0)
            room.get("recipientName").asText() shouldBe targetName
            room.get("memoryCount").asInt() shouldBe 0
            room.get("id").asText().isNotBlank() shouldBe true
            room.get("targetInfoId").asText().isNotBlank() shouldBe true
        }

        test("기억을 N개 추가하면 내 방 목록의 memoryCount가 N으로 반환된다") {
            // Given
            val email = "xroom-me-count@example.com"
            val password = "password123"
            val memberId = insertMember(email = email, rawPassword = password)
            val targetInfoId = insertTargetInfo(registerId = memberId)
            val xroomId = insertXroom(ownerId = memberId, targetInfoId = targetInfoId)
            val accessToken = login(email, password)

            val memoryCount = 2
            repeat(memoryCount) { insertMemory(xroomId) }

            // When
            val result = mockMvc.getJson("/api/xrooms/me") {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then
            val rooms = mapper.readTree(result.response.contentAsByteArray).get("rooms")
            rooms.get(0).get("memoryCount").asInt() shouldBe memoryCount
        }

        test("만든 방이 없으면 빈 목록을 반환한다") {
            // Given
            val email = "xroom-empty@example.com"
            val password = "password123"
            insertMember(email = email, rawPassword = password)
            val accessToken = login(email, password)

            // When
            val result = mockMvc.getJson("/api/xrooms/me") {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then
            val rooms = mapper.readTree(result.response.contentAsByteArray).get("rooms")
            rooms.size() shouldBe 0
        }

        test("인증 토큰 없이 내 방 목록을 조회하면 401이 반환된다") {
            // When & Then
            mockMvc.getJson("/api/xrooms/me")
                .andExpect { status { isUnauthorized() } }
        }
    }

    context("POST /api/xrooms") {

        test("로그인 후 본인 TargetInfo로 방을 생성하면 성공하고 내 방 목록에 노출된다") {
            // Given
            val email = "xroom-create@example.com"
            val password = "password123"
            val memberId = insertMember(email = email, rawPassword = password)
            val targetName = "김철수"
            val targetInfoId = insertTargetInfo(registerId = memberId, targetName = targetName)
            val accessToken = login(email, password)

            val request = mapOf(
                "targetInfoId" to idObfuscator.encode(ObfuscationType.TARGET_INFO, targetInfoId),
                "finalMessage" to "보고 싶었어",
            )

            // When
            val createResult = mockMvc.postJson("/api/xrooms") {
                content = mapper.writeValueAsString(request)
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isCreated() } }
                .andReturn()

            // Then - 응답에 암호화된 xroomId가 담긴다
            val createBody = mapper.readTree(createResult.response.contentAsString)
            createBody.get("xroomId").asText().isNotBlank() shouldBe true

            // Then - 생성한 방이 내 방 목록에 노출된다
            val listResult = mockMvc.getJson("/api/xrooms/me") {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            val rooms = mapper.readTree(listResult.response.contentAsByteArray).get("rooms")
            rooms.size() shouldBe 1
            rooms.get(0).get("recipientName").asText() shouldBe targetName
        }

        test("인증 토큰 없이 방을 생성하면 401이 반환된다") {
            // Given
            val request = mapOf(
                "targetInfoId" to idObfuscator.encode(ObfuscationType.TARGET_INFO, 1L),
                "finalMessage" to "보고 싶었어",
            )

            // When & Then
            mockMvc.postJson("/api/xrooms") {
                content = mapper.writeValueAsString(request)
            }
                .andExpect { status { isUnauthorized() } }
        }
    }

    context("POST /api/xrooms/with-memories") {

        fun xroomRows(targetInfoId: Long) = transaction {
            XroomTable.selectAll().where { XroomTable.targetInfoId eq targetInfoId }.toList()
        }

        fun activeMemories(xroomId: Long) = transaction {
            MemoryTable.selectAll()
                .where { (MemoryTable.xroomId eq xroomId) and (MemoryTable.deleted eq false) }
                .toList()
        }

        fun emotionTagsOf(memoryId: Long) = transaction {
            MemoryEmotionTagTable.selectAll()
                .where { MemoryEmotionTagTable.memoryId eq memoryId }
                .map { it[MemoryEmotionTagTable.tag] }
        }

        fun totalXroomCount() = transaction { XroomTable.selectAll().count() }

        test("방과 기억 2개를 일괄 생성하면 201과 함께 방·기억·감정태그가 모두 저장된다") {
            // Given
            val email = "xroom-bulk-create@example.com"
            val password = "password123"
            val memberId = insertMember(email = email, rawPassword = password)
            val targetInfoId = insertTargetInfo(registerId = memberId)
            val accessToken = login(email, password)

            val firstTitle = "첫 만남"
            val firstTags = listOf("설렘", "행복")
            val secondTitle = "마지막 인사"
            val secondTags = listOf("그리움")
            val request = mapOf(
                "targetInfoId" to idObfuscator.encode(ObfuscationType.TARGET_INFO, targetInfoId),
                "finalMessage" to "고마웠어",
                "memories" to listOf(
                    mapOf(
                        "title" to firstTitle,
                        "eventDate" to "2019-05-10",
                        "eventDatePrecision" to "DAY",
                        "location" to "연남동 카페",
                        "emotionTags" to firstTags,
                        "text" to "그날의 기억",
                    ),
                    mapOf(
                        "title" to secondTitle,
                        "eventDate" to "2020-08-15",
                        "eventDatePrecision" to "DAY",
                        "emotionTags" to secondTags,
                        "letter" to "보고 싶었어",
                    ),
                ),
            )

            // When
            val result = mockMvc.postJson("/api/xrooms/with-memories") {
                content = mapper.writeValueAsString(request)
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isCreated() } }
                .andReturn()

            // Then - 응답에 암호화된 xroomId와 요청 개수만큼의 memoryIds가 담긴다
            val body = mapper.readTree(result.response.contentAsString)
            body.get("xroomId").asText().isNotBlank() shouldBe true
            body.get("memoryIds").size() shouldBe 2

            // Then - XROOMS 1건이 응답의 xroomId로 저장된다
            val activeXrooms = xroomRows(targetInfoId).filterNot { it[XroomTable.deleted] }
            activeXrooms.size shouldBe 1
            val xroomId = activeXrooms.single()[XroomTable.id].value
            idObfuscator.decode(ObfuscationType.XROOM, body.get("xroomId").asText()) shouldBe xroomId

            // Then - MEMORIES 2건이 그 방에 저장된다
            val memories = activeMemories(xroomId)
            memories.size shouldBe 2
            memories.map { it[MemoryTable.title] } shouldContainExactlyInAnyOrder listOf(firstTitle, secondTitle)

            // Then - MEMORY_EMOTION_TAGS 에 각 기억의 태그가 반영된다
            val firstMemoryId = memories.single { it[MemoryTable.title] == firstTitle }[MemoryTable.id].value
            val secondMemoryId = memories.single { it[MemoryTable.title] == secondTitle }[MemoryTable.id].value
            emotionTagsOf(firstMemoryId) shouldContainExactlyInAnyOrder firstTags
            emotionTagsOf(secondMemoryId) shouldContainExactlyInAnyOrder secondTags
        }

        test("응답 memoryIds의 순서는 요청 memories의 순서와 같다") {
            // Given
            val email = "xroom-bulk-order@example.com"
            val password = "password123"
            val memberId = insertMember(email = email, rawPassword = password)
            val targetInfoId = insertTargetInfo(registerId = memberId)
            val accessToken = login(email, password)

            // 시점을 뒤섞어 넣어, 응답이 시점 정렬이 아니라 요청 순서를 따르는지 드러낸다
            val titles = listOf("세 번째 만남", "첫 만남", "두 번째 만남")
            val eventDates = listOf("2021-03-03", "2019-01-01", "2020-02-02")
            val request = mapOf(
                "targetInfoId" to idObfuscator.encode(ObfuscationType.TARGET_INFO, targetInfoId),
                "finalMessage" to "고마웠어",
                "memories" to titles.indices.map { index ->
                    mapOf(
                        "title" to titles[index],
                        "eventDate" to eventDates[index],
                        "eventDatePrecision" to "DAY",
                        "emotionTags" to listOf("설렘"),
                        "text" to "그날의 기억",
                    )
                },
            )

            // When
            val result = mockMvc.postJson("/api/xrooms/with-memories") {
                content = mapper.writeValueAsString(request)
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isCreated() } }
                .andReturn()

            // Then - 응답의 암호화 memoryId를 복호화해 DB의 title과 대조하면 요청 순서와 같다
            val body = mapper.readTree(result.response.contentAsString)
            val memoryIds = body.get("memoryIds").map { idObfuscator.decode(ObfuscationType.MEMORY, it.asText()) }
            memoryIds.size shouldBe titles.size

            val titleById = transaction {
                MemoryTable.selectAll()
                    .where { MemoryTable.deleted eq false }
                    .associate { it[MemoryTable.id].value to it[MemoryTable.title] }
            }
            memoryIds.map { titleById[it] } shouldBe titles
        }

        test("기억 하나의 eventDate 형식이 잘못되면 400이 반환되고 XROOMS row가 남지 않는다") {
            // Given
            val email = "xroom-bulk-invalid-date@example.com"
            val password = "password123"
            val memberId = insertMember(email = email, rawPassword = password)
            val targetInfoId = insertTargetInfo(registerId = memberId)
            val accessToken = login(email, password)

            val request = mapOf(
                "targetInfoId" to idObfuscator.encode(ObfuscationType.TARGET_INFO, targetInfoId),
                "finalMessage" to "고마웠어",
                "memories" to listOf(
                    mapOf(
                        "title" to "첫 만남",
                        "eventDate" to "2019-05-10",
                        "eventDatePrecision" to "DAY",
                        "emotionTags" to listOf("설렘"),
                        "text" to "그날의 기억",
                    ),
                    // DAY 정밀도인데 일자 세그먼트가 없다
                    mapOf(
                        "title" to "두 번째 만남",
                        "eventDate" to "2020-08",
                        "eventDatePrecision" to "DAY",
                        "emotionTags" to listOf("그리움"),
                        "text" to "바뀐 기억",
                    ),
                ),
            )

            // When & Then
            mockMvc.postJson("/api/xrooms/with-memories") {
                content = mapper.writeValueAsString(request)
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isBadRequest() } }

            // Then - 방이 절반만 만들어져 남는 일이 없어야 한다 (핵심 회귀 방지)
            totalXroomCount() shouldBe 0L
        }

        test("memories가 빈 배열이면 400이 반환되고 XROOMS row가 남지 않는다") {
            // Given
            val email = "xroom-bulk-empty@example.com"
            val password = "password123"
            val memberId = insertMember(email = email, rawPassword = password)
            val targetInfoId = insertTargetInfo(registerId = memberId)
            val accessToken = login(email, password)

            val request = mapOf(
                "targetInfoId" to idObfuscator.encode(ObfuscationType.TARGET_INFO, targetInfoId),
                "finalMessage" to "고마웠어",
                "memories" to emptyList<Map<String, Any?>>(),
            )

            // When & Then
            mockMvc.postJson("/api/xrooms/with-memories") {
                content = mapper.writeValueAsString(request)
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isBadRequest() } }

            totalXroomCount() shouldBe 0L
        }

        test("memories가 상한(50개)을 초과하면 400이 반환되고 XROOMS row가 남지 않는다") {
            // Given
            val email = "xroom-bulk-over-limit@example.com"
            val password = "password123"
            val memberId = insertMember(email = email, rawPassword = password)
            val targetInfoId = insertTargetInfo(registerId = memberId)
            val accessToken = login(email, password)

            val request = mapOf(
                "targetInfoId" to idObfuscator.encode(ObfuscationType.TARGET_INFO, targetInfoId),
                "finalMessage" to "고마웠어",
                "memories" to (1..51).map {
                    mapOf(
                        "title" to "기억 $it",
                        "eventDate" to "2019-05-10",
                        "eventDatePrecision" to "DAY",
                        "emotionTags" to listOf("설렘"),
                        "text" to "그날의 기억",
                    )
                },
            )

            // When & Then
            mockMvc.postJson("/api/xrooms/with-memories") {
                content = mapper.writeValueAsString(request)
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isBadRequest() } }

            totalXroomCount() shouldBe 0L
        }

        test("같은 TargetInfo에 기억이 있는 활성 방이 있으면 409가 반환된다") {
            // Given
            val email = "xroom-bulk-duplicate@example.com"
            val password = "password123"
            val memberId = insertMember(email = email, rawPassword = password)
            val targetInfoId = insertTargetInfo(registerId = memberId)
            val existingXroomId = insertXroom(ownerId = memberId, targetInfoId = targetInfoId)
            insertMemory(existingXroomId)
            val accessToken = login(email, password)

            val request = mapOf(
                "targetInfoId" to idObfuscator.encode(ObfuscationType.TARGET_INFO, targetInfoId),
                "finalMessage" to "고마웠어",
                "memories" to listOf(
                    mapOf(
                        "title" to "첫 만남",
                        "eventDate" to "2019-05-10",
                        "eventDatePrecision" to "DAY",
                        "emotionTags" to listOf("설렘"),
                        "text" to "그날의 기억",
                    ),
                ),
            )

            // When & Then
            mockMvc.postJson("/api/xrooms/with-memories") {
                content = mapper.writeValueAsString(request)
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isConflict() } }

            // Then - 기존 방은 그대로 살아있고 새 방은 생기지 않는다
            val rows = xroomRows(targetInfoId)
            rows.size shouldBe 1
            rows.single()[XroomTable.deleted] shouldBe false
        }

        test("같은 TargetInfo에 기억이 0개인 활성 방이 있으면 기존 방을 정리하고 201로 새 방을 만든다") {
            // Given - 과거 저장 실패로 남은 빈 방
            val email = "xroom-bulk-empty-room@example.com"
            val password = "password123"
            val memberId = insertMember(email = email, rawPassword = password)
            val targetInfoId = insertTargetInfo(registerId = memberId)
            val emptyXroomId = insertXroom(ownerId = memberId, targetInfoId = targetInfoId)
            val accessToken = login(email, password)

            val request = mapOf(
                "targetInfoId" to idObfuscator.encode(ObfuscationType.TARGET_INFO, targetInfoId),
                "finalMessage" to "고마웠어",
                "memories" to listOf(
                    mapOf(
                        "title" to "첫 만남",
                        "eventDate" to "2019-05-10",
                        "eventDatePrecision" to "DAY",
                        "emotionTags" to listOf("설렘"),
                        "text" to "그날의 기억",
                    ),
                ),
            )

            // When
            val result = mockMvc.postJson("/api/xrooms/with-memories") {
                content = mapper.writeValueAsString(request)
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isCreated() } }
                .andReturn()

            // Then - 기존 빈 방은 soft delete, 활성 방은 새로 만들어진 1건뿐이다
            val rows = xroomRows(targetInfoId)
            rows.size shouldBe 2
            rows.single { it[XroomTable.id].value == emptyXroomId }[XroomTable.deleted] shouldBe true

            val activeXrooms = rows.filterNot { it[XroomTable.deleted] }
            activeXrooms.size shouldBe 1
            val newXroomId = activeXrooms.single()[XroomTable.id].value
            newXroomId shouldNotBe emptyXroomId

            val body = mapper.readTree(result.response.contentAsString)
            idObfuscator.decode(ObfuscationType.XROOM, body.get("xroomId").asText()) shouldBe newXroomId
            activeMemories(newXroomId).size shouldBe 1
        }

        test("남의 TargetInfo로 일괄 생성하면 403이 반환된다") {
            // Given
            val ownerId = insertMember(email = "xroom-bulk-forbidden-owner@example.com")
            val strangerEmail = "xroom-bulk-forbidden-stranger@example.com"
            val strangerPassword = "password123"
            insertMember(email = strangerEmail, rawPassword = strangerPassword)
            val targetInfoId = insertTargetInfo(registerId = ownerId)
            val accessToken = login(strangerEmail, strangerPassword)

            val request = mapOf(
                "targetInfoId" to idObfuscator.encode(ObfuscationType.TARGET_INFO, targetInfoId),
                "finalMessage" to "고마웠어",
                "memories" to listOf(
                    mapOf(
                        "title" to "첫 만남",
                        "eventDate" to "2019-05-10",
                        "eventDatePrecision" to "DAY",
                        "emotionTags" to listOf("설렘"),
                        "text" to "그날의 기억",
                    ),
                ),
            )

            // When & Then
            mockMvc.postJson("/api/xrooms/with-memories") {
                content = mapper.writeValueAsString(request)
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isForbidden() } }

            totalXroomCount() shouldBe 0L
        }

        test("인증 토큰 없이 일괄 생성하면 401이 반환된다") {
            // Given
            val request = mapOf(
                "targetInfoId" to idObfuscator.encode(ObfuscationType.TARGET_INFO, 1L),
                "finalMessage" to "고마웠어",
                "memories" to listOf(
                    mapOf(
                        "title" to "첫 만남",
                        "eventDate" to "2019-05-10",
                        "eventDatePrecision" to "DAY",
                        "emotionTags" to listOf("설렘"),
                        "text" to "그날의 기억",
                    ),
                ),
            )

            // When & Then
            mockMvc.postJson("/api/xrooms/with-memories") {
                content = mapper.writeValueAsString(request)
            }
                .andExpect { status { isUnauthorized() } }
        }
    }

    context("PATCH /api/xrooms/{xroomId}") {

        test("작성자가 로그인 후 마지막 메시지를 수정하면 성공한다") {
            // Given
            val email = "xroom-patch@example.com"
            val password = "password123"
            val memberId = insertMember(email = email, rawPassword = password)
            val targetInfoId = insertTargetInfo(registerId = memberId)
            val xroomId = insertXroom(ownerId = memberId, targetInfoId = targetInfoId)
            val accessToken = login(email, password)

            val encodedXroomId = idObfuscator.encode(ObfuscationType.XROOM, xroomId)
            val request = mapOf("finalMessage" to "마지막 인사를 전해")

            // When
            val result = mockMvc.patchJson("/api/xrooms/{xroomId}", encodedXroomId) {
                content = mapper.writeValueAsString(request)
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then
            val body = mapper.readTree(result.response.contentAsString)
            body.get("xroomId").asText() shouldBe encodedXroomId
        }

        test("인증 토큰 없이 마지막 메시지를 수정하면 401이 반환된다") {
            // Given
            val encodedXroomId = idObfuscator.encode(ObfuscationType.XROOM, 1L)
            val request = mapOf("finalMessage" to "마지막 인사를 전해")

            // When & Then
            mockMvc.patchJson("/api/xrooms/{xroomId}", encodedXroomId) {
                content = mapper.writeValueAsString(request)
            }
                .andExpect { status { isUnauthorized() } }
        }
    }

    context("GET /api/xrooms/received") {

        test("수신자가 로그인하면 자신에게 도착한 방 목록을 보낸 사람 이름과 함께 반환한다") {
            // Given
            val ownerName = "김작성"
            val ownerId = insertMember(email = "xroom-owner@example.com", name = ownerName)
            val recipientPassword = "password123"
            val recipientEmail = "xroom-recipient@example.com"
            val recipientId = insertMember(email = recipientEmail, rawPassword = recipientPassword)
            val targetInfoId = insertTargetInfo(registerId = ownerId)
            insertMatchingResult(
                registerId = ownerId,
                targetInfoId = targetInfoId,
                targetId = recipientId,
                claimStatus = ClaimStatus.CLAIMED,
            )
            insertXroom(ownerId = ownerId, targetInfoId = targetInfoId)
            val accessToken = login(recipientEmail, recipientPassword)

            // When
            val result = mockMvc.getJson("/api/xrooms/received") {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then
            val rooms = mapper.readTree(result.response.contentAsByteArray).get("rooms")
            rooms.size() shouldBe 1
            val room = rooms.get(0)
            room.get("senderName").asText() shouldBe ownerName
            room.get("memoryCount").asInt() shouldBe 0
            room.get("id").asText().isNotBlank() shouldBe true
        }

        test("작성자가 기억을 N개 추가하면 수신함의 memoryCount가 N으로 반환된다") {
            // Given
            val ownerEmail = "xroom-received-count-owner@example.com"
            val ownerPassword = "password123"
            val ownerId = insertMember(email = ownerEmail, rawPassword = ownerPassword)
            val recipientEmail = "xroom-received-count-recipient@example.com"
            val recipientPassword = "password123"
            val recipientId = insertMember(email = recipientEmail, rawPassword = recipientPassword)
            val targetInfoId = insertTargetInfo(registerId = ownerId)
            insertMatchingResult(
                registerId = ownerId,
                targetInfoId = targetInfoId,
                targetId = recipientId,
                claimStatus = ClaimStatus.CLAIMED,
            )
            val xroomId = insertXroom(ownerId = ownerId, targetInfoId = targetInfoId)

            // 작성자가 만든 방에 기억이 쌓인다
            val memoryCount = 3
            repeat(memoryCount) { insertMemory(xroomId) }

            // When - 수신자가 수신함을 조회한다
            val recipientToken = login(recipientEmail, recipientPassword)
            val result = mockMvc.getJson("/api/xrooms/received") {
                authorization("Bearer $recipientToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then
            val rooms = mapper.readTree(result.response.contentAsByteArray).get("rooms")
            rooms.get(0).get("memoryCount").asInt() shouldBe memoryCount
        }

        test("수신자가 claim을 거절(REJECTED)하면 해당 방이 수신함에서 제외된다") {
            // Given
            val ownerId = insertMember(email = "xroom-rejected-received-owner@example.com")
            val recipientEmail = "xroom-rejected-received-recipient@example.com"
            val recipientPassword = "password123"
            val recipientId = insertMember(email = recipientEmail, rawPassword = recipientPassword)
            val targetInfoId = insertTargetInfo(registerId = ownerId)
            insertMatchingResult(
                registerId = ownerId,
                targetInfoId = targetInfoId,
                targetId = recipientId,
                claimStatus = ClaimStatus.REJECTED,
            )
            insertXroom(ownerId = ownerId, targetInfoId = targetInfoId)
            val accessToken = login(recipientEmail, recipientPassword)

            // When
            val result = mockMvc.getJson("/api/xrooms/received") {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then - 거절한 매칭의 방은 수신함에서 사라진다 (거절 = 연결 해제)
            val rooms = mapper.readTree(result.response.contentAsByteArray).get("rooms")
            rooms.size() shouldBe 0
        }

        test("수신한 방이 없으면 빈 목록을 반환한다") {
            // Given
            val email = "xroom-no-received@example.com"
            val password = "password123"
            insertMember(email = email, rawPassword = password)
            val accessToken = login(email, password)

            // When
            val result = mockMvc.getJson("/api/xrooms/received") {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then
            val rooms = mapper.readTree(result.response.contentAsByteArray).get("rooms")
            rooms.size() shouldBe 0
        }

        test("인증 토큰 없이 수신한 방 목록을 조회하면 401이 반환된다") {
            // When & Then
            mockMvc.getJson("/api/xrooms/received")
                .andExpect { status { isUnauthorized() } }
        }
    }

    context("GET /api/xrooms/{xroomId}") {

        test("작성자가 자신의 방을 조회하면 상세 정보를 반환한다") {
            // Given
            val email = "xroom-detail-owner@example.com"
            val password = "password123"
            val ownerId = insertMember(email = email, rawPassword = password)
            val targetName = "홍길동"
            val targetInfoId = insertTargetInfo(registerId = ownerId, targetName = targetName)
            val xroomId = insertXroom(ownerId = ownerId, targetInfoId = targetInfoId)
            val accessToken = login(email, password)

            val encodedXroomId = idObfuscator.encode(ObfuscationType.XROOM, xroomId)

            // When
            val result = mockMvc.getJson("/api/xrooms/{xroomId}", encodedXroomId) {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then
            val body = mapper.readTree(result.response.contentAsByteArray)
            body.get("id").asText() shouldBe encodedXroomId
            body.get("recipientName").asText() shouldBe targetName
            body.get("template").asText() shouldBe "chat_memory"
            body.get("finalMessage").asText().isNotBlank() shouldBe true
            body.get("memories").size() shouldBe 0
        }

        test("수신자가 자신에게 도착한 방을 조회하면 상세 정보를 반환한다") {
            // Given
            val ownerId = insertMember(email = "xroom-detail-sender@example.com")
            val recipientEmail = "xroom-detail-recipient@example.com"
            val recipientPassword = "password123"
            val recipientId = insertMember(email = recipientEmail, rawPassword = recipientPassword)
            val targetInfoId = insertTargetInfo(registerId = ownerId)
            insertMatchingResult(
                registerId = ownerId,
                targetInfoId = targetInfoId,
                targetId = recipientId,
                claimStatus = ClaimStatus.CLAIMED,
            )
            val xroomId = insertXroom(ownerId = ownerId, targetInfoId = targetInfoId)
            val accessToken = login(recipientEmail, recipientPassword)

            val encodedXroomId = idObfuscator.encode(ObfuscationType.XROOM, xroomId)

            // When & Then
            mockMvc.getJson("/api/xrooms/{xroomId}", encodedXroomId) {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
        }

        test("수신자가 거절(REJECTED)한 방의 상세에 접근하면 403이 반환된다") {
            // Given
            val ownerId = insertMember(email = "xroom-rejected-detail-owner@example.com")
            val recipientEmail = "xroom-rejected-detail-recipient@example.com"
            val recipientPassword = "password123"
            val recipientId = insertMember(email = recipientEmail, rawPassword = recipientPassword)
            val targetInfoId = insertTargetInfo(registerId = ownerId)
            insertMatchingResult(
                registerId = ownerId,
                targetInfoId = targetInfoId,
                targetId = recipientId,
                claimStatus = ClaimStatus.REJECTED,
            )
            val xroomId = insertXroom(ownerId = ownerId, targetInfoId = targetInfoId)
            val accessToken = login(recipientEmail, recipientPassword)

            val encodedXroomId = idObfuscator.encode(ObfuscationType.XROOM, xroomId)

            // When & Then - 거절한 수신자는 더 이상 방 상세에 접근할 수 없다
            mockMvc.getJson("/api/xrooms/{xroomId}", encodedXroomId) {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isForbidden() } }
        }

        test("작성자도 수신자도 아니면 403이 반환된다") {
            // Given
            val ownerId = insertMember(email = "xroom-detail-other-owner@example.com")
            val strangerEmail = "xroom-detail-stranger@example.com"
            val strangerPassword = "password123"
            insertMember(email = strangerEmail, rawPassword = strangerPassword)
            val targetInfoId = insertTargetInfo(registerId = ownerId)
            val xroomId = insertXroom(ownerId = ownerId, targetInfoId = targetInfoId)
            val accessToken = login(strangerEmail, strangerPassword)

            val encodedXroomId = idObfuscator.encode(ObfuscationType.XROOM, xroomId)

            // When & Then
            mockMvc.getJson("/api/xrooms/{xroomId}", encodedXroomId) {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isForbidden() } }
        }

        test("인증 토큰 없이 방 상세를 조회하면 401이 반환된다") {
            // Given
            val encodedXroomId = idObfuscator.encode(ObfuscationType.XROOM, 1L)

            // When & Then
            mockMvc.getJson("/api/xrooms/{xroomId}", encodedXroomId)
                .andExpect { status { isUnauthorized() } }
        }
    }

    context("POST /api/xrooms/{xroomId}/memories") {

        test("작성자가 기억을 추가하면 201과 암호화된 memoryId를 반환하고 상세에 노출된다") {
            // Given
            val email = "xroom-memory-add@example.com"
            val password = "password123"
            val ownerId = insertMember(email = email, rawPassword = password)
            val targetInfoId = insertTargetInfo(registerId = ownerId)
            val xroomId = insertXroom(ownerId = ownerId, targetInfoId = targetInfoId)
            val accessToken = login(email, password)
            val encodedXroomId = idObfuscator.encode(ObfuscationType.XROOM, xroomId)

            val title = "첫 만남"
            val eventDate = "2019-05-10"
            val emotionTags = listOf("설렘", "행복")
            val text = "그날의 기억"
            val request = mapOf(
                "title" to title,
                "eventDate" to eventDate,
                "eventDatePrecision" to "DAY",
                "emotionTags" to emotionTags,
                "text" to text,
            )

            // When
            val createResult = mockMvc.postJson("/api/xrooms/{xroomId}/memories", encodedXroomId) {
                content = mapper.writeValueAsString(request)
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isCreated() } }
                .andReturn()

            // Then - 응답에 암호화된 memoryId가 담긴다
            val createBody = mapper.readTree(createResult.response.contentAsString)
            createBody.get("memoryId").asText().isNotBlank() shouldBe true

            // Then - 추가한 기억이 상세 조회에 노출된다
            val detailResult = mockMvc.getJson("/api/xrooms/{xroomId}", encodedXroomId) {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            val memories = mapper.readTree(detailResult.response.contentAsByteArray).get("memories")
            memories.size() shouldBe 1
            val memory = memories.get(0)
            memory.get("title").asText() shouldBe title
            memory.get("eventDate").asText() shouldBe eventDate
            memory.get("eventDatePrecision").asText() shouldBe "DAY"
            memory.get("text").asText() shouldBe text
            memory.get("photoUrl").isNull shouldBe true
            memory.get("emotionTags").map { it.asText() } shouldContainExactlyInAnyOrder emotionTags
        }

        test("여러 기억을 시점을 뒤섞어 추가하면 상세에서 시점 오름차순으로 반환된다") {
            // Given
            val email = "xroom-memory-sort@example.com"
            val password = "password123"
            val ownerId = insertMember(email = email, rawPassword = password)
            val targetInfoId = insertTargetInfo(registerId = ownerId)
            val xroomId = insertXroom(ownerId = ownerId, targetInfoId = targetInfoId)
            val accessToken = login(email, password)
            val encodedXroomId = idObfuscator.encode(ObfuscationType.XROOM, xroomId)

            insertMemory(xroomId, eventDate = LocalDate.of(2021, 3, 3))
            insertMemory(xroomId, eventDate = LocalDate.of(2019, 1, 1))
            insertMemory(xroomId, eventDate = LocalDate.of(2020, 2, 2))

            // When
            val detailResult = mockMvc.getJson("/api/xrooms/{xroomId}", encodedXroomId) {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then
            val memories = mapper.readTree(detailResult.response.contentAsByteArray).get("memories")
            memories.map { it.get("eventDate").asText() } shouldBe
                listOf("2019-01-01", "2020-02-02", "2021-03-03")
        }

        test("작성자가 아닌 수신자가 기억을 추가하면 403이 반환된다") {
            // Given
            val ownerId = insertMember(email = "xroom-memory-owner@example.com")
            val recipientEmail = "xroom-memory-recipient@example.com"
            val recipientPassword = "password123"
            val recipientId = insertMember(email = recipientEmail, rawPassword = recipientPassword)
            val targetInfoId = insertTargetInfo(registerId = ownerId)
            insertMatchingResult(
                registerId = ownerId,
                targetInfoId = targetInfoId,
                targetId = recipientId,
                claimStatus = ClaimStatus.CLAIMED,
            )
            val xroomId = insertXroom(ownerId = ownerId, targetInfoId = targetInfoId)
            val accessToken = login(recipientEmail, recipientPassword)
            val encodedXroomId = idObfuscator.encode(ObfuscationType.XROOM, xroomId)

            val request = mapOf(
                "title" to "첫 만남",
                "eventDate" to "2019-05-10",
                "eventDatePrecision" to "DAY",
                "emotionTags" to listOf("설렘"),
                "text" to "그날의 기억",
            )

            // When & Then
            mockMvc.postJson("/api/xrooms/{xroomId}/memories", encodedXroomId) {
                content = mapper.writeValueAsString(request)
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isForbidden() } }
        }

        test("인증 토큰 없이 기억을 추가하면 401이 반환된다") {
            // Given
            val encodedXroomId = idObfuscator.encode(ObfuscationType.XROOM, 1L)
            val request = mapOf(
                "title" to "첫 만남",
                "eventDate" to "2019-05-10",
                "eventDatePrecision" to "DAY",
                "emotionTags" to listOf("설렘"),
                "text" to "그날의 기억",
            )

            // When & Then
            mockMvc.postJson("/api/xrooms/{xroomId}/memories", encodedXroomId) {
                content = mapper.writeValueAsString(request)
            }
                .andExpect { status { isUnauthorized() } }
        }

        test("text와 letter를 모두 입력하면 400이 반환된다") {
            // Given
            val email = "xroom-memory-both@example.com"
            val password = "password123"
            val memberId = insertMember(email = email, rawPassword = password)
            val targetInfoId = insertTargetInfo(registerId = memberId)
            val xroomId = insertXroom(ownerId = memberId, targetInfoId = targetInfoId)
            val accessToken = login(email, password)
            val encodedXroomId = idObfuscator.encode(ObfuscationType.XROOM, xroomId)

            val request = mapOf(
                "title" to "첫 만남",
                "eventDate" to "2019-05-10",
                "eventDatePrecision" to "DAY",
                "emotionTags" to listOf("설렘"),
                "text" to "그날의 기억",
                "letter" to "보고 싶었어",
            )

            // When & Then
            mockMvc.postJson("/api/xrooms/{xroomId}/memories", encodedXroomId) {
                content = mapper.writeValueAsString(request)
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isBadRequest() } }
        }
    }

    context("PATCH /api/xrooms/{xroomId}/memories/{memoryId}") {

        test("작성자가 기억을 수정하면 200과 암호화된 memoryId를 반환하고 상세에 반영된다") {
            // Given
            val email = "xroom-memory-update@example.com"
            val password = "password123"
            val ownerId = insertMember(email = email, rawPassword = password)
            val targetInfoId = insertTargetInfo(registerId = ownerId)
            val xroomId = insertXroom(ownerId = ownerId, targetInfoId = targetInfoId)
            val accessToken = login(email, password)
            val encodedXroomId = idObfuscator.encode(ObfuscationType.XROOM, xroomId)

            val memoryId = insertMemory(xroomId, title = "첫 만남", text = "그날의 기억")
            val encodedMemoryId = idObfuscator.encode(ObfuscationType.MEMORY, memoryId)

            val newTitle = "두 번째 만남"
            val newEventDate = "2020-08-15"
            val newEmotionTags = listOf("그리움")
            val newText = "바뀐 기억"
            val request = mapOf(
                "title" to newTitle,
                "eventDate" to newEventDate,
                "eventDatePrecision" to "DAY",
                "emotionTags" to newEmotionTags,
                "text" to newText,
            )

            // When
            val result = mockMvc.patchJson(
                "/api/xrooms/{xroomId}/memories/{memoryId}",
                encodedXroomId,
                encodedMemoryId,
            ) {
                content = mapper.writeValueAsString(request)
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then - 응답 memoryId가 암호화된 입력과 일치한다
            val body = mapper.readTree(result.response.contentAsString)
            body.get("memoryId").asText() shouldBe encodedMemoryId

            // Then - 상세 재조회 시 변경된 값이 반영된다
            val detailResult = mockMvc.getJson("/api/xrooms/{xroomId}", encodedXroomId) {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            val memory = mapper.readTree(detailResult.response.contentAsByteArray).get("memories").get(0)
            memory.get("title").asText() shouldBe newTitle
            memory.get("eventDate").asText() shouldBe newEventDate
            memory.get("text").asText() shouldBe newText
            memory.get("emotionTags").map { it.asText() } shouldContainExactlyInAnyOrder newEmotionTags
        }

        test("작성자가 아닌 수신자가 기억을 수정하면 403이 반환된다") {
            // Given
            val ownerId = insertMember(email = "xroom-memory-update-owner@example.com")
            val recipientEmail = "xroom-memory-update-recipient@example.com"
            val recipientPassword = "password123"
            val recipientId = insertMember(email = recipientEmail, rawPassword = recipientPassword)
            val targetInfoId = insertTargetInfo(registerId = ownerId)
            insertMatchingResult(
                registerId = ownerId,
                targetInfoId = targetInfoId,
                targetId = recipientId,
                claimStatus = ClaimStatus.CLAIMED,
            )
            val xroomId = insertXroom(ownerId = ownerId, targetInfoId = targetInfoId)
            val memoryId = insertMemory(xroomId)
            val accessToken = login(recipientEmail, recipientPassword)
            val encodedXroomId = idObfuscator.encode(ObfuscationType.XROOM, xroomId)
            val encodedMemoryId = idObfuscator.encode(ObfuscationType.MEMORY, memoryId)

            val request = mapOf(
                "title" to "두 번째 만남",
                "eventDate" to "2020-08-15",
                "eventDatePrecision" to "DAY",
                "emotionTags" to listOf("그리움"),
                "text" to "바뀐 기억",
            )

            // When & Then
            mockMvc.patchJson(
                "/api/xrooms/{xroomId}/memories/{memoryId}",
                encodedXroomId,
                encodedMemoryId,
            ) {
                content = mapper.writeValueAsString(request)
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isForbidden() } }
        }

        test("다른 방의 memoryId로 수정하면 403이 반환된다") {
            // Given - 한 작성자가 방 A·방 B를 소유, 기억은 방 B에만 존재
            val email = "xroom-memory-update-cross@example.com"
            val password = "password123"
            val ownerId = insertMember(email = email, rawPassword = password)
            val targetInfoIdA = insertTargetInfo(registerId = ownerId)
            val targetInfoIdB = insertTargetInfo(registerId = ownerId)
            val xroomIdA = insertXroom(ownerId = ownerId, targetInfoId = targetInfoIdA, title = "방 A")
            val xroomIdB = insertXroom(ownerId = ownerId, targetInfoId = targetInfoIdB, title = "방 B")
            val memoryIdInB = insertMemory(xroomIdB)
            val accessToken = login(email, password)

            // 방 A 경로로 방 B의 기억을 수정 시도
            val encodedXroomIdA = idObfuscator.encode(ObfuscationType.XROOM, xroomIdA)
            val encodedMemoryIdInB = idObfuscator.encode(ObfuscationType.MEMORY, memoryIdInB)

            val request = mapOf(
                "title" to "두 번째 만남",
                "eventDate" to "2020-08-15",
                "eventDatePrecision" to "DAY",
                "emotionTags" to listOf("그리움"),
                "text" to "바뀐 기억",
            )

            // When & Then
            mockMvc.patchJson(
                "/api/xrooms/{xroomId}/memories/{memoryId}",
                encodedXroomIdA,
                encodedMemoryIdInB,
            ) {
                content = mapper.writeValueAsString(request)
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isForbidden() } }
        }

        test("text와 letter를 모두 입력하면 400이 반환된다") {
            // Given
            val email = "xroom-memory-update-both@example.com"
            val password = "password123"
            val ownerId = insertMember(email = email, rawPassword = password)
            val targetInfoId = insertTargetInfo(registerId = ownerId)
            val xroomId = insertXroom(ownerId = ownerId, targetInfoId = targetInfoId)
            val memoryId = insertMemory(xroomId)
            val accessToken = login(email, password)
            val encodedXroomId = idObfuscator.encode(ObfuscationType.XROOM, xroomId)
            val encodedMemoryId = idObfuscator.encode(ObfuscationType.MEMORY, memoryId)

            val request = mapOf(
                "title" to "두 번째 만남",
                "eventDate" to "2020-08-15",
                "eventDatePrecision" to "DAY",
                "emotionTags" to listOf("그리움"),
                "text" to "바뀐 기억",
                "letter" to "보고 싶었어",
            )

            // When & Then
            mockMvc.patchJson(
                "/api/xrooms/{xroomId}/memories/{memoryId}",
                encodedXroomId,
                encodedMemoryId,
            ) {
                content = mapper.writeValueAsString(request)
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isBadRequest() } }
        }

        test("인증 토큰 없이 기억을 수정하면 401이 반환된다") {
            // Given
            val encodedXroomId = idObfuscator.encode(ObfuscationType.XROOM, 1L)
            val encodedMemoryId = idObfuscator.encode(ObfuscationType.MEMORY, 1L)
            val request = mapOf(
                "title" to "두 번째 만남",
                "eventDate" to "2020-08-15",
                "eventDatePrecision" to "DAY",
                "emotionTags" to listOf("그리움"),
                "text" to "바뀐 기억",
            )

            // When & Then
            mockMvc.patchJson(
                "/api/xrooms/{xroomId}/memories/{memoryId}",
                encodedXroomId,
                encodedMemoryId,
            ) {
                content = mapper.writeValueAsString(request)
            }
                .andExpect { status { isUnauthorized() } }
        }
    }

    context("DELETE /api/xrooms/{xroomId}/memories/{memoryId}") {

        test("작성자가 기억을 삭제하면 200과 deleted=true를 반환하고 상세·카운트에서 사라진다") {
            // Given
            val email = "xroom-memory-delete@example.com"
            val password = "password123"
            val ownerId = insertMember(email = email, rawPassword = password)
            val targetInfoId = insertTargetInfo(registerId = ownerId)
            val xroomId = insertXroom(ownerId = ownerId, targetInfoId = targetInfoId)
            val accessToken = login(email, password)
            val encodedXroomId = idObfuscator.encode(ObfuscationType.XROOM, xroomId)

            val memoryId = insertMemory(xroomId)
            val encodedMemoryId = idObfuscator.encode(ObfuscationType.MEMORY, memoryId)

            // When
            val result = mockMvc.deleteJson(
                "/api/xrooms/{xroomId}/memories/{memoryId}",
                encodedXroomId,
                encodedMemoryId,
            ) {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then - 응답에 암호화된 memoryId와 deleted=true가 담긴다
            val body = mapper.readTree(result.response.contentAsString)
            body.get("memoryId").asText() shouldBe encodedMemoryId
            body.get("deleted").asBoolean() shouldBe true

            // Then - 상세 재조회 시 기억이 사라진다
            val detailResult = mockMvc.getJson("/api/xrooms/{xroomId}", encodedXroomId) {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            mapper.readTree(detailResult.response.contentAsByteArray).get("memories").size() shouldBe 0

            // Then - 내 방 목록의 memoryCount가 0으로 감소한다
            val listResult = mockMvc.getJson("/api/xrooms/me") {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            val rooms = mapper.readTree(listResult.response.contentAsByteArray).get("rooms")
            rooms.get(0).get("memoryCount").asInt() shouldBe 0
        }

        test("작성자가 아닌 수신자가 기억을 삭제하면 403이 반환된다") {
            // Given
            val ownerId = insertMember(email = "xroom-memory-delete-owner@example.com")
            val recipientEmail = "xroom-memory-delete-recipient@example.com"
            val recipientPassword = "password123"
            val recipientId = insertMember(email = recipientEmail, rawPassword = recipientPassword)
            val targetInfoId = insertTargetInfo(registerId = ownerId)
            insertMatchingResult(
                registerId = ownerId,
                targetInfoId = targetInfoId,
                targetId = recipientId,
                claimStatus = ClaimStatus.CLAIMED,
            )
            val xroomId = insertXroom(ownerId = ownerId, targetInfoId = targetInfoId)
            val memoryId = insertMemory(xroomId)
            val accessToken = login(recipientEmail, recipientPassword)
            val encodedXroomId = idObfuscator.encode(ObfuscationType.XROOM, xroomId)
            val encodedMemoryId = idObfuscator.encode(ObfuscationType.MEMORY, memoryId)

            // When & Then
            mockMvc.deleteJson(
                "/api/xrooms/{xroomId}/memories/{memoryId}",
                encodedXroomId,
                encodedMemoryId,
            ) {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isForbidden() } }
        }

        test("다른 방의 memoryId로 삭제하면 403이 반환된다") {
            // Given - 한 작성자가 방 A·방 B를 소유, 기억은 방 B에만 존재
            val email = "xroom-memory-delete-cross@example.com"
            val password = "password123"
            val ownerId = insertMember(email = email, rawPassword = password)
            val targetInfoIdA = insertTargetInfo(registerId = ownerId)
            val targetInfoIdB = insertTargetInfo(registerId = ownerId)
            val xroomIdA = insertXroom(ownerId = ownerId, targetInfoId = targetInfoIdA, title = "방 A")
            val xroomIdB = insertXroom(ownerId = ownerId, targetInfoId = targetInfoIdB, title = "방 B")
            val memoryIdInB = insertMemory(xroomIdB)
            val accessToken = login(email, password)

            val encodedXroomIdA = idObfuscator.encode(ObfuscationType.XROOM, xroomIdA)
            val encodedMemoryIdInB = idObfuscator.encode(ObfuscationType.MEMORY, memoryIdInB)

            // When & Then - 방 A 경로로 방 B의 기억을 삭제 시도
            mockMvc.deleteJson(
                "/api/xrooms/{xroomId}/memories/{memoryId}",
                encodedXroomIdA,
                encodedMemoryIdInB,
            ) {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isForbidden() } }
        }

        test("인증 토큰 없이 기억을 삭제하면 401이 반환된다") {
            // Given
            val encodedXroomId = idObfuscator.encode(ObfuscationType.XROOM, 1L)
            val encodedMemoryId = idObfuscator.encode(ObfuscationType.MEMORY, 1L)

            // When & Then
            mockMvc.deleteJson(
                "/api/xrooms/{xroomId}/memories/{memoryId}",
                encodedXroomId,
                encodedMemoryId,
            )
                .andExpect { status { isUnauthorized() } }
        }
    }

    context("POST /api/xrooms/{xroomId}/memories/{memoryId}/photo") {

        test("작성자가 사진을 업로드하면 201과 함께 MEMORY_MEDIA에 active 1행이 적재되고 응답에 photoUrl·thumbnailUrl이 담긴다") {
            // Given
            val email = "xroom-photo-upload@example.com"
            val password = "password123"
            val ownerId = insertMember(email = email, rawPassword = password)
            val targetInfoId = insertTargetInfo(registerId = ownerId)
            val xroomId = insertXroom(ownerId = ownerId, targetInfoId = targetInfoId)
            val memoryId = insertMemory(xroomId)
            val accessToken = login(email, password)
            val encodedXroomId = idObfuscator.encode(ObfuscationType.XROOM, xroomId)
            val encodedMemoryId = idObfuscator.encode(ObfuscationType.MEMORY, memoryId)

            val photo = pngPhotoFile("memory-photo.png")

            // When
            val result = mockMvc.multipart(
                "/api/xrooms/{xroomId}/memories/{memoryId}/photo",
                encodedXroomId,
                encodedMemoryId,
            ) {
                file(photo)
                header("Authorization", "Bearer $accessToken")
            }
                .andExpect { status { isCreated() } }
                .andReturn()

            // Then - MEMORY_MEDIA에 active 1행이 상대 storageKey·mimeType·fileSize와 함께 적재된다
            val medias = activeMedias(memoryId)
            medias.size shouldBe 1
            val storageKey = medias[0][MemoryMediaTable.storageKey]
            storageKey.startsWith("memory/memory-photo/$memoryId/") shouldBe true
            storageKey.startsWith("/") shouldBe false
            medias[0][MemoryMediaTable.mimeType] shouldBe MediaType.IMAGE_PNG_VALUE
            medias[0][MemoryMediaTable.fileSize] shouldBe photo.size

            // Then - 응답의 photoUrl은 "/files/" + storageKey, thumbnailUrl도 채워진다
            val body = mapper.readTree(result.response.contentAsString)
            body.get("mediaId").asText().isNotBlank() shouldBe true
            body.get("photoUrl").asText() shouldBe "/files/$storageKey"
            body.get("thumbnailUrl").asText().startsWith("/files/memory/thumbnail/$memoryId/") shouldBe true
        }

        test("같은 기억에 사진을 재업로드하면 active 1행만 유지되고 기존 행은 soft delete 된다") {
            // Given
            val email = "xroom-photo-replace@example.com"
            val password = "password123"
            val ownerId = insertMember(email = email, rawPassword = password)
            val targetInfoId = insertTargetInfo(registerId = ownerId)
            val xroomId = insertXroom(ownerId = ownerId, targetInfoId = targetInfoId)
            val memoryId = insertMemory(xroomId)
            val accessToken = login(email, password)
            val encodedXroomId = idObfuscator.encode(ObfuscationType.XROOM, xroomId)
            val encodedMemoryId = idObfuscator.encode(ObfuscationType.MEMORY, memoryId)

            uploadPhoto(encodedXroomId, encodedMemoryId, accessToken, "first.png")
                .andExpect { status { isCreated() } }

            // When - 두 번째 업로드
            uploadPhoto(encodedXroomId, encodedMemoryId, accessToken, "second.png")
                .andExpect { status { isCreated() } }

            // Then - active는 1행(두 번째), 전체는 2행(첫 번째는 soft delete)
            val active = activeMedias(memoryId)
            active.size shouldBe 1
            active[0][MemoryMediaTable.originalFilename] shouldBe "second.png"

            val total = transaction {
                MemoryMediaTable.selectAll().where { MemoryMediaTable.memoryId eq memoryId }.toList()
            }
            total.size shouldBe 2
            total.count { it[MemoryMediaTable.deleted] } shouldBe 1
        }

        test("업로드한 사진은 정적 경로로 서빙되어 200을 반환한다") {
            // Given
            val email = "xroom-photo-serve@example.com"
            val password = "password123"
            val ownerId = insertMember(email = email, rawPassword = password)
            val targetInfoId = insertTargetInfo(registerId = ownerId)
            val xroomId = insertXroom(ownerId = ownerId, targetInfoId = targetInfoId)
            val memoryId = insertMemory(xroomId)
            val accessToken = login(email, password)
            val encodedXroomId = idObfuscator.encode(ObfuscationType.XROOM, xroomId)
            val encodedMemoryId = idObfuscator.encode(ObfuscationType.MEMORY, memoryId)

            val uploadResult = uploadPhoto(encodedXroomId, encodedMemoryId, accessToken)
                .andExpect { status { isCreated() } }
                .andReturn()
            val photoUrl = mapper.readTree(uploadResult.response.contentAsString).get("photoUrl").asText()

            // When & Then - 업로드된 실제 파일이 resource handler로 서빙된다.
            // /files/memory/** 는 permitAll 이므로 토큰 없이도(브라우저 <img> 처럼) 200을 반환한다.
            mockMvc.get(photoUrl)
                .andExpect { status { isOk() } }
        }

        test("작성자가 아닌 수신자가 사진을 업로드하면 403이 반환된다") {
            // Given
            val ownerId = insertMember(email = "xroom-photo-owner@example.com")
            val recipientEmail = "xroom-photo-recipient@example.com"
            val recipientPassword = "password123"
            val recipientId = insertMember(email = recipientEmail, rawPassword = recipientPassword)
            val targetInfoId = insertTargetInfo(registerId = ownerId)
            insertMatchingResult(
                registerId = ownerId,
                targetInfoId = targetInfoId,
                targetId = recipientId,
                claimStatus = ClaimStatus.CLAIMED,
            )
            val xroomId = insertXroom(ownerId = ownerId, targetInfoId = targetInfoId)
            val memoryId = insertMemory(xroomId)
            val accessToken = login(recipientEmail, recipientPassword)
            val encodedXroomId = idObfuscator.encode(ObfuscationType.XROOM, xroomId)
            val encodedMemoryId = idObfuscator.encode(ObfuscationType.MEMORY, memoryId)

            // When & Then
            uploadPhoto(encodedXroomId, encodedMemoryId, accessToken)
                .andExpect { status { isForbidden() } }
        }

        test("다른 방의 memoryId로 사진을 업로드하면 403이 반환된다") {
            // Given - 한 작성자가 방 A·방 B를 소유, 기억은 방 B에만 존재
            val email = "xroom-photo-cross@example.com"
            val password = "password123"
            val ownerId = insertMember(email = email, rawPassword = password)
            val targetInfoIdA = insertTargetInfo(registerId = ownerId)
            val targetInfoIdB = insertTargetInfo(registerId = ownerId)
            val xroomIdA = insertXroom(ownerId = ownerId, targetInfoId = targetInfoIdA, title = "방 A")
            val xroomIdB = insertXroom(ownerId = ownerId, targetInfoId = targetInfoIdB, title = "방 B")
            val memoryIdInB = insertMemory(xroomIdB)
            val accessToken = login(email, password)

            // 방 A 경로로 방 B의 기억에 사진 업로드 시도
            val encodedXroomIdA = idObfuscator.encode(ObfuscationType.XROOM, xroomIdA)
            val encodedMemoryIdInB = idObfuscator.encode(ObfuscationType.MEMORY, memoryIdInB)

            // When & Then
            uploadPhoto(encodedXroomIdA, encodedMemoryIdInB, accessToken)
                .andExpect { status { isForbidden() } }
        }

        test("인증 토큰 없이 사진을 업로드하면 401이 반환된다") {
            // Given
            val encodedXroomId = idObfuscator.encode(ObfuscationType.XROOM, 1L)
            val encodedMemoryId = idObfuscator.encode(ObfuscationType.MEMORY, 1L)

            // When & Then
            mockMvc.multipart(
                "/api/xrooms/{xroomId}/memories/{memoryId}/photo",
                encodedXroomId,
                encodedMemoryId,
            ) {
                file(pngPhotoFile())
            }
                .andExpect { status { isUnauthorized() } }
        }
    }

    context("DELETE /api/xrooms/{xroomId}/memories/{memoryId}/photo") {

        test("작성자가 사진을 삭제하면 200·photoDeleted=true를 반환하고 active 행이 soft delete 된다") {
            // Given
            val email = "xroom-photo-delete@example.com"
            val password = "password123"
            val ownerId = insertMember(email = email, rawPassword = password)
            val targetInfoId = insertTargetInfo(registerId = ownerId)
            val xroomId = insertXroom(ownerId = ownerId, targetInfoId = targetInfoId)
            val memoryId = insertMemory(xroomId)
            val accessToken = login(email, password)
            val encodedXroomId = idObfuscator.encode(ObfuscationType.XROOM, xroomId)
            val encodedMemoryId = idObfuscator.encode(ObfuscationType.MEMORY, memoryId)

            uploadPhoto(encodedXroomId, encodedMemoryId, accessToken)
                .andExpect { status { isCreated() } }

            // When
            val result = mockMvc.deleteJson(
                "/api/xrooms/{xroomId}/memories/{memoryId}/photo",
                encodedXroomId,
                encodedMemoryId,
            ) {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then - 응답에 암호화된 memoryId와 photoDeleted=true가 담긴다
            val body = mapper.readTree(result.response.contentAsString)
            body.get("memoryId").asText() shouldBe encodedMemoryId
            body.get("photoDeleted").asBoolean() shouldBe true

            // Then - active 미디어가 사라진다
            activeMedias(memoryId).size shouldBe 0
        }

        test("작성자가 아닌 수신자가 사진을 삭제하면 403이 반환된다") {
            // Given
            val ownerEmail = "xroom-photo-delete-owner@example.com"
            val ownerPassword = "password123"
            val ownerId = insertMember(email = ownerEmail, rawPassword = ownerPassword)
            val recipientEmail = "xroom-photo-delete-recipient@example.com"
            val recipientPassword = "password123"
            val recipientId = insertMember(email = recipientEmail, rawPassword = recipientPassword)
            val targetInfoId = insertTargetInfo(registerId = ownerId)
            insertMatchingResult(
                registerId = ownerId,
                targetInfoId = targetInfoId,
                targetId = recipientId,
                claimStatus = ClaimStatus.CLAIMED,
            )
            val xroomId = insertXroom(ownerId = ownerId, targetInfoId = targetInfoId)
            val memoryId = insertMemory(xroomId)
            val encodedXroomId = idObfuscator.encode(ObfuscationType.XROOM, xroomId)
            val encodedMemoryId = idObfuscator.encode(ObfuscationType.MEMORY, memoryId)

            // 작성자가 먼저 사진을 올린다
            val ownerToken = login(ownerEmail, ownerPassword)
            uploadPhoto(encodedXroomId, encodedMemoryId, ownerToken)
                .andExpect { status { isCreated() } }

            // When & Then - 수신자가 삭제를 시도하면 403
            val recipientToken = login(recipientEmail, recipientPassword)
            mockMvc.deleteJson(
                "/api/xrooms/{xroomId}/memories/{memoryId}/photo",
                encodedXroomId,
                encodedMemoryId,
            ) {
                authorization("Bearer $recipientToken")
            }
                .andExpect { status { isForbidden() } }
        }

        test("인증 토큰 없이 사진을 삭제하면 401이 반환된다") {
            // Given
            val encodedXroomId = idObfuscator.encode(ObfuscationType.XROOM, 1L)
            val encodedMemoryId = idObfuscator.encode(ObfuscationType.MEMORY, 1L)

            // When & Then
            mockMvc.deleteJson(
                "/api/xrooms/{xroomId}/memories/{memoryId}/photo",
                encodedXroomId,
                encodedMemoryId,
            )
                .andExpect { status { isUnauthorized() } }
        }
    }

    context("사진과 기억 상세·연쇄 삭제 연계") {

        test("사진을 업로드한 기억은 상세 조회 시 photoUrl이 채워지고 사진 없는 기억은 null이다") {
            // Given
            val email = "xroom-photo-detail@example.com"
            val password = "password123"
            val ownerId = insertMember(email = email, rawPassword = password)
            val targetInfoId = insertTargetInfo(registerId = ownerId)
            val xroomId = insertXroom(ownerId = ownerId, targetInfoId = targetInfoId)
            val accessToken = login(email, password)
            val encodedXroomId = idObfuscator.encode(ObfuscationType.XROOM, xroomId)

            // 시점 오름차순: photoMemory(2019) → noPhotoMemory(2020)
            val photoMemoryId = insertMemory(xroomId, eventDate = LocalDate.of(2019, 1, 1))
            insertMemory(xroomId, eventDate = LocalDate.of(2020, 2, 2))
            val encodedPhotoMemoryId = idObfuscator.encode(ObfuscationType.MEMORY, photoMemoryId)

            uploadPhoto(encodedXroomId, encodedPhotoMemoryId, accessToken)
                .andExpect { status { isCreated() } }

            // When
            val detailResult = mockMvc.getJson("/api/xrooms/{xroomId}", encodedXroomId) {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then
            val memories = mapper.readTree(detailResult.response.contentAsByteArray).get("memories")
            memories.size() shouldBe 2
            memories.get(0).get("photoUrl").asText().startsWith("/files/memory/memory-photo/$photoMemoryId/") shouldBe true
            memories.get(1).get("photoUrl").isNull shouldBe true
        }

        test("사진이 있는 기억을 삭제하면 기억과 사진이 함께 soft delete 된다") {
            // Given
            val email = "xroom-photo-cascade@example.com"
            val password = "password123"
            val ownerId = insertMember(email = email, rawPassword = password)
            val targetInfoId = insertTargetInfo(registerId = ownerId)
            val xroomId = insertXroom(ownerId = ownerId, targetInfoId = targetInfoId)
            val memoryId = insertMemory(xroomId)
            val accessToken = login(email, password)
            val encodedXroomId = idObfuscator.encode(ObfuscationType.XROOM, xroomId)
            val encodedMemoryId = idObfuscator.encode(ObfuscationType.MEMORY, memoryId)

            uploadPhoto(encodedXroomId, encodedMemoryId, accessToken)
                .andExpect { status { isCreated() } }

            // When - 사진이 아니라 기억 자체를 삭제한다
            mockMvc.deleteJson(
                "/api/xrooms/{xroomId}/memories/{memoryId}",
                encodedXroomId,
                encodedMemoryId,
            ) {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }

            // Then - 상세에서 기억이 사라지고, 그 기억의 사진도 active에서 제외된다
            val detailResult = mockMvc.getJson("/api/xrooms/{xroomId}", encodedXroomId) {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            mapper.readTree(detailResult.response.contentAsByteArray).get("memories").size() shouldBe 0
            activeMedias(memoryId).size shouldBe 0
        }
    }
})
