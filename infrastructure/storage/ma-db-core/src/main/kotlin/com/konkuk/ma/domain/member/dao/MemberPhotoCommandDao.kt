package com.konkuk.ma.domain.member.dao

import com.konkuk.ma.domain.member.domain.photo.NewPhoto
import com.konkuk.ma.domain.member.entity.table.MemberPhotoTable
import org.jetbrains.exposed.sql.insertAndGetId
import org.springframework.stereotype.Component

@Component
class MemberPhotoCommandDao {

    fun save(newPhoto: NewPhoto): Long {
        return MemberPhotoTable.insertAndGetId {
            it[memberEmail] = newPhoto.memberEmail.value
            it[filePath] = newPhoto.filePath
            it[originalFileName] = newPhoto.originalFileName
            it[thumbnailPath] = newPhoto.thumbnailPath
            it[createdBy] = newPhoto.memberEmail.value
            it[lastModifiedBy] = newPhoto.memberEmail.value
        }.value
    }

    fun delete(email: String) {
        MemberPhotoTable.softDelete({ MemberPhotoTable.memberEmail eq email }, email)
    }
}
