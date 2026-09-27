package com.konkuk.ma.domain.common.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * 부분 수정(PATCH)의 3상태(tri-state)를 표현하는 값 객체 계약 테스트.
 *
 * - 래퍼 자체가 null      → "변경 없음" (요청 본문에 필드가 없었다)
 * - Changed(null)        → "비우기"   (요청 본문에 명시적 null 이 왔다)
 * - Changed(value)       → "변경"     (요청 본문에 값이 왔다)
 *
 * 이 세 상태가 서로 구분되지 않으면 부분 수정 계약 자체가 성립하지 않으므로,
 * "구분된다"는 사실을 동등성 수준까지 못박는다.
 */
class ChangedTest : FunSpec({

    context("3상태 표현") {

        test("Changed(null)은 비우기 지시이며 값으로 null을 담는다") {
            // Given & When
            val clear: Changed<String>? = Changed(null)

            // Then
            clear shouldNotBe null
            clear!!.value.shouldBeNull()
        }

        test("Changed(값)은 변경 지시이며 전달한 값을 그대로 담는다") {
            // Given & When
            val change = Changed("건대부고")

            // Then
            change.value shouldBe "건대부고"
        }

        test("래퍼가 null이면 변경 없음이며 Changed(null)과 구분된다") {
            // Given
            val noChange: Changed<String>? = null
            val clear: Changed<String>? = Changed(null)

            // Then
            noChange.shouldBeNull()
            noChange shouldNotBe clear
        }
    }

    context("동등성") {

        test("같은 값을 담은 Changed는 서로 같다") {
            Changed("건대부고") shouldBe Changed("건대부고")
        }

        test("다른 값을 담은 Changed는 서로 다르다") {
            Changed("건대부고") shouldNotBe Changed("건대사대부고")
        }

        test("Changed(null)끼리는 서로 같다") {
            Changed<String>(null) shouldBe Changed<String>(null)
        }

        test("Changed(null)과 값을 담은 Changed는 서로 다르다") {
            Changed<String>(null) shouldNotBe Changed("건대부고")
        }

        test("같은 값을 담은 Changed는 해시코드도 같다") {
            Changed("건국대").hashCode() shouldBe Changed("건국대").hashCode()
        }
    }

    context("경계값") {

        test("빈 문자열도 하나의 값으로 취급한다") {
            // Given & When
            val change = Changed("")

            // Then — 빈 문자열은 비우기(Changed(null))가 아니다
            change.value shouldBe ""
            change shouldNotBe Changed<String>(null)
        }

        test("공백 문자열도 하나의 값으로 취급한다") {
            Changed(" ").value shouldBe " "
        }

        test("문자열이 아닌 타입도 담을 수 있다") {
            Changed(42L).value shouldBe 42L
        }
    }
})
