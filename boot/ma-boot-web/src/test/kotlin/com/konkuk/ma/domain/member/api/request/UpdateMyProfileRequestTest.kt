package com.konkuk.ma.domain.member.api.request

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.domain.common.domain.Changed
import com.konkuk.ma.domain.member.domain.Region
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.util.Optional

@SpringBootTest
@ActiveProfiles("test")
class UpdateMyProfileRequestTest(
    private val mapper: ObjectMapper
) : FunSpec({

    fun deserialize(json: String): UpdateMyProfileRequest =
        mapper.readValue(json, UpdateMyProfileRequest::class.java)

    context("D3-a 역직렬화 — highSchool tri-state") {

        test("필드를 생략하면 null 로 역직렬화된다 (변경 없음)") {
            val request = deserialize("""{}""")

            request.highSchool.shouldBeNull()
        }

        test("명시적 null 을 보내면 Optional.empty() 로 역직렬화된다 (비우기)") {
            val request = deserialize("""{"highSchool":null}""")

            request.highSchool shouldBe Optional.empty<String>()
        }

        test("값을 보내면 Optional.of(값) 으로 역직렬화된다 (변경)") {
            val request = deserialize("""{"highSchool":"건대부고"}""")

            request.highSchool shouldBe Optional.of("건대부고")
        }

        test("빈 문자열을 보내면 Optional.of(\"\") 로 역직렬화된다 (비우기가 아니다)") {
            val request = deserialize("""{"highSchool":""}""")

            request.highSchool shouldBe Optional.of("")
        }
    }

    context("D3-a 역직렬화 — university tri-state") {

        test("필드를 생략하면 null 로 역직렬화된다 (변경 없음)") {
            val request = deserialize("""{}""")

            request.university.shouldBeNull()
        }

        test("명시적 null 을 보내면 Optional.empty() 로 역직렬화된다 (비우기)") {
            val request = deserialize("""{"university":null}""")

            request.university shouldBe Optional.empty<String>()
        }

        test("값을 보내면 Optional.of(값) 으로 역직렬화된다 (변경)") {
            val request = deserialize("""{"university":"건국대"}""")

            request.university shouldBe Optional.of("건국대")
        }
    }

    context("D3-a 역직렬화 — 두 필드가 서로 독립적으로 해석된다") {

        test("한쪽은 비우기, 다른 쪽은 변경으로 동시에 해석한다") {
            val request = deserialize("""{"highSchool":null,"university":"건국대"}""")

            request.highSchool shouldBe Optional.empty<String>()
            request.university shouldBe Optional.of("건국대")
        }

        test("한쪽만 보내면 다른 쪽은 생략(null)으로 남는다") {
            val request = deserialize("""{"university":"건국대"}""")

            request.highSchool.shouldBeNull()
            request.university shouldBe Optional.of("건국대")
        }
    }

    context("역직렬화 — nickname · region") {

        test("nickname 을 생략하면 null 이다") {
            deserialize("""{}""").nickname.shouldBeNull()
        }

        test("nickname 에 값을 보내면 그 값이 담긴다") {
            deserialize("""{"nickname":"새닉네임"}""").nickname shouldBe "새닉네임"
        }

        test("region 을 생략하면 null 이다") {
            deserialize("""{}""").region.shouldBeNull()
        }

        test("region 에 enum 코드를 보내면 해당 enum 이 담긴다") {
            deserialize("""{"region":"BUSAN"}""").region shouldBe Region.BUSAN
        }
    }

    context("toProfileChanges") {

        test("아무 필드도 없는 요청은 네 지시가 모두 없는 변경으로 바뀐다") {
            val request = deserialize("""{}""")

            val changes = request.toProfileChanges()

            changes.nickname.shouldBeNull()
            changes.region.shouldBeNull()
            changes.highSchool.shouldBeNull()
            changes.university.shouldBeNull()
        }

        test("nickname 을 그대로 전달한다") {
            val request = deserialize("""{"nickname":"새닉네임"}""")

            val changes = request.toProfileChanges()

            changes.nickname shouldBe request.nickname
        }

        test("region 을 그대로 전달한다") {
            val request = deserialize("""{"region":"JEJU_DO"}""")

            val changes = request.toProfileChanges()

            changes.region shouldBe Region.JEJU_DO
        }

        test("highSchool 값은 Changed(값) 으로 바뀐다") {
            val request = deserialize("""{"highSchool":"건대부고"}""")

            val changes = request.toProfileChanges()

            changes.highSchool shouldBe Changed("건대부고")
        }

        test("highSchool 명시적 null 은 Changed(null) 비우기 지시로 바뀐다") {
            val request = deserialize("""{"highSchool":null}""")

            val changes = request.toProfileChanges()

            changes.highSchool shouldBe Changed<String>(null)
        }

        test("highSchool 생략은 지시 없음(null) 그대로 남는다") {
            val request = deserialize("""{}""")

            val changes = request.toProfileChanges()

            changes.highSchool.shouldBeNull()
        }

        test("university 값은 Changed(값) 으로 바뀐다") {
            val request = deserialize("""{"university":"건국대"}""")

            val changes = request.toProfileChanges()

            changes.university shouldBe Changed("건국대")
        }

        test("university 명시적 null 은 Changed(null) 비우기 지시로 바뀐다") {
            val request = deserialize("""{"university":null}""")

            val changes = request.toProfileChanges()

            changes.university shouldBe Changed<String>(null)
        }

        test("university 생략은 지시 없음(null) 그대로 남는다") {
            val request = deserialize("""{}""")

            val changes = request.toProfileChanges()

            changes.university.shouldBeNull()
        }

        test("빈 문자열은 비우기가 아니라 Changed(\"\") 값 변경 지시다") {
            val request = deserialize("""{"highSchool":""}""")

            val changes = request.toProfileChanges()

            changes.highSchool shouldBe Changed("")
        }

        test("네 필드를 모두 담은 요청은 네 지시를 모두 전달한다") {
            val request = deserialize(
                """{"nickname":"새닉네임","region":"BUSAN","highSchool":"건대부고","university":"건국대"}"""
            )

            val changes = request.toProfileChanges()

            changes.nickname shouldBe "새닉네임"
            changes.region shouldBe Region.BUSAN
            changes.highSchool shouldBe Changed("건대부고")
            changes.university shouldBe Changed("건국대")
        }

        test("비우기와 변경이 섞인 요청도 필드마다 그대로 전달한다") {
            val request = deserialize("""{"highSchool":null,"university":"건국대"}""")

            val changes = request.toProfileChanges()

            changes.highSchool shouldBe Changed<String>(null)
            changes.university shouldBe Changed("건국대")
        }
    }
})
