package com.konkuk.ma.domain.member.dao

import com.konkuk.ma.domain.member.entity.MemberEntity
import com.konkuk.ma.domain.member.entity.table.MemberTable
import org.jetbrains.exposed.sql.insertAndGetId
import org.springframework.stereotype.Component

@Component
class MemberCommandDao {
    
    fun save(memberEntity: MemberEntity): Long {
        return MemberTable.insertAndGetId { row ->
            row[email] = memberEntity.email
            row[password] = memberEntity.password
            row[nickname] = memberEntity.nickname
            row[phoneNumber] = memberEntity.phoneNumber
            row[name] = memberEntity.name
            row[birthDate] = memberEntity.birthDate
            row[highSchool] = memberEntity.highSchool
            row[university] = memberEntity.university
        }.value
    }
} 