package com.konkuk.ma.domain.member.domain

import com.konkuk.ma.domain.common.domain.Changed
import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.matching.fixture.MemberFixture
import com.konkuk.ma.domain.member.domain.policy.WithdrawnSentinel
import com.konkuk.ma.domain.member.exception.AlreadyWithdrawalRequestedException
import com.konkuk.ma.domain.member.exception.NotWithdrawalRequestedException
import com.konkuk.ma.domain.member.exception.WithdrawalPendingLoginException
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.LocalDateTime

class MemberTest : FunSpec({

    context("changeProfile") {

        test("닉네임 변경 지시가 있으면 닉네임을 바꾼다") {
            // Given
            val member = MemberFixture.create(nickname = "기존닉네임")
            val changes = ProfileChanges(nickname = "새닉네임", region = null, highSchool = null, university = null)

            // When
            member.changeProfile(changes)

            // Then
            member.nickname shouldBe changes.nickname
        }

        test("닉네임 변경 지시가 없으면(null) 기존 닉네임을 유지한다") {
            // Given
            val member = MemberFixture.create(nickname = "기존닉네임")
            val original = member.nickname
            val changes = ProfileChanges(nickname = null, region = null, highSchool = null, university = null)

            // When
            member.changeProfile(changes)

            // Then
            member.nickname shouldBe original
        }

        test("지역 변경 지시가 있으면 지역을 바꾼다") {
            // Given
            val member = MemberFixture.create(region = Region.SEOUL)
            val changes = ProfileChanges(nickname = null, region = Region.BUSAN, highSchool = null, university = null)

            // When
            member.changeProfile(changes)

            // Then
            member.region shouldBe changes.region
        }

        test("지역 변경 지시가 없으면(null) 기존 지역을 유지한다") {
            // Given
            val member = MemberFixture.create(region = Region.SEOUL)
            val original = member.region
            val changes = ProfileChanges(nickname = null, region = null, highSchool = null, university = null)

            // When
            member.changeProfile(changes)

            // Then
            member.region shouldBe original
        }

        test("고등학교에 값 변경 지시가 오면 값을 바꾼다") {
            // Given
            val member = MemberFixture.create(highSchool = "기존고")
            val changes = ProfileChanges(
                nickname = null,
                region = null,
                highSchool = Changed("건대부고"),
                university = null,
            )

            // When
            member.changeProfile(changes)

            // Then
            member.highSchool shouldBe changes.highSchool!!.value
        }

        test("고등학교에 비우기 지시(Changed(null))가 오면 null로 비운다") {
            // Given
            val member = MemberFixture.create(highSchool = "기존고")
            val changes = ProfileChanges(
                nickname = null,
                region = null,
                highSchool = Changed(null),
                university = null,
            )

            // When
            member.changeProfile(changes)

            // Then
            member.highSchool shouldBe null
        }

        test("고등학교 변경 지시가 없으면(래퍼 자체가 null) 기존 값을 유지한다") {
            // Given — 생략과 비우기가 구분되는지가 부분 수정 계약의 핵심이다
            val member = MemberFixture.create(highSchool = "기존고")
            val original = member.highSchool
            val changes = ProfileChanges(nickname = null, region = null, highSchool = null, university = null)

            // When
            member.changeProfile(changes)

            // Then
            member.highSchool shouldBe original
        }

        test("대학교에 값 변경 지시가 오면 값을 바꾼다") {
            // Given
            val member = MemberFixture.create(university = "기존대")
            val changes = ProfileChanges(
                nickname = null,
                region = null,
                highSchool = null,
                university = Changed("건국대"),
            )

            // When
            member.changeProfile(changes)

            // Then
            member.university shouldBe changes.university!!.value
        }

        test("대학교에 비우기 지시(Changed(null))가 오면 null로 비운다") {
            // Given
            val member = MemberFixture.create(university = "기존대")
            val changes = ProfileChanges(
                nickname = null,
                region = null,
                highSchool = null,
                university = Changed(null),
            )

            // When
            member.changeProfile(changes)

            // Then
            member.university shouldBe null
        }

        test("대학교 변경 지시가 없으면(래퍼 자체가 null) 기존 값을 유지한다") {
            // Given
            val member = MemberFixture.create(university = "기존대")
            val original = member.university
            val changes = ProfileChanges(nickname = null, region = null, highSchool = null, university = null)

            // When
            member.changeProfile(changes)

            // Then
            member.university shouldBe original
        }

        test("네 필드를 한 번에 지시하면 모두 반영한다") {
            // Given
            val member = MemberFixture.create(
                nickname = "기존닉네임",
                region = Region.SEOUL,
                highSchool = "기존고",
                university = "기존대",
            )
            val changes = ProfileChanges(
                nickname = "새닉네임",
                region = Region.JEJU_DO,
                highSchool = Changed("건대부고"),
                university = Changed("건국대"),
            )

            // When
            member.changeProfile(changes)

            // Then
            member.nickname shouldBe changes.nickname
            member.region shouldBe changes.region
            member.highSchool shouldBe changes.highSchool!!.value
            member.university shouldBe changes.university!!.value
        }

        test("비우기와 변경이 섞여도 필드마다 독립적으로 적용한다") {
            // Given
            val member = MemberFixture.create(highSchool = "기존고", university = "기존대")
            val changes = ProfileChanges(
                nickname = null,
                region = null,
                highSchool = Changed(null),
                university = Changed("건국대"),
            )

            // When
            member.changeProfile(changes)

            // Then
            member.highSchool shouldBe null
            member.university shouldBe changes.university!!.value
        }

        test("아무 지시도 없는 변경이면 네 필드가 모두 그대로다") {
            // Given
            val member = MemberFixture.create(
                nickname = "기존닉네임",
                region = Region.SEOUL,
                highSchool = "기존고",
                university = "기존대",
            )
            val changes = ProfileChanges(nickname = null, region = null, highSchool = null, university = null)

            // When
            member.changeProfile(changes)

            // Then
            member.nickname shouldBe "기존닉네임"
            member.region shouldBe Region.SEOUL
            member.highSchool shouldBe "기존고"
            member.university shouldBe "기존대"
        }

        test("이미 null인 고등학교에 비우기 지시가 와도 null을 유지한다") {
            // Given
            val member = MemberFixture.create(highSchool = null)
            val changes = ProfileChanges(
                nickname = null,
                region = null,
                highSchool = Changed(null),
                university = null,
            )

            // When
            member.changeProfile(changes)

            // Then
            member.highSchool shouldBe null
        }

        test("빈 문자열 변경 지시는 비우기가 아니라 빈 문자열 저장이다") {
            // Given
            val member = MemberFixture.create(highSchool = "기존고")
            val changes = ProfileChanges(
                nickname = null,
                region = null,
                highSchool = Changed(""),
                university = null,
            )

            // When
            member.changeProfile(changes)

            // Then
            member.highSchool shouldBe ""
        }

        test("같은 값으로 변경 지시가 와도 그 값을 유지한다") {
            // Given
            val member = MemberFixture.create(nickname = "기존닉네임")
            val changes = ProfileChanges(
                nickname = member.nickname,
                region = null,
                highSchool = null,
                university = null,
            )

            // When
            member.changeProfile(changes)

            // Then
            member.nickname shouldBe "기존닉네임"
        }

        test("수정 대상이 아닌 필드(이메일·이름·성별·전화번호·생년월일)는 건드리지 않는다") {
            // Given — name·gender 는 매칭 하드 필터라 수정 대상에서 제외됐다(D1)
            val member = MemberFixture.create(
                email = "profile@example.com",
                name = "홍길동",
                gender = Gender.FEMALE,
                phoneNumber = "01012345678",
            )
            val changes = ProfileChanges(
                nickname = "새닉네임",
                region = Region.BUSAN,
                highSchool = Changed("건대부고"),
                university = Changed("건국대"),
            )

            // When
            member.changeProfile(changes)

            // Then
            member.email shouldBe Email("profile@example.com")
            member.name shouldBe "홍길동"
            member.gender shouldBe Gender.FEMALE
            member.phoneNumber.fullNumber shouldBe "01012345678"
            member.birthDate shouldBe LocalDate.of(1999, 12, 31)
        }

        test("탈퇴 신청 시각은 프로필 수정의 영향을 받지 않는다") {
            // Given
            val member = MemberFixture.create()
            val requestedAt = LocalDateTime.of(2026, 8, 20, 10, 0)
            member.requestWithdrawal(requestedAt)
            val changes = ProfileChanges(nickname = "새닉네임", region = null, highSchool = null, university = null)

            // When
            member.changeProfile(changes)

            // Then
            member.withdrawalRequestedAt shouldBe requestedAt
        }
    }

    context("requestWithdrawal") {

        test("처음 신청하면 withdrawalRequestedAt을 설정하고 시각을 반환한다") {
            val member = MemberFixture.create()
            val now = LocalDateTime.of(2026, 5, 1, 10, 0)

            val requestedAt = member.requestWithdrawal(now)

            requestedAt shouldBe now
            member.withdrawalRequestedAt shouldBe now
        }

        test("이미 신청 상태면 AlreadyWithdrawalRequestedException을 던진다") {
            val member = MemberFixture.create()
            member.requestWithdrawal(LocalDateTime.now())

            shouldThrow<AlreadyWithdrawalRequestedException> {
                member.requestWithdrawal(LocalDateTime.now())
            }
        }
    }

    context("cancelWithdrawal") {

        test("신청 상태에서 호출하면 withdrawalRequestedAt을 null로 만든다") {
            val member = MemberFixture.create()
            member.requestWithdrawal(LocalDateTime.now())

            member.cancelWithdrawal()

            member.withdrawalRequestedAt shouldBe null
        }

        test("신청 상태가 아니면 NotWithdrawalRequestedException을 던진다") {
            val member = MemberFixture.create()

            shouldThrow<NotWithdrawalRequestedException> {
                member.cancelWithdrawal()
            }
        }
    }

    context("verifyLogin") {

        test("활성 회원이면 통과한다") {
            val member = MemberFixture.create()

            shouldNotThrowAny {
                member.verifyLogin()
            }
        }

        test("탈퇴 신청 상태면 WithdrawalPendingLoginException을 던진다") {
            val member = MemberFixture.create()
            member.requestWithdrawal(LocalDateTime.now())

            shouldThrow<WithdrawalPendingLoginException> {
                member.verifyLogin()
            }
        }
    }

    context("anonymize") {

        test("개인정보를 sentinel 값으로 치환하고 탈퇴 신청 시각은 보존한다") {
            val requestedAt = LocalDateTime.of(2026, 5, 1, 10, 0)
            val member = MemberFixture.create(
                id = 42L,
                gender = Gender.FEMALE,
                highSchool = "건국고",
                university = "건국대",
            )
            member.requestWithdrawal(requestedAt)

            val anonymized = member.anonymize()

            anonymized.id shouldBe 42L
            anonymized.email shouldBe Email.withdrawn(42L)
            anonymized.nickname shouldBe WithdrawnSentinel.nickname(42L)
            anonymized.name shouldBe WithdrawnSentinel.NAME
            anonymized.password shouldBe WithdrawnSentinel.PASSWORD
            anonymized.highSchool shouldBe null
            anonymized.university shouldBe null
            anonymized.gender shouldBe Gender.FEMALE
            anonymized.withdrawalRequestedAt shouldBe requestedAt
        }
    }
})
