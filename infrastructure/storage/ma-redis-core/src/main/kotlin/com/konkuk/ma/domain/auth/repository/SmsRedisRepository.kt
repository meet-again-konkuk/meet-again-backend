package com.konkuk.ma.domain.auth.repository

import com.konkuk.ma.domain.auth.domain.SmsVerification
import com.konkuk.ma.domain.auth.domain.port.SmsRepository
import com.konkuk.ma.domain.auth.dao.SmsVerificationFindDao
import com.konkuk.ma.domain.auth.dao.SmsVerificationSaveDao
import org.springframework.stereotype.Repository

@Repository
class SmsRedisRepository(
    private val smsVerificationSaveDao: SmsVerificationSaveDao,

    private val smsVerificationFindDao: SmsVerificationFindDao
) : SmsRepository {
    override fun save(smsVerification: SmsVerification) {
        smsVerificationSaveDao.save(smsVerification)
    }

    override fun findOrNull(phoneNumber: String): Int? {
        return smsVerificationFindDao.find(phoneNumber)
    }

    override fun confirmVerificationCode(phoneNumber: String) {
        smsVerificationSaveDao.save(phoneNumber)
    }

    override fun getConfirmed(phoneNumber: String): Boolean {
        return smsVerificationFindDao.getConfirmed(phoneNumber)
    }
}
