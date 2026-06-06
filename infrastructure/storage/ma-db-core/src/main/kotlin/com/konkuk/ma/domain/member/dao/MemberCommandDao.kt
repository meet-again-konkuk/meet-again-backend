package com.konkuk.ma.domain.member.dao

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.member.domain.NewMember
import com.konkuk.ma.domain.member.entity.table.MemberTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.update
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class MemberCommandDao {

    fun save(newMember: NewMember): Long {
        return MemberTable.insertAndGetId {
            it[email] = newMember.email.value
            it[password] = newMember.password
            it[nickname] = newMember.nickname
            it[phoneNumber] = newMember.phoneNumber.fullNumber
            it[gender] = newMember.gender.name
            it[name] = newMember.name
            it[region] = newMember.region.name
            it[birthDate] = newMember.birthDate
            it[highSchool] = newMember.highSchool
            it[university] = newMember.university
            it[createdBy] = newMember.email.value
            it[lastModifiedBy] = newMember.email.value
        }.value
    }

    fun requestWithdrawal(email: Email, requestedAt: LocalDateTime) {
        MemberTable.update({ MemberTable.email eq email.value }) {
            it[withdrawalRequestedAt] = requestedAt
            it[lastModifiedBy] = email.value
        }
    }

    fun cancelWithdrawal(email: Email) {
        MemberTable.update({ MemberTable.email eq email.value }) {
            it[withdrawalRequestedAt] = null
            it[lastModifiedBy] = email.value
        }
    }
}
