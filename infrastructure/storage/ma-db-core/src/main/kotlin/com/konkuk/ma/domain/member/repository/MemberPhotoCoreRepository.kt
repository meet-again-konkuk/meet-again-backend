package com.konkuk.ma.domain.member.repository

import com.konkuk.ma.domain.member.dao.MemberPhotoCommandDao
import com.konkuk.ma.domain.member.dao.MemberPhotoQueryDao
import com.konkuk.ma.domain.member.domain.photo.MemberPhoto
import com.konkuk.ma.domain.member.domain.photo.MemberPhotos
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

    override fun findOne(email: String): MemberPhoto? {
        return memberPhotoQueryDao.findOne(email)?.toDomain()
    }

    override fun delete(email: String) {
        memberPhotoCommandDao.delete(email)
    }

    override fun find(emails: Set<String>): MemberPhotos {
        return MemberPhotos(memberPhotoQueryDao.find(emails).map { it.toDomain() })
    }
}
