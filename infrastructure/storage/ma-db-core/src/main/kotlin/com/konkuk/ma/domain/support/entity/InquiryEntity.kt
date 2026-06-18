package com.konkuk.ma.domain.support.entity

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.support.domain.Inquiry
import com.konkuk.ma.domain.support.entity.table.InquiryTable
import org.jetbrains.exposed.sql.ResultRow
import java.time.LocalDateTime

class InquiryEntity(
    val id: Long,
    val authorEmail: String,
    val title: String,
    val content: String,
    val createdDate: LocalDateTime,
) {
    fun toDomain(): Inquiry {
        return Inquiry(
            id = id,
            authorEmail = Email(authorEmail),
            title = title,
            content = content,
            createdDate = createdDate,
        )
    }

    companion object {
        fun from(row: ResultRow): InquiryEntity {
            return InquiryEntity(
                id = row[InquiryTable.id].value,
                authorEmail = row[InquiryTable.authorEmail],
                title = row[InquiryTable.title],
                content = row[InquiryTable.content],
                createdDate = row[InquiryTable.createdDate],
            )
        }
    }
}
