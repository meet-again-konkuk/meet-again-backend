package com.konkuk.ma.domain.member.repository

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.member.dao.MemberQueryDao
import com.konkuk.ma.domain.member.domain.Member
import com.konkuk.ma.domain.member.domain.PhoneNumber
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.exception.EntityType
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class MemberQueryCoreRepository(
    private val memberQueryDao: MemberQueryDao
) : MemberQueryRepository {
    override fun existsByNickname(nickname: String): Boolean {
        return memberQueryDao.existsByNickname(nickname)
    }

    override fun exists(email: Email): Boolean {
        return memberQueryDao.existsByEmail(email.value)
    }

    override fun exists(phoneNumber: PhoneNumber): Boolean {
        return memberQueryDao.existsByPhoneNumber(phoneNumber.fullNumber)
    }

    override fun existsPending(email: Email): Boolean {
        return memberQueryDao.existsPendingByEmail(email.value)
    }

    override fun existsPending(phoneNumber: PhoneNumber): Boolean {
        return memberQueryDao.existsPendingByPhoneNumber(phoneNumber.fullNumber)
    }

    override fun findOne(email: Email): Member {
        return findOneOrNull(email)
            ?: throw EntityNotFoundException(EntityType.MEMBER, email.value)
    }

    override fun findOneOrNull(email: Email): Member? {
        return memberQueryDao.findOne(email.value)?.toDomain()
    }

    override fun findByNames(names: Set<String>): List<Member> {
        return memberQueryDao.findByNames(names)
            .map { it.toDomain() }
    }

    override fun findByEmails(emails: Set<Email>): List<Member> {
        return memberQueryDao.findByEmails(emails.map { it.value }.toSet())
            .map { it.toDomain() }
    }

    override fun findExpiredWithdrawalRequests(expiredBefore: LocalDateTime, cursorId: Long?, pageSize: Int): List<Member> {
        return memberQueryDao.findExpiredWithdrawalRequests(expiredBefore, cursorId, pageSize)
            .map { it.toDomain() }
    }
}
