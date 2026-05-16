package com.konkuk.ma.domain.support.repository

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.support.dao.InquiryCommandDao
import com.konkuk.ma.domain.support.domain.NewInquiry
import com.konkuk.ma.domain.support.domain.port.InquiryCommandRepository
import org.springframework.stereotype.Repository

@Repository
class InquiryCommandCoreRepository(
    private val inquiryCommandDao: InquiryCommandDao,
) : InquiryCommandRepository {
    override fun save(newInquiry: NewInquiry): Long {
        return inquiryCommandDao.save(newInquiry)
    }

    override fun anonymizeAuthor(oldEmail: Email, newEmail: Email) {
        inquiryCommandDao.anonymizeAuthor(oldEmail.value, newEmail.value)
    }
}
