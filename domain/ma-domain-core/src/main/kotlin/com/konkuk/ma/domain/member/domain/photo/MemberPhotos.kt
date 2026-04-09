package com.konkuk.ma.domain.member.domain.photo

import com.konkuk.ma.domain.common.domain.Email

class MemberPhotos(val data: List<MemberPhoto>) {

    fun findOne(email: Email): MemberPhoto? = data.find { it.memberEmail == email }
}
