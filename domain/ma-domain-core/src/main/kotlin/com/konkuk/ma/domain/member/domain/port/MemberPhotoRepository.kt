package com.konkuk.ma.domain.member.domain.port

import com.konkuk.ma.domain.member.domain.photo.MemberPhoto
import com.konkuk.ma.domain.member.domain.photo.MemberPhotos
import com.konkuk.ma.domain.member.domain.photo.NewPhoto

interface MemberPhotoRepository {
    fun save(newPhoto: NewPhoto): Long
    fun findOne(email: String): MemberPhoto?
    fun delete(email: String)
    fun find(emails: Set<String>): MemberPhotos
}
