package com.konkuk.ma.domain.member.repository

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

    override fun findOne(memberId: Long): MemberPhoto? {
        return memberPhotoQueryDao.findOne(memberId)?.toDomain()
    }

    override fun delete(memberId: Long) {
        memberPhotoCommandDao.delete(memberId)
    }

    override fun find(memberIds: Set<Long>): List<MemberPhoto> {
        return memberPhotoQueryDao.find(memberIds)
            .map { it.toDomain() }
    }
}
