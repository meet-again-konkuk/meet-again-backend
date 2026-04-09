package com.konkuk.ma.domain.member.repository

import com.konkuk.ma.domain.common.domain.Email
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

    override fun existsByEmail(email: Email): Boolean {
        return memberQueryDao.existsByEmail(email.value)
    }

    override fun findOne(email: Email): Member {
        return memberQueryDao.findOne(email.value)
            .toDomain()
    }

    override fun findByNames(names: Set<String>): List<Member> {
        return memberQueryDao.findByNames(names)
            .map { it.toDomain() }
    }

    override fun findByEmails(emails: Set<Email>): List<Member> {
        return memberQueryDao.findByEmails(emails.map { it.value }.toSet())
            .map { it.toDomain() }
    }
}
