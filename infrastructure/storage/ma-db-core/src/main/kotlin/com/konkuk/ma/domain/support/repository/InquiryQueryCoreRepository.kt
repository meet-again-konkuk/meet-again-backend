package com.konkuk.ma.domain.support.repository

import com.konkuk.ma.domain.support.dao.InquiryQueryDao
import com.konkuk.ma.domain.support.domain.Inquiry
import com.konkuk.ma.domain.support.domain.port.InquiryQueryRepository
import org.springframework.stereotype.Repository

@Repository
class InquiryQueryCoreRepository(
    private val inquiryQueryDao: InquiryQueryDao,
) : InquiryQueryRepository {
    override fun find(authorId: Long): List<Inquiry> {
        return inquiryQueryDao.find(authorId).map { it.toDomain() }
    }
}
