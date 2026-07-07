package com.konkuk.ma.integration

import com.konkuk.ma.domain.common.domain.file.PhotoFile
import com.konkuk.ma.domain.community.application.PostImageCommandService
import com.konkuk.ma.domain.community.entity.table.PostImageTable
import com.konkuk.ma.domain.community.entity.table.PostTable
import com.konkuk.ma.domain.member.entity.table.MemberTable
import com.konkuk.ma.exception.AccessDeniedException
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.exception.InvalidValueException
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
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
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * 게시글 이미지 첨부/교체/삭제(REQ-013 Phase 4) Service 레이어 E2E 통합 테스트.
 *
 * PostImageCommandService.upload/delete 가 실제 H2 DB + 실제 파일 스토리지(임시 디렉토리) + 실제 이미지 처리기를
 * 관통하여 소유권 검증 · 최초/교체 판별 · 이전 이미지 soft delete · 확장자/매직바이트 검증을 정확히 처리하는지 검증한다.
 *
 * (kotest-writing 규칙: Service 는 모킹 단위 테스트가 아니라 실제 인프라를 관통하는 E2E 로 검증한다.
 *  HTTP 계층은 PostImageIntegrationTest 에서 별도로 검증하므로 여기서는 Service 빈을 직접 주입한다.)
 */
@SpringBootTest
@ActiveProfiles("test")
class PostImageCommandServiceIntegrationTest(
    private val postImageCommandService: PostImageCommandService,
) : FunSpec({

    var memberSeq = 0L
    // 실제 LocalFileStorage 가 파일을 쓰는 경로(ma-file-storage test 프로파일과 동일). 기존 web E2E 관례대로 하드코딩.
    val testUploadDir = Paths.get(System.getProperty("java.io.tmpdir"), "meet-again-test", "uploads").toFile()

    fun cleanUploadDir() {
        testUploadDir.deleteRecursively()
    }

    beforeSpec {
        transaction {
            SchemaUtils.create(MemberTable, PostTable, PostImageTable)
        }
    }

    afterEach {
        transaction {
            PostImageTable.deleteAll()
            PostTable.deleteAll()
            MemberTable.deleteAll()
        }
        cleanUploadDir()
    }

    afterSpec {
        transaction {
            SchemaUtils.drop(PostImageTable, PostTable, MemberTable)
        }
        cleanUploadDir()
    }

    fun insertMember(): Long {
        memberSeq += 1
        return transaction {
            MemberTable.insertAndGetId {
                it[email] = "member$memberSeq@example.com"
                it[password] = "encoded"
                it[nickname] = "회원$memberSeq"
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

    fun insertActivePostImage(postId: Long, storageKey: String = "community/post-image/$postId/old.png"): Long {
        return transaction {
            PostImageTable.insertAndGetId {
                it[PostImageTable.postId] = postId
                it[PostImageTable.storageKey] = storageKey
                it[originalFilename] = "old.png"
                it[mimeType] = "image/png"
                it[fileSize] = 1024L
                it[thumbnailKey] = "community/thumbnail/$postId/thumb_old.png"
            }.value
        }
    }

    fun activeImages(postId: Long) = transaction {
        PostImageTable.selectAll()
            .where { (PostImageTable.postId eq postId) and (PostImageTable.deleted eq false) }
            .toList()
    }

    fun totalImages(postId: Long) = transaction {
        PostImageTable.selectAll().where { PostImageTable.postId eq postId }.toList()
    }

    fun readImageRow(imageId: Long) = transaction {
        PostImageTable.selectAll().where { PostImageTable.id eq imageId }.first()
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

    fun pngPhotoFile(fileName: String = "image.png"): PhotoFile {
        val bytes = pngBytes()
        return PhotoFile.create(fileName, bytes.size.toLong(), bytes)
    }

    context("upload") {

        test("작성자가 업로드하면 active 이미지가 1행 적재되고 replaced=false, imageUrl 이 채워진다") {
            // Given
            val authorId = insertMember()
            val postId = insertPost(authorId)

            // When
            val result = postImageCommandService.upload(postId, authorId, pngPhotoFile())

            // Then
            result.replaced.shouldBeFalse()
            result.postId shouldBe postId
            result.imageUrl shouldStartWith "/files/community/post-image/$postId/"
            activeImages(postId).size shouldBe 1
        }

        test("이미 이미지가 있는 글에 업로드하면 replaced=true 이고 active 1행만 유지되며 이전 이미지는 soft delete 된다") {
            // Given - 첫 이미지 업로드
            val authorId = insertMember()
            val postId = insertPost(authorId)
            postImageCommandService.upload(postId, authorId, pngPhotoFile("first.png"))

            // When - 두 번째 업로드(교체)
            val result = postImageCommandService.upload(postId, authorId, pngPhotoFile("second.png"))

            // Then - active 는 1행(두 번째), 전체는 2행(첫 번째는 soft delete)
            result.replaced.shouldBeTrue()
            activeImages(postId).size shouldBe 1
            val total = totalImages(postId)
            total.size shouldBe 2
            total.count { it[PostImageTable.deleted] } shouldBe 1
        }

        test("타인이 업로드하면 AccessDeniedException 이 발생한다") {
            // Given
            val authorId = insertMember()
            val otherId = insertMember()
            val postId = insertPost(authorId)

            // When & Then
            shouldThrow<AccessDeniedException> {
                postImageCommandService.upload(postId, otherId, pngPhotoFile())
            }
        }

        test("존재하지 않는 게시글에 업로드하면 EntityNotFoundException 이 발생한다") {
            // Given
            val memberId = insertMember()

            // When & Then
            shouldThrow<EntityNotFoundException> {
                postImageCommandService.upload(999L, memberId, pngPhotoFile())
            }
        }

        test("커뮤니티에서 배제하는 svg 확장자면 InvalidValueException 이 발생한다") {
            // Given - svg 는 PhotoFile 생성은 통과하지만 처리기가 거부한다
            val authorId = insertMember()
            val postId = insertPost(authorId)
            val svgBytes = "<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>".toByteArray()
            val svg = PhotoFile.create("vector.svg", svgBytes.size.toLong(), svgBytes)

            // When & Then
            shouldThrow<InvalidValueException> {
                postImageCommandService.upload(postId, authorId, svg)
            }
        }

        test("확장자만 이미지로 위장하고 내용물 서명이 불일치하면 InvalidValueException 이 발생한다") {
            // Given - 확장자는 jpg 이지만 내용물은 PNG 서명
            val authorId = insertMember()
            val postId = insertPost(authorId)
            val disguised = PhotoFile.create("fake.jpg", pngBytes().size.toLong(), pngBytes())

            // When & Then
            shouldThrow<InvalidValueException> {
                postImageCommandService.upload(postId, authorId, disguised)
            }
        }
    }

    context("delete") {

        test("작성자가 삭제하면 active 이미지가 soft delete 된다") {
            // Given
            val authorId = insertMember()
            val postId = insertPost(authorId)
            val imageId = insertActivePostImage(postId)

            // When
            postImageCommandService.delete(postId, authorId)

            // Then
            readImageRow(imageId)[PostImageTable.deleted].shouldBeTrue()
            activeImages(postId).size shouldBe 0
        }

        test("active 이미지가 없어도 삭제는 예외 없이 정상 동작한다") {
            // Given - 이미지가 없는 게시글
            val authorId = insertMember()
            val postId = insertPost(authorId)

            // When & Then
            shouldNotThrowAny {
                postImageCommandService.delete(postId, authorId)
            }
        }

        test("타인이 삭제하면 AccessDeniedException 이 발생한다") {
            // Given
            val authorId = insertMember()
            val otherId = insertMember()
            val postId = insertPost(authorId)
            insertActivePostImage(postId)

            // When & Then
            shouldThrow<AccessDeniedException> {
                postImageCommandService.delete(postId, otherId)
            }
        }
    }
})
