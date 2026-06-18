package com.konkuk.ma.domain.support.dao

import com.konkuk.ma.domain.support.entity.InquiryEntity
import com.konkuk.ma.domain.support.entity.table.InquiryTable
import org.springframework.stereotype.Component

@Component
class InquiryQueryDao {
    fun find(authorEmail: String): List<InquiryEntity> {
        return InquiryTable
            .activeRows { InquiryTable.authorEmail eq authorEmail }
            .map { InquiryEntity.from(it) }
    }
}
