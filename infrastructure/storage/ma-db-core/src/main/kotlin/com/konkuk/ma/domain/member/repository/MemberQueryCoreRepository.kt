package com.konkuk.ma.domain.member.repository

import com.konkuk.ma.domain.member.dao.MemberQueryDao
import com.konkuk.ma.domain.member.domain.Member
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import org.springframework.stereotype.Repository

@Repository
class MemberQueryCoreRepository(
    private val memberQueryDao: MemberQueryDao
) : MemberQueryRepository {
    override fun existsByNickname(nickname: String): Boolean {
        return memberQueryDao.existsByNickname(nickname)
    }

    override fun existsByEmail(email: String): Boolean {
        return memberQueryDao.existsByEmail(email)
    }

    override fun findByEmail(email: String): Member {
        return memberQueryDao.findByEmail(email)
            .toDomain()
    }
}
