package com.konkuk.ma.domain.member.dao

import com.konkuk.ma.config.DatabaseTest
import com.konkuk.ma.config.TestDatabaseConfig
import com.konkuk.ma.domain.member.domain.photo.NewPhoto
import com.konkuk.ma.domain.member.entity.table.MemberPhotoTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [TestDatabaseConfig::class, MemberPhotoCommandDao::class])
@DatabaseTest
class MemberPhotoCommandDaoTest(
    private val memberPhotoCommandDao: MemberPhotoCommandDao
) : FunSpec() {

    override fun extensions() = listOf(SpringExtension)

    init {
        beforeEach {
            SchemaUtils.create(MemberPhotoTable)
        }

        afterEach {
            SchemaUtils.drop(MemberPhotoTable)
        }

        context("save") {

            test("사진을 저장하고 ID를 반환한다") {
                val newPhoto = createNewPhoto()

                val id = memberPhotoCommandDao.save(newPhoto)

                id shouldBeGreaterThan 0L
                MemberPhotoTable.selectAll().count() shouldBe 1
            }

            test("저장된 사진의 필드 값이 정확히 일치한다") {
                val newPhoto = createNewPhoto(
                    memberId = 42L,
                    storageKey = "member/profile/42/photo.jpg",
                    originalFileName = "원본사진.jpg",
                    thumbnailKey = "member/thumbnail/42/thumb_photo.jpg"
                )

                memberPhotoCommandDao.save(newPhoto)

                val row = MemberPhotoTable.selectAll().first()
                row[MemberPhotoTable.memberId] shouldBe newPhoto.memberId
                row[MemberPhotoTable.storageKey] shouldBe newPhoto.storageKey
                row[MemberPhotoTable.originalFileName] shouldBe newPhoto.originalFileName
                row[MemberPhotoTable.thumbnailKey] shouldBe newPhoto.thumbnailKey
            }

            test("thumbnailKey가 null인 사진을 저장한다") {
                val newPhoto = createNewPhoto(thumbnailKey = null)

                val id = memberPhotoCommandDao.save(newPhoto)

                id shouldBeGreaterThan 0L
                val row = MemberPhotoTable.selectAll().first()
                row[MemberPhotoTable.thumbnailKey] shouldBe null
            }
        }

        context("delete") {

            test("해당 회원의 사진을 soft delete 처리한다") {
                val memberId = 1L
                insertMemberPhoto(memberId = memberId)

                memberPhotoCommandDao.delete(memberId)

                val photo = MemberPhotoTable.selectAll().first()
                photo[MemberPhotoTable.deleted] shouldBe true
                photo[MemberPhotoTable.deletedBy] shouldBe memberId.toString()
                photo[MemberPhotoTable.deletedDate] shouldNotBe null
            }

            test("같은 회원의 사진이 여러 개이면 모두 soft delete 처리한다") {
                val memberId = 1L
                insertMemberPhoto(memberId = memberId, storageKey = "member/profile/1/1.jpg")
                insertMemberPhoto(memberId = memberId, storageKey = "member/profile/1/2.jpg")

                memberPhotoCommandDao.delete(memberId)

                val photos = MemberPhotoTable.selectAll().toList()
                photos.size shouldBe 2
                photos.all { it[MemberPhotoTable.deleted] } shouldBe true
            }

            test("다른 회원의 사진은 soft delete 처리하지 않는다") {
                insertMemberPhoto(memberId = 1L)
                insertMemberPhoto(memberId = 2L)

                memberPhotoCommandDao.delete(1L)

                val otherPhoto = MemberPhotoTable.selectAll()
                    .where { MemberPhotoTable.memberId eq 2L }
                    .first()
                otherPhoto[MemberPhotoTable.deleted] shouldBe false
            }

            test("존재하지 않는 회원으로 삭제해도 예외가 발생하지 않는다") {
                memberPhotoCommandDao.delete(99L)

                MemberPhotoTable.selectAll().count() shouldBe 0
            }
        }
    }

    private fun createNewPhoto(
        memberId: Long = 1L,
        storageKey: String = "member/profile/1/photo.jpg",
        originalFileName: String = "원본.jpg",
        thumbnailKey: String? = "member/thumbnail/1/thumb_photo.jpg",
    ): NewPhoto {
        return NewPhoto(
            memberId = memberId,
            storageKey = storageKey,
            originalFileName = originalFileName,
            thumbnailKey = thumbnailKey
        )
    }

    private fun insertMemberPhoto(
        memberId: Long = 1L,
        storageKey: String = "member/profile/1/photo.jpg",
    ) {
        MemberPhotoTable.insert {
            it[MemberPhotoTable.memberId] = memberId
            it[MemberPhotoTable.storageKey] = storageKey
            it[originalFileName] = "원본.jpg"
        }
    }
}
