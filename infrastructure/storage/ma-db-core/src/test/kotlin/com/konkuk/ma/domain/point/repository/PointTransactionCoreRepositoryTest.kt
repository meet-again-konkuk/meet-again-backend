package com.konkuk.ma.domain.point.repository

import com.konkuk.ma.config.DatabaseTest
import com.konkuk.ma.config.TestDatabaseConfig
import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.common.domain.Money
import com.konkuk.ma.domain.point.dao.PointTransactionDao
import com.konkuk.ma.domain.point.domain.balance.PointQuantity
import com.konkuk.ma.domain.point.domain.payment.PaymentMethod
import com.konkuk.ma.domain.point.domain.transaction.NewPointTransaction
import com.konkuk.ma.domain.point.domain.transaction.PointTransactionType
import com.konkuk.ma.domain.point.entity.table.PointTransactionTable
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
        PointTransactionDao::class,
        PointTransactionCoreRepository::class,
    ],
)
@DatabaseTest
class PointTransactionCoreRepositoryTest(
    private val pointTransactionCoreRepository: PointTransactionCoreRepository,
) : FunSpec() {

    override fun extensions() = listOf(SpringExtension)

    init {
        beforeEach {
            SchemaUtils.create(PointTransactionTable)
        }

        afterEach {
            SchemaUtils.drop(PointTransactionTable)
        }

        context("save") {

            test("NewPointTransaction을 저장하고 생성된 ID를 반환한다") {
                // Given
                val newTransaction = createNewCharge(idempotencyKey = "idem-key-1")

                // When
                val savedId = pointTransactionCoreRepository.save(newTransaction)

                // Then
                savedId shouldNotBe 0L
                val row = PointTransactionTable.selectAll().first()
                row[PointTransactionTable.ownerEmail] shouldBe newTransaction.ownerEmail.value
                row[PointTransactionTable.pointProductId] shouldBe newTransaction.pointProductId
                row[PointTransactionTable.transactionType] shouldBe newTransaction.transactionType.name
                row[PointTransactionTable.quantity] shouldBe newTransaction.quantity.toInt()
                row[PointTransactionTable.paidAmount] shouldBe newTransaction.paidAmount.toInt()
                row[PointTransactionTable.paymentMethod] shouldBe newTransaction.paymentMethod?.name
                row[PointTransactionTable.idempotencyKey] shouldBe newTransaction.idempotencyKey
                row[PointTransactionTable.approvalNumber] shouldBe newTransaction.approvalNumber
            }

            test("동일한 idempotencyKey로 두 번 save하면 UNIQUE 제약 위반 예외가 발생한다") {
                // Given
                val duplicatedKey = "duplicated-key"
                pointTransactionCoreRepository.save(createNewCharge(idempotencyKey = duplicatedKey))

                // When & Then
                shouldThrow<ExposedSQLException> {
                    pointTransactionCoreRepository.save(createNewCharge(idempotencyKey = duplicatedKey))
                }
            }
        }

        context("findOneOrNull") {

            test("해당 멱등키의 거래가 존재하면 도메인 객체로 반환한다") {
                // Given
                val idempotencyKey = "existing-key"
                val newTransaction = createNewCharge(idempotencyKey = idempotencyKey)
                val savedId = pointTransactionCoreRepository.save(newTransaction)

                // When
                val result = pointTransactionCoreRepository.findOneOrNull(idempotencyKey)

                // Then
                result shouldNotBe null
                result!!.id shouldBe savedId
                result.ownerEmail shouldBe newTransaction.ownerEmail
                result.idempotencyKey shouldBe idempotencyKey
                result.quantity shouldBe newTransaction.quantity
                result.paidAmount shouldBe newTransaction.paidAmount
                result.transactionType shouldBe newTransaction.transactionType
                result.paymentMethod shouldBe newTransaction.paymentMethod
                result.approvalNumber shouldBe newTransaction.approvalNumber
            }

            test("해당 멱등키의 거래가 없으면 null을 반환한다") {
                // When
                val result = pointTransactionCoreRepository.findOneOrNull("not-exist")

                // Then
                result shouldBe null
            }
        }
    }

    private fun createNewCharge(
        ownerEmail: Email = Email("holeman@naver.com"),
        pointProductId: Long = 1L,
        quantity: PointQuantity = PointQuantity(10),
        paidAmount: Money = Money.wons(1000),
        paymentMethod: PaymentMethod = PaymentMethod.CARD,
        idempotencyKey: String = "idem-key",
        approvalNumber: String = "MOCK-APPROVAL",
    ): NewPointTransaction {
        return NewPointTransaction(
            ownerEmail = ownerEmail,
            pointProductId = pointProductId,
            transactionType = PointTransactionType.CHARGE,
            quantity = quantity,
            paidAmount = paidAmount,
            paymentMethod = paymentMethod,
            idempotencyKey = idempotencyKey,
            approvalNumber = approvalNumber,
        )
    }
}
