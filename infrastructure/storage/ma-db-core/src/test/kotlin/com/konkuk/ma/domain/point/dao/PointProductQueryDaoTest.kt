package com.konkuk.ma.domain.point.dao

import com.konkuk.ma.config.DatabaseTest
import com.konkuk.ma.config.TestDatabaseConfig
import com.konkuk.ma.domain.point.entity.table.PointProductTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.update
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [TestDatabaseConfig::class, PointProductQueryDao::class])
@DatabaseTest
class PointProductQueryDaoTest(
    private val pointProductQueryDao: PointProductQueryDao,
) : FunSpec() {

    override fun extensions() = listOf(SpringExtension)

    init {
        beforeEach {
            SchemaUtils.create(PointProductTable)
        }

        afterEach {
            SchemaUtils.drop(PointProductTable)
        }

        context("find") {

            test("활성 상품 목록을 displayOrder 오름차순으로 조회한다") {
                // Given
                insertPointProduct(name = "50개", displayOrder = 3)
                insertPointProduct(name = "10개", displayOrder = 1)
                insertPointProduct(name = "30개", displayOrder = 2)

                // When
                val result = pointProductQueryDao.find()

                // Then
                result shouldHaveSize 3
                result[0].name shouldBe "10개"
                result[1].name shouldBe "30개"
                result[2].name shouldBe "50개"
            }

            test("상품이 없으면 빈 리스트를 반환한다") {
                // When
                val result = pointProductQueryDao.find()

                // Then
                result shouldHaveSize 0
            }

            test("deleted=true인 상품은 조회되지 않는다") {
                // Given
                insertPointProduct(name = "활성 상품", displayOrder = 1)
                insertPointProduct(name = "삭제된 상품", displayOrder = 2)
                PointProductTable.update({ PointProductTable.name eq "삭제된 상품" }) {
                    it[deleted] = true
                }

                // When
                val result = pointProductQueryDao.find()

                // Then
                result shouldHaveSize 1
                result[0].name shouldBe "활성 상품"
            }
        }
    }

    private fun insertPointProduct(
        name: String = "인연 10개",
        quantity: Int = 10,
        price: Int = 1000,
        displayOrder: Int = 1,
    ) {
        PointProductTable.insert {
            it[PointProductTable.name] = name
            it[PointProductTable.quantity] = quantity
            it[PointProductTable.price] = price
            it[PointProductTable.displayOrder] = displayOrder
        }
    }
}
