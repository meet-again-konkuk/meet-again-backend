package com.konkuk.ma.integration

import com.konkuk.ma.domain.community.application.BlockQueryService
import com.konkuk.ma.domain.community.entity.table.BlockTable
import com.konkuk.ma.domain.member.entity.table.MemberTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * 차단 목록 조회(REQ-014) Service 레이어 E2E 통합 테스트.
 *
 * BlockQueryService.findBlocks 가 실제 H2 DB 를 관통하여 차단자 본인의 활성 차단만을
 * (blockId, 차단당한 회원 닉네임, 차단 시각)으로 조립하는지 검증한다.
 *
 * Service 본문이 아직 TODO 인 TDD RED 단계이므로 MockMvc 대신 Service 빈을 직접 주입한다.
 */
@SpringBootTest
@ActiveProfiles("test")
class BlockQueryServiceIntegrationTest(
    private val blockQueryService: BlockQueryService,
) : FunSpec({

    var memberSeq = 0L

    beforeSpec {
        transaction {
            SchemaUtils.create(MemberTable, BlockTable)
        }
    }

    afterEach {
        transaction {
            BlockTable.deleteAll()
            MemberTable.deleteAll()
        }
    }

    afterSpec {
        transaction {
            SchemaUtils.drop(BlockTable, MemberTable)
        }
    }

    fun insertMember(nickname: String = "회원"): Long {
        memberSeq += 1
        return transaction {
            MemberTable.insertAndGetId {
                it[MemberTable.email] = "member$memberSeq@example.com"
                it[password] = "encoded"
                it[MemberTable.nickname] = nickname
                it[gender] = "MALE"
                it[phoneNumber] = "01012345678"
                it[name] = "김테스트"
                it[birthDate] = LocalDate.of(1990, 1, 1)
                it[region] = "SEOUL"
            }.value
        }
    }

    fun insertBlock(blockerId: Long, blockedId: Long): Long {
        return transaction {
            BlockTable.insertAndGetId {
                it[BlockTable.blockerId] = blockerId
                it[BlockTable.blockedId] = blockedId
            }.value
        }
    }

    fun softDeleteBlock(blockId: Long) {
        transaction {
            BlockTable.update({ BlockTable.id eq blockId }) {
                it[deleted] = true
            }
        }
    }

    context("findBlocks") {

        test("내가 차단한 회원들을 blockId·닉네임과 함께 목록으로 반환한다") {
            // Given
            val blockerId = insertMember(nickname = "차단자")
            val firstBlocked = insertMember(nickname = "첫번째차단대상")
            val secondBlocked = insertMember(nickname = "두번째차단대상")
            val firstBlockId = insertBlock(blockerId = blockerId, blockedId = firstBlocked)
            val secondBlockId = insertBlock(blockerId = blockerId, blockedId = secondBlocked)

            // When
            val blocks = blockQueryService.findBlocks(blockerId)

            // Then
            blocks shouldHaveSize 2
            blocks.first { it.blockId == firstBlockId }.nickname shouldBe "첫번째차단대상"
            blocks.first { it.blockId == secondBlockId }.nickname shouldBe "두번째차단대상"
        }

        test("다른 회원이 만든 차단은 내 차단 목록에 포함되지 않는다") {
            // Given
            val blockerId = insertMember(nickname = "차단자")
            val otherBlockerId = insertMember(nickname = "다른차단자")
            val blockedId = insertMember(nickname = "차단대상")
            val myBlockId = insertBlock(blockerId = blockerId, blockedId = blockedId)
            insertBlock(blockerId = otherBlockerId, blockedId = blockedId)

            // When
            val blocks = blockQueryService.findBlocks(blockerId)

            // Then - 내가 만든 차단 1건만
            blocks shouldHaveSize 1
            blocks.first().blockId shouldBe myBlockId
        }

        test("해제(soft delete)된 차단은 목록에서 제외된다") {
            // Given
            val blockerId = insertMember(nickname = "차단자")
            val activeBlocked = insertMember(nickname = "유지대상")
            val releasedBlocked = insertMember(nickname = "해제대상")
            val activeBlockId = insertBlock(blockerId = blockerId, blockedId = activeBlocked)
            val releasedBlockId = insertBlock(blockerId = blockerId, blockedId = releasedBlocked)
            softDeleteBlock(releasedBlockId)

            // When
            val blocks = blockQueryService.findBlocks(blockerId)

            // Then - 활성 차단 1건만
            blocks shouldHaveSize 1
            blocks.first().blockId shouldBe activeBlockId
        }

        test("차단한 회원이 없으면 빈 목록을 반환한다") {
            // Given
            val blockerId = insertMember(nickname = "차단자")

            // When & Then
            blockQueryService.findBlocks(blockerId).shouldBeEmpty()
        }
    }
})
