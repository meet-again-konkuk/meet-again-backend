package com.konkuk.ma.domain.member.repository

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

    override fun requestWithdrawal(memberId: Long, requestedAt: LocalDateTime) {
        memberCommandDao.requestWithdrawal(memberId, requestedAt)
    }

    override fun cancelWithdrawal(memberId: Long) {
        memberCommandDao.cancelWithdrawal(memberId)
    }

    override fun anonymizeAndSoftDelete(member: Member) {
        val anonymized = MemberEntity.from(member.anonymize())
        memberCommandDao.anonymizeAndSoftDelete(anonymized)
    }
}

