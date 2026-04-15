package com.konkuk.ma.domain.member.repository

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.member.dao.MemberPhotoCommandDao
import com.konkuk.ma.domain.member.dao.MemberPhotoQueryDao
import com.konkuk.ma.domain.member.domain.photo.MemberPhoto
import com.konkuk.ma.domain.member.domain.photo.NewPhoto
import com.konkuk.ma.domain.member.domain.port.MemberPhotoRepository
import org.springframework.stereotype.Repository

@Repository
class MemberPhotoCoreRepository(
    private val memberPhotoCommandDao: MemberPhotoCommandDao,
    private val memberPhotoQueryDao: MemberPhotoQueryDao
) : MemberPhotoRepository {

    override fun save(newPhoto: NewPhoto): Long {
        return memberPhotoCommandDao.save(newPhoto)
    }

    override fun findOne(email: Email): MemberPhoto? {
        return memberPhotoQueryDao.findOne(email.value)?.toDomain()
    }

    override fun delete(email: Email) {
        memberPhotoCommandDao.softDelete(email.value)
    }

    override fun find(emails: Set<Email>): List<MemberPhoto> {
        return memberPhotoQueryDao.find(emails.map { it.value }.toSet())
            .map { it.toDomain() }
    }
}
