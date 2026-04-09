package com.konkuk.ma.domain.member.dao

import com.konkuk.ma.domain.member.domain.NewMember
import com.konkuk.ma.domain.member.entity.table.MemberTable
import org.jetbrains.exposed.sql.insertAndGetId
import org.springframework.stereotype.Component

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
} 
