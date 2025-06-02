package com.konkuk.ma.domain.member.repository

import com.konkuk.ma.domain.member.dao.MemberValidateDao
import com.konkuk.ma.member.domain.MemberQueryRepository
import org.springframework.stereotype.Repository

@Repository
class MemberQueryCoreRepository(
    private val memberValidateDao: MemberValidateDao
) : MemberQueryRepository {
    override fun existsByNickname(nickname: String): Boolean {
        return memberValidateDao.existsByNickname(nickname)
    }
}
