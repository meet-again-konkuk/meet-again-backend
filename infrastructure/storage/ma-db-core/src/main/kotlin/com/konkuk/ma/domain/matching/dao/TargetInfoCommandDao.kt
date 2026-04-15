package com.konkuk.ma.domain.matching.dao

import com.konkuk.ma.domain.matching.domain.NewTargetInfo
import com.konkuk.ma.domain.matching.domain.TargetInfo
import com.konkuk.ma.domain.matching.entity.table.TargetInfoTable
import com.konkuk.ma.domain.member.domain.Gender
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.update
import org.springframework.stereotype.Component

@Component
class TargetInfoCommandDao {
    fun save(newTargetInfo: NewTargetInfo, targetGender: Gender): Long {
        return TargetInfoTable.insertAndGetId {
            it[TargetInfoTable.registerEmail] = newTargetInfo.registerEmail.value
            it[name] = newTargetInfo.targetName
            it[TargetInfoTable.targetGender] = targetGender.name
            it[middleNumber] = newTargetInfo.middleNumber?.value
            it[lastNumber] = newTargetInfo.lastNumber?.value
            it[year] = newTargetInfo.year?.value
            it[month] = newTargetInfo.month?.value
            it[day] = newTargetInfo.day?.value
            it[region] = newTargetInfo.region?.name
            it[createdBy] = newTargetInfo.registerEmail.value
            it[lastModifiedBy] = newTargetInfo.registerEmail.value
        }.value
    }

    fun update(targetInfo: TargetInfo) {
        TargetInfoTable.update({ TargetInfoTable.id eq targetInfo.targetInfoId }) {
            it[name] = targetInfo.targetName
            it[middleNumber] = targetInfo.middleNumber?.value
            it[lastNumber] = targetInfo.lastNumber?.value
            it[year] = targetInfo.year?.value
            it[month] = targetInfo.month?.value
            it[day] = targetInfo.day?.value
            it[region] = targetInfo.region?.name
            it[lastModifiedBy] = targetInfo.registerEmail.value
        }
    }
}
