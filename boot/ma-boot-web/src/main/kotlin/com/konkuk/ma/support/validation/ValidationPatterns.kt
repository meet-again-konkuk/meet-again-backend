package com.konkuk.ma.support.validation

object ValidationPatterns {
    const val NICKNAME = "^[a-zA-Z가-힣0-9]{2,8}$"
    const val NAME = "^[가-힣]{2,10}$"
    const val PASSWORD = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,16}$"
    const val PHONE_NUMBER = "^010\\d{7,8}$"
    const val PHONE_NUMBER_11 = "^010\\d{8}$"
    const val FOUR_DIGIT = "^\\d{4}$"
}

object ValidationMessages {
    const val NICKNAME_INVALID = "유효하지 않은 닉네임 형식입니다."
    const val NAME_INVALID = "이름은 한글 2자 이상 10자 이하여야 합니다."
    const val PASSWORD_INVALID = "비밀번호는 영문/숫자 조합 8자리 이상 16자리 이하여야 합니다."
    const val PHONE_NUMBER_INVALID = "유효하지 않은 휴대폰 번호 형식입니다."
    const val FOUR_DIGIT_MIDDLE_INVALID = "전화번호 중간자리는 4자리 숫자여야 합니다."
    const val FOUR_DIGIT_LAST_INVALID = "전화번호 뒷자리는 4자리 숫자여야 합니다."
    const val EMAIL_REQUIRED = "이메일은 필수입니다."
    const val EMAIL_INVALID = "유효하지 않은 이메일 형식입니다."
    const val PASSWORD_REQUIRED = "비밀번호는 필수입니다."
    const val PHONE_NUMBER_REQUIRED = "휴대폰 번호는 필수입니다."
    const val BIRTH_DATE_REQUIRED = "생년월일은 필수입니다."
    const val REGION_REQUIRED = "지역은 필수입니다."
    const val NAME_REQUIRED = "이름은 필수입니다."
    const val REFRESH_TOKEN_REQUIRED = "리프레시 토큰은 필수입니다."
    const val POST_CATEGORY_REQUIRED = "카테고리는 필수입니다."
    const val POST_TITLE_REQUIRED = "제목은 필수입니다."
    const val POST_TITLE_SIZE = "제목은 40자 이하여야 합니다."
    const val POST_CONTENT_REQUIRED = "내용은 필수입니다."
}
