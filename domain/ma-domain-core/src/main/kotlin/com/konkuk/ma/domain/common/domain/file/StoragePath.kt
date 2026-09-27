package com.konkuk.ma.domain.common.domain.file

import java.time.LocalDate

class StoragePath private constructor(val value: String) {

    companion object {
        fun of(domain: StorageDomainType, usage: StorageUsageType, id: Long): StoragePath {
            return StoragePath("${domain.path}/${usage.path}/$id")
        }

        fun withDate(domain: StorageDomainType, usage: StorageUsageType, id: Long, date: LocalDate = LocalDate.now()): StoragePath {
            return StoragePath("${domain.path}/${usage.path}/$id/$date")
        }
    }
}
