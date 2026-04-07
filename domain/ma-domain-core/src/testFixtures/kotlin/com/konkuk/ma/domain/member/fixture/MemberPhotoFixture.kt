package com.konkuk.ma.domain.member.fixture

import com.konkuk.ma.domain.member.domain.photo.ApprovalStatus
import com.konkuk.ma.domain.member.domain.photo.MemberPhoto

object MemberPhotoFixture {
    fun create(
        id: Long = 1L,
        memberEmail: String = "test@example.com",
        filePath: String = "member/profile/test@example.com/photo.jpg",
        originalFileName: String = "photo.jpg",
        approvalStatus: ApprovalStatus = ApprovalStatus.PENDING,
        thumbnailPath: String? = null
    ): MemberPhoto {
        return MemberPhoto(
            id = id,
            memberEmail = memberEmail,
            filePath = filePath,
            originalFileName = originalFileName,
            approvalStatus = approvalStatus,
            thumbnailPath = thumbnailPath
        )
    }
}
