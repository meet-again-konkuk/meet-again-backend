package com.konkuk.ma.domain.member.repository

import com.konkuk.ma.domain.member.dao.MemberCommandDao
import com.konkuk.ma.member.domain.NewMember
import com.konkuk.ma.member.domain.port.MemberCommandRepository
import org.springframework.stereotype.Repository

@Repository
class MemberCommandCoreRepository(
    private val memberCommandDao: MemberCommandDao
) : MemberCommandRepository {
    
    override fun save(newMember: NewMember): Long {
        return memberCommandDao.save(newMember)
    }
} 
