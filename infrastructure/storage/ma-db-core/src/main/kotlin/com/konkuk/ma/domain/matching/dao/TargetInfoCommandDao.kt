package com.konkuk.ma.domain.matching.dao

import com.konkuk.ma.domain.matching.domain.NewTargetInfo
import com.konkuk.ma.domain.matching.entity.table.TargetInfoTable
import org.jetbrains.exposed.sql.insertAndGetId
import org.springframework.stereotype.Component

@Component
class TargetInfoCommandDao {
    fun save(newTargetInfo: NewTargetInfo): Long {
        return TargetInfoTable.insertAndGetId {
            it[registerEmail] = newTargetInfo.registerEmail
            it[name] = newTargetInfo.targetName
            it[targetGender] = newTargetInfo.targetGender.name
            it[middleNumber] = newTargetInfo.middleNumber
            it[lastNumber] = newTargetInfo.lastNumber
            it[year] = newTargetInfo.year?.value
            it[month] = newTargetInfo.month?.value
            it[day] = newTargetInfo.day?.value
            it[region] = newTargetInfo.region?.name
            it[createdBy] = newTargetInfo.registerEmail
            it[lastModifiedBy] = newTargetInfo.registerEmail
        }.value
    }
}
