package com.konkuk.ma.domain.member.repository

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.member.dao.MemberCommandDao
import com.konkuk.ma.domain.member.domain.Member
import com.konkuk.ma.domain.member.domain.NewMember
import com.konkuk.ma.domain.member.domain.port.MemberCommandRepository
import com.konkuk.ma.domain.member.entity.MemberEntity
import java.time.LocalDateTime
import org.springframework.stereotype.Repository

@Repository
class MemberCommandCoreRepository(
    private val memberCommandDao: MemberCommandDao
) : MemberCommandRepository {

    override fun save(newMember: NewMember): Long {
        return memberCommandDao.save(newMember)
    }

    override fun requestWithdrawal(email: Email, requestedAt: LocalDateTime) {
        memberCommandDao.requestWithdrawal(email, requestedAt)
    }

    override fun cancelWithdrawal(email: Email) {
        memberCommandDao.cancelWithdrawal(email)
    }

    override fun anonymizeAndSoftDelete(member: Member) {
        val anonymized = MemberEntity.from(member.anonymize())
        memberCommandDao.anonymizeAndSoftDelete(member.email, anonymized)
    }
}

