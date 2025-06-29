package com.konkuk.ma.domain.member.repository

import com.konkuk.ma.domain.member.dao.MemberCommandDao
import com.konkuk.ma.domain.member.entity.MemberEntity
import com.konkuk.ma.member.domain.NewMember
import com.konkuk.ma.member.domain.port.MemberCommandRepository
import org.springframework.stereotype.Repository

@Repository
class MemberCommandCoreRepository(
    private val memberCommandDao: MemberCommandDao
) : MemberCommandRepository {
    
    override fun save(newMember: NewMember): Long {
        val memberEntity = MemberEntity(
            email = newMember.email,
            password = newMember.password,
            nickname = newMember.nickname,
            phoneNumber = newMember.phoneNumber,
            name = newMember.name,
            birthDate = newMember.birthDate,
            highSchool = newMember.highSchool,
            university = newMember.university
        )
        
        return memberCommandDao.save(memberEntity)
    }
} 
