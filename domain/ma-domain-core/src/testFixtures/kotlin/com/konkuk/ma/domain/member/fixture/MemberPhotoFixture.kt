package com.konkuk.ma.domain.member.fixture

import com.konkuk.ma.domain.member.domain.photo.ApprovalStatus
import com.konkuk.ma.domain.member.domain.photo.MemberPhoto

object MemberPhotoFixture {
    fun create(
        id: Long = 1L,
        memberId: Long = 1L,
        filePath: String = "member/profile/1/photo.jpg",
        originalFileName: String = "photo.jpg",
        approvalStatus: ApprovalStatus = ApprovalStatus.PENDING,
        thumbnailPath: String? = null
    ): MemberPhoto {
        return MemberPhoto(
            id = id,
            memberId = memberId,
            filePath = filePath,
            originalFileName = originalFileName,
            approvalStatus = approvalStatus,
            thumbnailPath = thumbnailPath
        )
    }
}
