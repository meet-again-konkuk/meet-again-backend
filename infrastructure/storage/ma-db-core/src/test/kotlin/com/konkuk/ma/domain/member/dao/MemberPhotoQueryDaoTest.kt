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

            test("회원에 해당하는 사진을 조회한다") {
                // Given
                val memberId = 1L
                insertMemberPhoto(memberId = memberId)

                // When
                val result = memberPhotoQueryDao.findOne(memberId)

                // Then
                result shouldNotBe null
                result!!.memberId shouldBe memberId
            }

            test("deleted가 true인 사진은 조회되지 않는다") {
                // Given
                val memberId = 1L
                insertMemberPhoto(memberId = memberId)
                MemberPhotoTable.update({ MemberPhotoTable.memberId eq memberId }) {
                    it[deleted] = true
                }

                // When
                val result = memberPhotoQueryDao.findOne(memberId)

                // Then
                result shouldBe null
            }

            test("존재하지 않는 회원으로 조회하면 null을 반환한다") {
                // When
                val result = memberPhotoQueryDao.findOne(99L)

                // Then
                result shouldBe null
            }
        }

        context("find") {

            test("여러 회원에 해당하는 사진을 조회한다") {
                // Given
                val memberId1 = 1L
                val memberId2 = 2L
                insertMemberPhoto(memberId = memberId1)
                insertMemberPhoto(memberId = memberId2)

                // When
                val result = memberPhotoQueryDao.find(setOf(memberId1, memberId2))

                // Then
                result shouldHaveSize 2
            }

            test("deleted가 true인 사진은 제외한다") {
                // Given
                val memberId1 = 1L
                val memberId2 = 2L
                insertMemberPhoto(memberId = memberId1)
                insertMemberPhoto(memberId = memberId2)
                MemberPhotoTable.update({ MemberPhotoTable.memberId eq memberId2 }) {
                    it[deleted] = true
                }

                // When
                val result = memberPhotoQueryDao.find(setOf(memberId1, memberId2))

                // Then
                result shouldHaveSize 1
                result[0].memberId shouldBe memberId1
            }

            test("빈 회원 Set으로 조회하면 빈 리스트를 반환한다") {
                // When
                val result = memberPhotoQueryDao.find(emptySet())

                // Then
                result shouldHaveSize 0
            }

            test("일치하는 회원이 없으면 빈 리스트를 반환한다") {
                // When
                val result = memberPhotoQueryDao.find(setOf(99L))

                // Then
                result shouldHaveSize 0
            }
        }
    }

    private fun insertMemberPhoto(
        memberId: Long = 1L,
        filePath: String = "/uploads/photo.jpg",
    ) {
        MemberPhotoTable.insert {
            it[MemberPhotoTable.memberId] = memberId
            it[MemberPhotoTable.filePath] = filePath
            it[originalFileName] = "원본.jpg"
        }
    }
}
