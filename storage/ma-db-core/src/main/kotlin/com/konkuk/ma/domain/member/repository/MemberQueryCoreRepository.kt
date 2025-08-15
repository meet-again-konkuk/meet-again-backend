package com.konkuk.ma.domain.member.repository

import com.konkuk.ma.domain.member.dao.MemberValidateDao
import com.konkuk.ma.member.domain.Member
import com.konkuk.ma.member.domain.port.MemberQueryRepository
import org.springframework.stereotype.Repository

@Repository
class MemberQueryCoreRepository(
    private val memberValidateDao: MemberValidateDao
) : MemberQueryRepository {
    override fun existsByNickname(nickname: String): Boolean {
        return memberValidateDao.existsByNickname(nickname)
    }

    override fun existsByEmail(email: String): Boolean {
        return memberValidateDao.existsByEmail(email)
    }

    override fun findByEmail(email: String): Member {
        return memberValidateDao.findByEmail(email)
            .toDomain()
    }
}
