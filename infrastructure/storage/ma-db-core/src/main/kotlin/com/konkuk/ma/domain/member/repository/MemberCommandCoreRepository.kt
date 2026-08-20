package com.konkuk.ma.domain.member.repository

import com.konkuk.ma.domain.member.dao.MemberCommandDao
import com.konkuk.ma.domain.member.domain.Member
import com.konkuk.ma.domain.member.domain.NewMember
import com.konkuk.ma.domain.member.domain.port.MemberCommandRepository
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

    override fun updatePassword(memberId: Long, encodedPassword: String) {
        memberCommandDao.updatePassword(memberId, encodedPassword)
    }

    override fun updateProfile(member: Member) {
        memberCommandDao.updateProfile(member)
    }

    override fun anonymizeAndSoftDelete(member: Member) {
        memberCommandDao.anonymizeAndSoftDelete(member.anonymize())
    }
}

