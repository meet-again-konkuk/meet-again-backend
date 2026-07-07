package com.konkuk.ma.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.domain.auth.entity.table.RefreshTokenTable
import com.konkuk.ma.domain.community.entity.table.PostImageTable
import com.konkuk.ma.domain.community.entity.table.PostTable
import com.konkuk.ma.domain.member.entity.table.MemberTable
import com.konkuk.ma.extension.postJson
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.Paths
import java.time.LocalDate
import javax.imageio.ImageIO
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteAll
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
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.multipart

/**
 * 게시글 이미지 첨부/삭제(REQ-013 Phase 4) API E2E 통합 테스트.
 *
 * POST/DELETE /api/community/posts/{postId}/image 를 실제 HTTP(multipart) 요청으로
 * 컨트롤러→Service→도메인→인프라→DB 까지 관통 검증한다. (REST Docs 스니펫은 별도 단계)
 * - 최초 업로드 201 / 교체 200, 응답 imageUrl·PostImageTable active 행 검증
 * - 교체 시 이전 이미지 soft delete(전체 2행·deleted 1행)·삭제 시 soft delete(행 유지·deleted=true·active 0행)는
 *   응답 body 로 확인 불가하므로 transaction { PostImageTable ... } 직접 읽기로 단언한다.
 * - 권한/존재: 타인 403, 없는 글 404, 이미지 없는 글 삭제도 204
 * - 입력 검증: 미지원 확장자 400, 위장 파일 400, 10MB 초과 400, 인증 없음 401
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PostImageIntegrationTest(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper,
) : FunSpec({

    val passwordEncoder = BCryptPasswordEncoder()
    // 실제 LocalFileStorage 가 파일을 쓰는 경로(ma-file-storage test 프로파일과 동일). 기존 web E2E 관례대로 하드코딩.
    val testUploadDir = Paths.get(System.getProperty("java.io.tmpdir"), "meet-again-test", "uploads").toFile()

    fun cleanUploadDir() {
        testUploadDir.deleteRecursively()
    }

    beforeSpec {
        transaction {
            SchemaUtils.create(MemberTable, RefreshTokenTable, PostTable, PostImageTable)
        }
    }

    afterEach {
        transaction {
            PostImageTable.deleteAll()
            PostTable.deleteAll()
            RefreshTokenTable.deleteAll()
            MemberTable.deleteAll()
        }
        cleanUploadDir()
    }

    afterSpec {
        transaction {
            SchemaUtils.drop(PostImageTable, PostTable, RefreshTokenTable, MemberTable)
        }
        cleanUploadDir()
    }

    fun insertMember(email: String, rawPassword: String = "password123"): Long {
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

    fun insertPost(authorId: Long): Long {
        return transaction {
            PostTable.insertAndGetId {
                it[PostTable.authorId] = authorId
                it[category] = "SUCCESS_STORY"
                it[title] = "원본 제목"
                it[content] = "원본 내용"
            }.value
        }
    }

    fun login(email: String, rawPassword: String = "password123"): String {
        val request = mapOf("email" to email, "password" to rawPassword)
        val result = mockMvc.postJson("/api/auth/login") {
            content = mapper.writeValueAsString(request)
        }
            .andExpect { status { isOk() } }
            .andReturn()
        return mapper.readTree(result.response.contentAsString).get("accessToken").asText()
    }

    fun pngBytes(): ByteArray {
        val image = BufferedImage(120, 90, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        graphics.fillRect(0, 0, 120, 90)
        graphics.dispose()
        val out = ByteArrayOutputStream()
        ImageIO.write(image, "png", out)
        return out.toByteArray()
    }

    fun imagePart(fileName: String = "image.png", contentType: String = MediaType.IMAGE_PNG_VALUE, bytes: ByteArray = pngBytes()) =
        MockMultipartFile("image", fileName, contentType, bytes)

    fun uploadImage(postId: Long, token: String, part: MockMultipartFile = imagePart()) =
        mockMvc.multipart("/api/community/posts/{postId}/image", postId) {
            file(part)
            header("Authorization", "Bearer $token")
        }

    fun activeImages(postId: Long) = transaction {
        PostImageTable.selectAll()
            .where { (PostImageTable.postId eq postId) and (PostImageTable.deleted eq false) }
            .toList()
    }

    fun totalImages(postId: Long) = transaction {
        PostImageTable.selectAll().where { PostImageTable.postId eq postId }.toList()
    }

    context("POST /api/community/posts/{postId}/image") {

        test("작성자가 이미지를 업로드하면 201과 함께 active 이미지 1행이 적재되고 응답에 imageUrl 이 담긴다") {
            // Given
            val email = "image-upload@example.com"
            val authorId = insertMember(email)
            val token = login(email)
            val postId = insertPost(authorId)

            // When
            val result = uploadImage(postId, token)
                .andExpect { status { isCreated() } }
                .andReturn()

            // Then - 응답 imageUrl 과 active 이미지 1행
            val body = mapper.readTree(result.response.contentAsString)
            body.get("postId").asLong() shouldBe postId
            body.get("imageUrl").asText().startsWith("/files/community/post-image/$postId/").shouldBeTrue()
            activeImages(postId).size shouldBe 1
        }

        test("이미지가 있는 글에 다시 업로드하면 200(교체)을 반환하고 active 1행만 유지되며 이전 이미지는 soft delete 된다") {
            // Given - 최초 업로드
            val email = "image-replace@example.com"
            val authorId = insertMember(email)
            val token = login(email)
            val postId = insertPost(authorId)
            uploadImage(postId, token, imagePart("first.png"))
                .andExpect { status { isCreated() } }

            // When - 재업로드
            uploadImage(postId, token, imagePart("second.png"))
                .andExpect { status { isOk() } }

            // Then - active 는 1행(두 번째), 전체는 2행(첫 번째는 soft delete)
            activeImages(postId).size shouldBe 1
            val total = totalImages(postId)
            total.size shouldBe 2
            total.count { it[PostImageTable.deleted] } shouldBe 1
        }

        test("타인이 업로드하면 403을 반환한다") {
            // Given
            val ownerEmail = "image-owner@example.com"
            val ownerId = insertMember(ownerEmail)
            val postId = insertPost(ownerId)
            val otherEmail = "image-other@example.com"
            insertMember(otherEmail)
            val otherToken = login(otherEmail)

            // When & Then
            uploadImage(postId, otherToken)
                .andExpect { status { isForbidden() } }
        }

        test("존재하지 않는 게시글에 업로드하면 404를 반환한다") {
            // Given
            val email = "image-notfound@example.com"
            insertMember(email)
            val token = login(email)

            // When & Then
            uploadImage(999L, token)
                .andExpect { status { isNotFound() } }
        }

        test("지원하지 않는 확장자(gif)면 400을 반환한다") {
            // Given - gif 는 AllowedExtension 자체에 없어 컨트롤러의 PhotoFile 생성 단계에서 거부된다
            val email = "image-gif@example.com"
            val authorId = insertMember(email)
            val token = login(email)
            val postId = insertPost(authorId)

            // When & Then
            uploadImage(postId, token, imagePart("anim.gif", MediaType.IMAGE_GIF_VALUE, pngBytes()))
                .andExpect { status { isBadRequest() } }
        }

        test("커뮤니티에서 배제하는 svg 확장자면 400을 반환한다") {
            // Given - svg 는 PhotoFile 생성은 통과하지만 처리기가 거부한다
            val email = "image-svg@example.com"
            val authorId = insertMember(email)
            val token = login(email)
            val postId = insertPost(authorId)
            val svgBytes = "<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>".toByteArray()

            // When & Then
            uploadImage(postId, token, imagePart("vector.svg", "image/svg+xml", svgBytes))
                .andExpect { status { isBadRequest() } }
        }

        test("확장자만 이미지로 위장하고 내용물 서명이 불일치하면 400을 반환한다") {
            // Given - 확장자는 jpg 이지만 내용물은 PNG 서명
            val email = "image-disguise@example.com"
            val authorId = insertMember(email)
            val token = login(email)
            val postId = insertPost(authorId)

            // When & Then
            uploadImage(postId, token, imagePart("fake.jpg", MediaType.IMAGE_JPEG_VALUE, pngBytes()))
                .andExpect { status { isBadRequest() } }
        }

        test("10MB 를 초과하는 이미지를 업로드하면 400을 반환한다") {
            // Given - 10MB 초과 크기
            val email = "image-toobig@example.com"
            val authorId = insertMember(email)
            val token = login(email)
            val postId = insertPost(authorId)
            val tooLarge = ByteArray(10 * 1024 * 1024 + 1024)

            // When & Then
            uploadImage(postId, token, imagePart("big.png", MediaType.IMAGE_PNG_VALUE, tooLarge))
                .andExpect { status { isBadRequest() } }
        }

        test("인증 토큰 없이 업로드하면 401을 반환한다") {
            // Given
            val postId = insertPost(1L)

            // When & Then
            mockMvc.multipart("/api/community/posts/{postId}/image", postId) {
                file(imagePart())
            }
                .andExpect { status { isUnauthorized() } }
        }
    }

    context("DELETE /api/community/posts/{postId}/image") {

        test("작성자가 삭제하면 204를 반환하고 active 이미지가 soft delete 된다") {
            // Given
            val email = "image-delete-owner@example.com"
            val authorId = insertMember(email)
            val token = login(email)
            val postId = insertPost(authorId)
            uploadImage(postId, token)
                .andExpect { status { isCreated() } }

            // When
            mockMvc.delete("/api/community/posts/{postId}/image", postId) {
                header("Authorization", "Bearer $token")
            }
                .andExpect { status { isNoContent() } }

            // Then - 행은 남되 deleted=true, active 0행
            activeImages(postId).size shouldBe 0
            val total = totalImages(postId)
            total.size shouldBe 1
            total.first()[PostImageTable.deleted].shouldBeTrue()
        }

        test("이미지가 없는 글이어도 작성자 삭제는 204로 정상 동작한다") {
            // Given - 이미지가 없는 게시글
            val email = "image-delete-empty@example.com"
            val authorId = insertMember(email)
            val token = login(email)
            val postId = insertPost(authorId)

            // When & Then
            mockMvc.delete("/api/community/posts/{postId}/image", postId) {
                header("Authorization", "Bearer $token")
            }
                .andExpect { status { isNoContent() } }
        }

        test("타인이 삭제하면 403을 반환한다") {
            // Given
            val ownerEmail = "image-delete-owner2@example.com"
            val ownerId = insertMember(ownerEmail)
            val postId = insertPost(ownerId)
            val otherEmail = "image-delete-other@example.com"
            insertMember(otherEmail)
            val otherToken = login(otherEmail)

            // When & Then
            mockMvc.delete("/api/community/posts/{postId}/image", postId) {
                header("Authorization", "Bearer $otherToken")
            }
                .andExpect { status { isForbidden() } }
        }

        test("인증 토큰 없이 삭제하면 401을 반환한다") {
            // Given
            val postId = insertPost(1L)

            // When & Then
            mockMvc.delete("/api/community/posts/{postId}/image", postId)
                .andExpect { status { isUnauthorized() } }
        }
    }
})
