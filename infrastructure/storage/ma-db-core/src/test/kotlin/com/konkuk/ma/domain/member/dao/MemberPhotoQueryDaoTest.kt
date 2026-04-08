package com.konkuk.ma.domain.member.dao

import com.konkuk.ma.config.DatabaseTest
import com.konkuk.ma.config.TestDatabaseConfig
import com.konkuk.ma.domain.member.entity.table.MemberPhotoTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.update
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [TestDatabaseConfig::class, MemberPhotoQueryDao::class])
@DatabaseTest
class MemberPhotoQueryDaoTest(
    private val memberPhotoQueryDao: MemberPhotoQueryDao
) : FunSpec() {

    override fun extensions() = listOf(SpringExtension)

    init {
        beforeEach {
            SchemaUtils.create(MemberPhotoTable)
        }

        afterEach {
            SchemaUtils.drop(MemberPhotoTable)
        }

        context("findOne") {

            test("이메일에 해당하는 사진을 조회한다") {
                // Given
                val email = "user@example.com"
                insertMemberPhoto(memberEmail = email)

                // When
                val result = memberPhotoQueryDao.findOne(email)

                // Then
                result shouldNotBe null
                result!!.memberEmail shouldBe email
            }

            test("deleted가 true인 사진은 조회되지 않는다") {
                // Given
                val email = "user@example.com"
                insertMemberPhoto(memberEmail = email)
                MemberPhotoTable.update({ MemberPhotoTable.memberEmail eq email }) {
                    it[deleted] = true
                }

                // When
                val result = memberPhotoQueryDao.findOne(email)

                // Then
                result shouldBe null
            }

            test("존재하지 않는 이메일로 조회하면 null을 반환한다") {
                // When
                val result = memberPhotoQueryDao.findOne("nobody@example.com")

                // Then
                result shouldBe null
            }
        }

        context("find") {

            test("여러 이메일에 해당하는 사진을 조회한다") {
                // Given
                val email1 = "user1@example.com"
                val email2 = "user2@example.com"
                insertMemberPhoto(memberEmail = email1)
                insertMemberPhoto(memberEmail = email2)

                // When
                val result = memberPhotoQueryDao.find(setOf(email1, email2))

                // Then
                result shouldHaveSize 2
            }

            test("deleted가 true인 사진은 제외한다") {
                // Given
                val email1 = "user1@example.com"
                val email2 = "user2@example.com"
                insertMemberPhoto(memberEmail = email1)
                insertMemberPhoto(memberEmail = email2)
                MemberPhotoTable.update({ MemberPhotoTable.memberEmail eq email2 }) {
                    it[deleted] = true
                }

                // When
                val result = memberPhotoQueryDao.find(setOf(email1, email2))

                // Then
                result shouldHaveSize 1
                result[0].memberEmail shouldBe email1
            }

            test("빈 이메일 Set으로 조회하면 빈 리스트를 반환한다") {
                // When
                val result = memberPhotoQueryDao.find(emptySet())

                // Then
                result shouldHaveSize 0
            }

            test("일치하는 이메일이 없으면 빈 리스트를 반환한다") {
                // When
                val result = memberPhotoQueryDao.find(setOf("nobody@example.com"))

                // Then
                result shouldHaveSize 0
            }
        }
    }

    private fun insertMemberPhoto(
        memberEmail: String = "user@example.com",
        filePath: String = "/uploads/photo.jpg",
    ) {
        MemberPhotoTable.insert {
            it[MemberPhotoTable.memberEmail] = memberEmail
            it[MemberPhotoTable.filePath] = filePath
            it[originalFileName] = "원본.jpg"
        }
    }
}
