package com.konkuk.ma.domain.point.repository

import com.konkuk.ma.config.DatabaseTest
import com.konkuk.ma.config.TestDatabaseConfig
import com.konkuk.ma.domain.common.domain.Money
import com.konkuk.ma.domain.point.dao.PointHistoryDao
import com.konkuk.ma.domain.point.domain.balance.PointQuantity
import com.konkuk.ma.domain.point.domain.history.NewPointHistory
import com.konkuk.ma.domain.point.domain.history.PointHistoryType
import com.konkuk.ma.domain.point.domain.payment.PaymentMethod
import com.konkuk.ma.domain.point.entity.table.PointHistoryTable
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(
    classes = [
        TestDatabaseConfig::class,
        PointHistoryDao::class,
        PointHistoryCoreRepository::class,
    ],
)
@DatabaseTest
class PointHistoryCoreRepositoryTest(
    private val pointHistoryCoreRepository: PointHistoryCoreRepository,
) : FunSpec() {

    override fun extensions() = listOf(SpringExtension)

    init {
        beforeEach {
            SchemaUtils.create(PointHistoryTable)
        }

        afterEach {
            SchemaUtils.drop(PointHistoryTable)
        }

        context("save") {

            test("NewPointHistory를 저장하고 생성된 ID를 반환한다") {
                // Given
                val newHistory = createNewCharge(idempotencyKey = "idem-key-1")

                // When
                val savedId = pointHistoryCoreRepository.save(newHistory)

                // Then
                savedId shouldNotBe 0L
                val row = PointHistoryTable.selectAll().first()
                row[PointHistoryTable.ownerId] shouldBe newHistory.ownerId
                row[PointHistoryTable.pointProductId] shouldBe newHistory.pointProductId
                row[PointHistoryTable.historyType] shouldBe newHistory.historyType.name
                row[PointHistoryTable.quantity] shouldBe newHistory.quantity.toInt()
                row[PointHistoryTable.paidAmount] shouldBe newHistory.paidAmount.toInt()
                row[PointHistoryTable.paymentMethod] shouldBe newHistory.paymentMethod?.name
                row[PointHistoryTable.idempotencyKey] shouldBe newHistory.idempotencyKey
                row[PointHistoryTable.approvalNumber] shouldBe newHistory.approvalNumber
            }

            test("동일한 idempotencyKey로 두 번 save하면 UNIQUE 제약 위반 예외가 발생한다") {
                // Given
                val duplicatedKey = "duplicated-key"
                pointHistoryCoreRepository.save(createNewCharge(idempotencyKey = duplicatedKey))

                // When & Then
                shouldThrow<ExposedSQLException> {
                    pointHistoryCoreRepository.save(createNewCharge(idempotencyKey = duplicatedKey))
                }
            }
        }

        context("findOneOrNull") {

            test("해당 멱등키의 이력이 존재하면 도메인 객체로 반환한다") {
                // Given
                val idempotencyKey = "existing-key"
                val newHistory = createNewCharge(idempotencyKey = idempotencyKey)
                val savedId = pointHistoryCoreRepository.save(newHistory)

                // When
                val result = pointHistoryCoreRepository.findOneOrNull(idempotencyKey)

                // Then
                result shouldNotBe null
                result!!.id shouldBe savedId
                result.ownerId shouldBe newHistory.ownerId
                result.idempotencyKey shouldBe idempotencyKey
                result.quantity shouldBe newHistory.quantity
                result.paidAmount shouldBe newHistory.paidAmount
                result.historyType shouldBe newHistory.historyType
                result.paymentMethod shouldBe newHistory.paymentMethod
                result.approvalNumber shouldBe newHistory.approvalNumber
            }

            test("해당 멱등키의 이력이 없으면 null을 반환한다") {
                // When
                val result = pointHistoryCoreRepository.findOneOrNull("not-exist")

                // Then
                result shouldBe null
            }
        }
    }

    private fun createNewCharge(
        ownerId: Long = 1L,
        pointProductId: Long = 1L,
        quantity: PointQuantity = PointQuantity(10),
        paidAmount: Money = Money.wons(1000),
        paymentMethod: PaymentMethod = PaymentMethod.CARD,
        idempotencyKey: String = "idem-key",
        approvalNumber: String = "MOCK-APPROVAL",
    ): NewPointHistory {
        return NewPointHistory(
            ownerId = ownerId,
            pointProductId = pointProductId,
            historyType = PointHistoryType.CHARGE,
            quantity = quantity,
            paidAmount = paidAmount,
            paymentMethod = paymentMethod,
            idempotencyKey = idempotencyKey,
            approvalNumber = approvalNumber,
        )
    }
}
