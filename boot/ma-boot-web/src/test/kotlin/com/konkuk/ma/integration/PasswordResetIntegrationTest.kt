package com.konkuk.ma.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.domain.auth.domain.port.PasswordEncryptor
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
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PasswordResetIntegrationTest(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper,
    private val smsRepository: SmsRepository,
    private val passwordEncryptor: PasswordEncryptor,
) : FunSpec({

    var memberSeq = 0

    val oldRawPassword = "oldpass12"
    val newPassword = "newpass34"

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
        return "find-password-test$memberSeq@example.com"
    }

    fun insertMember(
        email: String = nextEmail(),
        name: String = "김철수",
        phoneNumber: String = "01012345678",
        rawPassword: String = oldRawPassword,
        deleted: Boolean = false,
        withdrawalRequestedAt: LocalDateTime? = null,
    ) {
        transaction {
            MemberTable.insert {
                it[MemberTable.email] = email
                it[password] = passwordEncryptor.encode(rawPassword)
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

    fun storedPasswordOf(email: String): String = transaction {
        MemberTable.selectAll().where { MemberTable.email eq email }.first()[MemberTable.password]
    }

    fun confirmSms(phone: String) = smsRepository.confirmVerificationCode(phone)

    fun resetPassword(email: String, name: String, phone: String, newPassword: String) =
        mockMvc.postJson("/api/auth/find-password") {
            content = mapper.writeValueAsString(
                mapOf("email" to email, "name" to name, "phone" to phone, "newPassword" to newPassword)
            )
        }

    context("POST /api/auth/find-password") {

        context("정상 재설정 (SMS 인증 완료 + 3필드 일치 → 204 + 비밀번호 갱신)") {

            test("SMS 인증 완료 + email·name·phone 이 모두 일치하면 204를 반환하고 비밀번호를 새 값으로 갱신한다") {
                val name = "김철수"
                val phone = "01012345678"
                val email = "holeman@naver.com"
                insertMember(email = email, name = name, phoneNumber = phone)
                confirmSms(phone)

                resetPassword(email = email, name = name, phone = phone, newPassword = newPassword)
                    .andExpect { status { isNoContent() } }

                val stored = storedPasswordOf(email)
                passwordEncryptor.matches(newPassword, stored) shouldBe true
                passwordEncryptor.matches(oldRawPassword, stored) shouldBe false
            }

            test("SMS 인증 완료 + 탈퇴 유예 중(deleted=false, withdrawalRequestedAt 존재)인 회원도 204와 비밀번호 갱신을 지원한다") {
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

                resetPassword(email = email, name = name, phone = phone, newPassword = newPassword)
                    .andExpect { status { isNoContent() } }

                val stored = storedPasswordOf(email)
                passwordEncryptor.matches(newPassword, stored) shouldBe true
            }

            test("재설정된 비밀번호는 암호화되어 저장되며 평문으로 저장되지 않는다") {
                val name = "성춘향"
                val phone = "01022223333"
                val email = "chunhyang@example.com"
                insertMember(email = email, name = name, phoneNumber = phone)
                confirmSms(phone)

                resetPassword(email = email, name = name, phone = phone, newPassword = newPassword)
                    .andExpect { status { isNoContent() } }

                val stored = storedPasswordOf(email)
                (stored == newPassword) shouldBe false
                passwordEncryptor.matches(newPassword, stored) shouldBe true
            }
        }

        context("SMS 미인증 (조회 이전에 400 INVALID_INPUT_VALUE, 비밀번호 미변경)") {

            test("SMS 미인증이면 3필드가 일치하는 회원이 있어도 400을 반환하고 비밀번호를 바꾸지 않는다") {
                val name = "박영희"
                val phone = "01044445555"
                val email = "park@example.com"
                insertMember(email = email, name = name, phoneNumber = phone)

                val result = resetPassword(email = email, name = name, phone = phone, newPassword = newPassword)
                    .andExpect { status { isBadRequest() } }
                    .andReturn()

                val body = mapper.readTree(result.response.contentAsByteArray)
                body.get("code").asText() shouldBe "INVALID_INPUT_VALUE"
                passwordEncryptor.matches(oldRawPassword, storedPasswordOf(email)) shouldBe true
            }

            test("SMS 미인증이면 회원이 존재하지 않아도 404가 아니라 400을 반환한다") {
                resetPassword(email = "nobody@example.com", name = "홍길동", phone = "01066667777", newPassword = newPassword)
                    .andExpect { status { isBadRequest() } }
            }
        }

        context("조회 실패 (SMS 인증 완료 후에도 3필드 불일치면 404 ENTITY_NOT_FOUND, 비밀번호 미변경)") {

            test("SMS 인증 완료 + email 만 다르면 404를 반환한다") {
                val name = "김철수"
                val phone = "01012345678"
                insertMember(email = "correct@example.com", name = name, phoneNumber = phone)
                confirmSms(phone)

                val result = resetPassword(email = "wrong@example.com", name = name, phone = phone, newPassword = newPassword)
                    .andExpect { status { isNotFound() } }
                    .andReturn()

                val body = mapper.readTree(result.response.contentAsByteArray)
                body.get("code").asText() shouldBe "ENTITY_NOT_FOUND"
            }

            test("SMS 인증 완료 + name 만 다르면 404를 반환한다") {
                val email = "member@example.com"
                val phone = "01012345678"
                insertMember(email = email, name = "김철수", phoneNumber = phone)
                confirmSms(phone)

                resetPassword(email = email, name = "이몽룡", phone = phone, newPassword = newPassword)
                    .andExpect { status { isNotFound() } }
            }

            test("SMS 인증 완료 + phone 만 다르면 404를 반환한다") {
                val email = "member@example.com"
                val name = "김철수"
                insertMember(email = email, name = name, phoneNumber = "01012345678")
                val phone = "01088887777"
                confirmSms(phone)

                resetPassword(email = email, name = name, phone = phone, newPassword = newPassword)
                    .andExpect { status { isNotFound() } }
            }

            test("SMS 인증 완료 + 익명화(deleted=true)된 회원의 원래 3필드로 조회하면 404를 반환한다") {
                val name = "최익명"
                val phone = "01055556666"
                val email = "anon@example.com"
                insertMember(email = email, name = name, phoneNumber = phone, deleted = true)
                confirmSms(phone)

                resetPassword(email = email, name = name, phone = phone, newPassword = newPassword)
                    .andExpect { status { isNotFound() } }
            }
        }

        context("전화번호 매칭 (요청은 하이픈 없이, DB 는 fullNumber 로 저장)") {

            test("하이픈 없는 전화번호(정규형)로 저장된 회원을 하이픈 없는 요청으로 조회하면 204를 반환한다") {
                val name = "김철수"
                val phone = "01012345678"
                val email = "match@example.com"
                insertMember(email = email, name = name, phoneNumber = phone)
                confirmSms(phone)

                resetPassword(email = email, name = name, phone = phone, newPassword = newPassword)
                    .andExpect { status { isNoContent() } }

                passwordEncryptor.matches(newPassword, storedPasswordOf(email)) shouldBe true
            }

            test("요청 전화번호에 하이픈이 포함되면 매칭 이전에 400(bean validation)을 반환한다") {
                val name = "김철수"
                val phone = "01012345678"
                val email = "hyphen@example.com"
                insertMember(email = email, name = name, phoneNumber = phone)
                confirmSms(phone)

                resetPassword(email = email, name = name, phone = "010-1234-5678", newPassword = newPassword)
                    .andExpect { status { isBadRequest() } }
            }
        }

        context("요청 형식 검증 (bean validation 최우선 → 400 INVALID_INPUT_VALUE)") {

            test("이메일 형식이 올바르지 않으면 SMS/조회 이전에 400을 반환한다") {
                val result = resetPassword(email = "not-an-email", name = "김철수", phone = "01012345678", newPassword = newPassword)
                    .andExpect { status { isBadRequest() } }
                    .andReturn()

                val body = mapper.readTree(result.response.contentAsByteArray)
                body.get("code").asText() shouldBe "INVALID_INPUT_VALUE"
            }

            test("새 비밀번호가 형식(영문+숫자 8~16)을 위반하면 SMS/조회 이전에 400을 반환한다") {
                resetPassword(email = "member@example.com", name = "김철수", phone = "01012345678", newPassword = "12345678")
                    .andExpect { status { isBadRequest() } }
            }

            test("이름 형식이 올바르지 않으면 SMS/조회 이전에 400을 반환한다") {
                resetPassword(email = "member@example.com", name = "John", phone = "01012345678", newPassword = newPassword)
                    .andExpect { status { isBadRequest() } }
            }
        }
    }
})
