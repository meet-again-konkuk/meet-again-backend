package com.konkuk.ma.member

import com.konkuk.ma.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "members")
class Member(
    val email: String,
) : BaseEntity() {
}
