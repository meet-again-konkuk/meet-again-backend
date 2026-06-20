package com.konkuk.ma.domain.point.repository

import com.konkuk.ma.domain.point.dao.MemberPointDao
import com.konkuk.ma.domain.point.domain.balance.MemberPoint
import com.konkuk.ma.domain.point.domain.port.MemberPointRepository
import com.konkuk.ma.domain.point.entity.MemberPointEntity
import org.springframework.stereotype.Repository

@Repository
class MemberPointCoreRepository(
    private val memberPointDao: MemberPointDao,
) : MemberPointRepository {
    override fun findOneOrInitial(ownerId: Long): MemberPoint {
        val entity = memberPointDao.findOneForUpdateOrNull(ownerId)
        return entity?.toDomain() ?: MemberPoint.initial(ownerId)
    }

    override fun save(memberPoint: MemberPoint): Long {
        val entity = MemberPointEntity.from(memberPoint)
        val id = entity.id
        if (id != null) {
            memberPointDao.updateBalance(id, entity.balance)
            return id
        }
        return memberPointDao.insert(entity)
    }

    override fun delete(ownerId: Long) {
        memberPointDao.delete(ownerId)
    }
}
