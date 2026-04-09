package com.konkuk.ma.domain.member.domain.port

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.member.domain.photo.MemberPhoto
import com.konkuk.ma.domain.member.domain.photo.NewPhoto

interface MemberPhotoRepository {
    fun save(newPhoto: NewPhoto): Long
    fun findOne(email: Email): MemberPhoto?
    fun delete(email: Email)
    fun find(emails: Set<Email>): List<MemberPhoto>
}
