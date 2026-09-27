package com.konkuk.ma.domain.member.entity.table

import com.konkuk.ma.domain.common.entity.table.BaseTable
import com.konkuk.ma.domain.member.domain.photo.ApprovalStatus

object MemberPhotoTable : BaseTable("MEMBER_PHOTOS", "MEMBER_PHOTO_ID") {
    val memberId = long("MEMBER_ID").index()
    val storageKey = varchar("STORAGE_KEY", 512)
    val originalFileName = varchar("ORIGINAL_FILE_NAME", 255)
    val approvalStatus = varchar("APPROVAL_STATUS", 32).clientDefault { ApprovalStatus.PENDING.name }
    val thumbnailKey = varchar("THUMBNAIL_KEY", 512).nullable()
}
