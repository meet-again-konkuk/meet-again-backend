package com.konkuk.ma.domain.member.dao

import com.konkuk.ma.domain.member.entity.MemberPhotoEntity
import com.konkuk.ma.domain.member.entity.table.MemberPhotoTable
import org.springframework.stereotype.Component

@Component
class MemberPhotoQueryDao {

    fun findOne(memberId: Long): MemberPhotoEntity? {
        return MemberPhotoTable
            .activeRows { MemberPhotoTable.memberId eq memberId }
            .limit(1)
            .firstOrNull()
            ?.let { MemberPhotoEntity.from(it) }
    }

    fun find(memberIds: Set<Long>): List<MemberPhotoEntity> {
        if (memberIds.isEmpty()) return emptyList()
        return MemberPhotoTable
            .activeRows { MemberPhotoTable.memberId inList memberIds }
            .map { MemberPhotoEntity.from(it) }
    }
}
