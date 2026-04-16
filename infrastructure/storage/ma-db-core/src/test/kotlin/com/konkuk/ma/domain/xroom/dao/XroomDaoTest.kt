package com.konkuk.ma.domain.xroom.dao

import com.konkuk.ma.config.DatabaseTest
import com.konkuk.ma.config.TestDatabaseConfig
import com.konkuk.ma.domain.xroom.domain.NewXroom
import com.konkuk.ma.domain.xroom.entity.table.XroomTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jetbrains.exposed.sql.SchemaUtils
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [TestDatabaseConfig::class, XroomCommandDao::class, XroomQueryDao::class])
@DatabaseTest
class XroomDaoTest(
    private val xroomCommandDao: XroomCommandDao,
    private val xroomQueryDao: XroomQueryDao,
) : FunSpec() {

    override fun extensions() = listOf(SpringExtension)

    init {
        beforeEach {
            SchemaUtils.create(XroomTable)
        }

        afterEach {
            SchemaUtils.drop(XroomTable)
        }

        context("save") {

            test("X룸을 저장하고 ID를 반환한다") {
                val newXroom = NewXroom(ownerEmail = "test@example.com", targetInfoId = 1L)

                val id = xroomCommandDao.save(newXroom)

                id shouldNotBe null
                id shouldBe 1L
            }
        }

        context("existsByTargetInfoId") {

            test("해당 targetInfoId의 X룸이 존재하면 true를 반환한다") {
                val newXroom = NewXroom(ownerEmail = "test@example.com", targetInfoId = 1L)
                xroomCommandDao.save(newXroom)

                xroomQueryDao.existsByTargetInfoId(1L).shouldBeTrue()
            }

            test("해당 targetInfoId의 X룸이 없으면 false를 반환한다") {
                xroomQueryDao.existsByTargetInfoId(999L).shouldBeFalse()
            }

            test("deleted=true인 X룸은 존재하지 않는 것으로 판단한다") {
                val newXroom = NewXroom(ownerEmail = "test@example.com", targetInfoId = 1L)
                xroomCommandDao.save(newXroom)
                XroomTable.softDelete({ XroomTable.targetInfoId eq 1L }, "test@example.com")

                xroomQueryDao.existsByTargetInfoId(1L).shouldBeFalse()
            }
        }
    }
}
