package com.konkuk.ma.domain.support.application

import com.konkuk.ma.domain.support.domain.NewInquiry
import com.konkuk.ma.domain.support.domain.port.InquiryCommandRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class InquiryCommandService(
    private val inquiryCommandRepository: InquiryCommandRepository,
) {
    fun create(newInquiry: NewInquiry): Long {
        return inquiryCommandRepository.save(newInquiry)
    }
}
