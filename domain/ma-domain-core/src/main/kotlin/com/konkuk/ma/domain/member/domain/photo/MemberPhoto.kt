package com.konkuk.ma.domain.member.domain.photo

import com.konkuk.ma.domain.common.domain.Email

class MemberPhoto(
    val id: Long,
    val memberEmail: Email,
    val filePath: String,
    val originalFileName: String,
    val approvalStatus: ApprovalStatus,
    val thumbnailPath: String? = null
) {
    fun belongsTo(email: Email): Boolean = memberEmail == email

    fun hasThumbnail(): Boolean = thumbnailPath != null
}
