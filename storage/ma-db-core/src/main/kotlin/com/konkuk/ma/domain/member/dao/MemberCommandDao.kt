package com.konkuk.ma.domain.member.dao

import com.konkuk.ma.domain.member.entity.table.MemberTable
import com.konkuk.ma.domain.member.domain.NewMember
import org.jetbrains.exposed.sql.insertAndGetId
import org.springframework.stereotype.Component

@Component
class MemberCommandDao {
    
    fun save(newMember: NewMember): Long {
        return MemberTable.insertAndGetId { row ->
            row[email] = newMember.email
            row[password] = newMember.password
            row[nickname] = newMember.nickname
            row[phoneNumber] = newMember.phoneNumber
            row[name] = newMember.name
            row[birthDate] = newMember.birthDate
            row[highSchool] = newMember.highSchool
            row[university] = newMember.university
            row[createdBy] = newMember.email
            row[lastModifiedBy] = newMember.email
        }.value
    }
} 
