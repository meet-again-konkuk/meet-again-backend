package com.konkuk.ma.domain.point.repository

import com.konkuk.ma.config.DatabaseTest
import com.konkuk.ma.config.TestDatabaseConfig
import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.point.dao.MemberPointDao
import com.konkuk.ma.domain.point.domain.balance.MemberPoint
import com.konkuk.ma.domain.point.domain.balance.PointQuantity
import com.konkuk.ma.domain.point.entity.table.MemberPointTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(
    classes = [
        TestDatabaseConfig::class,
        MemberPointDao::class,
        MemberPointCoreRepository::class,
    ],
)
@DatabaseTest
class MemberPointCoreRepositoryTest(
    private val memberPointCoreRepository: MemberPointCoreRepository,
) : FunSpec() {

    override fun extensions() = listOf(SpringExtension)

    init {
        beforeEach {
            SchemaUtils.create(MemberPointTable)
        }

        afterEach {
            SchemaUtils.drop(MemberPointTable)
        }

        context("findOneOrInitial") {

            test("기존에 MemberPoint가 존재하면 해당 도메인 객체를 반환한다") {
                // Given
                val email = Email("holeman@naver.com")
                insertMemberPoint(email = email.value, balance = 30)

                // When
                val result = memberPointCoreRepository.findOneOrInitial(email)

                // Then
                result.id shouldNotBe null
                result.ownerEmail shouldBe email
                result.balance shouldBe PointQuantity(30)
                result.isPersisted().shouldBeTrue()
            }

            test("존재하지 않는 이메일이면 id가 null인 초기 MemberPoint를 반환한다") {
                // Given
                val email = Email("new-user@example.com")

                // When
                val result = memberPointCoreRepository.findOneOrInitial(email)

                // Then
                result.id shouldBe null
                result.ownerEmail shouldBe email
                result.balance shouldBe PointQuantity.ZERO
            }

            test("deleted=true인 레코드는 무시되고 초기 MemberPoint를 반환한다") {
                // Given
                val email = Email("deleted-user@example.com")
                insertMemberPoint(email = email.value, balance = 100, deleted = true)

                // When
                val result = memberPointCoreRepository.findOneOrInitial(email)

                // Then
                result.id shouldBe null
                result.balance shouldBe PointQuantity.ZERO
            }
        }

        context("save") {

            test("id가 null이면 새로 insert하고 생성된 ID를 반환한다") {
                // Given
                val email = Email("new-user@example.com")
                val memberPoint = MemberPoint.initial(email).charge(PointQuantity(10))

                // When
                val savedId = memberPointCoreRepository.save(memberPoint)

                // Then
                savedId shouldNotBe 0L
                val rows = MemberPointTable.selectAll().toList()
                rows.size shouldBe 1
                rows[0][MemberPointTable.ownerEmail] shouldBe email.value
                rows[0][MemberPointTable.balance] shouldBe 10
            }

            test("id가 존재하면 해당 레코드의 balance를 update한다") {
                // Given
                val email = Email("existing@example.com")
                val existingId = insertMemberPoint(email = email.value, balance = 30)
                val updated = MemberPoint(
                    id = existingId,
                    ownerEmail = email,
                    balance = PointQuantity(50),
                )

                // When
                val returnedId = memberPointCoreRepository.save(updated)

                // Then
                returnedId shouldBe existingId
                val row = MemberPointTable.selectAll().first()
                row[MemberPointTable.balance] shouldBe 50
                MemberPointTable.selectAll().count() shouldBe 1L
            }

            test("findOneOrInitial로 조회한 MemberPoint를 charge 후 save하면 balance가 증가한다") {
                // Given
                val email = Email("charger@example.com")
                insertMemberPoint(email = email.value, balance = 10)

                // When
                val found = memberPointCoreRepository.findOneOrInitial(email)
                val charged = found.charge(PointQuantity(25))
                memberPointCoreRepository.save(charged)

                // Then
                val row = MemberPointTable.selectAll().first()
                row[MemberPointTable.balance] shouldBe 35
            }
        }
    }

    private fun insertMemberPoint(
        email: String = "holeman@naver.com",
        balance: Int = 0,
        deleted: Boolean = false,
    ): Long {
        return MemberPointTable.insert {
            it[ownerEmail] = email
            it[MemberPointTable.balance] = balance
            it[MemberPointTable.deleted] = deleted
        }[MemberPointTable.id].value
    }
}
