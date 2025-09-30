package com.konkuk.ma.domain.common.entity.table

import java.time.LocalDateTime
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.datetime

abstract class BaseTable(name: String, idName: String) : LongIdTable(name, idName) {
    val createdDate = datetime("CREATED_DATE").clientDefault { LocalDateTime.now() }
    val createdBy = varchar("CREATED_BY", 255).clientDefault { "MEET_AGAIN" }
    val lastModifiedDate = datetime("LAST_MODIFIED_DATE").clientDefault { LocalDateTime.now() }
    val lastModifiedBy = varchar("LAST_MODIFIED_BY", 255).clientDefault { "MEET_AGAIN" }
    val deleted = bool("DELETED").clientDefault { false }
}
