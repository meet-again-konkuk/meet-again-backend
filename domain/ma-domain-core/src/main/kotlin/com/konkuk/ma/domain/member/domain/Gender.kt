package com.konkuk.ma.domain.member.domain

enum class Gender {
    MALE,
    FEMALE;

    fun getOtherGender(): Gender = when (this) {
        MALE -> FEMALE
        FEMALE -> MALE
    }
}
