package com.konkuk.ma.domain.member.domain.photo

class MemberPhoto(
    val id: Long,
    val memberId: Long,
    val storageKey: String,
    val originalFileName: String,
    val approvalStatus: ApprovalStatus,
    val thumbnailKey: String? = null
) {
    fun pickImageKey(): String = thumbnailKey ?: storageKey
}
