package com.konkuk.ma.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.domain.auth.domain.port.SmsRepository
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FindEmailIntegrationTest(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper,
    private val smsRepository: SmsRepository,
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
        return "find-email-test$memberSeq@example.com"
    }

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
                it[nickname] = "테스터-$email"
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

    fun confirmSms(phone: String) = smsRepository.confirmVerificationCode(phone)

    fun findEmail(name: String, phone: String) =
        mockMvc.postJson("/api/auth/find-email") {
            content = mapper.writeValueAsString(mapOf("name" to name, "phone" to phone))
        }

    context("POST /api/auth/find-email") {

        context("정상 조회 (SMS 인증 완료 → 200 + 전체 이메일)") {

            test("SMS 인증 완료 + 이름·전화번호가 일치하는 회원이 있으면 200과 마스킹되지 않은 전체 이메일을 반환한다") {
                val name = "김철수"
                val phone = "01012345678"
                val email = "holeman@naver.com"
                insertMember(email = email, name = name, phoneNumber = phone)
                confirmSms(phone)

                val result = findEmail(name = name, phone = phone)
                    .andExpect { status { isOk() } }
                    .andReturn()

                val body = mapper.readTree(result.response.contentAsByteArray)
                body.get("email").asText() shouldBe email
            }

            test("SMS 인증 완료 + 탈퇴 유예 중(deleted=false, withdrawalRequestedAt 존재)인 회원도 200과 전체 이메일을 반환한다") {
                val name = "이몽룡"
                val phone = "01033334444"
                val email = "grace@example.com"
                insertMember(
                    email = email,
                    name = name,
                    phoneNumber = phone,
                    withdrawalRequestedAt = LocalDateTime.now(),
                )
                confirmSms(phone)

                val result = findEmail(name = name, phone = phone)
                    .andExpect { status { isOk() } }
                    .andReturn()

                val body = mapper.readTree(result.response.contentAsByteArray)
                body.get("email").asText() shouldBe email
            }
        }

        context("SMS 미인증 (조회 이전에 400 INVALID_INPUT_VALUE)") {

            test("SMS 미인증이면 이름·전화번호가 일치하는 회원이 있어도 400을 반환한다") {
                val name = "박영희"
                val phone = "01044445555"
                insertMember(name = name, phoneNumber = phone)

                val result = findEmail(name = name, phone = phone)
                    .andExpect { status { isBadRequest() } }
                    .andReturn()

                val body = mapper.readTree(result.response.contentAsByteArray)
                body.get("code").asText() shouldBe "INVALID_INPUT_VALUE"
            }

            test("SMS 미인증이면 회원이 존재하지 않아도 404가 아니라 400을 반환한다") {
                findEmail(name = "홍길동", phone = "01066667777")
                    .andExpect { status { isBadRequest() } }
            }
        }

        context("조회 실패 (SMS 인증 완료 후에도 404 ENTITY_NOT_FOUND)") {

            test("SMS 인증 완료 + 이름과 전화번호 모두 일치하는 회원이 없으면 404를 반환한다") {
                insertMember(name = "김철수", phoneNumber = "01012345678")
                val name = "홍길동"
                val phone = "01099998888"
                confirmSms(phone)

                val result = findEmail(name = name, phone = phone)
                    .andExpect { status { isNotFound() } }
                    .andReturn()

                val body = mapper.readTree(result.response.contentAsByteArray)
                body.get("code").asText() shouldBe "ENTITY_NOT_FOUND"
            }

            test("SMS 인증 완료 + 이름은 일치하지만 전화번호가 다르면 404를 반환한다") {
                val name = "김철수"
                insertMember(name = name, phoneNumber = "01012345678")
                val phone = "01088887777"
                confirmSms(phone)

                findEmail(name = name, phone = phone)
                    .andExpect { status { isNotFound() } }
            }

            test("SMS 인증 완료 + 전화번호는 일치하지만 이름이 다르면 404를 반환한다") {
                val phone = "01012345678"
                insertMember(name = "김철수", phoneNumber = phone)
                confirmSms(phone)

                findEmail(name = "이몽룡", phone = phone)
                    .andExpect { status { isNotFound() } }
            }

            test("SMS 인증 완료 + 익명화(deleted=true)된 회원의 원래 이름과 전화번호로 조회하면 404를 반환한다") {
                val name = "최익명"
                val phone = "01055556666"
                insertMember(name = name, phoneNumber = phone, deleted = true)
                confirmSms(phone)

                findEmail(name = name, phone = phone)
                    .andExpect { status { isNotFound() } }
            }
        }

        context("요청 형식 검증 (bean validation 최우선 → 400 INVALID_INPUT_VALUE)") {

            test("이름이 빈 값이면 400을 반환한다") {
                val result = findEmail(name = "", phone = "01012345678")
                    .andExpect { status { isBadRequest() } }
                    .andReturn()

                val body = mapper.readTree(result.response.contentAsByteArray)
                body.get("code").asText() shouldBe "INVALID_INPUT_VALUE"
            }

            test("이름이 1글자면 400을 반환한다") {
                findEmail(name = "김", phone = "01012345678")
                    .andExpect { status { isBadRequest() } }
            }

            test("이름이 11글자면 400을 반환한다") {
                findEmail(name = "김".repeat(11), phone = "01012345678")
                    .andExpect { status { isBadRequest() } }
            }

            test("이름이 영문이면 400을 반환한다") {
                findEmail(name = "John", phone = "01012345678")
                    .andExpect { status { isBadRequest() } }
            }

            test("이름에 특수문자가 포함되면 400을 반환한다") {
                findEmail(name = "김철수!", phone = "01012345678")
                    .andExpect { status { isBadRequest() } }
            }

            test("전화번호가 빈 값이면 400을 반환한다") {
                findEmail(name = "김철수", phone = "")
                    .andExpect { status { isBadRequest() } }
            }

            test("전화번호에 하이픈이 포함되면 400을 반환한다") {
                findEmail(name = "김철수", phone = "010-1234-5678")
                    .andExpect { status { isBadRequest() } }
            }

            test("전화번호가 010으로 시작하지 않으면 400을 반환한다") {
                findEmail(name = "김철수", phone = "01112345678")
                    .andExpect { status { isBadRequest() } }
            }

            test("전화번호 자릿수가 부족하면 400을 반환한다") {
                findEmail(name = "김철수", phone = "010123456")
                    .andExpect { status { isBadRequest() } }
            }

            test("전화번호 자릿수가 초과되면 400을 반환한다") {
                findEmail(name = "김철수", phone = "010123456789")
                    .andExpect { status { isBadRequest() } }
            }
        }
    }
})
