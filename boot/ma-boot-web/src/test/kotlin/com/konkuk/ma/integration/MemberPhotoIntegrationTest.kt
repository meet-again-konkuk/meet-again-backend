package com.konkuk.ma.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.domain.auth.entity.table.RefreshTokenTable
import com.konkuk.ma.domain.member.entity.table.MemberPhotoTable
import com.konkuk.ma.domain.member.entity.table.MemberTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
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
import com.konkuk.ma.extension.deleteJson
import com.konkuk.ma.extension.postJson
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.multipart
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import javax.imageio.ImageIO

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MemberPhotoIntegrationTest(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper
) : FunSpec({

    val passwordEncoder = BCryptPasswordEncoder()
    val testUploadDir = Paths.get(System.getProperty("java.io.tmpdir"), "meet-again-test", "uploads")

    beforeSpec {
        transaction {
            SchemaUtils.create(MemberTable, RefreshTokenTable, MemberPhotoTable)
        }
    }

    afterEach {
        transaction {
            MemberPhotoTable.deleteAll()
            RefreshTokenTable.deleteAll()
            MemberTable.deleteAll()
        }
        // 테스트에서 생성된 업로드 파일 정리
        if (Files.exists(testUploadDir)) {
            Files.walk(testUploadDir)
                .sorted(Comparator.reverseOrder())
                .filter { it != testUploadDir }
                .forEach { Files.deleteIfExists(it) }
        }
    }

    afterSpec {
        transaction {
            SchemaUtils.drop(MemberPhotoTable, RefreshTokenTable, MemberTable)
        }
        // 임시 디렉토리 삭제
        if (Files.exists(testUploadDir)) {
            Files.walk(testUploadDir)
                .sorted(Comparator.reverseOrder())
                .forEach { Files.deleteIfExists(it) }
        }
    }

    fun insertMember(
        email: String = "photo-test@example.com",
        rawPassword: String = "password123"
    ): Long {
        return transaction {
            MemberTable.insertAndGetId {
                it[MemberTable.email] = email
                it[password] = passwordEncoder.encode(rawPassword)
                it[nickname] = "포토테스터-$email"
                it[gender] = "MALE"
                it[phoneNumber] = "01012345678"
                it[name] = "김테스트"
                it[birthDate] = LocalDate.of(1990, 1, 1)
                it[region] = "SEOUL"
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

    fun createTestPngBytes(): ByteArray {
        val image = BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        graphics.fillRect(0, 0, 800, 600)
        graphics.dispose()

        val outputStream = ByteArrayOutputStream()
        ImageIO.write(image, "png", outputStream)
        return outputStream.toByteArray()
    }

    context("POST /api/members/photos") {

        test("사진을 업로드하면 원본과 썸네일이 저장되고 DB에 기록된다") {
            // Given
            val email = "photo-test@example.com"
            val password = "password123"
            val memberId = insertMember(email = email, rawPassword = password)
            val accessToken = login(email, password)

            val pngBytes = createTestPngBytes()
            val photoFile = MockMultipartFile(
                "photo",
                "test-photo.png",
                MediaType.IMAGE_PNG_VALUE,
                pngBytes
            )

            // When
            mockMvc.multipart("/api/members/photos") {
                file(photoFile)
                header("Authorization", "Bearer $accessToken")
            }
                .andExpect { status { isCreated() } }

            // Then - DB에 사진 레코드가 저장되었는지 확인
            val savedPhoto = transaction {
                MemberPhotoTable.selectAll()
                    .where { MemberPhotoTable.memberId eq memberId }
                    .singleOrNull()
            }

            savedPhoto shouldNotBe null
            savedPhoto!![MemberPhotoTable.originalFileName] shouldBe "test-photo.png"

            // DB에는 서버 파일시스템 절대경로가 아니라 상대 storageKey 만 담긴다
            val storageKey = savedPhoto[MemberPhotoTable.storageKey]
            storageKey shouldStartWith "member/profile/$memberId/"
            storageKey shouldEndWith ".png"
            storageKey shouldNotContain testUploadDir.toString()

            savedPhoto[MemberPhotoTable.thumbnailKey] shouldBe "member/thumbnail/$memberId/thumb_test-photo.png"
        }

        test("기존 사진이 있을 때 새 사진을 업로드하면 기존 사진이 교체된다") {
            // Given
            val email = "photo-replace@example.com"
            val password = "password123"
            val memberId = insertMember(email = email, rawPassword = password)
            val accessToken = login(email, password)

            val pngBytes = createTestPngBytes()

            // 첫 번째 업로드
            val firstPhoto = MockMultipartFile("photo", "first.png", MediaType.IMAGE_PNG_VALUE, pngBytes)
            mockMvc.multipart("/api/members/photos") {
                file(firstPhoto)
                header("Authorization", "Bearer $accessToken")
            }.andExpect { status { isCreated() } }

            // When - 두 번째 업로드
            val secondPhoto = MockMultipartFile("photo", "second.png", MediaType.IMAGE_PNG_VALUE, pngBytes)
            mockMvc.multipart("/api/members/photos") {
                file(secondPhoto)
                header("Authorization", "Bearer $accessToken")
            }.andExpect { status { isCreated() } }

            // Then - 활성 사진은 1개, 기존 사진은 soft delete
            val activePhotos = transaction {
                MemberPhotoTable.selectAll()
                    .where { (MemberPhotoTable.memberId eq memberId) and (MemberPhotoTable.deleted eq false) }
                    .toList()
            }

            activePhotos.size shouldBe 1
            activePhotos[0][MemberPhotoTable.originalFileName] shouldBe "second.png"
        }

        test("인증 토큰 없이 사진을 업로드하면 401이 반환된다") {
            // Given
            val pngBytes = createTestPngBytes()
            val photoFile = MockMultipartFile("photo", "test.png", MediaType.IMAGE_PNG_VALUE, pngBytes)

            // When & Then
            mockMvc.multipart("/api/members/photos") {
                file(photoFile)
            }.andExpect { status { isUnauthorized() } }
        }
    }

    context("DELETE /api/members/photos") {

        test("업로드된 사진을 삭제하면 soft delete 처리된다") {
            // Given
            val email = "photo-delete@example.com"
            val password = "password123"
            val memberId = insertMember(email = email, rawPassword = password)
            val accessToken = login(email, password)

            val pngBytes = createTestPngBytes()
            val photoFile = MockMultipartFile("photo", "to-delete.png", MediaType.IMAGE_PNG_VALUE, pngBytes)
            mockMvc.multipart("/api/members/photos") {
                file(photoFile)
                header("Authorization", "Bearer $accessToken")
            }.andExpect { status { isCreated() } }

            // When
            mockMvc.deleteJson("/api/members/photos") {
                authorization("Bearer $accessToken")
            }.andExpect { status { isOk() } }

            // Then
            val photo = transaction {
                MemberPhotoTable.selectAll()
                    .where { MemberPhotoTable.memberId eq memberId }
                    .singleOrNull()
            }

            photo shouldNotBe null
            photo!![MemberPhotoTable.deleted] shouldBe true
            photo[MemberPhotoTable.deletedBy] shouldBe memberId.toString()
            photo[MemberPhotoTable.deletedDate] shouldNotBe null
        }
    }
})
