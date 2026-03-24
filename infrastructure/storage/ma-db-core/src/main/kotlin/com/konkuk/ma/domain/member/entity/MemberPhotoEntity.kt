package com.konkuk.ma.domain.member.entity

import com.konkuk.ma.domain.member.domain.photo.ApprovalStatus
import com.konkuk.ma.domain.member.domain.photo.MemberPhoto
import com.konkuk.ma.domain.member.entity.table.MemberPhotoTable
import org.jetbrains.exposed.sql.ResultRow

class MemberPhotoEntity(
    val id: Long,
    val memberEmail: String,
    val filePath: String,
    val originalFileName: String,
    val approvalStatus: String
) {
    fun toDomain(): MemberPhoto {
        return MemberPhoto(
            id = id,
            memberEmail = memberEmail,
            filePath = filePath,
            originalFileName = originalFileName,
            approvalStatus = ApprovalStatus.valueOf(approvalStatus)
        )
    }

    companion object {
        fun from(row: ResultRow): MemberPhotoEntity {
            return MemberPhotoEntity(
                id = row[MemberPhotoTable.id].value,
                memberEmail = row[MemberPhotoTable.memberEmail],
                filePath = row[MemberPhotoTable.filePath],
                originalFileName = row[MemberPhotoTable.originalFileName],
                approvalStatus = row[MemberPhotoTable.approvalStatus]
            )
        }
    }
}
