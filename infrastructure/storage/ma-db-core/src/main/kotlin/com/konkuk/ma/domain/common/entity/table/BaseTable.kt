package com.konkuk.ma.domain.common.entity.table

import java.time.LocalDateTime
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.datetime

abstract class BaseTable(name: String, idName: String) : LongIdTable(name, idName) {

    companion object {
        private const val DEFAULT_AUDIT_USER = "MEET_AGAIN"
    }

    val createdDate = datetime("CREATED_DATE").clientDefault { LocalDateTime.now() }
    val createdBy = varchar("CREATED_BY", 255).clientDefault { DEFAULT_AUDIT_USER }
    val lastModifiedDate = datetime("LAST_MODIFIED_DATE").clientDefault { LocalDateTime.now() }
    val lastModifiedBy = varchar("LAST_MODIFIED_BY", 255).clientDefault { DEFAULT_AUDIT_USER }
    val deleted = bool("DELETED").clientDefault { false }
    val deletedDate = datetime("DELETED_DATE").nullable()
    val deletedBy = varchar("DELETED_BY", 255).nullable()
}
