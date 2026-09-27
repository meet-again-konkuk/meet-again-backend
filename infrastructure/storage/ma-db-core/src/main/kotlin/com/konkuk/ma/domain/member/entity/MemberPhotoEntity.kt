package com.konkuk.ma.domain.member.entity

import com.konkuk.ma.domain.member.domain.photo.ApprovalStatus
import com.konkuk.ma.domain.member.domain.photo.MemberPhoto
import com.konkuk.ma.domain.member.entity.table.MemberPhotoTable
import org.jetbrains.exposed.sql.ResultRow

class MemberPhotoEntity(
    val id: Long,
    val memberId: Long,
    val storageKey: String,
    val originalFileName: String,
    val approvalStatus: String,
    val thumbnailKey: String?
) {
    fun toDomain(): MemberPhoto {
        return MemberPhoto(
            id = id,
            memberId = memberId,
            storageKey = storageKey,
            originalFileName = originalFileName,
            approvalStatus = ApprovalStatus.valueOf(approvalStatus),
            thumbnailKey = thumbnailKey
        )
    }

    companion object {
        fun from(row: ResultRow): MemberPhotoEntity {
            return MemberPhotoEntity(
                id = row[MemberPhotoTable.id].value,
                memberId = row[MemberPhotoTable.memberId],
                storageKey = row[MemberPhotoTable.storageKey],
                originalFileName = row[MemberPhotoTable.originalFileName],
                approvalStatus = row[MemberPhotoTable.approvalStatus],
                thumbnailKey = row[MemberPhotoTable.thumbnailKey]
            )
        }
    }
}
