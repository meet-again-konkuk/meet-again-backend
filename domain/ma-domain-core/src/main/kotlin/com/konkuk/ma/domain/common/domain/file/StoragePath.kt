package com.konkuk.ma.domain.common.domain.file

import java.time.LocalDate

class StoragePath private constructor(val value: String) {

    companion object {
        fun of(domain: StorageDomainType, usage: StorageUsageType, email: String): StoragePath {
            return StoragePath("${domain.path}/${usage.path}/$email")
        }

        fun withDate(domain: StorageDomainType, usage: StorageUsageType, email: String, date: LocalDate = LocalDate.now()): StoragePath {
            return StoragePath("${domain.path}/${usage.path}/$email/$date")
        }
    }
}
