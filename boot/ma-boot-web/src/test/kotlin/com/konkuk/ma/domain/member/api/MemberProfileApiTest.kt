package com.konkuk.ma.domain.member.api

import com.konkuk.ma.auth.JwtManager
import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.common.domain.id.port.IdObfuscator
import com.konkuk.ma.domain.matching.fixture.MemberFixture
import com.konkuk.ma.domain.member.application.MemberProfileCommandService
import com.konkuk.ma.domain.member.application.MemberProfileQueryService
import com.konkuk.ma.domain.member.domain.Gender
import com.konkuk.ma.domain.member.domain.MemberProfile
import com.konkuk.ma.domain.member.domain.Region
import com.konkuk.ma.exception.DuplicateException
import com.konkuk.ma.exception.EntityType
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.getJson
import com.konkuk.ma.extension.patchJson
import com.konkuk.ma.extension.requestBody
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.vocabulary.authorizationHeader
import com.konkuk.ma.vocabulary.birthDate
import com.konkuk.ma.vocabulary.email
import com.konkuk.ma.vocabulary.gender
import com.konkuk.ma.vocabulary.highSchool
import com.konkuk.ma.vocabulary.memberId
import com.konkuk.ma.vocabulary.name
import com.konkuk.ma.vocabulary.nickname
import com.konkuk.ma.vocabulary.phoneNumber
import com.konkuk.ma.vocabulary.profileImageUrl
import com.konkuk.ma.vocabulary.region
import com.konkuk.ma.vocabulary.university
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import java.time.LocalDate

@WebMvcTest(MemberProfileApi::class)
@BaseApiTest
class MemberProfileApiTest(
    private val mockMvc: MockMvc,
    private val jwtManager: JwtManager,
    private val idObfuscator: IdObfuscator,
    @MockkBean private val memberProfileQueryService: MemberProfileQueryService,
    @MockkBean private val memberProfileCommandService: MemberProfileCommandService,
) : FunSpec({

    val accessToken = jwtManager.generateAccessToken(AUTHENTICATED_MEMBER_ID)
    val encodedMemberId = idObfuscator.encode(ObfuscationType.MEMBER, AUTHENTICATED_MEMBER_ID)

    test("내 프로필 조회 API 문서화") {
        // Given
        every { memberProfileQueryService.findOne(AUTHENTICATED_MEMBER_ID) } returns profileOf()

        // When & Then
        mockMvc.getJson("/api/members/me") {
            authorization("Bearer $accessToken")
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.memberId") { value(encodedMemberId) }
                jsonPath("$.email") { value("holeman@naver.com") }
                jsonPath("$.nickname") { value("홀맨") }
                jsonPath("$.name") { value("김테스트") }
                jsonPath("$.gender") { value("MALE") }
                jsonPath("$.birthDate") { value("1995-03-07") }
                jsonPath("$.phoneNumber") { value("01012345678") }
                jsonPath("$.region") { value("SEOUL") }
                jsonPath("$.highSchool") { value("건국고등학교") }
                jsonPath("$.university") { value("건국대학교") }
                jsonPath("$.profileImageUrl") { value("/files/member/thumbnail/1/thumb_photo.jpg") }
            }
            .andDocument(
                "member-profile-me",
                authorizationHeader(),
                responseBody(*myProfileFields())
            )
    }

    test("내 프로필 조회 - 학교 정보와 프로필 사진이 없으면 null 로 내려온다") {
        // Given
        every { memberProfileQueryService.findOne(AUTHENTICATED_MEMBER_ID) } returns
            profileOf(highSchool = null, university = null, profileImageUrl = null)

        // When & Then
        mockMvc.getJson("/api/members/me") {
            authorization("Bearer $accessToken")
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.highSchool") { value(null) }
                jsonPath("$.university") { value(null) }
                jsonPath("$.profileImageUrl") { value(null) }
            }
            .andDocument(
                "member-profile-me-empty-optional",
                authorizationHeader(),
                responseBody(*myProfileFields())
            )
    }

    test("내 프로필 수정 API 문서화") {
        // Given
        every { memberProfileCommandService.update(eq(AUTHENTICATED_MEMBER_ID), any()) } returns
            profileOf(
                nickname = "새닉네임",
                region = Region.GYEONGGI_DO,
                highSchool = "새고등학교",
                university = "새대학교",
            )

        // When & Then
        mockMvc.patchJson("/api/members/me") {
            authorization("Bearer $accessToken")
            content = """
                {
                  "nickname": "새닉네임",
                  "region": "GYEONGGI_DO",
                  "highSchool": "새고등학교",
                  "university": "새대학교"
                }
            """.trimIndent()
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.nickname") { value("새닉네임") }
                jsonPath("$.region") { value("GYEONGGI_DO") }
                jsonPath("$.highSchool") { value("새고등학교") }
                jsonPath("$.university") { value("새대학교") }
            }
            .andDocument(
                "member-profile-update",
                authorizationHeader(),
                requestBody(*updateRequestFields()),
                responseBody(*myProfileFields())
            )
    }

    test("내 프로필 수정 - 생략한 필드는 변경되지 않는다") {
        // Given
        every { memberProfileCommandService.update(eq(AUTHENTICATED_MEMBER_ID), any()) } returns
            profileOf(nickname = "새닉네임")

        // When & Then
        mockMvc.patchJson("/api/members/me") {
            authorization("Bearer $accessToken")
            content = """{"nickname":"새닉네임"}"""
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.nickname") { value("새닉네임") }
                jsonPath("$.region") { value("SEOUL") }
                jsonPath("$.highSchool") { value("건국고등학교") }
                jsonPath("$.university") { value("건국대학교") }
            }
            .andDocument(
                "member-profile-update-partial",
                authorizationHeader(),
                requestBody(*updateRequestFields()),
                responseBody(*myProfileFields())
            )
    }

    test("내 프로필 수정 - highSchool·university 에 null 을 명시하면 비워진다") {
        // Given
        every { memberProfileCommandService.update(eq(AUTHENTICATED_MEMBER_ID), any()) } returns
            profileOf(highSchool = null, university = null)

        // When & Then
        mockMvc.patchJson("/api/members/me") {
            authorization("Bearer $accessToken")
            content = """{"highSchool":null,"university":null}"""
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.highSchool") { value(null) }
                jsonPath("$.university") { value(null) }
            }
            .andDocument(
                "member-profile-update-clear",
                authorizationHeader(),
                requestBody(
                    highSchool() means "출신 고등학교 (null 을 명시하면 비워진다)" isOptional true,
                    university() means "출신 대학교 (null 을 명시하면 비워진다)" isOptional true,
                ),
                responseBody(*myProfileFields())
            )
    }

    test("내 프로필 수정 - 닉네임 형식이 유효하지 않으면 400 을 반환한다") {
        // When & Then
        mockMvc.patchJson("/api/members/me") {
            authorization("Bearer $accessToken")
            content = """{"nickname":"a"}"""
        }
            .andExpect { status { isBadRequest() } }
            .andDocument(
                "member-profile-update-invalid-nickname",
                authorizationHeader(),
                requestBody(
                    nickname() means "닉네임 (영문·한글·숫자 2~8자 형식을 위반한 값)" isOptional true,
                )
            )
    }

    test("내 프로필 수정 - nickname 에 명시적 null 을 보내면 400 을 반환한다") {
        // When & Then
        mockMvc.patchJson("/api/members/me") {
            authorization("Bearer $accessToken")
            content = """{"nickname":null}"""
        }
            .andExpect { status { isBadRequest() } }
    }

    test("내 프로필 수정 - region 에 명시적 null 을 보내면 400 을 반환한다") {
        // When & Then
        mockMvc.patchJson("/api/members/me") {
            authorization("Bearer $accessToken")
            content = """{"region":null}"""
        }
            .andExpect { status { isBadRequest() } }
    }

    test("내 프로필 수정 - 알 수 없는 region 코드를 보내면 400 을 반환한다") {
        // When & Then
        mockMvc.patchJson("/api/members/me") {
            authorization("Bearer $accessToken")
            content = """{"region":"MARS"}"""
        }
            .andExpect { status { isBadRequest() } }
    }

    test("내 프로필 수정 - 이미 사용중인 닉네임이면 409 를 반환한다") {
        // Given
        every { memberProfileCommandService.update(eq(AUTHENTICATED_MEMBER_ID), any()) } throws
            DuplicateException(EntityType.MEMBER, "nickname", "중복닉네임")

        // When & Then
        mockMvc.patchJson("/api/members/me") {
            authorization("Bearer $accessToken")
            content = """{"nickname":"중복닉네임"}"""
        }
            .andExpect { status { isConflict() } }
    }
})

private const val AUTHENTICATED_MEMBER_ID = 1L

private fun profileOf(
    nickname: String = "홀맨",
    region: Region = Region.SEOUL,
    highSchool: String? = "건국고등학교",
    university: String? = "건국대학교",
    profileImageUrl: String? = "/files/member/thumbnail/1/thumb_photo.jpg",
): MemberProfile = MemberProfile.of(
    MemberFixture.create(
        id = AUTHENTICATED_MEMBER_ID,
        email = "holeman@naver.com",
        nickname = nickname,
        gender = Gender.MALE,
        phoneNumber = "01012345678",
        name = "김테스트",
        region = region,
        birthDate = LocalDate.of(1995, 3, 7),
        highSchool = highSchool,
        university = university,
    ),
    profileImageUrl,
)

private fun myProfileFields() = arrayOf(
    memberId(),
    email(),
    nickname(),
    name(),
    gender(),
    birthDate(),
    phoneNumber(),
    region(),
    highSchool() isOptional true,
    university() isOptional true,
    profileImageUrl("profileImageUrl") isOptional true,
)

private fun updateRequestFields() = arrayOf(
    nickname() means "닉네임 (생략 시 변경 없음, null 은 400)" isOptional true,
    region() means "거주 지역 코드 (생략 시 변경 없음, null 은 400)" isOptional true,
    highSchool() means "출신 고등학교 (생략 시 변경 없음, null 은 비우기)" isOptional true,
    university() means "출신 대학교 (생략 시 변경 없음, null 은 비우기)" isOptional true,
)
