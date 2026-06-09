package com.konkuk.ma.domain.support.dao

import com.konkuk.ma.domain.support.domain.NewInquiry
import com.konkuk.ma.domain.support.entity.table.InquiryTable
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.update
import org.springframework.stereotype.Component

@Component
class InquiryCommandDao {
    fun save(newInquiry: NewInquiry): Long {
        return InquiryTable.insertAndGetId {
            it[authorEmail] = newInquiry.authorEmail.value
            it[title] = newInquiry.title
            it[content] = newInquiry.content
            it[createdBy] = newInquiry.authorEmail.value
            it[lastModifiedBy] = newInquiry.authorEmail.value
        }.value
    }

    fun anonymizeAuthor(oldEmail: String, newEmail: String) {
        InquiryTable.update({ InquiryTable.authorEmail eq oldEmail }) {
            it[authorEmail] = newEmail
            it[lastModifiedBy] = newEmail
        }
    }
}
