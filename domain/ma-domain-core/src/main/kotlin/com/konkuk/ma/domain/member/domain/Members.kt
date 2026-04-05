package com.konkuk.ma.domain.member.domain

class Members(val data: List<Member>) {

    fun findByEmail(email: String): Member? = data.find { it.email == email }
}
