package com.konkuk.ma.domain.member.api.response

class MemberPhotoResponse(
    val message: String
) {
    companion object {
        fun uploaded(): MemberPhotoResponse =
            MemberPhotoResponse(message = "사진이 업로드되었습니다.")

        fun deleted(): MemberPhotoResponse =
            MemberPhotoResponse(message = "사진이 삭제되었습니다.")
    }
}
