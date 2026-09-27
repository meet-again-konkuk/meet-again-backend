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

/**
 * 부분 수정 요청 DTO 계약 테스트.
 *
 * ## D3-a — Optional<String>? tri-state 역직렬화 실증 (plan §13 리스크 2)
 *
 * 이 프로젝트의 Jackson 설정에서 실제로 아래 3상태가 구분되는지가 설계 전제다.
 * 여기가 깨지면 "생략 vs 명시적 null" 구분이 성립하지 않아 PATCH 계약 전체가 무너진다.
 *
 * | 요청 JSON                   | 기대 역직렬화 결과      | 의미      |
 * |----------------------------|----------------------|-----------|
 * | 필드 생략                    | null                 | 변경 없음  |
 * | "highSchool": null         | Optional.empty()     | 비우기     |
 * | "highSchool": "건대부고"     | Optional.of("건대부고") | 변경      |
 *
 * 그래서 테스트용 ObjectMapper 를 새로 만들지 않고 **Spring 이 만든 ObjectMapper 빈**을 주입받는다.
 * (Jdk8Module 자동 등록 여부까지 포함해 실제 런타임 설정을 검증해야 의미가 있다)
 *
 * `nickname` · `region` 의 "명시적 null → 400" 계약은 HTTP 상태 코드로 관측되는 계약이므로
 * `MemberProfileIntegrationTest` 가 소유한다 (여기서 예외 타입을 못박으면 구현 방식을 과하게 묶는다).
 */
@SpringBootTest
@ActiveProfiles("test")
class UpdateMyProfileRequestTest(
    private val mapper: ObjectMapper
) : FunSpec({

    fun deserialize(json: String): UpdateMyProfileRequest =
        mapper.readValue(json, UpdateMyProfileRequest::class.java)

    context("D3-a 역직렬화 — highSchool tri-state") {

        test("필드를 생략하면 null 로 역직렬화된다 (변경 없음)") {
            // When
            val request = deserialize("""{}""")

            // Then
            request.highSchool.shouldBeNull()
        }

        test("명시적 null 을 보내면 Optional.empty() 로 역직렬화된다 (비우기)") {
            // When
            val request = deserialize("""{"highSchool":null}""")

            // Then
            request.highSchool shouldBe Optional.empty<String>()
        }

        test("값을 보내면 Optional.of(값) 으로 역직렬화된다 (변경)") {
            // When
            val request = deserialize("""{"highSchool":"건대부고"}""")

            // Then
            request.highSchool shouldBe Optional.of("건대부고")
        }

        test("빈 문자열을 보내면 Optional.of(\"\") 로 역직렬화된다 (비우기가 아니다)") {
            // When
            val request = deserialize("""{"highSchool":""}""")

            // Then
            request.highSchool shouldBe Optional.of("")
        }
    }

    context("D3-a 역직렬화 — university tri-state") {

        test("필드를 생략하면 null 로 역직렬화된다 (변경 없음)") {
            // When
            val request = deserialize("""{}""")

            // Then
            request.university.shouldBeNull()
        }

        test("명시적 null 을 보내면 Optional.empty() 로 역직렬화된다 (비우기)") {
            // When
            val request = deserialize("""{"university":null}""")

            // Then
            request.university shouldBe Optional.empty<String>()
        }

        test("값을 보내면 Optional.of(값) 으로 역직렬화된다 (변경)") {
            // When
            val request = deserialize("""{"university":"건국대"}""")

            // Then
            request.university shouldBe Optional.of("건국대")
        }
    }

    context("D3-a 역직렬화 — 두 필드가 서로 독립적으로 해석된다") {

        test("한쪽은 비우기, 다른 쪽은 변경으로 동시에 해석한다") {
            // When
            val request = deserialize("""{"highSchool":null,"university":"건국대"}""")

            // Then
            request.highSchool shouldBe Optional.empty<String>()
            request.university shouldBe Optional.of("건국대")
        }

        test("한쪽만 보내면 다른 쪽은 생략(null)으로 남는다") {
            // When
            val request = deserialize("""{"university":"건국대"}""")

            // Then
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
            // Given
            val request = deserialize("""{}""")

            // When
            val changes = request.toProfileChanges()

            // Then
            changes.nickname.shouldBeNull()
            changes.region.shouldBeNull()
            changes.highSchool.shouldBeNull()
            changes.university.shouldBeNull()
        }

        test("nickname 을 그대로 전달한다") {
            // Given
            val request = deserialize("""{"nickname":"새닉네임"}""")

            // When
            val changes = request.toProfileChanges()

            // Then
            changes.nickname shouldBe request.nickname
        }

        test("region 을 그대로 전달한다") {
            // Given
            val request = deserialize("""{"region":"JEJU_DO"}""")

            // When
            val changes = request.toProfileChanges()

            // Then
            changes.region shouldBe Region.JEJU_DO
        }

        test("highSchool 값은 Changed(값) 으로 바뀐다") {
            // Given
            val request = deserialize("""{"highSchool":"건대부고"}""")

            // When
            val changes = request.toProfileChanges()

            // Then
            changes.highSchool shouldBe Changed("건대부고")
        }

        test("highSchool 명시적 null 은 Changed(null) 비우기 지시로 바뀐다") {
            // Given
            val request = deserialize("""{"highSchool":null}""")

            // When
            val changes = request.toProfileChanges()

            // Then
            changes.highSchool shouldBe Changed<String>(null)
        }

        test("highSchool 생략은 지시 없음(null) 그대로 남는다") {
            // Given
            val request = deserialize("""{}""")

            // When
            val changes = request.toProfileChanges()

            // Then
            changes.highSchool.shouldBeNull()
        }

        test("university 값은 Changed(값) 으로 바뀐다") {
            // Given
            val request = deserialize("""{"university":"건국대"}""")

            // When
            val changes = request.toProfileChanges()

            // Then
            changes.university shouldBe Changed("건국대")
        }

        test("university 명시적 null 은 Changed(null) 비우기 지시로 바뀐다") {
            // Given
            val request = deserialize("""{"university":null}""")

            // When
            val changes = request.toProfileChanges()

            // Then
            changes.university shouldBe Changed<String>(null)
        }

        test("university 생략은 지시 없음(null) 그대로 남는다") {
            // Given
            val request = deserialize("""{}""")

            // When
            val changes = request.toProfileChanges()

            // Then
            changes.university.shouldBeNull()
        }

        test("빈 문자열은 비우기가 아니라 Changed(\"\") 값 변경 지시다") {
            // Given
            val request = deserialize("""{"highSchool":""}""")

            // When
            val changes = request.toProfileChanges()

            // Then
            changes.highSchool shouldBe Changed("")
        }

        test("네 필드를 모두 담은 요청은 네 지시를 모두 전달한다") {
            // Given
            val request = deserialize(
                """{"nickname":"새닉네임","region":"BUSAN","highSchool":"건대부고","university":"건국대"}"""
            )

            // When
            val changes = request.toProfileChanges()

            // Then
            changes.nickname shouldBe "새닉네임"
            changes.region shouldBe Region.BUSAN
            changes.highSchool shouldBe Changed("건대부고")
            changes.university shouldBe Changed("건국대")
        }

        test("비우기와 변경이 섞인 요청도 필드마다 그대로 전달한다") {
            // Given
            val request = deserialize("""{"highSchool":null,"university":"건국대"}""")

            // When
            val changes = request.toProfileChanges()

            // Then
            changes.highSchool shouldBe Changed<String>(null)
            changes.university shouldBe Changed("건국대")
        }
    }
})
