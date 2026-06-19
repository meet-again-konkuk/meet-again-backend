package com.konkuk.ma.domain.support.dao

import com.konkuk.ma.config.DatabaseTest
import com.konkuk.ma.config.TestDatabaseConfig
import com.konkuk.ma.domain.support.domain.NewInquiry
import com.konkuk.ma.domain.support.entity.table.InquiryTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [TestDatabaseConfig::class, InquiryCommandDao::class])
@DatabaseTest
class InquiryCommandDaoTest(
    private val inquiryCommandDao: InquiryCommandDao
) : FunSpec() {

    override fun extensions() = listOf(SpringExtension)

    init {
        beforeEach {
            SchemaUtils.create(InquiryTable)
        }

        afterEach {
            SchemaUtils.drop(InquiryTable)
        }

        context("save") {

            test("문의를 저장하고 생성된 ID를 반환한다") {
                // Given
                val newInquiry = NewInquiry(
                    authorId = 1L,
                    title = "문의 제목",
                    content = "문의 내용입니다."
                )

                // When
                val id = inquiryCommandDao.save(newInquiry)

                // Then
                id shouldBeGreaterThan 0L
            }

            test("저장된 문의의 필드가 올바르게 저장된다") {
                // Given
                val newInquiry = NewInquiry(
                    authorId = 1L,
                    title = "문의 제목",
                    content = "문의 내용입니다."
                )

                // When
                inquiryCommandDao.save(newInquiry)

                // Then
                val saved = InquiryTable.selectAll().first()
                saved[InquiryTable.authorId] shouldBe newInquiry.authorId
                saved[InquiryTable.title] shouldBe newInquiry.title
                saved[InquiryTable.content] shouldBe newInquiry.content
                saved[InquiryTable.createdBy] shouldBe newInquiry.authorId.toString()
                saved[InquiryTable.lastModifiedBy] shouldBe newInquiry.authorId.toString()
            }
        }
    }
}
