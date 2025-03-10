package com.konkuk.ma

import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import java.time.LocalDateTime
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @CreatedDate
    val createdDate: LocalDateTime = LocalDateTime.MIN

    @LastModifiedDate
    val lastModifiedDate: LocalDateTime = LocalDateTime.MIN
}
