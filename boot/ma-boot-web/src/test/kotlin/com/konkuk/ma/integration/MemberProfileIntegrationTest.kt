package com.konkuk.ma.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.domain.auth.entity.table.RefreshTokenTable
import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.common.domain.id.port.IdObfuscator
import com.konkuk.ma.domain.member.entity.table.MemberPhotoTable
import com.konkuk.ma.domain.member.domain.policy.WithdrawnSentinel
import com.konkuk.ma.domain.member.entity.table.MemberTable
import com.konkuk.ma.extension.getJson
import com.konkuk.ma.extension.patchJson
import com.konkuk.ma.extension.postJson
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import java.time.LocalDate

/**
 * `GET /api/members/me` · `PATCH /api/members/me` API→DB E2E 테스트 (REQ-016 Part A).
 *
 * Service 를 모킹하지 않고 실제 HTTP 요청으로 컨트롤러→Service→도메인→인프라→H2 를 관통한다.
 * 응답으로 증명할 수 없는 것(수정되지 않아야 할 컬럼, 검증 실패 시 무변경)은
 * `transaction { }` 으로 MEMBERS 테이블을 직접 읽어 확인한다.
 *
 * 한글 문자열은 `contentAsByteArray` 로 읽는다 (contentAsString 은 mojibake).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MemberProfileIntegrationTest(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper,
    private val idObfuscator: IdObfuscator,
) : FunSpec({

    val passwordEncoder = BCryptPasswordEncoder()
    val rawPassword = "password123"

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
    }

    afterSpec {
        transaction {
            SchemaUtils.drop(MemberPhotoTable, RefreshTokenTable, MemberTable)
        }
    }

    fun insertMember(
        email: String,
        nickname: String,
        name: String = "홍길동",
        gender: String = "FEMALE",
        phoneNumber: String = "01012345678",
        birthDate: LocalDate = LocalDate.of(1995, 3, 7),
        region: String = "SEOUL",
        highSchool: String? = "기존고",
        university: String? = "기존대",
        deleted: Boolean = false,
    ): Long {
        return transaction {
            MemberTable.insertAndGetId {
                it[MemberTable.email] = email
                it[password] = passwordEncoder.encode(rawPassword)
                it[MemberTable.nickname] = nickname
                it[MemberTable.gender] = gender
                it[MemberTable.phoneNumber] = phoneNumber
                it[MemberTable.name] = name
                it[MemberTable.birthDate] = birthDate
                it[MemberTable.region] = region
                it[MemberTable.highSchool] = highSchool
                it[MemberTable.university] = university
                it[MemberTable.deleted] = deleted
            }.value
        }
    }

    fun anonymizeAndSoftDelete(memberId: Long) {
        transaction {
            MemberTable.update({ MemberTable.id eq memberId }) {
                it[nickname] = WithdrawnSentinel.nickname(memberId)
                it[deleted] = true
            }
        }
    }

    fun insertPhoto(
        memberId: Long,
        storageKey: String = "member/profile/$memberId/photo.jpg",
        thumbnailKey: String? = null,
        deleted: Boolean = false,
    ) {
        transaction {
            MemberPhotoTable.insert {
                it[MemberPhotoTable.memberId] = memberId
                it[MemberPhotoTable.storageKey] = storageKey
                it[originalFileName] = "photo.jpg"
                it[MemberPhotoTable.thumbnailKey] = thumbnailKey
                it[MemberPhotoTable.deleted] = deleted
            }
        }
    }

    fun login(email: String): String {
        val result = mockMvc.postJson("/api/auth/login") {
            content = mapper.writeValueAsString(mapOf("email" to email, "password" to rawPassword))
        }
            .andExpect { status { isOk() } }
            .andReturn()

        return mapper.readTree(result.response.contentAsString).get("accessToken").asText()
    }

    fun memberRow(memberId: Long) = transaction {
        MemberTable.selectAll().where { MemberTable.id eq memberId }.single()
    }

    context("GET /api/members/me") {

        test("내 프로필을 조회하면 11개 필드를 모두 내려준다") {
            // Given
            val email = "profile-get@example.com"
            val memberId = insertMember(
                email = email,
                nickname = "응원단장",
                name = "홍길동",
                gender = "FEMALE",
                phoneNumber = "01012345678",
                birthDate = LocalDate.of(1995, 3, 7),
                region = "BUSAN",
                highSchool = "건대부고",
                university = "건국대",
            )
            val accessToken = login(email)

            // When
            val result = mockMvc.getJson("/api/members/me") {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then
            val body = mapper.readTree(result.response.contentAsByteArray)
            body.get("memberId").asText() shouldBe idObfuscator.encode(ObfuscationType.MEMBER, memberId)
            body.get("email").asText() shouldBe email
            body.get("nickname").asText() shouldBe "응원단장"
            body.get("name").asText() shouldBe "홍길동"
            body.get("gender").asText() shouldBe "FEMALE"
            body.get("birthDate").asText() shouldBe "1995-03-07"
            body.get("phoneNumber").asText() shouldBe "01012345678"
            body.get("region").asText() shouldBe "BUSAN"
            body.get("highSchool").asText() shouldBe "건대부고"
            body.get("university").asText() shouldBe "건국대"
            body.get("profileImageUrl").isNull shouldBe true
        }

        test("응답 필드는 정확히 11개다") {
            // Given
            val email = "profile-get-count@example.com"
            insertMember(email = email, nickname = "필드수")
            val accessToken = login(email)

            // When
            val result = mockMvc.getJson("/api/members/me") {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then
            mapper.readTree(result.response.contentAsByteArray).size() shouldBe 11
        }

        test("지역은 displayName 이 아니라 enum 코드로 내려간다") {
            // Given
            val email = "profile-get-region@example.com"
            insertMember(email = email, nickname = "제주민", region = "JEJU_DO")
            val accessToken = login(email)

            // When
            val result = mockMvc.getJson("/api/members/me") {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then
            mapper.readTree(result.response.contentAsByteArray).get("region").asText() shouldBe "JEJU_DO"
        }

        test("프로필 사진이 있으면 썸네일 키로 만든 /files URL 을 내려준다") {
            // Given
            val email = "profile-get-photo@example.com"
            val memberId = insertMember(email = email, nickname = "사진있음")
            insertPhoto(
                memberId = memberId,
                storageKey = "member/profile/$memberId/photo.jpg",
                thumbnailKey = "member/thumbnail/$memberId/thumb_photo.jpg",
            )
            val accessToken = login(email)

            // When
            val result = mockMvc.getJson("/api/members/me") {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then
            mapper.readTree(result.response.contentAsByteArray).get("profileImageUrl").asText() shouldBe
                "/files/member/thumbnail/$memberId/thumb_photo.jpg"
        }

        test("썸네일이 없으면 원본 키로 만든 /files URL 을 내려준다") {
            // Given
            val email = "profile-get-photo-nothumb@example.com"
            val memberId = insertMember(email = email, nickname = "썸네일없음")
            insertPhoto(
                memberId = memberId,
                storageKey = "member/profile/$memberId/photo.jpg",
                thumbnailKey = null,
            )
            val accessToken = login(email)

            // When
            val result = mockMvc.getJson("/api/members/me") {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then
            mapper.readTree(result.response.contentAsByteArray).get("profileImageUrl").asText() shouldBe
                "/files/member/profile/$memberId/photo.jpg"
        }

        test("삭제된 사진만 있으면 profileImageUrl 이 null 이다") {
            // Given
            val email = "profile-get-photo-deleted@example.com"
            val memberId = insertMember(email = email, nickname = "삭제된사진")
            insertPhoto(memberId = memberId, deleted = true)
            val accessToken = login(email)

            // When
            val result = mockMvc.getJson("/api/members/me") {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then
            mapper.readTree(result.response.contentAsByteArray).get("profileImageUrl").isNull shouldBe true
        }

        test("고등학교와 대학교가 없는 회원은 두 필드가 null 로 내려간다") {
            // Given
            val email = "profile-get-null-school@example.com"
            insertMember(email = email, nickname = "학교없음", highSchool = null, university = null)
            val accessToken = login(email)

            // When
            val result = mockMvc.getJson("/api/members/me") {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then
            val body = mapper.readTree(result.response.contentAsByteArray)
            body.get("highSchool").isNull shouldBe true
            body.get("university").isNull shouldBe true
        }

        test("인증 토큰 없이 조회하면 401을 반환한다") {
            // When & Then
            mockMvc.getJson("/api/members/me")
                .andExpect { status { isUnauthorized() } }
        }

        test("회원이 존재하지 않으면 404를 반환한다") {
            // Given
            val email = "profile-get-gone@example.com"
            val memberId = insertMember(email = email, nickname = "사라짐")
            val accessToken = login(email)
            transaction {
                MemberTable.deleteWhere { MemberTable.id eq memberId }
            }

            // When & Then
            mockMvc.getJson("/api/members/me") {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isNotFound() } }
        }
    }

    context("PATCH /api/members/me — 부분 수정") {

        test("네 필드를 모두 보내면 모두 반영된다") {
            // Given
            val email = "profile-patch-all@example.com"
            val memberId = insertMember(email = email, nickname = "기존닉", region = "SEOUL")
            val accessToken = login(email)
            val request = mapOf<String, Any?>(
                "nickname" to "새닉네임",
                "region" to "BUSAN",
                "highSchool" to "건대부고",
                "university" to "건국대",
            )

            // When
            val result = mockMvc.patchJson("/api/members/me") {
                content = mapper.writeValueAsString(request)
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then — 응답
            val body = mapper.readTree(result.response.contentAsByteArray)
            body.get("nickname").asText() shouldBe "새닉네임"
            body.get("region").asText() shouldBe "BUSAN"
            body.get("highSchool").asText() shouldBe "건대부고"
            body.get("university").asText() shouldBe "건국대"

            // Then — DB
            val row = memberRow(memberId)
            row[MemberTable.nickname] shouldBe "새닉네임"
            row[MemberTable.region] shouldBe "BUSAN"
            row[MemberTable.highSchool] shouldBe "건대부고"
            row[MemberTable.university] shouldBe "건국대"
        }

        test("닉네임만 보내면 나머지 세 필드는 그대로 유지된다") {
            // Given — 부분 수정 계약의 핵심
            val email = "profile-patch-partial@example.com"
            val memberId = insertMember(
                email = email,
                nickname = "기존닉",
                region = "SEOUL",
                highSchool = "기존고",
                university = "기존대",
            )
            val accessToken = login(email)

            // When
            mockMvc.patchJson("/api/members/me") {
                content = mapper.writeValueAsString(mapOf("nickname" to "새닉네임"))
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }

            // Then
            val row = memberRow(memberId)
            row[MemberTable.nickname] shouldBe "새닉네임"
            row[MemberTable.region] shouldBe "SEOUL"
            row[MemberTable.highSchool] shouldBe "기존고"
            row[MemberTable.university] shouldBe "기존대"
        }

        test("highSchool 에 명시적 null 을 보내면 DB 가 null 로 비워진다") {
            // Given
            val email = "profile-patch-clear-high@example.com"
            val memberId = insertMember(email = email, nickname = "비우기", highSchool = "기존고")
            val accessToken = login(email)

            // When
            mockMvc.patchJson("/api/members/me") {
                content = mapper.writeValueAsString(mapOf<String, Any?>("highSchool" to null))
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }

            // Then
            memberRow(memberId)[MemberTable.highSchool] shouldBe null
        }

        test("highSchool 을 생략하면 기존 값이 유지된다 (생략 != 비우기)") {
            // Given — D3-a tri-state 의 종단 실증
            val email = "profile-patch-omit-high@example.com"
            val memberId = insertMember(email = email, nickname = "생략", highSchool = "기존고")
            val accessToken = login(email)

            // When — highSchool 을 아예 담지 않는다
            mockMvc.patchJson("/api/members/me") {
                content = mapper.writeValueAsString(mapOf("university" to "건국대"))
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }

            // Then
            val row = memberRow(memberId)
            row[MemberTable.highSchool] shouldBe "기존고"
            row[MemberTable.university] shouldBe "건국대"
        }

        test("university 에 명시적 null 을 보내면 DB 가 null 로 비워진다") {
            // Given
            val email = "profile-patch-clear-univ@example.com"
            val memberId = insertMember(email = email, nickname = "대학비움", university = "기존대")
            val accessToken = login(email)

            // When
            mockMvc.patchJson("/api/members/me") {
                content = mapper.writeValueAsString(mapOf<String, Any?>("university" to null))
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }

            // Then
            memberRow(memberId)[MemberTable.university] shouldBe null
        }

        test("university 를 생략하면 기존 값이 유지된다") {
            // Given
            val email = "profile-patch-omit-univ@example.com"
            val memberId = insertMember(email = email, nickname = "대학유지", university = "기존대")
            val accessToken = login(email)

            // When
            mockMvc.patchJson("/api/members/me") {
                content = mapper.writeValueAsString(mapOf("highSchool" to "건대부고"))
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }

            // Then
            memberRow(memberId)[MemberTable.university] shouldBe "기존대"
        }

        test("한쪽은 비우고 다른 쪽은 바꾸는 요청도 필드마다 독립적으로 적용된다") {
            // Given
            val email = "profile-patch-mixed@example.com"
            val memberId = insertMember(
                email = email,
                nickname = "혼합",
                highSchool = "기존고",
                university = "기존대",
            )
            val accessToken = login(email)
            val request = mapOf<String, Any?>("highSchool" to null, "university" to "건국대")

            // When
            mockMvc.patchJson("/api/members/me") {
                content = mapper.writeValueAsString(request)
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }

            // Then
            val row = memberRow(memberId)
            row[MemberTable.highSchool] shouldBe null
            row[MemberTable.university] shouldBe "건국대"
        }

        test("빈 본문을 보내면 200이고 아무것도 바뀌지 않는다") {
            // Given
            val email = "profile-patch-empty@example.com"
            val memberId = insertMember(
                email = email,
                nickname = "빈본문",
                region = "SEOUL",
                highSchool = "기존고",
                university = "기존대",
            )
            val accessToken = login(email)

            // When
            mockMvc.patchJson("/api/members/me") {
                content = "{}"
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }

            // Then
            val row = memberRow(memberId)
            row[MemberTable.nickname] shouldBe "빈본문"
            row[MemberTable.region] shouldBe "SEOUL"
            row[MemberTable.highSchool] shouldBe "기존고"
            row[MemberTable.university] shouldBe "기존대"
        }

        test("highSchool 에 빈 문자열을 보내면 빈 문자열로 저장된다 (비우기가 아니다)") {
            // Given
            val email = "profile-patch-blank@example.com"
            val memberId = insertMember(email = email, nickname = "빈문자열", highSchool = "기존고")
            val accessToken = login(email)

            // When
            mockMvc.patchJson("/api/members/me") {
                content = mapper.writeValueAsString(mapOf("highSchool" to ""))
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }

            // Then
            memberRow(memberId)[MemberTable.highSchool] shouldBe ""
        }
    }

    context("PATCH /api/members/me — 응답 계약") {

        test("응답은 수정 후 값을 담는다") {
            // Given
            val email = "profile-patch-response@example.com"
            insertMember(email = email, nickname = "응답확인", region = "SEOUL")
            val accessToken = login(email)

            // When
            val result = mockMvc.patchJson("/api/members/me") {
                content = mapper.writeValueAsString(mapOf("region" to "GYEONGGI_DO"))
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then
            mapper.readTree(result.response.contentAsByteArray).get("region").asText() shouldBe "GYEONGGI_DO"
        }

        test("응답은 GET 과 동일한 11개 필드이며 프로필 사진 URL 도 포함한다") {
            // Given
            val email = "profile-patch-response-fields@example.com"
            val memberId = insertMember(email = email, nickname = "동일필드")
            insertPhoto(
                memberId = memberId,
                thumbnailKey = "member/thumbnail/$memberId/thumb_photo.jpg",
            )
            val accessToken = login(email)

            // When
            val result = mockMvc.patchJson("/api/members/me") {
                content = mapper.writeValueAsString(mapOf("nickname" to "바뀐닉"))
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then
            val body = mapper.readTree(result.response.contentAsByteArray)
            body.size() shouldBe 11
            body.get("memberId").asText() shouldBe idObfuscator.encode(ObfuscationType.MEMBER, memberId)
            body.get("nickname").asText() shouldBe "바뀐닉"
            body.get("profileImageUrl").asText() shouldBe
                "/files/member/thumbnail/$memberId/thumb_photo.jpg"
        }
    }

    context("PATCH /api/members/me — 검증 실패") {

        test("닉네임이 1자면 400을 반환한다") {
            // Given
            val email = "profile-patch-nick-short@example.com"
            insertMember(email = email, nickname = "짧은닉")
            val accessToken = login(email)

            // When & Then
            mockMvc.patchJson("/api/members/me") {
                content = mapper.writeValueAsString(mapOf("nickname" to "가"))
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isBadRequest() } }
        }

        test("닉네임이 9자면 400을 반환한다") {
            // Given — 경계: 8자까지 허용
            val email = "profile-patch-nick-long@example.com"
            insertMember(email = email, nickname = "긴닉네임")
            val accessToken = login(email)

            // When & Then
            mockMvc.patchJson("/api/members/me") {
                content = mapper.writeValueAsString(mapOf("nickname" to "가나다라마바사아자"))
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isBadRequest() } }
        }

        test("닉네임에 특수문자가 있으면 400을 반환한다") {
            // Given
            val email = "profile-patch-nick-special@example.com"
            insertMember(email = email, nickname = "특수문자")
            val accessToken = login(email)

            // When & Then
            mockMvc.patchJson("/api/members/me") {
                content = mapper.writeValueAsString(mapOf("nickname" to "닉네임!"))
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isBadRequest() } }
        }

        test("닉네임이 빈 문자열이면 400을 반환한다") {
            // Given
            val email = "profile-patch-nick-blank@example.com"
            insertMember(email = email, nickname = "빈닉네임")
            val accessToken = login(email)

            // When & Then
            mockMvc.patchJson("/api/members/me") {
                content = mapper.writeValueAsString(mapOf("nickname" to ""))
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isBadRequest() } }
        }

        test("닉네임이 8자면 통과한다") {
            // Given — 경계: 상한 허용값
            val email = "profile-patch-nick-max@example.com"
            val memberId = insertMember(email = email, nickname = "경계닉네임")
            val accessToken = login(email)

            // When
            mockMvc.patchJson("/api/members/me") {
                content = mapper.writeValueAsString(mapOf("nickname" to "가나다라마바사아"))
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }

            // Then
            memberRow(memberId)[MemberTable.nickname] shouldBe "가나다라마바사아"
        }

        test("닉네임 형식이 틀리면 DB 는 하나도 바뀌지 않는다") {
            // Given
            val email = "profile-patch-nick-nochange@example.com"
            val memberId = insertMember(email = email, nickname = "무변경", region = "SEOUL")
            val accessToken = login(email)
            val request = mapOf("nickname" to "가", "region" to "BUSAN")

            // When
            mockMvc.patchJson("/api/members/me") {
                content = mapper.writeValueAsString(request)
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isBadRequest() } }

            // Then
            val row = memberRow(memberId)
            row[MemberTable.nickname] shouldBe "무변경"
            row[MemberTable.region] shouldBe "SEOUL"
        }

        test("nickname 에 명시적 null 을 보내면 400을 반환한다") {
            // Given — 닉네임은 비울 수 없는 필드다 (생략은 되지만 null 은 안 된다)
            val email = "profile-patch-nick-null@example.com"
            insertMember(email = email, nickname = "널닉네임")
            val accessToken = login(email)

            // When & Then
            mockMvc.patchJson("/api/members/me") {
                content = mapper.writeValueAsString(mapOf<String, Any?>("nickname" to null))
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isBadRequest() } }
        }

        test("region 에 명시적 null 을 보내면 400을 반환한다") {
            // Given — 지역도 비울 수 없는 필드다
            val email = "profile-patch-region-null@example.com"
            insertMember(email = email, nickname = "널지역")
            val accessToken = login(email)

            // When & Then
            mockMvc.patchJson("/api/members/me") {
                content = mapper.writeValueAsString(mapOf<String, Any?>("region" to null))
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isBadRequest() } }
        }

        test("nickname 에 명시적 null 을 보내면 DB 가 바뀌지 않는다") {
            // Given
            val email = "profile-patch-nick-null-db@example.com"
            val memberId = insertMember(email = email, nickname = "널무변경", region = "SEOUL")
            val accessToken = login(email)
            val request = mapOf<String, Any?>("nickname" to null, "region" to "BUSAN")

            // When
            mockMvc.patchJson("/api/members/me") {
                content = mapper.writeValueAsString(request)
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isBadRequest() } }

            // Then
            val row = memberRow(memberId)
            row[MemberTable.nickname] shouldBe "널무변경"
            row[MemberTable.region] shouldBe "SEOUL"
        }

        test("알 수 없는 region 코드를 보내면 400을 반환한다") {
            // Given
            val email = "profile-patch-region-unknown@example.com"
            insertMember(email = email, nickname = "이상지역")
            val accessToken = login(email)

            // When & Then
            mockMvc.patchJson("/api/members/me") {
                content = mapper.writeValueAsString(mapOf("region" to "NOWHERE"))
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isBadRequest() } }
        }

        test("지역 displayName(\"서울\") 을 보내면 400을 반환한다") {
            // Given — 계약은 enum 코드다
            val email = "profile-patch-region-display@example.com"
            insertMember(email = email, nickname = "한글지역")
            val accessToken = login(email)

            // When & Then
            mockMvc.patchJson("/api/members/me") {
                content = mapper.writeValueAsString(mapOf("region" to "서울"))
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isBadRequest() } }
        }

        test("인증 토큰 없이 수정하면 401을 반환한다") {
            // When & Then
            mockMvc.patchJson("/api/members/me") {
                content = mapper.writeValueAsString(mapOf("nickname" to "새닉네임"))
            }
                .andExpect { status { isUnauthorized() } }
        }
    }

    context("PATCH /api/members/me — 닉네임 중복") {

        test("다른 회원이 쓰고 있는 닉네임이면 409를 반환한다") {
            // Given
            val ownerEmail = "profile-patch-dup-owner@example.com"
            val takenNickname = "선점닉네임"
            insertMember(email = ownerEmail, nickname = takenNickname)

            val email = "profile-patch-dup@example.com"
            insertMember(email = email, nickname = "중복시도")
            val accessToken = login(email)

            // When & Then
            mockMvc.patchJson("/api/members/me") {
                content = mapper.writeValueAsString(mapOf("nickname" to takenNickname))
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isConflict() } }
        }

        test("닉네임이 중복이면 DB 는 하나도 바뀌지 않는다") {
            // Given
            val ownerEmail = "profile-patch-dup-owner2@example.com"
            val takenNickname = "선점닉네임"
            insertMember(email = ownerEmail, nickname = takenNickname)

            val email = "profile-patch-dup2@example.com"
            val memberId = insertMember(email = email, nickname = "중복시도", region = "SEOUL")
            val accessToken = login(email)
            val request = mapOf("nickname" to takenNickname, "region" to "BUSAN")

            // When
            mockMvc.patchJson("/api/members/me") {
                content = mapper.writeValueAsString(request)
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isConflict() } }

            // Then
            val row = memberRow(memberId)
            row[MemberTable.nickname] shouldBe "중복시도"
            row[MemberTable.region] shouldBe "SEOUL"
        }

        test("자기 닉네임을 그대로 보내면 200을 반환한다") {
            // Given — 자기 자신 때문에 409가 나면 안 된다
            val email = "profile-patch-same-nick@example.com"
            val nickname = "그대로닉"
            val memberId = insertMember(email = email, nickname = nickname)
            val accessToken = login(email)

            // When
            mockMvc.patchJson("/api/members/me") {
                content = mapper.writeValueAsString(mapOf("nickname" to nickname, "region" to "BUSAN"))
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }

            // Then
            val row = memberRow(memberId)
            row[MemberTable.nickname] shouldBe nickname
            row[MemberTable.region] shouldBe "BUSAN"
        }

        test("탈퇴 완료된 회원이 쓰던 닉네임은 다시 사용할 수 있다") {
            // Given — 탈퇴 완료 배치가 닉네임을 sentinel 로 익명화하고 소프트 삭제한 상태
            val releasedNickname = "탈퇴닉네임"
            val withdrawnId = insertMember(
                email = "profile-patch-withdrawn@example.com",
                nickname = releasedNickname,
            )
            anonymizeAndSoftDelete(withdrawnId)

            val email = "profile-patch-reuse-nick@example.com"
            val memberId = insertMember(email = email, nickname = "재사용")
            val accessToken = login(email)

            // When
            mockMvc.patchJson("/api/members/me") {
                content = mapper.writeValueAsString(mapOf("nickname" to releasedNickname))
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }

            // Then
            memberRow(memberId)[MemberTable.nickname] shouldBe releasedNickname
            memberRow(withdrawnId)[MemberTable.nickname] shouldBe WithdrawnSentinel.nickname(withdrawnId)
        }
    }
})
