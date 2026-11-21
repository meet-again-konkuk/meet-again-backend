package com.konkuk.ma.domain.common.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ChangedTest : FunSpec({

    context("3상태 표현") {

        test("Changed(null)은 비우기 지시이며 값으로 null을 담는다") {
            val clear: Changed<String>? = Changed(null)

            clear shouldNotBe null
            clear!!.value.shouldBeNull()
        }

        test("Changed(값)은 변경 지시이며 전달한 값을 그대로 담는다") {
            val change = Changed("건대부고")

            change.value shouldBe "건대부고"
        }

        test("래퍼가 null이면 변경 없음이며 Changed(null)과 구분된다") {
            val noChange: Changed<String>? = null
            val clear: Changed<String>? = Changed(null)

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
            val change = Changed("")

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
