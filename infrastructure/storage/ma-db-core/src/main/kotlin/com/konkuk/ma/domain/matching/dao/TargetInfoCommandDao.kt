package com.konkuk.ma.domain.matching.dao

import com.konkuk.ma.domain.matching.domain.NewTargetInfo
import com.konkuk.ma.domain.matching.domain.UpdateTargetInfo
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

    fun update(id: Long, email: String, updateTargetInfo: UpdateTargetInfo) {
        TargetInfoTable.update({ TargetInfoTable.id eq id }) {
            updateTargetInfo.targetName?.let { targetName -> it[name] = targetName }
            it[middleNumber] = updateTargetInfo.middleNumber?.value
            it[lastNumber] = updateTargetInfo.lastNumber?.value
            it[year] = updateTargetInfo.year?.value
            it[month] = updateTargetInfo.month?.value
            it[day] = updateTargetInfo.day?.value
            it[region] = updateTargetInfo.region?.name
            it[lastModifiedBy] = email
        }
    }

    fun delete(id: Long, email: String) {
        TargetInfoTable.softDelete({ TargetInfoTable.id eq id }, email)
    }
}
