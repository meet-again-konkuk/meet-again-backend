package com.konkuk.ma.domain.member.domain.port

import com.konkuk.ma.domain.member.domain.photo.MemberPhoto
import com.konkuk.ma.domain.member.domain.photo.MemberPhotos
import com.konkuk.ma.domain.member.domain.photo.NewPhoto

interface MemberPhotoRepository {
    fun save(newPhoto: NewPhoto): Long
    fun findByMemberEmail(email: String): MemberPhoto?
    fun deleteByMemberEmail(email: String)
    fun findByEmails(emails: Set<String>): MemberPhotos
}
