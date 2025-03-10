package com.konkuk.ma.domain.auth.repository

import com.konkuk.ma.auth.domain.SmsRedisRepository
import com.konkuk.ma.auth.domain.SmsVerification
import com.konkuk.ma.domain.auth.dao.SmsVerificationFindDao
import com.konkuk.ma.domain.auth.dao.SmsVerificationSaveDao
import org.springframework.stereotype.Repository

@Repository
class SmsRedisCoreRepository(
    private val smsVerificationSaveDao: SmsVerificationSaveDao,

    private val smsVerificationFindDao: SmsVerificationFindDao
) : SmsRedisRepository {
    override fun save(smsVerification: SmsVerification) {
        smsVerificationSaveDao.save(smsVerification)
    }

    override fun find(phoneNumber: String): Int? {
        return smsVerificationFindDao.find(phoneNumber)
    }
}
