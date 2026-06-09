package com.konkuk.ma.domain.matching.repository

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.matching.dao.TargetInfoCommandDao
import com.konkuk.ma.domain.matching.domain.NewTargetInfo
import com.konkuk.ma.domain.matching.domain.UpdateTargetInfo
import com.konkuk.ma.domain.matching.domain.port.TargetInfoCommandRepository
import com.konkuk.ma.domain.member.domain.Gender
import org.springframework.stereotype.Repository

@Repository
class TargetInfoCommandCoreRepository(
    private val targetInfoCommandDao: TargetInfoCommandDao,
) : TargetInfoCommandRepository {
    override fun save(newTargetInfo: NewTargetInfo, targetGender: Gender): Long {
        return targetInfoCommandDao.save(newTargetInfo, targetGender)
    }

    override fun update(id: Long, email: Email, updateTargetInfo: UpdateTargetInfo) {
        targetInfoCommandDao.update(id, email.value, updateTargetInfo)
    }

    override fun delete(id: Long, email: Email) {
        targetInfoCommandDao.delete(id, email.value)
    }

    override fun delete(email: Email) {
        targetInfoCommandDao.delete(email.value)
    }
}
