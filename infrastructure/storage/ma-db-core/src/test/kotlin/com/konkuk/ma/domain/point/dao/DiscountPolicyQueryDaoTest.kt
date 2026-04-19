package com.konkuk.ma.domain.point.dao

import com.konkuk.ma.config.DatabaseTest
import com.konkuk.ma.config.TestDatabaseConfig
import com.konkuk.ma.domain.point.domain.discount.AmountDiscountPolicy
import com.konkuk.ma.domain.point.domain.discount.PercentDiscountPolicy
import com.konkuk.ma.domain.point.entity.table.AmountDiscountPolicyTable
import com.konkuk.ma.domain.point.entity.table.DiscountPolicyTable
import com.konkuk.ma.domain.point.entity.table.PercentDiscountPolicyTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.update
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate

@ContextConfiguration(
    classes = [
        TestDatabaseConfig::class,
        DiscountPolicyQueryDao::class,
        AmountDiscountPolicyEntityFactory::class,
        PercentDiscountPolicyEntityFactory::class,
    ],
)
@DatabaseTest
class DiscountPolicyQueryDaoTest(
    private val discountPolicyQueryDao: DiscountPolicyQueryDao,
) : FunSpec() {

    override fun extensions() = listOf(SpringExtension)

    init {
        beforeEach {
            SchemaUtils.create(DiscountPolicyTable, AmountDiscountPolicyTable, PercentDiscountPolicyTable)
        }

        afterEach {
            SchemaUtils.drop(AmountDiscountPolicyTable, PercentDiscountPolicyTable, DiscountPolicyTable)
        }

        context("find") {

            test("AMOUNT 정책을 조회해 AmountDiscountPolicy 도메인으로 변환한다") {
                val id = insertAmountPolicy(amount = 500)

                val results = discountPolicyQueryDao.find(setOf(id))

                results shouldHaveSize 1
                val domain = results[0].toDomain()
                domain.shouldBeInstanceOf<AmountDiscountPolicy>()
                domain.discountPolicyId shouldBe id
                domain.discountAmount.toInt() shouldBe 500
            }

            test("PERCENT 정책을 조회해 PercentDiscountPolicy 도메인으로 변환한다") {
                val id = insertPercentPolicy(percent = 20)

                val results = discountPolicyQueryDao.find(setOf(id))

                results shouldHaveSize 1
                val domain = results[0].toDomain()
                domain.shouldBeInstanceOf<PercentDiscountPolicy>()
                domain.discountPercent shouldBe 20
            }

            test("AMOUNT/PERCENT 혼합 ID를 한 쿼리로 조회한다") {
                val amountId = insertAmountPolicy(amount = 300)
                val percentId = insertPercentPolicy(percent = 15)

                val results = discountPolicyQueryDao.find(setOf(amountId, percentId))

                results shouldHaveSize 2
                val domains = results.map { it.toDomain() }
                domains.any { it is AmountDiscountPolicy && it.discountAmount.toInt() == 300 } shouldBe true
                domains.any { it is PercentDiscountPolicy && it.discountPercent == 15 } shouldBe true
            }

            test("빈 Set이면 DB 쿼리 없이 빈 리스트를 반환한다") {
                discountPolicyQueryDao.find(emptySet()).shouldBeEmpty()
            }

            test("존재하지 않는 ID는 결과에서 제외된다") {
                val id = insertAmountPolicy(amount = 100)

                val results = discountPolicyQueryDao.find(setOf(id, 9999L))

                results shouldHaveSize 1
                results[0].toDomain().discountPolicyId shouldBe id
            }

            test("deleted=true인 정책은 조회되지 않는다") {
                val id = insertAmountPolicy(amount = 100)
                DiscountPolicyTable.update({ DiscountPolicyTable.id eq id }) { it[deleted] = true }

                discountPolicyQueryDao.find(setOf(id)).shouldBeEmpty()
            }
        }
    }

    private fun insertAmountPolicy(amount: Int): Long {
        val id = DiscountPolicyTable.insert {
            it[policyType] = "AMOUNT"
            it[startDate] = LocalDate.now().minusDays(1)
            it[endDate] = LocalDate.now().plusDays(30)
        } get DiscountPolicyTable.id
        AmountDiscountPolicyTable.insert {
            it[AmountDiscountPolicyTable.id] = id
            it[discountAmount] = amount
        }
        return id.value
    }

    private fun insertPercentPolicy(percent: Int): Long {
        val id = DiscountPolicyTable.insert {
            it[policyType] = "PERCENT"
            it[startDate] = LocalDate.now().minusDays(1)
            it[endDate] = LocalDate.now().plusDays(30)
        } get DiscountPolicyTable.id
        PercentDiscountPolicyTable.insert {
            it[PercentDiscountPolicyTable.id] = id
            it[discountPercent] = percent
        }
        return id.value
    }
}
