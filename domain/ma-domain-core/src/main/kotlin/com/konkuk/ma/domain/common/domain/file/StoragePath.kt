package com.konkuk.ma.domain.common.domain.file

import com.konkuk.ma.domain.common.domain.Email
import java.time.LocalDate

class StoragePath private constructor(val value: String) {

    companion object {
        fun of(domain: StorageDomainType, usage: StorageUsageType, email: Email): StoragePath {
            return StoragePath("${domain.path}/${usage.path}/${email.value}")
        }

        fun withDate(domain: StorageDomainType, usage: StorageUsageType, email: Email, date: LocalDate = LocalDate.now()): StoragePath {
            return StoragePath("${domain.path}/${usage.path}/${email.value}/$date")
        }
    }
}
