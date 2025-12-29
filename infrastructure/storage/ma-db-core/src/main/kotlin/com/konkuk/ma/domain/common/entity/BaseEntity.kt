package com.konkuk.ma.domain.common.entity

import java.time.LocalDateTime

abstract class BaseEntity(
    val createdBy: String,

    val lastModifiedBy: String,

    val createdDate: LocalDateTime,

    val lastModifiedDate: LocalDateTime,

    val deleted: Boolean = false
)
