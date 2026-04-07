package com.konkuk.ma.domain.member.domain.photo

class MemberPhotos(val data: List<MemberPhoto>) {

    fun findOne(email: String): MemberPhoto? = data.find { it.memberEmail == email }
}
