package com.konkuk.ma.domain.point.dao

import com.konkuk.ma.domain.point.domain.discount.DiscountType
import com.konkuk.ma.domain.point.entity.AmountDiscountPolicyEntity
import com.konkuk.ma.domain.point.entity.DiscountPolicyEntity
import com.konkuk.ma.domain.point.entity.PercentDiscountPolicyEntity
import com.konkuk.ma.domain.point.entity.table.AmountDiscountPolicyTable
import com.konkuk.ma.domain.point.entity.table.DiscountPolicyTable
import com.konkuk.ma.domain.point.entity.table.PercentDiscountPolicyTable
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Component

@Component
class DiscountPolicyQueryDao {
    fun find(ids: Set<Long>): List<DiscountPolicyEntity> {
        if (ids.isEmpty()) return emptyList()

        return DiscountPolicyTable
            .join(AmountDiscountPolicyTable, JoinType.LEFT, DiscountPolicyTable.id, AmountDiscountPolicyTable.id)
            .join(PercentDiscountPolicyTable, JoinType.LEFT, DiscountPolicyTable.id, PercentDiscountPolicyTable.id)
            .selectAll()
            .where { (DiscountPolicyTable.id inList ids) and (DiscountPolicyTable.deleted eq false) }
            .map { it.toEntity() }
    }

    private fun ResultRow.toEntity(): DiscountPolicyEntity {
        val id = this[DiscountPolicyTable.id].value
        val startDate = this[DiscountPolicyTable.startDate]
        val endDate = this[DiscountPolicyTable.endDate]
        return when (DiscountType.valueOf(this[DiscountPolicyTable.policyType])) {
            DiscountType.AMOUNT -> AmountDiscountPolicyEntity(
                id = id,
                startDate = startDate,
                endDate = endDate,
                discountAmount = this[AmountDiscountPolicyTable.discountAmount],
            )
            DiscountType.PERCENT -> PercentDiscountPolicyEntity(
                id = id,
                startDate = startDate,
                endDate = endDate,
                discountPercent = this[PercentDiscountPolicyTable.discountPercent],
            )
        }
    }
}
