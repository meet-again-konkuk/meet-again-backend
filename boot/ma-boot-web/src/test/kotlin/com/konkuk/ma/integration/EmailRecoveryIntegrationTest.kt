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

/**
 * 이메일 찾기(POST /api/auth/find-email) API E2E 통합 테스트 (REST Docs 제외, HTTP 상태/계약 검증).
 *
 * API → EmailRecoveryService → (SMS 인증 확인) + MemberQueryRepository(findOne) → MEMBERS 테이블을
 * 관통하며 아래 확정 계약을 검증한다. (사용자 확정 계약, 2026-07-12)
 *
 * 검증 순서 계약: ① bean validation(400) → ② SMS 인증(400) → ③ 회원 조회(404).
 *  - ① 요청 name/phone 형식 위반 → 400(INVALID_INPUT_VALUE), SMS/조회 이전에 차단.
 *  - ② find-email 호출 전 휴대폰 SMS 인증(confirmed)이 없으면 → 400(INVALID_INPUT_VALUE,
 *       SmsNotVerifiedException). 회원 존재/미존재와 무관하게 조회 이전에 차단(② < ③).
 *  - ③ SMS 인증 완료 후 name+phone 일치 활성 회원이 없으면 → 404(ENTITY_NOT_FOUND).
 *  - 정상: SMS 인증 완료 + 일치 활성 회원 → 200 + **전체 이메일**(마스킹 없음 — 인증으로 휴대폰 점유가
 *       증명되므로 전체 노출).
 *  - 탈퇴 유예(deleted=false, withdrawalRequestedAt≠null) 회원 → 200(복구 흐름 지원, 포함).
 *  - 익명화(deleted=true) 회원 → activeRows 필터로 제외되어 404.
 *
 * SMS 인증 완료 상태는 실제 /confirm 흐름 대신 SmsRepository.confirmVerificationCode(phone)로 셋업한다
 * (테스트 셋업이므로 허용 — 서비스 모킹 아님). test 프로파일에서 embedded Redis 가 기동되어 실제 Redis 를 사용한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmailRecoveryIntegrationTest(
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

    // 실제 /confirm 흐름을 대체하는 테스트 셋업 — 해당 phone 을 SMS 인증 완료(confirmed) 상태로 만든다.
    // confirmed 는 Redis 에 phone 키(TTL 10분)로 남으므로, "미인증" 케이스는 어떤 테스트도 confirm 하지 않는
    // 고유 phone(01044445555 / 01066667777)을 사용해 인증 상태 누수를 원천 차단한다.
    fun confirmSms(phone: String) = smsRepository.confirmVerificationCode(phone)

    // 인증 없이(permitAll) find-email 을 호출한다. (SMS 인증은 헤더가 아니라 본문 phone 의 confirmed 상태로 판정)
    fun findEmail(name: String, phone: String) =
        mockMvc.postJson("/api/auth/find-email") {
            content = mapper.writeValueAsString(mapOf("name" to name, "phone" to phone))
        }

    context("POST /api/auth/find-email") {

        context("정상 조회 (SMS 인증 완료 → 200 + 전체 이메일)") {

            test("SMS 인증 완료 + 이름·전화번호가 일치하는 회원이 있으면 200과 마스킹되지 않은 전체 이메일을 반환한다") {
                // Given - 일치하는 활성 회원 + 해당 phone SMS 인증 완료
                val name = "김철수"
                val phone = "01012345678"
                val email = "holeman@naver.com"
                insertMember(email = email, name = name, phoneNumber = phone)
                confirmSms(phone)

                // When
                val result = findEmail(name = name, phone = phone)
                    .andExpect { status { isOk() } }
                    .andReturn()

                // Then - 마스킹(hol***@...) 이 아니라 전체 이메일 그대로
                val body = mapper.readTree(result.response.contentAsByteArray)
                body.get("email").asText() shouldBe email
            }

            test("SMS 인증 완료 + 탈퇴 유예 중(deleted=false, withdrawalRequestedAt 존재)인 회원도 200과 전체 이메일을 반환한다") {
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
                val result = findEmail(name = name, phone = phone)
                    .andExpect { status { isOk() } }
                    .andReturn()

                // Then
                val body = mapper.readTree(result.response.contentAsByteArray)
                body.get("email").asText() shouldBe email
            }
        }

        context("SMS 미인증 (조회 이전에 400 INVALID_INPUT_VALUE)") {

            test("SMS 미인증이면 이름·전화번호가 일치하는 회원이 있어도 400을 반환한다") {
                // Given - 일치 회원은 존재하지만 phone 은 인증하지 않음
                val name = "박영희"
                val phone = "01044445555"
                insertMember(name = name, phoneNumber = phone)

                // When & Then - 인증 게이트에서 400 (회원이 있어도 200 이 아님)
                val result = findEmail(name = name, phone = phone)
                    .andExpect { status { isBadRequest() } }
                    .andReturn()

                // Then - 에러 응답 body 형식(INVALID_INPUT_VALUE) 확정
                val body = mapper.readTree(result.response.contentAsByteArray)
                body.get("code").asText() shouldBe "INVALID_INPUT_VALUE"
            }

            test("SMS 미인증이면 회원이 존재하지 않아도 404가 아니라 400을 반환한다") {
                // Given - 회원 없음 + 미인증(confirm 하지 않는 고유 phone). SMS 게이트(②)가 조회(③)보다 먼저 동작함을 검증
                // When & Then
                findEmail(name = "홍길동", phone = "01066667777")
                    .andExpect { status { isBadRequest() } }
            }
        }

        context("조회 실패 (SMS 인증 완료 후에도 404 ENTITY_NOT_FOUND)") {

            test("SMS 인증 완료 + 이름과 전화번호 모두 일치하는 회원이 없으면 404를 반환한다") {
                // Given - 일치하지 않는 회원만 존재 + 조회 phone 인증 완료
                insertMember(name = "김철수", phoneNumber = "01012345678")
                val name = "홍길동"
                val phone = "01099998888"
                confirmSms(phone)

                // When & Then
                val result = findEmail(name = name, phone = phone)
                    .andExpect { status { isNotFound() } }
                    .andReturn()

                // Then - 에러 응답 body 형식(ENTITY_NOT_FOUND) 확정
                val body = mapper.readTree(result.response.contentAsByteArray)
                body.get("code").asText() shouldBe "ENTITY_NOT_FOUND"
            }

            test("SMS 인증 완료 + 이름은 일치하지만 전화번호가 다르면 404를 반환한다") {
                // Given
                val name = "김철수"
                insertMember(name = name, phoneNumber = "01012345678")
                val phone = "01088887777"
                confirmSms(phone)

                // When & Then - 이름만 일치
                findEmail(name = name, phone = phone)
                    .andExpect { status { isNotFound() } }
            }

            test("SMS 인증 완료 + 전화번호는 일치하지만 이름이 다르면 404를 반환한다") {
                // Given
                val phone = "01012345678"
                insertMember(name = "김철수", phoneNumber = phone)
                confirmSms(phone)

                // When & Then - 전화번호만 일치
                findEmail(name = "이몽룡", phone = phone)
                    .andExpect { status { isNotFound() } }
            }

            test("SMS 인증 완료 + 익명화(deleted=true)된 회원의 원래 이름과 전화번호로 조회하면 404를 반환한다") {
                // Given - soft delete 된 회원 (activeRows 가 deleted=true 를 제외) + 조회 phone 인증 완료
                val name = "최익명"
                val phone = "01055556666"
                insertMember(name = name, phoneNumber = phone, deleted = true)
                confirmSms(phone)

                // When & Then - name/phone 이 그대로 일치해도 활성 필터에서 제외됨
                findEmail(name = name, phone = phone)
                    .andExpect { status { isNotFound() } }
            }
        }

        context("요청 형식 검증 (bean validation 최우선 → 400 INVALID_INPUT_VALUE)") {

            test("이름이 빈 값이면 400을 반환한다") {
                // When & Then - @NotBlank 위반, SMS/조회 이전에 차단
                val result = findEmail(name = "", phone = "01012345678")
                    .andExpect { status { isBadRequest() } }
                    .andReturn()

                // Then - 에러 응답 body 형식(INVALID_INPUT_VALUE) 확정
                val body = mapper.readTree(result.response.contentAsByteArray)
                body.get("code").asText() shouldBe "INVALID_INPUT_VALUE"
            }

            test("이름이 1글자면 400을 반환한다") {
                // When & Then - @Pattern(^[가-힣]{2,10}$) 위반
                findEmail(name = "김", phone = "01012345678")
                    .andExpect { status { isBadRequest() } }
            }

            test("이름이 11글자면 400을 반환한다") {
                // When & Then - 최대 10글자 초과
                findEmail(name = "김".repeat(11), phone = "01012345678")
                    .andExpect { status { isBadRequest() } }
            }

            test("이름이 영문이면 400을 반환한다") {
                // When & Then - 한글만 허용
                findEmail(name = "John", phone = "01012345678")
                    .andExpect { status { isBadRequest() } }
            }

            test("이름에 특수문자가 포함되면 400을 반환한다") {
                // When & Then
                findEmail(name = "김철수!", phone = "01012345678")
                    .andExpect { status { isBadRequest() } }
            }

            test("전화번호가 빈 값이면 400을 반환한다") {
                // When & Then - @NotBlank 위반
                findEmail(name = "김철수", phone = "")
                    .andExpect { status { isBadRequest() } }
            }

            test("전화번호에 하이픈이 포함되면 400을 반환한다") {
                // When & Then - @Pattern(^010\d{7,8}$)은 하이픈 불허
                findEmail(name = "김철수", phone = "010-1234-5678")
                    .andExpect { status { isBadRequest() } }
            }

            test("전화번호가 010으로 시작하지 않으면 400을 반환한다") {
                // When & Then
                findEmail(name = "김철수", phone = "01112345678")
                    .andExpect { status { isBadRequest() } }
            }

            test("전화번호 자릿수가 부족하면 400을 반환한다") {
                // When & Then - 010 + 6자리(총 9자리) → 최소 010+7자리 미달
                findEmail(name = "김철수", phone = "010123456")
                    .andExpect { status { isBadRequest() } }
            }

            test("전화번호 자릿수가 초과되면 400을 반환한다") {
                // When & Then - 010 + 9자리(총 12자리) → 최대 010+8자리 초과
                findEmail(name = "김철수", phone = "010123456789")
                    .andExpect { status { isBadRequest() } }
            }
        }
    }
})
