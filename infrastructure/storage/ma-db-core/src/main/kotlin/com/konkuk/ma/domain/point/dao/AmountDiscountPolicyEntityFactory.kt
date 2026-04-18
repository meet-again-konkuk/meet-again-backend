package com.konkuk.ma.domain.point.dao

import com.konkuk.ma.domain.point.domain.discount.DiscountType
import com.konkuk.ma.domain.point.entity.AmountDiscountPolicyEntity
import com.konkuk.ma.domain.point.entity.DiscountPolicyEntity
import com.konkuk.ma.domain.point.entity.table.AmountDiscountPolicyTable
import com.konkuk.ma.domain.point.entity.table.DiscountPolicyTable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ResultRow
import org.springframework.stereotype.Component

@Component
class AmountDiscountPolicyEntityFactory : DiscountPolicyEntityFactory {
    override val type: DiscountType = DiscountType.AMOUNT
    override val childTable: LongIdTable = AmountDiscountPolicyTable

    override fun createFrom(row: ResultRow): DiscountPolicyEntity {
        return AmountDiscountPolicyEntity(
            id = row[DiscountPolicyTable.id].value,
            startDate = row[DiscountPolicyTable.startDate],
            endDate = row[DiscountPolicyTable.endDate],
            discountAmount = row[AmountDiscountPolicyTable.discountAmount],
        )
    }
}
