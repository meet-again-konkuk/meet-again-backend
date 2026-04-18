package com.konkuk.ma.domain.point.dao

import com.konkuk.ma.domain.point.domain.discount.DiscountType
import com.konkuk.ma.domain.point.entity.DiscountPolicyEntity
import com.konkuk.ma.domain.point.entity.PercentDiscountPolicyEntity
import com.konkuk.ma.domain.point.entity.table.DiscountPolicyTable
import com.konkuk.ma.domain.point.entity.table.PercentDiscountPolicyTable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ResultRow
import org.springframework.stereotype.Component

@Component
class PercentDiscountPolicyEntityFactory : DiscountPolicyEntityFactory {
    override val type: DiscountType = DiscountType.PERCENT
    override val childTable: LongIdTable = PercentDiscountPolicyTable

    override fun createFrom(row: ResultRow): DiscountPolicyEntity {
        return PercentDiscountPolicyEntity(
            id = row[DiscountPolicyTable.id].value,
            startDate = row[DiscountPolicyTable.startDate],
            endDate = row[DiscountPolicyTable.endDate],
            discountPercent = row[PercentDiscountPolicyTable.discountPercent],
        )
    }
}
