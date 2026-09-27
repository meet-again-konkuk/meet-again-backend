package com.konkuk.ma.domain.point.dao

import com.konkuk.ma.domain.point.domain.discount.DiscountType
import com.konkuk.ma.domain.point.entity.DiscountPolicyEntity
import com.konkuk.ma.domain.point.entity.table.DiscountPolicyTable
import org.jetbrains.exposed.sql.ColumnSet
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Component

@Component
class DiscountPolicyQueryDao(
    private val factories: List<DiscountPolicyEntityFactory>,
) {
    fun find(ids: Set<Long>): List<DiscountPolicyEntity> {
        if (ids.isEmpty()) return emptyList()

        return buildJoinedSource()
            .selectAll()
            .where { (DiscountPolicyTable.id inList ids) and (DiscountPolicyTable.deleted eq false) }
            .map { row -> buildEntity(row) }
    }

    fun findOneOrNull(id: Long): DiscountPolicyEntity? {
        return buildJoinedSource()
            .selectAll()
            .where { (DiscountPolicyTable.id eq id) and (DiscountPolicyTable.deleted eq false) }
            .limit(1)
            .firstOrNull()
            ?.let { buildEntity(it) }
    }

    private fun buildJoinedSource(): ColumnSet {
        return factories.fold(DiscountPolicyTable as ColumnSet) { acc, factory ->
            acc.join(factory.childTable, JoinType.LEFT, DiscountPolicyTable.id, factory.childTable.id)
        }
    }

    private fun buildEntity(row: ResultRow): DiscountPolicyEntity {
        val type = DiscountType.valueOf(row[DiscountPolicyTable.policyType])
        val factory = factories.find { it.type == type }
            ?: error("등록되지 않은 할인 정책 타입: $type")
        return factory.createFrom(row)
    }
}
