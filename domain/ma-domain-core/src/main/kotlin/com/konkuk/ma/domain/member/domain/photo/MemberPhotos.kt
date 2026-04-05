package com.konkuk.ma.domain.member.domain.photo

class MemberPhotos(val data: List<MemberPhoto>) {

    fun findByEmail(email: String): MemberPhoto? = data.find { it.memberEmail == email }
}
