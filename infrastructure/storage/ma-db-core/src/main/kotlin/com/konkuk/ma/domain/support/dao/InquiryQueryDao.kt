package com.konkuk.ma.domain.support.dao

import com.konkuk.ma.domain.support.entity.InquiryEntity
import com.konkuk.ma.domain.support.entity.table.InquiryTable
import org.springframework.stereotype.Component

@Component
class InquiryQueryDao {
    fun find(authorId: Long): List<InquiryEntity> {
        return InquiryTable
            .activeRows { InquiryTable.authorId eq authorId }
            .map { InquiryEntity.from(it) }
    }
}
