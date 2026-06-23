package com.konkuk.ma.domain.xroom.dao

import com.konkuk.ma.config.DatabaseTest
import com.konkuk.ma.config.TestDatabaseConfig
import com.konkuk.ma.domain.xroom.domain.NewXroom
import com.konkuk.ma.domain.xroom.entity.table.XroomTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
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
                val newXroom = NewXroom(ownerId = 1L, targetInfoId = 1L)

                val id = xroomCommandDao.save(newXroom)

                id shouldNotBe null
                id shouldBe 1L
            }
        }

        context("exists") {

            test("해당 targetInfoId의 X룸이 존재하면 true를 반환한다") {
                val newXroom = NewXroom(ownerId = 1L, targetInfoId = 1L)
                xroomCommandDao.save(newXroom)

                xroomQueryDao.exists(1L).shouldBeTrue()
            }

            test("해당 targetInfoId의 X룸이 없으면 false를 반환한다") {
                xroomQueryDao.exists(999L).shouldBeFalse()
            }

            test("deleted=true인 X룸은 존재하지 않는 것으로 판단한다") {
                val newXroom = NewXroom(ownerId = 1L, targetInfoId = 1L)
                xroomCommandDao.save(newXroom)
                XroomTable.softDelete({ XroomTable.targetInfoId eq 1L }, "1")

                xroomQueryDao.exists(1L).shouldBeFalse()
            }
        }

        context("exists (복수)") {

            test("일부만 존재하면 존재하는 targetInfoId만 반환한다") {
                xroomCommandDao.save(NewXroom(ownerId = 1L, targetInfoId = 1L))
                xroomCommandDao.save(NewXroom(ownerId = 2L, targetInfoId = 2L))

                xroomQueryDao.exists(setOf(1L, 2L, 999L)) shouldContainExactlyInAnyOrder setOf(1L, 2L)
            }

            test("빈 Set이 입력되면 빈 Set을 반환한다") {
                xroomQueryDao.exists(emptySet()).shouldBeEmpty()
            }

            test("soft-deleted는 결과에서 제외된다") {
                xroomCommandDao.save(NewXroom(ownerId = 1L, targetInfoId = 1L))
                xroomCommandDao.save(NewXroom(ownerId = 2L, targetInfoId = 2L))
                XroomTable.softDelete({ XroomTable.targetInfoId eq 1L }, "1")

                xroomQueryDao.exists(setOf(1L, 2L)) shouldContainExactlyInAnyOrder setOf(2L)
            }
        }
    }
}
