package com.konkuk.ma.domain.member.repository

import com.konkuk.ma.domain.member.dao.MemberQueryDao
import com.konkuk.ma.domain.member.domain.Gender
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

    override fun findByNameAndGender(name: String, gender: Gender): List<Member> {
        TODO("Not yet implemented")
    }

    override fun findByNames(names: Set<String>): List<Member> {
        return memberQueryDao.findByNames(names)
            .map { it.toDomain() }
    }
}
