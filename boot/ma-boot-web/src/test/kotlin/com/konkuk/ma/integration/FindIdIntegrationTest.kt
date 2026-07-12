package com.konkuk.ma.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.domain.member.entity.table.MemberTable
import com.konkuk.ma.extension.postJson
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.LocalDateTime
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc

/**
 * 이메일 찾기(POST /api/auth/find-id) API E2E 통합 테스트 (REST Docs 제외, HTTP 상태/계약 검증).
 *
 * API → FindIdService → MemberQueryRepository(findOne) → MemberQueryDao(findOne)
 * → MEMBERS 테이블을 관통하며 다음 확정 계약을 검증한다.
 *  - 정상: name+phone 이 일치하는 활성 회원 존재 → 200 + 마스킹된 이메일(`hol***@naver.com` 형식).
 *  - 탈퇴 유예(deleted=false, withdrawalRequestedAt≠null) 회원 → 200(복구 흐름 지원, 포함).
 *  - 미존재 / name 만 일치 / phone 만 일치 / 익명화(deleted=true) → 404(ENTITY_NOT_FOUND).
 *  - name/phone 형식 위반 → 400(INVALID_INPUT_VALUE, bean validation 이 서비스 호출 전 차단).
 *  - 인증 불필요(permitAll): Authorization 헤더 없이 호출 가능 — 정상 케이스가 이를 겸해 검증한다.
 *
 * 마스킹 규칙(확정): local part 앞 3글자 유지 + `***` + `@도메인`. local part 가 3글자 이하이면 앞 1글자 + `***`.
 * 경계값의 상세 검증은 도메인 단위 테스트 EmailTest("masked" context)가 담당한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FindIdIntegrationTest(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper,
) : FunSpec({

    var memberSeq = 0

    beforeSpec {
        transaction {
            SchemaUtils.create(MemberTable)
        }
    }

    afterEach {
        transaction {
            MemberTable.deleteAll()
        }
    }

    afterSpec {
        transaction {
            SchemaUtils.drop(MemberTable)
        }
    }

    fun nextEmail(): String {
        memberSeq += 1
        return "find-id-test$memberSeq@example.com"
    }

    // MEMBERS 에 회원 1건을 직접 삽입한다. phoneNumber 는 저장 형식(fullNumber, 하이픈 없음)으로 넣는다.
    fun insertMember(
        email: String = nextEmail(),
        name: String = "김철수",
        phoneNumber: String = "01012345678",
        deleted: Boolean = false,
        withdrawalRequestedAt: LocalDateTime? = null,
    ) {
        transaction {
            MemberTable.insert {
                it[MemberTable.email] = email
                it[password] = "password"
                it[nickname] = "테스터"
                it[gender] = "MALE"
                it[MemberTable.phoneNumber] = phoneNumber
                it[MemberTable.name] = name
                it[birthDate] = LocalDate.of(1990, 1, 1)
                it[region] = "SEOUL"
                it[MemberTable.deleted] = deleted
                it[MemberTable.withdrawalRequestedAt] = withdrawalRequestedAt
            }
        }
    }

    // 인증 없이(permitAll) find-id 를 호출한다. Authorization 헤더를 붙이지 않는다.
    fun findId(name: String, phone: String) =
        mockMvc.postJson("/api/auth/find-id") {
            content = mapper.writeValueAsString(mapOf("name" to name, "phone" to phone))
        }

    context("POST /api/auth/find-id") {

        context("정상 조회 (200 + 마스킹된 이메일)") {

            test("이름과 전화번호가 일치하는 회원이 있으면 200과 마스킹된 이메일을 반환한다") {
                // Given - 이름/전화번호가 일치하는 활성 회원 (로컬파트 7글자)
                val name = "김철수"
                val phone = "01012345678"
                insertMember(email = "holeman@naver.com", name = name, phoneNumber = phone)

                // When - 인증 헤더 없이(permitAll) 조회
                val result = findId(name = name, phone = phone)
                    .andExpect { status { isOk() } }
                    .andReturn()

                // Then - 앞 3글자만 남고 나머지 마스킹, 도메인은 유지
                val body = mapper.readTree(result.response.contentAsByteArray)
                body.get("email").asText() shouldBe "hol***@naver.com"
            }

            test("로컬파트가 2글자인 이메일 회원도 200과 마스킹된 이메일을 반환한다") {
                // Given - 로컬파트가 짧은(2글자) 이메일 → 앞 1글자만 유지되어야 함
                val name = "박영희"
                val phone = "01022223333"
                insertMember(email = "ab@example.com", name = name, phoneNumber = phone)

                // When
                val result = findId(name = name, phone = phone)
                    .andExpect { status { isOk() } }
                    .andReturn()

                // Then - 파이프라인이 raw 가 아니라 masked() 결과를 내려주는지 확인
                val body = mapper.readTree(result.response.contentAsByteArray)
                body.get("email").asText() shouldBe "a***@example.com"
            }

            test("탈퇴 유예 중(deleted=false, withdrawalRequestedAt 존재)인 회원도 200과 마스킹된 이메일을 반환한다") {
                // Given - 탈퇴 신청만 한 회원(activeRows 에 포함, 복구 흐름 지원)
                val name = "이몽룡"
                val phone = "01033334444"
                insertMember(
                    email = "grace@example.com",
                    name = name,
                    phoneNumber = phone,
                    withdrawalRequestedAt = LocalDateTime.now(),
                )

                // When
                val result = findId(name = name, phone = phone)
                    .andExpect { status { isOk() } }
                    .andReturn()

                // Then
                val body = mapper.readTree(result.response.contentAsByteArray)
                body.get("email").asText() shouldBe "gra***@example.com"
            }
        }

        context("조회 실패 (404 ENTITY_NOT_FOUND)") {

            test("이름과 전화번호 모두 일치하는 회원이 없으면 404를 반환한다") {
                // Given - 일치하지 않는 회원만 존재
                insertMember(name = "김철수", phoneNumber = "01012345678")

                // When & Then
                val result = findId(name = "홍길동", phone = "01099998888")
                    .andExpect { status { isNotFound() } }
                    .andReturn()

                // Then - 에러 응답 body 형식(ENTITY_NOT_FOUND) 확정
                val body = mapper.readTree(result.response.contentAsByteArray)
                body.get("code").asText() shouldBe "ENTITY_NOT_FOUND"
            }

            test("이름은 일치하지만 전화번호가 다르면 404를 반환한다") {
                // Given
                val name = "김철수"
                insertMember(name = name, phoneNumber = "01012345678")

                // When & Then - 이름만 일치
                findId(name = name, phone = "01099998888")
                    .andExpect { status { isNotFound() } }
            }

            test("전화번호는 일치하지만 이름이 다르면 404를 반환한다") {
                // Given
                val phone = "01012345678"
                insertMember(name = "김철수", phoneNumber = phone)

                // When & Then - 전화번호만 일치
                findId(name = "이몽룡", phone = phone)
                    .andExpect { status { isNotFound() } }
            }

            test("익명화(deleted=true)된 회원의 원래 이름과 전화번호로 조회하면 404를 반환한다") {
                // Given - soft delete 된 회원 (activeRows 가 deleted=true 를 제외)
                val name = "최익명"
                val phone = "01055556666"
                insertMember(name = name, phoneNumber = phone, deleted = true)

                // When & Then - name/phone 이 그대로 일치해도 활성 필터에서 제외됨
                findId(name = name, phone = phone)
                    .andExpect { status { isNotFound() } }
            }
        }

        context("요청 형식 검증 (400 INVALID_INPUT_VALUE)") {

            test("이름이 빈 값이면 400을 반환한다") {
                // When & Then - @NotBlank 위반, 서비스 호출 전 차단
                val result = findId(name = "", phone = "01012345678")
                    .andExpect { status { isBadRequest() } }
                    .andReturn()

                // Then - 에러 응답 body 형식(INVALID_INPUT_VALUE) 확정
                val body = mapper.readTree(result.response.contentAsByteArray)
                body.get("code").asText() shouldBe "INVALID_INPUT_VALUE"
            }

            test("이름이 1글자면 400을 반환한다") {
                // When & Then - @Pattern(^[가-힣]{2,10}$) 위반
                findId(name = "김", phone = "01012345678")
                    .andExpect { status { isBadRequest() } }
            }

            test("이름이 11글자면 400을 반환한다") {
                // When & Then - 최대 10글자 초과
                findId(name = "김".repeat(11), phone = "01012345678")
                    .andExpect { status { isBadRequest() } }
            }

            test("이름이 영문이면 400을 반환한다") {
                // When & Then - 한글만 허용
                findId(name = "John", phone = "01012345678")
                    .andExpect { status { isBadRequest() } }
            }

            test("이름에 특수문자가 포함되면 400을 반환한다") {
                // When & Then
                findId(name = "김철수!", phone = "01012345678")
                    .andExpect { status { isBadRequest() } }
            }

            test("전화번호가 빈 값이면 400을 반환한다") {
                // When & Then - @NotBlank 위반
                findId(name = "김철수", phone = "")
                    .andExpect { status { isBadRequest() } }
            }

            test("전화번호에 하이픈이 포함되면 400을 반환한다") {
                // When & Then - @Pattern(^010\d{7,8}$)은 하이픈 불허
                findId(name = "김철수", phone = "010-1234-5678")
                    .andExpect { status { isBadRequest() } }
            }

            test("전화번호가 010으로 시작하지 않으면 400을 반환한다") {
                // When & Then
                findId(name = "김철수", phone = "01112345678")
                    .andExpect { status { isBadRequest() } }
            }

            test("전화번호 자릿수가 부족하면 400을 반환한다") {
                // When & Then - 010 + 6자리(총 9자리) → 최소 010+7자리 미달
                findId(name = "김철수", phone = "010123456")
                    .andExpect { status { isBadRequest() } }
            }

            test("전화번호 자릿수가 초과되면 400을 반환한다") {
                // When & Then - 010 + 9자리(총 12자리) → 최대 010+8자리 초과
                findId(name = "김철수", phone = "010123456789")
                    .andExpect { status { isBadRequest() } }
            }
        }
    }
})
