package com.konkuk.ma.domain.member.exception

import com.konkuk.ma.exception.BusinessException

class DuplicateNicknameException(
    nickname: String
) : BusinessException(
    message = MESSAGE,
    dataMessage = "nickname: $nickname",
    logLevel = LogLevel.WARN
) {
    companion object {
        const val MESSAGE = "이미 사용중인 닉네임입니다."
    }
}
