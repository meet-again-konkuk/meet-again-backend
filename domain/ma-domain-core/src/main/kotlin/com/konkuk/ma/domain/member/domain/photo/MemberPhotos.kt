package com.konkuk.ma.domain.member.domain.photo

class MemberPhotos(val data: List<MemberPhoto>) {

    fun findOne(memberId: Long): MemberPhoto? = data.find { it.belongsTo(memberId) }
}
