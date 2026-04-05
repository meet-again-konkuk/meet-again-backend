package com.konkuk.ma.domain.member.dao

import com.konkuk.ma.domain.member.domain.photo.NewPhoto
import com.konkuk.ma.domain.member.entity.table.MemberPhotoTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.springframework.stereotype.Component

@Component
class MemberPhotoCommandDao {

    fun save(newPhoto: NewPhoto): Long {
        return MemberPhotoTable.insertAndGetId {
            it[memberEmail] = newPhoto.memberEmail
            it[filePath] = newPhoto.filePath
            it[originalFileName] = newPhoto.originalFileName
            it[thumbnailPath] = newPhoto.thumbnailPath
            it[createdBy] = newPhoto.memberEmail
            it[lastModifiedBy] = newPhoto.memberEmail
        }.value
    }

    fun deleteByMemberEmail(email: String) {
        MemberPhotoTable.deleteWhere {
            MemberPhotoTable.memberEmail eq email
        }
    }
}
