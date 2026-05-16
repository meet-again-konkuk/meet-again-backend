package com.konkuk.ma.domain.support.domain.port

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.support.domain.NewInquiry

interface InquiryCommandRepository {
    fun save(newInquiry: NewInquiry): Long
    fun anonymizeAuthor(oldEmail: Email, newEmail: Email)
}
