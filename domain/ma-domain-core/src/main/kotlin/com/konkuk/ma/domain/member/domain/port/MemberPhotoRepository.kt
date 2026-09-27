package com.konkuk.ma.domain.member.domain.port

import com.konkuk.ma.domain.member.domain.photo.MemberPhoto
import com.konkuk.ma.domain.member.domain.photo.NewPhoto

interface MemberPhotoRepository {
    fun save(newPhoto: NewPhoto): Long
    fun findOne(memberId: Long): MemberPhoto?
    fun delete(memberId: Long)
    fun find(memberIds: Set<Long>): List<MemberPhoto>
}
