package com.konkuk.ma.domain.member.entity

import com.konkuk.ma.domain.member.domain.photo.ApprovalStatus
import com.konkuk.ma.domain.member.domain.photo.MemberPhoto
import com.konkuk.ma.domain.member.entity.table.MemberPhotoTable
import org.jetbrains.exposed.sql.ResultRow

class MemberPhotoEntity(
    val id: Long,
    val memberId: Long,
    val filePath: String,
    val originalFileName: String,
    val approvalStatus: String,
    val thumbnailPath: String?
) {
    fun toDomain(): MemberPhoto {
        return MemberPhoto(
            id = id,
            memberId = memberId,
            filePath = filePath,
            originalFileName = originalFileName,
            approvalStatus = ApprovalStatus.valueOf(approvalStatus),
            thumbnailPath = thumbnailPath
        )
    }

    companion object {
        fun from(row: ResultRow): MemberPhotoEntity {
            return MemberPhotoEntity(
                id = row[MemberPhotoTable.id].value,
                memberId = row[MemberPhotoTable.memberId],
                filePath = row[MemberPhotoTable.filePath],
                originalFileName = row[MemberPhotoTable.originalFileName],
                approvalStatus = row[MemberPhotoTable.approvalStatus],
                thumbnailPath = row[MemberPhotoTable.thumbnailPath]
            )
        }
    }
}
