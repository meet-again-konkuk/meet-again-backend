package com.konkuk.ma.domain.member.dao

import com.konkuk.ma.domain.member.entity.MemberPhotoEntity
import com.konkuk.ma.domain.member.entity.table.MemberPhotoTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Component

@Component
class MemberPhotoQueryDao {

    fun findByMemberEmail(email: String): MemberPhotoEntity? {
        return MemberPhotoTable.selectAll()
            .where { (MemberPhotoTable.memberEmail eq email) and (MemberPhotoTable.deleted eq false) }
            .limit(1)
            .firstOrNull()
            ?.let { MemberPhotoEntity.from(it) }
    }
}
