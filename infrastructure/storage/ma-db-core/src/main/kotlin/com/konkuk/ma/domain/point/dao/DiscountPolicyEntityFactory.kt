package com.konkuk.ma.domain.point.dao

import com.konkuk.ma.domain.point.domain.discount.DiscountType
import com.konkuk.ma.domain.point.entity.DiscountPolicyEntity
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ResultRow

interface DiscountPolicyEntityFactory {
    val type: DiscountType
    val childTable: LongIdTable

    fun createFrom(row: ResultRow): DiscountPolicyEntity
}
