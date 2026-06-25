package com.konkuk.ma.domain.member.dao

import com.konkuk.ma.domain.member.domain.Member
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

    fun requestWithdrawal(memberId: Long, requestedAt: LocalDateTime) {
        MemberTable.update({ MemberTable.id eq memberId }) {
            it[withdrawalRequestedAt] = requestedAt
            it[lastModifiedBy] = memberId.toString()
        }
    }

    fun cancelWithdrawal(memberId: Long) {
        MemberTable.update({ MemberTable.id eq memberId }) {
            it[withdrawalRequestedAt] = null
            it[lastModifiedBy] = memberId.toString()
        }
    }

    fun anonymizeAndSoftDelete(anonymized: Member) {
        MemberTable.update({ MemberTable.id eq anonymized.id }) {
            it[email] = anonymized.email.value
            it[password] = anonymized.password
            it[nickname] = anonymized.nickname
            it[name] = anonymized.name
            it[phoneNumber] = anonymized.phoneNumber.fullNumber
            it[birthDate] = anonymized.birthDate
            it[region] = anonymized.region.name
            it[highSchool] = anonymized.highSchool
            it[university] = anonymized.university
            it[profileImageUrl] = null
            it[deleted] = true
            it[lastModifiedBy] = anonymized.id.toString()
        }
    }
}
