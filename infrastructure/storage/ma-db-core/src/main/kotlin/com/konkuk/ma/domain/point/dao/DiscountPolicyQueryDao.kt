package com.konkuk.ma.domain.point.dao

import com.konkuk.ma.domain.point.entity.DiscountPolicyEntity
import com.konkuk.ma.domain.point.entity.table.AmountDiscountPolicyTable
import com.konkuk.ma.domain.point.entity.table.DiscountPolicyTable
import com.konkuk.ma.domain.point.entity.table.PercentDiscountPolicyTable
import org.jetbrains.exposed.sql.JoinType
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
            .map { row ->
                DiscountPolicyEntity(
                    id = row[DiscountPolicyTable.id].value,
                    policyType = row[DiscountPolicyTable.policyType],
                    startDate = row[DiscountPolicyTable.startDate],
                    endDate = row[DiscountPolicyTable.endDate],
                    discountAmount = row.getOrNull(AmountDiscountPolicyTable.discountAmount),
                    discountPercent = row.getOrNull(PercentDiscountPolicyTable.discountPercent),
                )
            }
    }
}
