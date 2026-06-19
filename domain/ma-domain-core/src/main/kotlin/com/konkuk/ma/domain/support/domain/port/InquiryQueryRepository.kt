package com.konkuk.ma.domain.support.domain.port

import com.konkuk.ma.domain.support.domain.Inquiry

interface InquiryQueryRepository {
    fun find(authorId: Long): List<Inquiry>
}
