package com.konkuk.ma.domain.auth.entity.table

import com.konkuk.ma.domain.common.entity.table.BaseTable
import org.jetbrains.exposed.sql.javatime.datetime

object RefreshTokenTable : BaseTable("REFRESH_TOKENS", "REFRESH_TOKEN_ID") {
    val email = varchar("EMAIL", 255).uniqueIndex()

    val expirationDate = datetime("EXPIRATION_DATE")

    val token = varchar("TOKEN", 1024)
}
