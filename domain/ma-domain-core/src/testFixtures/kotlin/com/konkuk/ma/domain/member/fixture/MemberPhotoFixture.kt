package com.konkuk.ma.domain.member.fixture

import com.konkuk.ma.domain.member.domain.photo.ApprovalStatus
import com.konkuk.ma.domain.member.domain.photo.MemberPhoto

object MemberPhotoFixture {
    fun create(
        id: Long = 1L,
        memberId: Long = 1L,
        storageKey: String = "member/profile/1/photo.jpg",
        originalFileName: String = "photo.jpg",
        approvalStatus: ApprovalStatus = ApprovalStatus.PENDING,
        thumbnailKey: String? = null
    ): MemberPhoto {
        return MemberPhoto(
            id = id,
            memberId = memberId,
            storageKey = storageKey,
            originalFileName = originalFileName,
            approvalStatus = approvalStatus,
            thumbnailKey = thumbnailKey
        )
    }
}
