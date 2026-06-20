package com.konkuk.ma.domain.member.domain.photo

class MemberPhoto(
    val id: Long,
    val memberId: Long,
    val filePath: String,
    val originalFileName: String,
    val approvalStatus: ApprovalStatus,
    val thumbnailPath: String? = null
) {
    fun belongsTo(memberId: Long): Boolean = this.memberId == memberId

    fun hasThumbnail(): Boolean = thumbnailPath != null
}
