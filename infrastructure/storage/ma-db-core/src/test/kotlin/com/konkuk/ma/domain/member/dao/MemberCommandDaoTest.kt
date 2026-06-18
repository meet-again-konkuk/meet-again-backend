package com.konkuk.ma.domain.member.dao

import com.konkuk.ma.config.DatabaseTest
import com.konkuk.ma.config.TestDatabaseConfig
import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.member.domain.Gender
import com.konkuk.ma.domain.member.domain.NewMember
import com.konkuk.ma.domain.member.domain.PhoneNumber
import com.konkuk.ma.domain.member.domain.Region
import com.konkuk.ma.domain.member.entity.MemberEntity
import com.konkuk.ma.domain.member.entity.table.MemberTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate

@ContextConfiguration(classes = [TestDatabaseConfig::class, MemberCommandDao::class])
@DatabaseTest
class MemberCommandDaoTest(
    private val memberCommandDao: MemberCommandDao
) : FunSpec() {

    override fun extensions() = listOf(SpringExtension)

    init {
        beforeEach {
            SchemaUtils.create(MemberTable)
        }

        afterEach {
            SchemaUtils.drop(MemberTable)
        }

        context("save") {

            test("필수 필드만 있는 NewMember를 저장하고 ID를 반환한다") {
                // Given
                val newMember = createNewMember()

                // When
                val id = memberCommandDao.save(newMember)

                // Then
                id shouldBeGreaterThan 0L
                MemberTable.selectAll().count() shouldBe 1
            }

            test("저장된 회원의 필드 값이 정확히 일치한다") {
                // Given
                val newMember = createNewMember(
                    email = "verify@example.com",
                    nickname = "검증닉네임"
                )

                // When
                memberCommandDao.save(newMember)

                // Then
                val row = MemberTable.selectAll().first()
                row[MemberTable.email] shouldBe newMember.email.value
                row[MemberTable.nickname] shouldBe newMember.nickname
                row[MemberTable.gender] shouldBe newMember.gender.name
                row[MemberTable.name] shouldBe newMember.name
                row[MemberTable.region] shouldBe newMember.region.name
                row[MemberTable.phoneNumber] shouldBe newMember.phoneNumber.fullNumber
            }

            test("highSchool과 university가 null인 회원을 저장한다") {
                // Given
                val newMember = createNewMember(highSchool = null, university = null)

                // When
                val id = memberCommandDao.save(newMember)

                // Then
                id shouldBeGreaterThan 0L
                val row = MemberTable.selectAll().first()
                row[MemberTable.highSchool] shouldBe null
                row[MemberTable.university] shouldBe null
            }

            test("highSchool과 university가 있는 회원을 저장한다") {
                // Given
                val newMember = createNewMember(
                    highSchool = "건국고등학교",
                    university = "건국대학교"
                )

                // When
                memberCommandDao.save(newMember)

                // Then
                val row = MemberTable.selectAll().first()
                row[MemberTable.highSchool] shouldBe "건국고등학교"
                row[MemberTable.university] shouldBe "건국대학교"
            }

            test("여러 회원을 저장하면 각각 다른 ID를 반환한다") {
                // Given
                val member1 = createNewMember(email = "user1@example.com", nickname = "닉네임1")
                val member2 = createNewMember(email = "user2@example.com", nickname = "닉네임2")

                // When
                val id1 = memberCommandDao.save(member1)
                val id2 = memberCommandDao.save(member2)

                // Then
                id1 shouldBeGreaterThan 0L
                id2 shouldBeGreaterThan id1
                MemberTable.selectAll().count() shouldBe 2
            }
        }

        context("anonymizeAndSoftDelete") {

            test("회원 정보를 익명 값으로 덮어쓰고 soft delete 처리한다") {
                // Given
                val originalEmail = Email("user@example.com")
                val id = memberCommandDao.save(createNewMember(email = originalEmail.value, nickname = "원래닉네임"))
                val anonymized = MemberEntity(
                    id = id,
                    email = "withdrawn_$id@deleted.local",
                    password = "",
                    nickname = "탈퇴한회원_$id",
                    gender = Gender.MALE,
                    phoneNumber = "01000000000",
                    name = "탈퇴한회원",
                    region = Region.SEOUL,
                    birthDate = LocalDate.of(1900, 1, 1),
                    highSchool = null,
                    university = null
                )

                // When
                memberCommandDao.anonymizeAndSoftDelete(originalEmail, anonymized)

                // Then
                val row = MemberTable.selectAll().first()
                row[MemberTable.email] shouldBe anonymized.email
                row[MemberTable.nickname] shouldBe anonymized.nickname
                row[MemberTable.name] shouldBe anonymized.name
                row[MemberTable.deleted] shouldBe true
            }
        }
    }

    private fun createNewMember(
        email: String = "test@example.com",
        nickname: String = "testNickname",
        highSchool: String? = null,
        university: String? = null,
    ): NewMember {
        return NewMember(
            email = Email(email),
            password = "password123",
            nickname = nickname,
            gender = Gender.MALE,
            phoneNumber = PhoneNumber("01012345678"),
            name = "김테스트",
            birthDate = LocalDate.of(1990, 1, 1),
            region = Region.SEOUL,
            highSchool = highSchool,
            university = university
        )
    }
}
