package com.konkuk.ma.domain.member.dao

import com.konkuk.ma.domain.member.domain.photo.NewPhoto
import com.konkuk.ma.domain.member.entity.table.MemberPhotoTable
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.update
import org.springframework.stereotype.Component
import java.time.LocalDateTime

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

    fun softDelete(email: String) {
        MemberPhotoTable.update({ MemberPhotoTable.memberEmail eq email }) {
            it[deleted] = true
            it[deletedDate] = LocalDateTime.now()
            it[deletedBy] = email
        }
    }
}
