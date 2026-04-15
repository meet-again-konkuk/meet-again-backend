package com.konkuk.ma.domain.member.dao

import com.konkuk.ma.domain.member.entity.MemberPhotoEntity
import com.konkuk.ma.domain.member.entity.table.MemberPhotoTable
import org.springframework.stereotype.Component

@Component
class MemberPhotoQueryDao {

    fun findOne(email: String): MemberPhotoEntity? {
        return MemberPhotoTable
            .activeRows { MemberPhotoTable.memberEmail eq email }
            .limit(1)
            .firstOrNull()
            ?.let { MemberPhotoEntity.from(it) }
    }

    fun find(emails: Set<String>): List<MemberPhotoEntity> {
        if (emails.isEmpty()) return emptyList()
        return MemberPhotoTable
            .activeRows { MemberPhotoTable.memberEmail inList emails }
            .map { MemberPhotoEntity.from(it) }
    }
}
