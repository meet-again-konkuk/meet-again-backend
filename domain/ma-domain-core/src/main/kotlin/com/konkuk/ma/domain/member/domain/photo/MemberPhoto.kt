package com.konkuk.ma.domain.member.domain.photo

class MemberPhoto(
    val id: Long,
    val memberEmail: String,
    val filePath: String,
    val originalFileName: String,
    val approvalStatus: ApprovalStatus
) {
    fun belongsTo(email: String): Boolean = memberEmail == email
}
