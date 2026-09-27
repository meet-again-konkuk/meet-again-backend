package com.konkuk.ma.domain.member.dao

import com.konkuk.ma.domain.member.domain.photo.NewPhoto
import com.konkuk.ma.domain.member.entity.table.MemberPhotoTable
import org.jetbrains.exposed.sql.insertAndGetId
import org.springframework.stereotype.Component

@Component
class MemberPhotoCommandDao {

    fun save(newPhoto: NewPhoto): Long {
        return MemberPhotoTable.insertAndGetId {
            it[memberId] = newPhoto.memberId
            it[storageKey] = newPhoto.storageKey
            it[originalFileName] = newPhoto.originalFileName
            it[thumbnailKey] = newPhoto.thumbnailKey
            it[createdBy] = newPhoto.memberId.toString()
            it[lastModifiedBy] = newPhoto.memberId.toString()
        }.value
    }

    fun delete(memberId: Long) {
        MemberPhotoTable.softDelete({ MemberPhotoTable.memberId eq memberId }, memberId.toString())
    }
}
