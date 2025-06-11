package com.konkuk.ma.domain.member.repository

import com.konkuk.ma.domain.member.dao.MemberCommandDao
import com.konkuk.ma.domain.member.entity.MemberEntity
import com.konkuk.ma.member.domain.Member
import com.konkuk.ma.member.domain.MemberCommandRepository
import org.springframework.stereotype.Repository

@Repository
class MemberCommandCoreRepository(
    private val memberCommandDao: MemberCommandDao
) : MemberCommandRepository {
    
    override fun save(member: Member): Long {
        val memberEntity = MemberEntity(
            email = member.email,
            password = member.password,
            nickname = member.nickname,
            phoneNumber = member.phoneNumber,
            name = member.name,
            birthDate = member.birthDate,
            highSchool = member.highSchool,
            university = member.university
        )
        
        return memberCommandDao.save(memberEntity)
    }
} 