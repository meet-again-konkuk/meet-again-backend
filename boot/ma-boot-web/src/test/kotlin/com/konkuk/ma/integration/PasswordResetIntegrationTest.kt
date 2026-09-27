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

/**
 * 비밀번호 재설정(POST /api/auth/find-password) API E2E 통합 테스트 (REST Docs 제외, HTTP 상태/DB 상태 검증).
 *
 * API → PasswordResetService → (SMS 인증 확인) + MemberQueryRepository(findOne 3필드) + PasswordEncryptor +
 * MemberCommandRepository(updatePassword) → MEMBERS 테이블을 관통하며 아래 확정 계약을 검증한다. (2026-07-12 확정)
 *
 * 검증 순서 계약: ① bean validation(400) → ② SMS 인증(400) → ③ email+name+phone 3필드 회원 조회(404) →
 *                암호화 → updatePassword → 204 No Content.
 *  - ① 요청 형식 위반 → 400(INVALID_INPUT_VALUE), SMS/조회 이전에 차단.
 *  - ② 요청 phone 의 SMS 인증(confirmed)이 없으면 → 400(SmsNotVerifiedException). 회원 존재/미존재와 무관하게
 *       조회 이전에 차단(② < ③).
 *  - ③ SMS 인증 완료 후 email+name+phone 이 모두 일치하는 활성 회원이 없으면 → 404(ENTITY_NOT_FOUND).
 *  - 정상: SMS 인증 완료 + 3필드 일치 활성 회원 → 204, MEMBERS.PASSWORD 가 새 비밀번호(암호화)로 갱신됨.
 *  - 탈퇴 유예(deleted=false, withdrawalRequestedAt≠null) 회원 → 204 + 비밀번호 갱신(복구 흐름 지원).
 *  - 익명화(deleted=true) 회원 → activeRows 필터로 제외되어 404.
 *
 * SMS 인증 완료 상태는 실제 /confirm 흐름 대신 SmsRepository.confirmVerificationCode(phone)로 셋업한다
 * (테스트 셋업이므로 허용 — 서비스 모킹 아님). test 프로파일에서 embedded Redis 가 기동되어 실제 Redis 를 사용한다.
 * DB 상태 검증은 transaction{} 으로 MEMBERS 테이블을 직접 읽어 확인한다(포트/서비스 경유 금지).
 */
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

    // 기존 비밀번호(암호화되어 저장)와 재설정할 새 비밀번호 — 서로 다르며 둘 다 PASSWORD 패턴(영문+숫자, 8~16)을 만족한다.
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

    // MEMBERS 에 회원 1건을 직접 삽입한다. phoneNumber 는 저장 형식(fullNumber, 하이픈 없음)으로, password 는
    // 기존 비밀번호를 암호화해 넣는다(재설정 후 값이 바뀌었는지 대조하기 위함).
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

    // MEMBERS 에 저장된 암호화 비밀번호를 직접 읽는다(서비스/포트 경유 없이 DB 상태 확인).
    fun storedPasswordOf(email: String): String = transaction {
        MemberTable.selectAll().where { MemberTable.email eq email }.first()[MemberTable.password]
    }

    // 실제 /confirm 흐름을 대체하는 테스트 셋업 — 해당 phone 을 SMS 인증 완료(confirmed) 상태로 만든다.
    // "미인증" 케이스는 어떤 테스트도 confirm 하지 않는 고유 phone 을 사용해 인증 상태 누수를 원천 차단한다.
    fun confirmSms(phone: String) = smsRepository.confirmVerificationCode(phone)

    // 인증 없이(permitAll) find-password 를 호출한다. (SMS 인증은 헤더가 아니라 본문 phone 의 confirmed 상태로 판정)
    fun resetPassword(email: String, name: String, phone: String, newPassword: String) =
        mockMvc.postJson("/api/auth/find-password") {
            content = mapper.writeValueAsString(
                mapOf("email" to email, "name" to name, "phone" to phone, "newPassword" to newPassword)
            )
        }

    context("POST /api/auth/find-password") {

        context("정상 재설정 (SMS 인증 완료 + 3필드 일치 → 204 + 비밀번호 갱신)") {

            test("SMS 인증 완료 + email·name·phone 이 모두 일치하면 204를 반환하고 비밀번호를 새 값으로 갱신한다") {
                // Given - 3필드 일치 활성 회원 + 해당 phone SMS 인증 완료
                val name = "김철수"
                val phone = "01012345678"
                val email = "holeman@naver.com"
                insertMember(email = email, name = name, phoneNumber = phone)
                confirmSms(phone)

                // When
                resetPassword(email = email, name = name, phone = phone, newPassword = newPassword)
                    .andExpect { status { isNoContent() } }

                // Then - MEMBERS.PASSWORD 가 새 비밀번호로 갱신되고, 기존 비밀번호로는 더 이상 매칭되지 않는다
                val stored = storedPasswordOf(email)
                passwordEncryptor.matches(newPassword, stored) shouldBe true
                passwordEncryptor.matches(oldRawPassword, stored) shouldBe false
            }

            test("SMS 인증 완료 + 탈퇴 유예 중(deleted=false, withdrawalRequestedAt 존재)인 회원도 204와 비밀번호 갱신을 지원한다") {
                // Given - 탈퇴 신청만 한 회원(activeRows 포함) + SMS 인증 완료
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

                // When
                resetPassword(email = email, name = name, phone = phone, newPassword = newPassword)
                    .andExpect { status { isNoContent() } }

                // Then
                val stored = storedPasswordOf(email)
                passwordEncryptor.matches(newPassword, stored) shouldBe true
            }

            test("재설정된 비밀번호는 암호화되어 저장되며 평문으로 저장되지 않는다") {
                // Given
                val name = "성춘향"
                val phone = "01022223333"
                val email = "chunhyang@example.com"
                insertMember(email = email, name = name, phoneNumber = phone)
                confirmSms(phone)

                // When
                resetPassword(email = email, name = name, phone = phone, newPassword = newPassword)
                    .andExpect { status { isNoContent() } }

                // Then - 저장값은 평문 newPassword 와 달라야 하고(암호화됨), matches 로만 일치한다
                val stored = storedPasswordOf(email)
                (stored == newPassword) shouldBe false
                passwordEncryptor.matches(newPassword, stored) shouldBe true
            }
        }

        context("SMS 미인증 (조회 이전에 400 INVALID_INPUT_VALUE, 비밀번호 미변경)") {

            test("SMS 미인증이면 3필드가 일치하는 회원이 있어도 400을 반환하고 비밀번호를 바꾸지 않는다") {
                // Given - 일치 회원은 존재하지만 phone 은 인증하지 않음
                val name = "박영희"
                val phone = "01044445555"
                val email = "park@example.com"
                insertMember(email = email, name = name, phoneNumber = phone)

                // When - 인증 게이트에서 400 (회원이 있어도 204 가 아님)
                val result = resetPassword(email = email, name = name, phone = phone, newPassword = newPassword)
                    .andExpect { status { isBadRequest() } }
                    .andReturn()

                // Then - 에러 코드(INVALID_INPUT_VALUE) 확정 + 비밀번호는 기존값 유지
                val body = mapper.readTree(result.response.contentAsByteArray)
                body.get("code").asText() shouldBe "INVALID_INPUT_VALUE"
                passwordEncryptor.matches(oldRawPassword, storedPasswordOf(email)) shouldBe true
            }

            test("SMS 미인증이면 회원이 존재하지 않아도 404가 아니라 400을 반환한다") {
                // Given - 회원 없음 + 미인증(confirm 하지 않는 고유 phone). SMS 게이트(②)가 조회(③)보다 먼저 동작함을 검증
                // When & Then
                resetPassword(email = "nobody@example.com", name = "홍길동", phone = "01066667777", newPassword = newPassword)
                    .andExpect { status { isBadRequest() } }
            }
        }

        context("조회 실패 (SMS 인증 완료 후에도 3필드 불일치면 404 ENTITY_NOT_FOUND, 비밀번호 미변경)") {

            test("SMS 인증 완료 + email 만 다르면 404를 반환한다") {
                // Given - name/phone 은 일치하지만 email 이 다른 회원만 존재
                val name = "김철수"
                val phone = "01012345678"
                insertMember(email = "correct@example.com", name = name, phoneNumber = phone)
                confirmSms(phone)

                // When & Then
                val result = resetPassword(email = "wrong@example.com", name = name, phone = phone, newPassword = newPassword)
                    .andExpect { status { isNotFound() } }
                    .andReturn()

                // Then - 에러 코드(ENTITY_NOT_FOUND) 확정
                val body = mapper.readTree(result.response.contentAsByteArray)
                body.get("code").asText() shouldBe "ENTITY_NOT_FOUND"
            }

            test("SMS 인증 완료 + name 만 다르면 404를 반환한다") {
                // Given - email/phone 은 일치하지만 name 이 다름
                val email = "member@example.com"
                val phone = "01012345678"
                insertMember(email = email, name = "김철수", phoneNumber = phone)
                confirmSms(phone)

                // When & Then
                resetPassword(email = email, name = "이몽룡", phone = phone, newPassword = newPassword)
                    .andExpect { status { isNotFound() } }
            }

            test("SMS 인증 완료 + phone 만 다르면 404를 반환한다") {
                // Given - email/name 은 일치하지만 phone 이 다름
                val email = "member@example.com"
                val name = "김철수"
                insertMember(email = email, name = name, phoneNumber = "01012345678")
                val phone = "01088887777"
                confirmSms(phone)

                // When & Then
                resetPassword(email = email, name = name, phone = phone, newPassword = newPassword)
                    .andExpect { status { isNotFound() } }
            }

            test("SMS 인증 완료 + 익명화(deleted=true)된 회원의 원래 3필드로 조회하면 404를 반환한다") {
                // Given - soft delete 된 회원 (activeRows 가 deleted=true 를 제외) + 조회 phone 인증 완료
                val name = "최익명"
                val phone = "01055556666"
                val email = "anon@example.com"
                insertMember(email = email, name = name, phoneNumber = phone, deleted = true)
                confirmSms(phone)

                // When & Then - 3필드가 그대로 일치해도 활성 필터에서 제외됨
                resetPassword(email = email, name = name, phone = phone, newPassword = newPassword)
                    .andExpect { status { isNotFound() } }
            }
        }

        context("전화번호 매칭 (요청은 하이픈 없이, DB 는 fullNumber 로 저장)") {

            test("하이픈 없는 전화번호(정규형)로 저장된 회원을 하이픈 없는 요청으로 조회하면 204를 반환한다") {
                // Given - DB 저장 형식(fullNumber)과 요청 형식이 모두 하이픈 없음 → 정규화 매칭 성립
                val name = "김철수"
                val phone = "01012345678"
                val email = "match@example.com"
                insertMember(email = email, name = name, phoneNumber = phone)
                confirmSms(phone)

                // When & Then
                resetPassword(email = email, name = name, phone = phone, newPassword = newPassword)
                    .andExpect { status { isNoContent() } }

                passwordEncryptor.matches(newPassword, storedPasswordOf(email)) shouldBe true
            }

            test("요청 전화번호에 하이픈이 포함되면 매칭 이전에 400(bean validation)을 반환한다") {
                // Given - 하이픈 포함 회원이라도 요청 phone 형식(^010\\d{7,8}$) 위반이 먼저 걸린다
                val name = "김철수"
                val phone = "01012345678"
                val email = "hyphen@example.com"
                insertMember(email = email, name = name, phoneNumber = phone)
                confirmSms(phone)

                // When & Then - 서비스/조회 도달 이전 bean validation 400
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
                // "12345678" = 숫자만 → 패턴 위반
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
