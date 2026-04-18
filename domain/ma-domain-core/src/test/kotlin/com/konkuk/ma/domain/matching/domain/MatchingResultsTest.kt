package com.konkuk.ma.domain.matching.domain

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.matching.fixture.MatchingResultFixture
import com.konkuk.ma.domain.matching.fixture.MemberFixture
import com.konkuk.ma.domain.member.domain.Members
import com.konkuk.ma.domain.member.domain.photo.MemberPhotos
import com.konkuk.ma.domain.member.fixture.MemberPhotoFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class MatchingResultsTest : FunSpec({

    context("생성자 isVisible 필터") {

        test("공개 시간이 지난 결과만 포함된다") {
            val visible = MatchingResultFixture.create(
                targetInfoId = 1L,
                showingExpiryDate = LocalDateTime.now().plusDays(29),
            )
            val notVisible = MatchingResultFixture.create(
                targetInfoId = 2L,
                showingExpiryDate = LocalDateTime.now().plusDays(31),
            )

            val results = MatchingResults(listOf(visible, notVisible))

            results.data shouldHaveSize 1
            results.data[0].targetInfoId shouldBe 1L
        }

        test("모두 공개 전이면 빈 목록이 된다") {
            val notVisible = MatchingResultFixture.create(
                showingExpiryDate = LocalDateTime.now().plusDays(31),
            )

            val results = MatchingResults(listOf(notVisible))

            results.data shouldHaveSize 0
        }
    }

    context("extractTargetEmails") {

        test("중복 없이 타겟 이메일을 추출한다") {
            val results = MatchingResults(
                listOf(
                    MatchingResultFixture.create(targetInfoId = 1L, targetEmail = "a@a.com"),
                    MatchingResultFixture.create(targetInfoId = 2L, targetEmail = "b@b.com"),
                    MatchingResultFixture.create(targetInfoId = 3L, targetEmail = "a@a.com")
                )
            )

            val emails = results.extractTargetEmails()

            emails shouldHaveSize 2
            emails shouldContainExactlyInAnyOrder listOf(Email("a@a.com"), Email("b@b.com"))
        }

        test("빈 리스트이면 빈 Set을 반환한다") {
            val results = MatchingResults(emptyList())

            results.extractTargetEmails() shouldHaveSize 0
        }

        test("모든 이메일이 다르면 전부 포함된다") {
            val results = MatchingResults(
                listOf(
                    MatchingResultFixture.create(targetInfoId = 1L, targetEmail = "a@a.com"),
                    MatchingResultFixture.create(targetInfoId = 2L, targetEmail = "b@b.com"),
                    MatchingResultFixture.create(targetInfoId = 3L, targetEmail = "c@c.com")
                )
            )

            results.extractTargetEmails() shouldHaveSize 3
        }
    }

    context("combineWithProfiles") {

        test("매칭결과와 회원정보와 사진정보를 조합하여 프로필을 생성한다") {
            val matchingResult = MatchingResultFixture.create(targetEmail = "target@example.com")
            val matchingResults = MatchingResults(listOf(matchingResult))

            val member = MemberFixture.create(email = matchingResult.targetEmail.value, name = "홍길동", nickname = "닉네임")
            val photo = MemberPhotoFixture.create(
                memberEmail = matchingResult.targetEmail.value,
                thumbnailPath = "thumb/photo.jpg"
            )

            val result = matchingResults.combineWithProfiles(
                Members(listOf(member)), MemberPhotos(listOf(photo))
            )

            result.data shouldHaveSize 1
            result.data[0].targetName shouldBe member.name
            result.data[0].targetNickname shouldBe member.nickname
            result.data[0].profileImageUrl shouldBe photo.thumbnailPath
        }

        test("탈퇴한 회원도 결과에 포함되며 isWithdrawn이 true이다") {
            val activeResult = MatchingResultFixture.create(targetEmail = "active@example.com")
            val withdrawnResult = MatchingResultFixture.create(targetEmail = "withdrawn@example.com")
            val matchingResults = MatchingResults(listOf(activeResult, withdrawnResult))

            val activeMember = MemberFixture.create(email = activeResult.targetEmail.value)

            val result = matchingResults.combineWithProfiles(
                Members(listOf(activeMember)), MemberPhotos(emptyList())
            )

            result.data shouldHaveSize 2
            result.data[0].isWithdrawn shouldBe false
            result.data[0].matchingResult shouldBe activeResult
            result.data[1].isWithdrawn shouldBe true
            result.data[1].targetName shouldBe null
            result.data[1].matchingResult shouldBe withdrawnResult
        }

        test("사진이 없는 회원은 profileImageUrl이 null이다") {
            val matchingResult = MatchingResultFixture.create(targetEmail = "target@example.com")
            val matchingResults = MatchingResults(listOf(matchingResult))

            val member = MemberFixture.create(email = matchingResult.targetEmail.value)

            val result = matchingResults.combineWithProfiles(
                Members(listOf(member)), MemberPhotos(emptyList())
            )

            result.data shouldHaveSize 1
            result.data[0].profileImageUrl shouldBe null
        }

        test("빈 매칭결과이면 빈 프로필 목록을 반환한다") {
            val matchingResults = MatchingResults(emptyList())

            val result = matchingResults.combineWithProfiles(
                Members(emptyList()), MemberPhotos(emptyList())
            )

            result.data shouldHaveSize 0
        }

        test("여러 매칭결과를 한번에 조합한다") {
            val result1 = MatchingResultFixture.create(targetEmail = "a@example.com", targetInfoId = 1L)
            val result2 = MatchingResultFixture.create(targetEmail = "b@example.com", targetInfoId = 2L)
            val matchingResults = MatchingResults(listOf(result1, result2))

            val memberA = MemberFixture.create(email = result1.targetEmail.value, name = "김철수", nickname = "철수")
            val memberB = MemberFixture.create(email = result2.targetEmail.value, name = "이영희", nickname = "영희")
            val photoA = MemberPhotoFixture.create(memberEmail = result1.targetEmail.value, thumbnailPath = "thumb/a.jpg")

            val result = matchingResults.combineWithProfiles(
                Members(listOf(memberA, memberB)), MemberPhotos(listOf(photoA))
            )

            result.data shouldHaveSize 2
            result.data[0].targetName shouldBe memberA.name
            result.data[0].profileImageUrl shouldBe photoA.thumbnailPath
            result.data[1].targetName shouldBe memberB.name
            result.data[1].profileImageUrl shouldBe null
        }
    }

    context("extractRegisterEmails") {

        test("중복 없이 등록자 이메일을 추출한다") {
            val results = MatchingResults(
                listOf(
                    MatchingResultFixture.create(targetInfoId = 1L, registerEmail = "a@a.com"),
                    MatchingResultFixture.create(targetInfoId = 2L, registerEmail = "b@b.com"),
                    MatchingResultFixture.create(targetInfoId = 3L, registerEmail = "a@a.com"),
                )
            )

            results.extractRegisterEmails() shouldContainExactlyInAnyOrder listOf(Email("a@a.com"), Email("b@b.com"))
        }
    }

    context("extractTargetInfoIds") {

        test("중복 없이 targetInfoId를 추출한다") {
            val results = MatchingResults(
                listOf(
                    MatchingResultFixture.create(targetInfoId = 1L),
                    MatchingResultFixture.create(targetInfoId = 2L),
                    MatchingResultFixture.create(targetInfoId = 1L),
                )
            )

            results.extractTargetInfoIds() shouldContainExactlyInAnyOrder setOf(1L, 2L)
        }
    }

    context("toClaimerProfiles") {

        test("요청자 프로필과 xroom 존재 여부를 조합한다") {
            val result1 = MatchingResultFixture.create(targetInfoId = 10L, registerEmail = "claimer1@a.com")
            val result2 = MatchingResultFixture.create(targetInfoId = 20L, registerEmail = "claimer2@a.com")
            val matchingResults = MatchingResults(listOf(result1, result2))

            val member1 = MemberFixture.create(email = "claimer1@a.com", name = "갑", nickname = "gap")
            val member2 = MemberFixture.create(email = "claimer2@a.com", name = "을", nickname = "eul")
            val photo1 = MemberPhotoFixture.create(memberEmail = "claimer1@a.com", thumbnailPath = "thumb/1.jpg")

            val profiles = matchingResults.toClaimerProfiles(
                Members(listOf(member1, member2)),
                MemberPhotos(listOf(photo1)),
                xroomExistTargetInfoIds = setOf(10L),
            )

            profiles.data shouldHaveSize 2
            profiles.data[0].name shouldBe "갑"
            profiles.data[0].profileImageUrl shouldBe "thumb/1.jpg"
            profiles.data[0].hasXroom shouldBe true
            profiles.data[1].name shouldBe "을"
            profiles.data[1].profileImageUrl shouldBe null
            profiles.data[1].hasXroom shouldBe false
        }

        test("xroomExistTargetInfoIds가 빈 집합이면 모든 hasXroom이 false이다") {
            val result = MatchingResultFixture.create(targetInfoId = 1L, registerEmail = "c@a.com")
            val matchingResults = MatchingResults(listOf(result))

            val profiles = matchingResults.toClaimerProfiles(
                Members(listOf(MemberFixture.create(email = "c@a.com"))),
                MemberPhotos(emptyList()),
                xroomExistTargetInfoIds = emptySet(),
            )

            profiles.data[0].hasXroom shouldBe false
        }

        test("탈퇴한 요청자는 memberId가 null이고 isWithdrawn이 true이다") {
            val result = MatchingResultFixture.create(targetInfoId = 1L, registerEmail = "withdrawn@a.com")
            val matchingResults = MatchingResults(listOf(result))

            val profiles = matchingResults.toClaimerProfiles(
                Members(emptyList()),
                MemberPhotos(emptyList()),
                xroomExistTargetInfoIds = emptySet(),
            )

            profiles.data[0].memberId shouldBe null
            profiles.data[0].isWithdrawn shouldBe true
        }

        test("빈 매칭결과이면 빈 프로필 목록을 반환한다") {
            val matchingResults = MatchingResults(emptyList())

            val profiles = matchingResults.toClaimerProfiles(
                Members(emptyList()),
                MemberPhotos(emptyList()),
                xroomExistTargetInfoIds = emptySet(),
            )

            profiles.data shouldHaveSize 0
        }
    }

})
